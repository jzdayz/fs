package io.github.jzdayz.reactor.netty;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebServer {
    public static void main(String[] args) {
        DisposableServer server =
                HttpServer.create()
                        .route(routes ->
                                routes
                                        .file("/index.html", Paths.get("/Users/huqingfeng/Documents/projects/web-videos/src/main/resources/template/index.html"))

                                        .get("/hello",
                                        (request, response) -> response.sendString(Mono.just("Hello World!")))
                                        .post("/echo",
                                                (request, response) -> response.send(request.receive().retain()))
                                        .get("/path/{param}",
                                                (request, response) -> response
                                                        .sendString(Mono.just(request.param("param"))))
                                        .ws("/ws",
                                                (wsInbound, wsOutbound) -> wsOutbound.sendString(Mono.just("1111"))))
                        .port(10101)
                        .bindNow();
        server.onDispose()
                .block();
    }
}
