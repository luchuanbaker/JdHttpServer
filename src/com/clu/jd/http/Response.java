package com.clu.jd.http;

import java.io.IOException;
import java.io.OutputStream;

/*
  HTTP Response = Status-Line
    *(( general-header | response-header | entity-header ) CRLF)
    CRLF
    [ message-body ]
    Status-Line = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
*/

public class Response {

    /*private static final int BUFFER_SIZE = 1024;*/
	
    private OutputStream output;

    public Response(OutputStream output) {
        this.output = output;
    }

    public void sendStaticResponse(String path) {
        /*byte[] bytes = new byte[BUFFER_SIZE];
        FileInputStream fis = null;
        try {
            //将web文件写入到OutputStream字节流中
            File file;
            if (path != null && (file = new File(HttpServer.WEB_ROOT, path)).exists() && file.isFile()) {
                fis = new FileInputStream(file);
                int ch = fis.read(bytes, 0, BUFFER_SIZE);
                output.write(TEXT_HEADER.getBytes());
                while (ch != -1) {
                    output.write(bytes, 0, ch);
                    ch = fis.read(bytes, 0, BUFFER_SIZE);
                }
            } else {
                // file not found
                String errorMessage = HTML_HEADER_404 + "<h1>File Not Found</h1>";
                output.write(errorMessage.getBytes());
            }
        } catch (Exception e) {
        	Logger.error(e.getMessage(), e);
        } finally {
            if (fis != null) {
            	try {
					fis.close();
				} catch (IOException e) {
					Logger.error(e.getMessage(), e);
				}
            }
        }*/
    }
    
    public void sendText(String content) {
    	try {
    		this.output.write(TEXT_HEADER.getBytes(JdHttpServer.ENCODING));
    		this.output.write(content.getBytes(JdHttpServer.ENCODING));
		} catch (IOException e) {
			Logger.error(e.getMessage(), e);
		}
    }
    
    private static final String TEXT_HEADER = "HTTP/1.1 200 OK\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n";
    
    // private static final String HTML_HEADER_404 = "HTTP/1.1 404 File Not Found\r\n Content-Type: text/html\r\nContent-Length: 23\r\n\r\n";
    
}
