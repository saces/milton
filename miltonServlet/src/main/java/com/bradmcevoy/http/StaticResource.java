package com.bradmcevoy.http;

import eu.medsea.util.MimeUtil;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

/**
 * Used to provide access to static files via Milton
 * 
 * @author brad
 */
public class StaticResource implements GetableResource {
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(StaticResource.class);
    private final File file;
    private final String url;
    
    public StaticResource(File file, String url) {
        if( file.isDirectory() ) throw new IllegalArgumentException("Static resource must be a file, this is a directory: " + file.getAbsolutePath());
        this.file = file;
        this.url = url;
    }

    @Override
    public String getUniqueId() {
        return file.hashCode() + "";
    }
    
    

    @Override 
    public int compareTo(Object o) {
        if( o instanceof Resource ) {
            Resource res = (Resource)o;
            return this.getName().compareTo(res.getName());
        } else {
            return -1;
        }
    }    
    
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bin = new BufferedInputStream(fis);
        final byte[] buffer = new byte[ 1024 ];
        int n = 0;
        while( -1 != (n = bin.read( buffer )) ) {
            out.write( buffer, 0, n );
        }        
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public Object authenticate(String user, String password) {
        return "ok";
    }

    @Override
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        return true;
    }

    @Override
    public String getRealm() {
        return "ettrema";   //TODO
    }

    @Override
    public Date getModifiedDate() {        
        Date dt = new Date(file.lastModified());
//        log.debug("static resource modified: " + dt);
        return dt;
    }

    @Override
    public Long getContentLength() {
        return file.length();
    }

    @Override
    public String getContentType(String accepts) {
        String s = MimeUtil.getMimeType(file.getAbsolutePath());
        s = MimeUtil.getPreferedMimeType(accepts,s);
        return s;
    }


    @Override
    public String checkRedirect(Request request) {
        return null;
    }

    @Override
    public Long getMaxAgeSeconds() {
        return (long)60*60*24;
    }

}
