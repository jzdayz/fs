package io.github.jzdayz.undertow;

import io.github.jzdayz.template.freemarker.Freemarker;
import io.undertow.Undertow;
import io.undertow.server.handlers.IPAddressAccessControlHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.util.Headers;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class Server {

    public static void main( String[] args) throws UnknownHostException {
        args = new String[]{"20010","D:\\BaiduNetdiskDownload\\xinqiudazhan"};
        Undertow server = Undertow.builder()
                .addHttpListener(Integer.parseInt(args[0]), "0.0.0.0")
                .setHandler(new IPAddressAccessControlHandler(new ResourceHandler(new FileResourceManager(new File(args[1])), exchange -> {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html;charset=UTF-8");
                    Map<Object,Object> map = new HashMap<>();
                    map.put("name",exchange.getRequestURI().substring(0,exchange.getRequestURI().length()-1));
                    exchange.getResponseSender().send(new String(Freemarker.process(map,"file.html")));
                }).setDirectoryListingEnabled(true)).setDefaultAllow(true))
                .build();
        server.start();

        System.out.println(String.format("http://%s:%s", InetAddress.getLocalHost().getHostAddress(),args[0]));
    }
}