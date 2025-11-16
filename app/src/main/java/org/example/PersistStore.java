package org.example;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import javax.swing.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PersistStore {
    private final MontoyaApi api;
    private final List<PersistEntry> items = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final Map<String, Deque<Integer>> pendingByKey = new HashMap<>();

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private boolean paused = false;
    private int seqCounter = 1;

    public PersistStore(MontoyaApi api) { this.api = api; }

    // Control de captura
    public synchronized void setPaused(boolean p) { this.paused = p; }
    public synchronized boolean isPaused() { return paused; }

    // Listeners UI
    public void addListener(Runnable r) { if (r != null) listeners.add(r); }
    private void notifyChange() {
        SwingUtilities.invokeLater(() -> {
            for (Runnable r : listeners) {
                try { r.run(); } catch (Exception ignored) {}
            }
        });
    }

    private String keyFor(HttpRequest req) {
        var svc = req.httpService();
        return svc.host() + ":" + svc.port() + ":" + (svc.secure() ? "https" : "http") + "|" + req.method() + "|" + req.url();
    }

    public synchronized void onRequest(ToolType tool, HttpRequest req) {
        if (paused) return;

        PersistEntry e = new PersistEntry();
        e.seq     = seqCounter++;
        e.time    = TS.format(ZonedDateTime.now());
        e.tool    = tool;
        e.request = req;
        e.method  = req.method();
        e.host    = req.httpService().host();
        e.url     = req.url();

        int idx = items.size();
        items.add(e);

        String k = keyFor(req);
        pendingByKey.computeIfAbsent(k, kk -> new ArrayDeque<>()).addLast(idx);

        notifyChange();
    }

    public synchronized void onResponse(ToolType tool, HttpRequest req, HttpResponse resp) {
        if (paused) return;

        String k = keyFor(req);
        Deque<Integer> q = pendingByKey.get(k);

        Integer idx = null;
        if (q != null) {
            while (!q.isEmpty()) {
                int cand = q.pollFirst();
                if (cand >= 0 && cand < items.size()) {
                    PersistEntry pe = items.get(cand);
                    if (pe.response == null) { idx = cand; break; }
                }
            }
            if (q.isEmpty()) pendingByKey.remove(k);
        }

        if (idx == null) {
            PersistEntry e = new PersistEntry();
            e.seq     = seqCounter++;
            e.time    = TS.format(ZonedDateTime.now());
            e.tool    = tool;
            e.request = req;
            e.method  = req.method();
            e.host    = req.httpService().host();
            e.url     = req.url();
            items.add(e);
            idx = items.size() - 1;
        }

        PersistEntry e = items.get(idx);
        e.response = resp;
        e.status   = (int) resp.statusCode();
        e.mime     = String.valueOf(resp.inferredMimeType());
        e.size     = resp.toByteArray().getBytes().length;

        notifyChange();
    }

    public synchronized int size() { return items.size(); }
    public synchronized PersistEntry get(int idx) { return items.get(idx); }

    public synchronized void clear() {
        items.clear();
        pendingByKey.clear();
        seqCounter = 1;
        notifyChange();
    }

    public synchronized List<PersistEntry> all() { return new ArrayList<>(items); }

    public synchronized void replaceAll(List<PersistEntry> list) {
        items.clear();
        pendingByKey.clear();
        seqCounter = 1;
        for (PersistEntry e : list) {
            e.seq = seqCounter++;
            items.add(e);
            if (e.request != null && e.response == null) {
                pendingByKey.computeIfAbsent(keyFor(e.request), kk -> new ArrayDeque<>()).addLast(items.size() - 1);
            }
        }
        notifyChange();
    }
}
