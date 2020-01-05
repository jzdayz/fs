/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.github.jzdayz.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.ServerSocket;


@Slf4j
public final class HttpStaticFileServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static int PORT = Integer.parseInt(System.getProperty("port", SSL? "8443" : "8080"));

    private static int randomPort(){
        int port = 9999;
        int maxTry = 20000;
        int tried = 0;
        boolean gotIt = false;
        do {
            try (ServerSocket socket = new ServerSocket(++port)) {
                gotIt = true;
            } catch (Exception e) {/*ignore*/}
        }while (!gotIt&&++tried<(maxTry)&&port<60000);
        return port;
    }

    public static void main(String[] args) throws Exception {

        if (args.length>0) {
            PORT = Integer.parseInt(args[0]);
        }else {
            PORT = randomPort();
        }
        if (args.length>1) {
            System.setProperty("web.basePath", args[1]);
        }else{
            System.setProperty("web.basePath",System.getProperty("user.dir"));
        }
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                .sslProvider(SslProvider.JDK).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new HttpStaticFileServerInitializer(sslCtx));

            Channel ch = b.bind(PORT).sync().channel();
            log.info("局域网: http://{}:{}",InetAddress.getLocalHost().getHostAddress(),PORT);
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

}
