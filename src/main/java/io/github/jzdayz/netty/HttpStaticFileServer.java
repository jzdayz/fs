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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.UnknownHostException;


@Slf4j
public final class HttpStaticFileServer {

    static final boolean SSL = System.getProperty("ssl") != null;

    private static int randomPort(){
        int port = 9999;
        int maxTry = 20000;
        boolean gotIt;
        do {
            gotIt = checkPort(port);
        }while ( !gotIt && ++port < maxTry );
        return port;
    }

    private static boolean checkPort(int port) {
        try (ServerSocket socket = new ServerSocket(++port)) {
            return true;
        } catch (Exception e) {/*ignore*/}
        return false;
    }

    public static void main(String[] args) throws Exception {
        AppInfo appInfo = AppInfo.builder().build();
        propertySet(appInfo);
        argumentSet(args, appInfo);
        setDefault(appInfo);
        check(appInfo);

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
             .childHandler(new HttpStaticFileServerInitializer(sslCtx, appInfo.getPath()));
            Channel ch = b.bind(appInfo.getPort()).sync().channel();
            log.info("局域网: http://{}:{}", lanAddress(),appInfo.getPort());
            log.info("公网: http://{}:{}", nanAddress(),appInfo.getPort());
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static String nanAddress() throws Exception{
        URL url = new URL("http://checkip.amazonaws.com");
        String ip = null;
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))
                ){
            ip = in.readLine(); //you get the IP as a String
        }catch (Exception e){/*ignore*/}
        return ip == null ? lanAddress() : ip ;
    }

    private static String lanAddress() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostAddress();
    }

    private static void check(AppInfo appInfo) {
        if (!checkPort(appInfo.getPort())){
            int port = randomPort();
            log.warn(" old port : {} , new port : {} ",appInfo.getPort(),port);
            appInfo.setPort(port);
        }
    }

    private static void setDefault(AppInfo appInfo) {
        if (appInfo.getPath() == null){
            appInfo.setPath(System.getProperty("user.dir"));
        }
        if (appInfo.getPort() == 0){
            appInfo.setPort(randomPort());
        }
    }

    private static void propertySet(AppInfo appInfo) {
        if (appInfo.getPath() == null){
            appInfo.setPath(System.getProperty("path"));
        }
        if (appInfo.getPort() == 0){
            appInfo.setPort(Integer.parseInt(System.getProperty("port","0")));
        }
    }

    private static void argumentSet(String[] args, AppInfo appInfo) {
        if (args.length == 1){
            appInfo.setPort(Integer.parseInt(args[0]));
        }else if (args.length == 2){
            appInfo.setPort(Integer.parseInt(args[0]));
            appInfo.setPath(args[1]);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppInfo{
        private String path;
        private int port;
    }

}
