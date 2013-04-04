package com.sumologic.log4j.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 2:38 PM
 */
public class AggregatingHttpHandler implements HttpHandler {

    private static String REQUEST_ENCODING = "UTF-8";
    private List<MaterializedHttpRequest> exchanges = new ArrayList<MaterializedHttpRequest>();

    // Extract and materialize HTTP Request Body into a String
    private String readRequestBody(HttpExchange httpExchange) throws IOException {
        StringBuilder content = new StringBuilder();
        InputStreamReader is = new InputStreamReader(httpExchange.getRequestBody(), REQUEST_ENCODING);
        int c;
        while ((c = is.read()) != -1) {
            content.append((char) c);
        }

        return content.toString();
    }

    // Extract and materialize HTTP Request from HTTP Exchange
    private MaterializedHttpRequest requestFor(HttpExchange exchange) throws IOException {
        MaterializedHttpRequest request = new MaterializedHttpRequest();
        request.setMethod(exchange.getRequestMethod());
        request.setHeaders(exchange.getRequestHeaders());
        request.setBody(readRequestBody(exchange));

        return request;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        exchanges.add(requestFor(httpExchange));

        // Thanks; come again!
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, 0);
        httpExchange.close();
    }

    public List<MaterializedHttpRequest> getExchanges() {
        return Collections.unmodifiableList(exchanges);
    }

    public void clearExchanges() {
        exchanges.clear();
    }

}
