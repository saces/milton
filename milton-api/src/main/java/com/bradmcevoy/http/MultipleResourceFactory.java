package com.bradmcevoy.http;

import java.util.ArrayList;
import java.util.List;

import freenet.log.Logger;

public class MultipleResourceFactory implements ResourceFactory {

    protected final List<ResourceFactory> factories;

    public MultipleResourceFactory() {
        factories = new ArrayList<ResourceFactory>();
    }

    public MultipleResourceFactory( List<ResourceFactory> factories ) {
        this.factories = factories;
    }
        

    public Resource getResource(String host, String url) {
        Logger.debug(this, "getResource: " + url);
        for( ResourceFactory rf : factories ) {
            Resource r = rf.getResource(host,url);
            if( r != null ) {
                return r;
            }
        }
        Logger.debug(this, "no resource factory supplied a resouce");
        return null;
    }  
}
