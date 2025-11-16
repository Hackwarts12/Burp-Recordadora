package org.example;

import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

import java.nio.charset.StandardCharsets;

public class PersistEntry {
    Integer seq;
    String time;
    ToolType tool;
    String method;
    String host;
    String url;
    Integer status;
    String mime;
    Integer size;
    Integer timeMs;
    HttpRequest request;
    HttpResponse response;

    public Integer seq()     { return seq; }
    public String  time()    { return time; }
    public String  toolName(){ return tool == null ? "" : tool.name(); }
    public String  method()  { return method == null ? "" : method; }
    public String  host()    { return host; }
    public String  url()     { return url; }
    public Integer status()  { return status; }
    public String  mime()    { return mime; }
    public Integer size()    { return size; }
    public Integer timeMs()  { return timeMs; }
    public HttpRequest  request()  { return request; }
    public HttpResponse response() { return response; }

    public String rawConcat() {
        StringBuilder sb = new StringBuilder();
        if (request != null)
            sb.append(new String(request.toByteArray().getBytes(), StandardCharsets.ISO_8859_1));
        sb.append("\n===RESPONSE===\n");
        if (response != null)
            sb.append(new String(response.toByteArray().getBytes(), StandardCharsets.ISO_8859_1));
        return sb.toString();
    }
}
