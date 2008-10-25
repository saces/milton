package com.bradmcevoy.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

public class ServletResponse extends AbstractResponse {
    
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ServletResponse.class);

    private static ThreadLocal<HttpServletResponse> tlResponse = new ThreadLocal<HttpServletResponse>();
    
    public static HttpServletResponse getResponse() {
        return tlResponse.get();
    }
    
    private final HttpServletResponse r;
    private ByteArrayOutputStream out = new ByteArrayOutputStream();
    private Long contentLength;
    private Response.Status status;
    private Map<String,String> headers = new HashMap<String, String>();
    
    public ServletResponse(HttpServletResponse r) {
        this.r = r;
        tlResponse.set(r);
    }

    @Override
    public String getNonStandardHeader(String code) {
        return headers.get(code);
    }

    
    @Override
    public void setNonStandardHeader(String name, String value) {
        r.addHeader(name,value);
        headers.put(name, value);
    }
    
    @Override
    public void setStatus(Response.Status status) {
        r.setStatus(status.code);
        this.status = status;
    }

    @Override
    public Response.Status getStatus() {
        return status;
    }
    
    
    @Override
    public void setContentLengthHeader(Long totalLength) {
        contentLength = totalLength;
        super.setContentLengthHeader(totalLength);
    }


    @Override
    public OutputStream getOutputStream() {        
        return out;
    }

    @Override
    public void close() {
        try {
            byte[] arr = out.toByteArray();
            long length = (long)arr.length;
            if( contentLength == null ) setContentLengthHeader(length);
            OutputStream o = r.getOutputStream();
            o.write( arr );
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }        
    }
}
