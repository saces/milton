package com.ettrema.http.caldav;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.webdav.ResourceTypeHelper;
import com.ettrema.http.CalendarResource;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author brad
 */
public class CalendarResourceTypeHelper implements ResourceTypeHelper {

    private ResourceTypeHelper wrapped = null;

    public CalendarResourceTypeHelper( ResourceTypeHelper wrapped ) {
        this.wrapped = wrapped;
    }

    public List<QName> getResourceTypes( Resource r ) {
        List<QName> list = wrapped.getResourceTypes( r );
        if( r instanceof CalendarResource ) {
            //TODO: Need to find out what the QNames for calendars are
            //QName qn = new QName( WebDavProtocol.NS_DAV, "collection");
            //list.add(qn);
        }
        return list;
    }

    public List<String> getSupportedLevels( Resource r ) {
        List<String> list = wrapped.getSupportedLevels( r );
        if( r instanceof CalendarResource ) {
            list.add( "calendar-access" );
        }
        return list;
    }
}
