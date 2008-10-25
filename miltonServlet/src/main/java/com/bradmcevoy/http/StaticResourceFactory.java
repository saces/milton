package com.bradmcevoy.http;

import java.io.File;

public class StaticResourceFactory implements ResourceFactory, Initable{
    
    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(StaticResourceFactory.class);
    
    private ApplicationConfig config;
    
    public StaticResourceFactory() {
    }

    @Override
    public Resource getResource(String host, String url) {
        if( config == null ) throw new RuntimeException("ResourceFactory was not configured. ApplicationConfig is null");
        if( config.servletContext == null ) throw new NullPointerException("config.servletContext is null");
        String path = "WEB-INF/static" + url;
        path = config.servletContext.getRealPath(path);
        File file = new File(path);
        if( file.exists() && !file.isDirectory() ) {
            return new StaticResource(file,url);
        } else {
            return null;
        }
    }

    @Override
    public void init(ApplicationConfig config, HttpManager manager) {
        this.config = config;
    }

    @Override
    public void destroy(HttpManager manager) {
    }

    @Override
    public String getSupportedLevels() {
        return "1,2";
    }
    
    
}
