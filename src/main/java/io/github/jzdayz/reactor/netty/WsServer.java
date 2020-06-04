package io.github.jzdayz.reactor.netty;

import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

public class WsServer {
    public static void main(String[] args) {
        HttpClient.create()
                .websocket()
                .uri("wss://echo.websocket.org")
                .handle((inbound, outbound) -> {
                    inbound.receive()
                            .asString()
                            .take(1)
                            .subscribe(System.out::println);

                    final byte[] msgBytes = "hello".getBytes(CharsetUtil.ISO_8859_1);
                    return outbound.send(Flux.just(Unpooled.wrappedBuffer(msgBytes), Unpooled.wrappedBuffer(msgBytes)))
                            .neverComplete();
                })
                .blockLast();
    }
}
