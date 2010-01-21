package com.ettrema.http.acl;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.webdav.ResourceTypeHelper;
import com.ettrema.http.AccessControlledResource;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author alex
 */
public class AccessControlledResourceTypeHelper implements ResourceTypeHelper {

    private ResourceTypeHelper wrapped = null;

    public AccessControlledResourceTypeHelper( ResourceTypeHelper wrapped ) {
        this.wrapped = wrapped;
    }

    public List<QName> getResourceTypes( Resource r ) {
        List<QName> list = wrapped.getResourceTypes( r );
        if( r instanceof AccessControlledResource ) {
            //TODO: Need to find out what the QNames for accessControlledResources are

            // BM: maybe there isnt one? its only if it should be added to the
            // resource-type property in a PROPFIND response

            //QName qn = new QName( WebDavProtocol.NS_DAV, "collection");
            //list.add(qn);
        }
        return list;
    }

    public List<String> getSupportedLevels( Resource r ) {
        List<String> list = wrapped.getSupportedLevels( r );
        if( r instanceof AccessControlledResource ) {
            list.add( "access-control" );
        }
        return list;
    }
}
