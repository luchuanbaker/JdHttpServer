package com.clu.jd.http;

import java.io.IOException;
import java.io.InputStream;

public class Request {

    private InputStream input;
    private String uri;
    private String requestString;

    public Request(InputStream input) {
        this.input = input;
        this.parse();
    }

    private void parse() {
        int length = -1;
        byte[] buffer = new byte[2048];
        try {
            length = input.read(buffer);
        } catch (IOException e) {
        	Logger.error(e.getMessage(), e);
        }
        
        if (length > -1) {
        	this.requestString = new String(buffer, 0, length);
        	this.parseUri();
        }
    }

    /**
     * 
     * GET /index.html HTTP/1.1
     * Host: localhost:8080
     * Connection: keep-alive
     * Cache-Control: max-age=0
     * ...
     */
    private void parseUri() {
        int index1, index2;
        index1 = requestString.indexOf(' ');
        if (index1 != -1) {
            index2 = requestString.indexOf(' ', index1 + 1);
            if (index2 > index1) {
            	this.uri = requestString.substring(index1 + 1, index2);
            	return;
            }
        }
        throw new RuntimeException("uri not found: \r\n" + this.requestString);
    }

    public String getUri() {
        return uri;
    }
    
    public String getRequestString() {
		return requestString;
	}

}
