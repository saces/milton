package com.ettrema.json;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class AjaxLoginResourceFactory implements ResourceFactory{

    private static final Logger log = LoggerFactory.getLogger( AjaxLoginResourceFactory.class );

    private final String suffix;
    private final ResourceFactory wrapped;

    public AjaxLoginResourceFactory( String suffix, ResourceFactory wrapped ) {
        this.suffix = suffix;
        this.wrapped = wrapped;
    }

    public Resource getResource( String host, String path ) {
        if(path.endsWith( suffix )) {
            int i = path.lastIndexOf( suffix);
            String p2 = path.substring( 0, i);
            Resource r = wrapped.getResource( host, p2);
            if( r != null) {
                if( r instanceof GetableResource) {
                    GetableResource gr = (GetableResource) r;
                    Path pathFull = Path.path( path );
                    log.debug( "found an ajax resource, wrapping a: " + gr.getClass());
                    return new AjaxLoginResource( pathFull.getName(), gr );
                } else {
                    return r;
                }
            }
        }
        return wrapped.getResource( host, path );
    }
}
