package org.example;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogIO {
    private static final Charset ENC = StandardCharsets.ISO_8859_1;

    // Marcadores
    private static final String SEP = "\n---PERSIST---\n";
    private static final String REQ = "\n<<<REQUEST>>>\n";
    private static final String RES = "\n<<<RESPONSE>>>\n";
    private static final String END = "\n<<<END>>>\n";

    // Regex tolerantes
    private static final Pattern SEP_RE = Pattern.compile("(?m)^\\s*---PERSIST---\\s*$");
    private static final Pattern BLOCK_RE = Pattern.compile(
            "(?is)\\A\\s*(?<meta>.*?)\\R*\\s*<<<\\s*REQUEST\\s*>>>\\s*\\R(?<req>.*?)" +
            "\\R*\\s*<<<\\s*RESPONSE\\s*>>>\\s*\\R(?<res>.*?)" +
            "\\R*\\s*<<<\\s*END\\s*>>>\\s*\\z"
    );
    private static final Pattern HOST_HDR_RE =
            Pattern.compile("(?im)^Host:\\s*([^:\\r\\n]+)\\s*(?::\\s*(\\d+))?\\s*$");

    public static class ImportResult {
        public final int totalBlocks;
        public final int imported;
        public final int skipped;
        public final List<String> errors;
        public ImportResult(int totalBlocks, int imported, int skipped, List<String> errors) {
            this.totalBlocks = totalBlocks;
            this.imported = imported;
            this.skipped = skipped;
            this.errors = errors;
        }
    }

    /** Exporta y devuelve cuántas entradas se escribieron. */
    public static int exportTo(File file, PersistStore store) throws IOException {
        int count = 0;
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), ENC))) {
            for (PersistEntry e : store.all()) {
                w.write(SEP);
                w.write("time=");   w.write(nz(e.time()));   w.write('\n');
                w.write("tool=");   w.write(nz(e.toolName()));   w.write('\n');
                w.write("method="); w.write(nz(e.method())); w.write('\n');
                w.write("host=");   w.write(nz(e.host()));   w.write('\n');
                w.write("url=");    w.write(nz(e.url()));    w.write('\n');
                w.write("status="); w.write(e.status()==null? "" : String.valueOf(e.status())); w.write('\n');
                w.write("mime=");   w.write(nz(e.mime()));   w.write('\n');
                w.write("size=");   w.write(e.size()==null? "" : String.valueOf(e.size())); w.write('\n');
                w.write(REQ);
                if (e.request()!=null) {
                    w.write(new String(e.request().toByteArray().getBytes(), ENC));
                }
                w.write(RES);
                if (e.response()!=null) {
                    w.write(new String(e.response().toByteArray().getBytes(), ENC));
                }
                w.write(END);
                count++;
            }
        }
        return count;
    }

    /** Importa con validación tolerante y devuelve un resumen. */
    public static ImportResult importFrom(File file, PersistStore store) throws IOException {
        String all = new String(Files.readAllBytes(file.toPath()), ENC);

        String[] parts = SEP_RE.split(all);
        int totalBlocks = 0, imported = 0, skipped = 0;
        List<PersistEntry> list = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (String rawBlock : parts) {
            String part = rawBlock.trim();
            if (part.isEmpty()) continue;
            totalBlocks++;

            Matcher m = BLOCK_RE.matcher(part);
            if (!m.matches()) {
                skipped++;
                errors.add("Bloque " + totalBlocks + " sin estructura válida.");
                continue;
            }

            String meta = m.group("meta").trim();
            String rawReq = m.group("req");
            String rawRes = m.group("res");

            try {
                PersistEntry e = new PersistEntry();
                for (String line : meta.split("\\R")) {
                    int k = line.indexOf('=');
                    if (k < 0) continue;
                    String key = line.substring(0, k).trim();
                    String val = line.substring(k + 1);
                    switch (key) {
                        case "time"   -> e.time = val;
                        case "tool"   -> e.tool = parseToolType(val);
                        case "method" -> e.method = val;
                        case "host"   -> e.host = val;
                        case "url"    -> e.url = val;
                        case "status" -> { if (!val.isEmpty()) e.status = Integer.parseInt(val.trim()); }
                        case "mime"   -> e.mime = val;
                        case "size"   -> { if (!val.isEmpty()) e.size = Integer.parseInt(val.trim()); }
                    }
                }

                String host = e.host;
                Integer port = null;
                Boolean secure = null;

                if (e.url != null && !e.url.isEmpty()) {
                    try {
                        URI u = new URI(e.url);
                        if (u.getScheme() != null) secure = "https".equalsIgnoreCase(u.getScheme());
                        if (u.getHost() != null) host = u.getHost();
                        if (u.getPort() != -1) port = u.getPort();
                    } catch (Exception ignored) {}
                }

                Matcher mh = HOST_HDR_RE.matcher(rawReq);
                if (mh.find()) {
                    if (host == null || host.isEmpty()) host = mh.group(1).trim();
                    if (port == null && mh.group(2) != null) {
                        try { port = Integer.parseInt(mh.group(2)); } catch (Exception ignored) {}
                    }
                }

                if (secure == null) {
                    secure = (port != null && port == 443) || (e.url != null && e.url.toLowerCase().startsWith("https"));
                }
                if (port == null) port = Boolean.TRUE.equals(secure) ? 443 : 80;

                if (!rawReq.isEmpty()) {
                    if (host != null && !host.isEmpty()) {
                        HttpService svc = HttpService.httpService(host, port, secure);
                        e.request = HttpRequest.httpRequest(svc, rawReq);
                    } else {
                        e.request = HttpRequest.httpRequest(rawReq);
                    }
                }
                if (!rawRes.isEmpty()) e.response = HttpResponse.httpResponse(rawRes);

                if ((e.url == null || e.url.isEmpty()) && (host == null || host.isEmpty())) {
                    skipped++;
                    errors.add("Bloque " + totalBlocks + " ignorado: meta incompleta (sin url ni host).");
                    continue;
                }

                list.add(e);
                imported++;
            } catch (Exception ex) {
                skipped++;
                errors.add("Bloque " + totalBlocks + " inválido: " + ex.getMessage());
            }
        }

        if (imported > 0) store.replaceAll(list);
        return new ImportResult(totalBlocks, imported, skipped, errors);
    }

    private static ToolType parseToolType(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        try { return ToolType.valueOf(v.toUpperCase()); }
        catch (Exception ignored) { return null; }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
