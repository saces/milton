package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.Resource;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class WebDavResourceTypeHelper implements ResourceTypeHelper {

    private static final Logger log = LoggerFactory.getLogger( WebDavResourceTypeHelper.class );

    public List<QName> getResourceTypes( Resource r ) {
        log.debug( "getResourceTypes" );
        if( r instanceof CollectionResource ) {
            QName qn = new QName( WebDavProtocol.NS_DAV, "collection" );
            return Arrays.asList( qn );
        } else {
            return null;
        }
    }

    public List<String> getSupportedLevels( Resource r ) {
        log.trace( "getSupportedLevels" );
        if( r instanceof LockableResource ) {
            return Arrays.asList( "1", "2" );
        } else {
            return Arrays.asList( "1" );
        }
    }
}
