package com.ettrema.json;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.DigestResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class JsonResourceFactory implements ResourceFactory {

    private static final Logger log = LoggerFactory.getLogger(JsonResourceFactory.class);

    final ResourceFactory wrapped;
    final JsonPropFindHandler propFindHandler;
    private static final String DAV_FOLDER = "_DAV";

    public JsonResourceFactory(ResourceFactory wrapped, JsonPropFindHandler propFindHandler) {
        this.wrapped = wrapped;
        this.propFindHandler = propFindHandler;
    }

    public JsonResourceFactory(ResourceFactory wrapped) {
        this.wrapped = wrapped;
        this.propFindHandler = new JsonPropFindHandler();
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
                    return wrapResource(wrappedResource, method, sPath);
                }
            }
        } else {
            return wrapped.getResource(host, sPath);
        }
        return null;
    }

    private Resource wrapResource(Resource wrappedResource, String method,String href) {
        if( Request.Method.PROPFIND.code.equals(method)) {
            if( wrappedResource instanceof PropFindableResource) {
                if( wrappedResource instanceof DigestResource ) {
                    return new DigestPropFindJsonResource((PropFindableResource)wrappedResource, propFindHandler, href);
                } else {
                    return new PropFindJsonResource((PropFindableResource)wrappedResource, propFindHandler, href);
                }
            }
        }
        if( Request.Method.PUT.code.equals(method)) {
            if( wrappedResource instanceof PutableResource) {
                if( wrappedResource instanceof DigestResource ) {
                    return new DigestPutJsonResource((PutableResource)wrappedResource, href);
                } else {
                    return new PutJsonResource((PutableResource)wrappedResource, href);
                }
            }
        }
        return null;
    }


}
