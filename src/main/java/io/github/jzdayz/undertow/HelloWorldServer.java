package io.github.jzdayz.undertow;

import io.undertow.Undertow;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;

public class HelloWorldServer {

    public static void main(final String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(new WebSocketProtocolHandshakeHandler(
                        (WebSocketConnectionCallback) (exchange, channel) -> {
                            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                                @Override
                                protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
                                    String data = message.getData();
                                    WebSockets.sendText(data, channel, null);
                                }
                            });
                            channel.resumeReceives();
                        }
                )).build();
        server.start();
    }
}