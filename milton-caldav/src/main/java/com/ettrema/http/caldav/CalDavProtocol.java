package com.ettrema.http.caldav;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HandlerHelper;
import com.bradmcevoy.http.HttpExtension;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.values.CData;
import com.bradmcevoy.http.values.HrefList;
import com.bradmcevoy.http.values.ValueWriters;
import com.bradmcevoy.http.webdav.PropFindPropertyBuilder;
import com.bradmcevoy.http.webdav.PropFindXmlGenerator;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.http.webdav.PropertyMap.StandardProperty;
import com.bradmcevoy.http.webdav.WebDavProtocol;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.bradmcevoy.property.PropertySource;
import com.ettrema.http.ICalResource;
import com.ettrema.http.acl.ACLHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class CalDavProtocol implements HttpExtension, PropertySource {

    private static final Logger log = LoggerFactory.getLogger( CalDavProtocol.class );

    // Standard caldav properties
    public static final String CALDAV_NS = "urn:ietf:params:xml:ns:caldav";

    // For extension properties
    public static final String CALSERVER_NS = "http://calendarserver.org/ns/";


    private final Set<Handler> handlers;
    private final PropertyMap propertyMapCalDav;
    private final PropertyMap propertyMapCalServer;


    public CalDavProtocol( ResourceFactory resourceFactory, WebDavResponseHandler responseHandler, HandlerHelper handlerHelper, WebDavProtocol webDavProtocol ) {
        propertyMapCalDav = new PropertyMap( CALDAV_NS );
        propertyMapCalDav.add( new CalenderDescriptionProperty() );
        propertyMapCalDav.add( new CalendarDataProperty());
        propertyMapCalDav.add( new CalenderHomeSetProperty());
        propertyMapCalDav.add( new CalenderUserAddressSetProperty());
        propertyMapCalDav.add( new ScheduleInboxProperty());
        propertyMapCalDav.add( new ScheduleOutboxProperty());

        propertyMapCalServer = new PropertyMap( CALSERVER_NS );
        propertyMapCalServer.add( new CTagProperty());
        propertyMapCalServer.add( new XMPPProperty());
        propertyMapCalServer.add( new DropBoxProperty());

        handlers = new HashSet<Handler>();
        handlers.add( new ACLHandler( responseHandler, handlerHelper ) );
        
        ValueWriters valueWriters = new ValueWriters();
        PropFindXmlGenerator gen = new PropFindXmlGenerator( valueWriters );
        webDavProtocol.addPropertySource( this );
        PropFindPropertyBuilder propertyBuilder = new PropFindPropertyBuilder( webDavProtocol.getPropertySources() );
        log.debug( "registering MultiGetReport");
        webDavProtocol.addReport( new MultiGetReport(resourceFactory, propertyBuilder, gen ));
    }

    public Set<Handler> getHandlers() {
        return Collections.unmodifiableSet( handlers );
    }



    //TODO: remove debug logging once it's working
    public Object getProperty( QName name, Resource r ) {
        log.debug( "getProperty: " + name.getLocalPart() );
        Object o;
        if( propertyMapCalDav.hasProperty( name )) {
            o = propertyMapCalDav.getProperty( name, r );
        } else {
            o = propertyMapCalServer.getProperty( name, r );
        }
        log.debug( "result : "+o );
        return o;
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

    class CalendarDataProperty implements StandardProperty<CData> {
        public String fieldName() {
            return "calendar-data";
        }

        public CData getValue( PropFindableResource res ) {
            log.debug( "getValue: " + res.getClass());
            if( res instanceof ICalResource) {
                ICalResource ical = (ICalResource) res;
                return new CData( ical.getICalData() );
            } else {
                log.warn( "getValue: not a ICalResource");
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

    class CalenderHomeSetProperty implements StandardProperty<String> {

        public String fieldName() {
            return "calendar-home-set";
        }

        public String getValue( PropFindableResource res ) {
            return "http://localhost:7080/caldavdemo/folder1/cal1";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    /* Scheduling support
       see : http://ietfreport.isoc.org/idref/draft-desruisseaux-caldav-sched/
       for details
     */
    class CalenderUserAddressSetProperty implements StandardProperty<HrefList> {

        public String fieldName() {
            return "calendar-user-address-set";
        }

        /**
         * How can I return an array here ?
         *

          <C:calendar-user-address-set xmlns:D="DAV:"
                                xmlns:C="urn:ietf:params:xml:ns:caldav">
            <D:href>mailto:bernard@example.com</D:href>
            <D:href>mailto:bernard.desruisseaux@example.com</D:href>
          </C:calendar-user-address-set>

         * @param res
         * @return
         */

        public HrefList getValue( PropFindableResource res ) {
            HrefList list = new HrefList();
            list.add("a1@b.com");
            list.add("a2@b.com");
            return list;
        }

        public Class<HrefList> getValueClass() {
            return HrefList.class;
        }
    }

    class ScheduleInboxProperty implements StandardProperty<String> {

        public String fieldName() {
            return "schedule-inbox-URL";
        }

        public String getValue( PropFindableResource res ) {
            return "http://localhost:7080/caldavdemo/folder1/cal1";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    class ScheduleOutboxProperty implements StandardProperty<String> {

        public String fieldName() {
            return "schedule-outbox-URL";
        }

        public String getValue( PropFindableResource res ) {
            return "http://localhost:7080/caldavdemo/folder1/cal1";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

   /**
     *  CalendarServer support
     *
     *  https://trac.calendarserver.org/browser/CalendarServer/trunk/doc/Extensions/caldav-ctag.txt\
     *  http://code.google.com/p/sabredav/wiki/ICal
     *
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

    class DropBoxProperty implements StandardProperty<String> {

        public String fieldName() {
            return "dropbox-home-URL";
        }

        public String getValue( PropFindableResource res ) {
            return "http://localhost:7080/caldavdemo/folder1/cal1";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    class XMPPProperty implements StandardProperty<String> {

        public String fieldName() {
            return "xmpp-uri";
        }

        public String getValue( PropFindableResource res ) {
            return "xmpp:romeo@montague.net";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }
}
