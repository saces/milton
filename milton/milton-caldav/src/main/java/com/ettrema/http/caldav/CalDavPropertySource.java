package com.ettrema.http.caldav;

import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.values.CData;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.http.webdav.PropertyMap.StandardProperty;
import com.bradmcevoy.property.PropertySource;
import com.ettrema.http.ICalResource;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author alex
 */
public class CalDavPropertySource implements PropertySource {

    private static final Logger log = LoggerFactory.getLogger( CalDavPropertySource.class );

    // Standard caldav properties
    public static final String CALDAV_NS = "urn:ietf:params:xml:ns:caldav";
    
    // For extension properties
    public static final String CALSERVER_NS = "http://calendarserver.org/ns/";
    private final PropertyMap propertyMapCalDav;
    private final PropertyMap propertyMapCalServer;
    private final ICalFormatter iCalFormatter = new ICalFormatter();

    public CalDavPropertySource() {
        log.debug( "--- CalDavPropertySource: ns: " + CALDAV_NS );
        propertyMapCalDav = new PropertyMap( CALDAV_NS );
        propertyMapCalDav.add( new CalenderDescriptionProperty() );
        propertyMapCalDav.add( new CalendarDataProperty());

        propertyMapCalServer = new PropertyMap( CALSERVER_NS );
        propertyMapCalServer.add( new CTagProperty());
    }

    public Object getProperty( QName name, Resource r ) {
        log.debug( "getProperty: " + name.getLocalPart() );
        if( propertyMapCalDav.hasProperty( name )) {
            return propertyMapCalDav.getProperty( name, r );
        } else {
            return propertyMapCalServer.getProperty( name, r );
        }
    }

    public void setProperty( QName name, Object value, Resource r ) {
        log.debug( "setProperty: " + name.getLocalPart() );
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public PropertyMetaData getPropertyMetaData( QName name, Resource r ) {
        log.debug( "getPropertyMetaData: " + name.getLocalPart() );
        if( propertyMapCalDav.hasProperty( name )) {
            return propertyMapCalDav.getPropertyMetaData( name, r );
        } else {
            return propertyMapCalServer.getPropertyMetaData( name, r );
        }
    }

    public void clearProperty( QName name, Resource r ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<QName> getAllPropertyNames( Resource r ) {
        log.debug( "getAllPropertyNames" );
        List<QName> list = new ArrayList<QName>();
        list.addAll( propertyMapCalDav.getAllPropertyNames( r ) );
        list.addAll( propertyMapCalServer.getAllPropertyNames( r ) );
        return list;
    }

    /**
     *  https://trac.calendarserver.org/browser/CalendarServer/trunk/doc/Extensions/caldav-ctag.txt
     *
     * 4.1.  getctag WebDAV Property
173
174	   Name:  getctag
175
176	   Namespace:  http://calendarserver.org/ns/
177
178	   Purpose:  Specifies a "synchronization" token used to indicate when
179	      the contents of a calendar or scheduling Inbox or Outbox
180	      collection have changed.
181
182	   Conformance:  This property MUST be defined on a calendar or
183	      scheduling Inbox or Outbox collection resource.  It MUST be
184	      protected and SHOULD be returned by a PROPFIND DAV:allprop request
185	      (as defined in Section 12.14.1 of [RFC2518]).
186
187	   Description:  The CS:getctag property allows clients to quickly
188	      determine if the contents of a calendar or scheduling Inbox or
189	      Outbox collection have changed since the last time a
190	      "synchronization" operation was done.  The CS:getctag property
191	      value MUST change each time the contents of the calendar or
192	      scheduling Inbox or Outbox collection change, and each change MUST
193	      result in a value that is different from any other used with that
194	      collection URI.
195
196	   Definition:
197
198	       <!ELEMENT getctag #PCDATA>
199
200	   Example:
201
202	       <T:getctag xmlns:T="http://calendarserver.org/ns/"
203	       >ABCD-GUID-IN-THIS-COLLECTION-20070228T122324010340</T:getctag>
     */
    class CTagProperty implements StandardProperty<String> {
        public String fieldName() {
            return "getctag";
        }

        public String getValue( PropFindableResource res ) {
            return res.getUniqueId();
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    class CalendarDataProperty implements StandardProperty<CData> {
        public String fieldName() {
            return "calendar-data";
        }

        public CData getValue( PropFindableResource res ) {
            if( res instanceof ICalResource) {
                return new CData(iCalFormatter.format((ICalResource) res)); 
            } else {
                return null;
            }
        }

        public Class<CData> getValueClass() {
            return CData.class;
        }
    }

    class CalenderDescriptionProperty implements StandardProperty<String> {

        public String fieldName() {
            return "calendar-description";
        }

        public String getValue( PropFindableResource res ) {
            return res.getName();
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }
}
