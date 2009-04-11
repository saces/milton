package com.ettrema.json;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class JsonResourceFactory implements ResourceFactory {

    private static final Logger log = LoggerFactory.getLogger(HttpManager.class);

    final ResourceFactory wrapped;
    private static final String DAV_FOLDER = "_DAV";

    public JsonResourceFactory(ResourceFactory wrapped) {
        this.wrapped = wrapped;
    }

    
    public Resource getResource(String host, String sPath) {
        log.debug(host + " :: " + sPath);
        Path path = Path.path(sPath);
        Path parent = path.getParent();
        if( parent != null && parent.getName() != null && parent.getName().equals(DAV_FOLDER)) {
            Path resourcePath = parent.getParent();
            if( resourcePath != null ) {
                String method = path.getName();
                Resource wrappedResource = wrapped.getResource(host, resourcePath.toString());
                if( wrappedResource != null ) {
                    return wrapResource(wrappedResource, method);
                }
            }
        } else {
            return wrapped.getResource(host, sPath);
        }
        return null;
    }

    public String getSupportedLevels() {
        return wrapped.getSupportedLevels();
    }

    private Resource wrapResource(Resource wrappedResource, String method) {
        if( Request.Method.PROPFIND.code.equals(method)) {
            if( wrappedResource instanceof PropFindableResource) {
                return new PropFindJsonResource((PropFindableResource)wrappedResource);
            }
        }
        return null;
    }


}