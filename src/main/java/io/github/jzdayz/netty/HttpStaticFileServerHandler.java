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
import com.google.common.net.MediaType;
import freemarker.template.TemplateException;
import io.github.jzdayz.template.freemarker.Freemarker;
import io.github.jzdayz.util.Constant;
import io.github.jzdayz.util.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.xnio.IoUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 *
 * <h3>How Browser Caching Works</h3>
 * <p>
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of {@code /file1.txt}.</li>
 * <li>Contents of {@code /file1.txt} is cached by the browser.</li>
 * <li>Request #2 for {@code /file1.txt} does not return the contents of the
 *     file again. Rather, a 304 Not Modified is returned. This tells the
 *     browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 *     {@code If-Modified-Since} date is the same as the file's last
 *     modified date.</li>
 * </ol>
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
@Slf4j
public class HttpStaticFileServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;

    private HttpRequest request;

    private static final HttpDataFactory factory = new DefaultHttpDataFactory(false);

    private final String basePath;

    public HttpStaticFileServerHandler(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        this.request = request;


        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (!GET.equals(request.method())) {
            this.sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }


        String uri = request.uri();
        if ("/upload".equals(uri)){


//            HttpPostRequestDecoder httpDecoder = new HttpPostRequestDecoder(factory, request);
//            httpDecoder.


//            return
        }
        uri = URLDecoder.decode(uri, StandardCharsets.UTF_8.name());
        log.info(" uri : {} ", uri);
        String path = sanitizeUri(uri);
        log.info("resource path : {}", path);
        if (uri.contains(".mp4-")) {
            QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
            Map<String, List<String>> parameters = queryStringDecoder.parameters();
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, MediaType.HTML_UTF_8);
            Map<Object, Object> map = new HashMap<>();
            String fileName = uri.substring(0, uri.lastIndexOf(".mp4")) + ".mp4";
            map.put("name", fileName);
            map.put("tittle", fileName);
            String webModel = Constant.Html.SIMPLE;
            if (!parameters.isEmpty()){
                if (parameters.getOrDefault("model",Collections.emptyList()).contains("plyr")){
                    webModel = Constant.Html.PLYR;
                }
            }
            byte[] process = Freemarker.process(map, webModel);
            ByteBuf byteBuf = ctx.alloc().buffer(process.length).writeBytes(process);
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, process.length);
            ctx.write(response);
            ctx.write(byteBuf);
            ctx.flush();
            return;

        }

        Set<String> classpath = new HashSet<>();
        classpath.add("/fileUpload.html");
        if(classpath.contains(uri)){
            byte[] bytes;
            try(InputStream resourceAsStream =
                    this.getClass().getClassLoader().getResourceAsStream("template"+uri)){
                bytes = Utils.toBytes(resourceAsStream);
            }
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            HttpUtil.setContentLength(response, bytes.length);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8");
            ctx.write(response);
            ctx.write(ctx.alloc().buffer(bytes.length).writeBytes(bytes));
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
            return;
        }




        if (path == null) {
            this.sendError(ctx, FORBIDDEN);
            return;
        }

        File file = new File(path);
        if (
//                file.isHidden()
//                ||
                        !file.exists()) {
            this.sendError(ctx, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                this.sendListing(ctx, file, uri);
            } else {
                this.sendRedirect(ctx, uri + '/');
            }
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        setContentTypeHeader(response, file);
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;

        // Tell clients that Partial Requests are available.
        response.headers().add(HttpHeaderNames.ACCEPT_RANGES, HttpHeaderValues.BYTES);

        String rangeHeader = request.headers().get(HttpHeaderNames.RANGE);
        log.info(HttpHeaderNames.RANGE + " = " + rangeHeader);
        if (rangeHeader != null && rangeHeader.length() > 0) { // Partial Request
            PartialRequestInfo partialRequestInfo = getPartialRequestInfo(rangeHeader, fileLength);

            // Set Response Header
            response.headers().add(HttpHeaderNames.CONTENT_RANGE, HttpHeaderValues.BYTES + " "
                    + partialRequestInfo.startOffset + "-" + partialRequestInfo.endOffset + "/" + fileLength);


            HttpUtil.setContentLength(response, partialRequestInfo.getChunkSize());


            response.setStatus(HttpResponseStatus.PARTIAL_CONTENT);

            // Write Response
            ctx.write(response);
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), partialRequestInfo.getStartOffset(),
                    partialRequestInfo.getChunkSize()), ctx.newProgressivePromise());
        } else {
            // Set Response Header
            HttpUtil.setContentLength(response, fileLength);

            // Write Response
            ctx.write(response);
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength),
                    ctx.newProgressivePromise());
        }

        lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    log.info(future.channel() + " Transfer progress: " + progress);
                } else {
                    log.info(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                log.info(future.channel() + " Transfer complete.");
            }
        });
        // Decide whether to close the connection or not.
        if (!HttpUtil.isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
        handleException(cause);
    }

    private static void handleException(Throwable throwable){
        if(Objects.equals(throwable.getMessage(),"Connection reset by peer")){
            log.info("用户断开连接");
            return;
        }
        log.error("handle ex ",throwable);
    }

    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private String sanitizeUri(String uri) {
        // Decode the path.
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

//        if (uri.isEmpty() || uri.charAt(0) != '/') {
//            return null;
//        }

        // Convert file separators.
        uri = uri.replace('/', File.separatorChar);

        // Simplistic dumb security check.
        // You will have to do something serious in the production environment.
        if (uri.contains(File.separator + '.') ||
                uri.contains('.' + File.separator) ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.'
//                ||
//            INSECURE_URI.matcher(uri).matches()
        ) {
            return null;
        }

        // Convert to absolute path.
        if(uri.equals(File.separator)){
            uri="";
        }else{
            uri = uri.substring(1);
        }
        return basePath + File.separator + uri;
    }

    private static final Pattern ALLOWED_FILE_NAME = Pattern.compile("[^-\\._]?[^<>&\\\"]*");

    private static final List<String> CLASS_NAME = Arrays.asList(
            "table-active","table-primary","table-secondary","table-success","table-danger",
            "table-warning","table-info","table-light","table-dark"
    );

    private void sendListing(ChannelHandlerContext ctx, File dir, String dirPath) throws IOException, TemplateException {
        List<FileNode> res = new ArrayList<>();
//        StringBuilder buf = new StringBuilder()
//                .append("<!DOCTYPE html>\r\n")
//                .append("<html><head><meta charset='utf-8' /><title>")
//                .append("Listing of: ")
//                .append(dirPath)
//                .append("</title></head><body>\r\n")
//
//                .append("<h3>Listing of: ")
//                .append(dirPath)
//                .append("</h3>\r\n")
//
//                .append("<ul>")
//                .append("<li><a href=\"../\">..</a></li>\r\n");

        int i = 0;
        for (File f : Objects.requireNonNull(dir.listFiles())) {
            if (f.isHidden() || !f.canRead()) {
                continue;
            }
            if (i > CLASS_NAME.size() - 1 ){
                i = 0 ;
            }

            String name = f.getName();
            FileNode fileNode = FileNode.builder()
                    .name(name)
                    .classStr(CLASS_NAME.get(i++))
                    .path(f.getPath())
                    .directory(f.isDirectory())
                    .size(f.length())
                    .href(name.endsWith(".mp4") ? name + "-" : name)
                    .lastUpdate(LocalDateTime.ofInstant(Instant.ofEpochMilli(f.lastModified()), ZoneId.systemDefault())).build();

            fileNode.setHref(fileNode.isDirectory() ? fileNode.getHref() + "/" : fileNode.getHref());
            res.add(fileNode);
//            if (!ALLOWED_FILE_NAME.matcher(name).matches()) {
//                continue;
//            }

//            buf.append("<li><a href=\"")
//                    .append(name.endsWith(".mp4") ? name + "-" : name)
//                    .append("\">")
//                    .append(name)
//                    .append("</a></li>\r\n");
        }

//        buf.append("</ul></body></html>\r\n");

        Map<String,Object> map = new HashMap<>();
        map.put("fileNodes",res);
        byte[] process = Freemarker.process(map, "index.html");


        ByteBuf buffer = ctx.alloc().buffer(process.length);
        buffer.writeBytes(process);

        log.info("{}",new String(process,StandardCharsets.UTF_8));

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        this.sendAndCleanupConnection(ctx, response);
    }

    private void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND, Unpooled.EMPTY_BUFFER);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        this.sendAndCleanupConnection(ctx, response);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        this.sendAndCleanupConnection(ctx, response);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx Context
     */
    private void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
        setDateHeader(response);

        this.sendAndCleanupConnection(ctx, response);
    }

    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    private void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpResponse response) {
        final HttpRequest request = this.request;
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        HttpUtil.setContentLength(response, response.content().readableBytes());
        if (!keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        ChannelFuture flushPromise = ctx.writeAndFlush(response);

        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response    HTTP response
     * @param fileToCache file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response HTTP response
     * @param file     file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }

    class PartialRequestInfo {
        private long startOffset;
        private long endOffset;
        private long chunkSize;

        public long getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        public long getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(long endOffset) {
            this.endOffset = endOffset;
        }

        public long getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(long chunkSize) {
            this.chunkSize = chunkSize;
        }
    }

    private PartialRequestInfo getPartialRequestInfo(String rangeHeader, long fileLength) {
        PartialRequestInfo partialRequestInfo = new PartialRequestInfo();
        long startOffset = 0;
        long endOffset;
        try {
            startOffset = Long
                    .parseLong(rangeHeader.trim().replace(HttpHeaderValues.BYTES + "=", "").replace("-", ""));
        } catch (NumberFormatException e) {
        }

        endOffset = fileLength - startOffset > MB_20 ? startOffset + MB_20 : fileLength;

        if (endOffset >= fileLength) {
            endOffset = fileLength - 1;
        }
        long chunkSize = endOffset - startOffset + 1;

        partialRequestInfo.setStartOffset(startOffset);
        partialRequestInfo.setEndOffset(endOffset);
        partialRequestInfo.setChunkSize(chunkSize);
        return partialRequestInfo;
    }

    private static final long MB_20 = 20 * 1024 * 1024;


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class FileNode{
        private String name;
        private String path;
        private LocalDateTime lastUpdate;
        private double size;
        private String href;
        private boolean directory;
        private String classStr;
    }
}
