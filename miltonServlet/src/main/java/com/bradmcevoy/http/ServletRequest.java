package com.bradmcevoy.http;

import com.bradmcevoy.http.Response.ContentType;
import com.bradmcevoy.http.upload.MonitoredDiskFileItemFactory;
import com.bradmcevoy.http.upload.UploadListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class ServletRequest extends AbstractRequest { 
    
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ServletRequest.class);
    
    final HttpServletRequest r;
    
    final Request.Method method;

    final String url;
    
    private Auth auth;
    
    private static HashMap<ContentType,String> contentTypes = new HashMap<ContentType,String>();    
    private static HashMap<String,ContentType> typeContents = new HashMap<String,ContentType>();
    
    static {
        contentTypes.put(ContentType.HTTP,Response.HTTP);
        contentTypes.put(ContentType.MULTIPART,Response.MULTIPART);
        contentTypes.put(ContentType.XML,Response.XML);
        for( ContentType key : contentTypes.keySet() ) {
            typeContents.put(contentTypes.get(key),key);
        }
    }
    
    private static ThreadLocal<HttpServletRequest> tlRequest = new ThreadLocal<HttpServletRequest>();
    
    public static HttpServletRequest getRequest() {
        return tlRequest.get();
    }
    
    public ServletRequest(HttpServletRequest r) {
        this.r = r;
        String sMethod = r.getMethod();        
        method = Request.Method.valueOf(sMethod);
        url = r.getRequestURL().toString(); //MiltonUtils.stripContext(r);
        tlRequest.set(r);        
    }

    public HttpSession getSession() {
        return r.getSession();
    }

    @Override
    public String getFromAddress() {
        return r.getRemoteHost();
    }
    
    
    @Override
    protected String getRequestHeader(Request.Header header) {
        return r.getHeader(header.code);
    }

    @Override
    public Request.Method getMethod() {
        return method;
    }

    @Override
    public String getAbsoluteUrl() {
        return url;
    }

    @Override
    public Auth getAuthorization() {
        if( auth != null ) return auth;
        String enc = getRequestHeader( Request.Header.AUTHORIZATION );
        if( enc == null ) return null;
        if( enc.length() == 0 ) return null;
        auth = new Auth(enc);
        return auth;
    }    

    
    
    @Override
    public InputStream getInputStream() throws IOException {
        return r.getInputStream();
    }    
    
    
    
    @Override
    public void parseRequestParameters(Map<String,String> params, Map<String,com.bradmcevoy.http.FileItem> files) throws RequestParseException {
        try {
            if( isMultiPart() ) {
                UploadListener listener = new UploadListener();
                MonitoredDiskFileItemFactory factory = new MonitoredDiskFileItemFactory(listener);
                ServletFileUpload upload = new ServletFileUpload(factory);
                List items = upload.parseRequest(this.r);
                
                parseQueryString(params);
                
                for( Object o : items ) {
                    FileItem item = (FileItem)o;
                    if( item.isFormField() ) {
                        params.put(item.getFieldName(),item.getString());
                    } else {
                        files.put(item.getFieldName(),new FileItemWrapper(item) );
                    }
                }
            } else {
                for( Enumeration en = r.getParameterNames(); en.hasMoreElements();  ) {
                    String nm = (String)en.nextElement();
                    String val = r.getParameter(nm);
                    params.put(nm,val);
                }
            }
        } catch (FileUploadException ex) {
            throw new RequestParseException("FileUploadException",ex);
        } catch (Throwable ex) {
            throw new RequestParseException(ex.getMessage(),ex);
        }
    }
    
    private void parseQueryString(Map<String,String> map) {
        String qs = r.getQueryString();
        parseQueryString(map,qs);
    }
    
    public static void parseQueryString(Map<String,String> map, String qs) {        
        if( qs == null ) return ;
        String[] nvs = qs.split("&");
        for( String nv : nvs ) {            
            String[] parts = nv.split("=");
            String key = parts[0];
            String val = null;
            if( parts.length > 1 ) val = parts[1];
            if( val != null ) {
                try {
                    val = URLDecoder.decode(val,"UTF-8");
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }
            map.put(key,val);
        }
    }
    
    protected Response.ContentType getRequestContentType() {
        String s = r.getContentType();
        if( s == null ) return null;
        if( s.contains(Response.MULTIPART) ) return ContentType.MULTIPART;
        return typeContents.get(s);        
    }

    protected boolean isMultiPart() {
        return ( ContentType.MULTIPART.equals( getRequestContentType() ) );
    }
}
