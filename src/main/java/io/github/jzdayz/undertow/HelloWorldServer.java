package io.github.jzdayz.undertow;

import io.github.jzdayz.template.freemarker.Freemarker;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HelloWorldServer {

    public static void main(final String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(10080, "localhost")
                .setHandler(new ResourceHandler(new FileResourceManager(new File("/Users/jzdayz/Downloads/aaa")),new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html;charset=UTF-8");
                        Map<Object,Object> map = new HashMap<>();
                        map.put("name",exchange.getRequestURI().substring(0,exchange.getRequestURI().length()-1));
                        exchange.getResponseSender().send(new String(Freemarker.process(map,"file.html")));
                    }
                }).setDirectoryListingEnabled(true))
                .build();
        server.start();
    }
}