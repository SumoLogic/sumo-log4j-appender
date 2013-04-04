package com.sumologic.log4j.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;


/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 3:58 AM
 */
public class MockHttpServer {

    private int port;
    private HttpHandler handler;
    private HttpServer server;


    public MockHttpServer(int port, HttpHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    public void start() throws IOException {
        InetSocketAddress addr = new InetSocketAddress(port);
        server = HttpServer.create(addr, 0);
        server.createContext("/", handler);
        server.setExecutor(null); // default executor

        server.start();

    }


    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
/*
public class MockHttpServer {

    private final static int PORT = 1023;

    private static HttpRequest createHttpRequest(final String requestString) throws IOException, HttpException {

        final HttpParams params = new BasicHttpParams();
        SessionInputBuffer inbuf = new AbstractSessionInputBuffer() {
            {
                init(new ByteArrayInputStream(
                        requestString.getBytes()),
                        requestString.length(),
                        params);
            }

            @Override
            public boolean isDataAvailable(int i) throws IOException {
                return true;
            }
        };


        HttpRequestFactory requestFactory = new DefaultHttpRequestFactory();
        HttpRequestParser requestParser = new HttpRequestParser(inbuf, null, requestFactory, params);
        HttpRequest request = (HttpRequest) requestParser.parse();

        return request;
    }


    class ListenerThread implements Runnable {

        private String readFully(InputStream is) throws IOException {
            StringBuilder builder = new StringBuilder();
            int c;
            while ((c = is.read()) != -1) {
                builder.append(c);
            }

            return builder.toString();
        }


        @Override
        public void run() {
            ServerSocket server = null;
            Socket socket = null;

            try {
                server = new ServerSocket(PORT);
                socket = server.accept();
                createHttpRequest(readFully(socket.getInputStream()));

            } catch (IOException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) try { socket.close(); } catch (Exception e) {}
                if (server != null) try { server.close(); } catch (Exception e) {}
            }
        }
    }

    public void start() throws IOException {

    }
}

*/