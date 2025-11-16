package org.example;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.extension.ExtensionUnloadingHandler;
import burp.api.montoya.http.handler.HttpHandler;
import burp.api.montoya.http.handler.HttpRequestToBeSent;
import burp.api.montoya.http.handler.HttpResponseReceived;
import burp.api.montoya.http.handler.RequestToBeSentAction;
import burp.api.montoya.http.handler.ResponseReceivedAction;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.message.responses.HttpResponse;

public class PersistExtension implements BurpExtension, HttpHandler, ExtensionUnloadingHandler {

    private MontoyaApi api;
    private PersistStore store;
    private PersistUI ui;

    @Override
    public void initialize(MontoyaApi api) {
        this.api = api;

        api.extension().setName("Recordadora");

        this.store = new PersistStore(api);
        this.ui    = new PersistUI(api, store);

        api.userInterface().registerSuiteTab("Recordadora", ui.root());
        api.http().registerHttpHandler(this);
        api.extension().registerUnloadingHandler(this);

        api.logging().logToOutput("Tu Recordadora fue cargada con éxito.");
    }

    @Override
    public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent evt) {
        HttpRequest req = evt; // HttpRequestToBeSent implementa HttpRequest
        store.onRequest(evt.toolSource().toolType(), req);
        return RequestToBeSentAction.continueWith(req);
    }

    @Override
    public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived evt) {
        HttpRequest  req  = evt.initiatingRequest();
        HttpResponse resp = evt; // HttpResponseReceived implementa HttpResponse
        store.onResponse(evt.toolSource().toolType(), req, resp);
        return ResponseReceivedAction.continueWith(resp);
    }

    @Override
    public void extensionUnloaded() {
        api.logging().logToOutput("Recordadora se descargó correctamente.");
    }
}
