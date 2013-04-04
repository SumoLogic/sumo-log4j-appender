package com.sumologic.log4j.server;

import com.sun.net.httpserver.Headers;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 3:10 PM
 */
public class MaterializedHttpRequest {
    private String method;
    private String body;
    private Headers headers;

    @Override
    public String toString() {
        return "HTTP Request: {" +
                "\nmethod='" + method + "'" +
                "\nheaders='" + headers + "'" +
                "\nbody='" + body + "' " +
                "\n}";
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Headers getHeaders() {
        return headers;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }
}
