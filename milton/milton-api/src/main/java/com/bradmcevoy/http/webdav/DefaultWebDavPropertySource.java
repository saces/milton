package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.DateUtils;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Utils;
import com.bradmcevoy.http.XmlWriter;
import com.bradmcevoy.http.http11.DefaultHttp11ResponseHandler;
import com.bradmcevoy.http.quota.DefaultQuotaDataAccessor;
import com.bradmcevoy.http.quota.QuotaDataAccessor;
import com.bradmcevoy.http.webdav.PropertyMap.StandardProperty;
import com.bradmcevoy.http.webdav.WebDavProtocol.SupportedLocks;
import com.bradmcevoy.property.PropertySource;
import java.util.Date;
import java.util.List;
import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author brad
 */
public class DefaultWebDavPropertySource implements PropertySource {

    private static final Logger log = LoggerFactory.getLogger( DefaultWebDavPropertySource.class );
    private final ResourceTypeHelper resourceTypeHelper;
    private final QuotaDataAccessor quotaDataAccessor;
    private final PropertyMap propertyMap;

    public DefaultWebDavPropertySource( ResourceTypeHelper resourceTypeHelper ) {
        this( resourceTypeHelper, new DefaultQuotaDataAccessor() );
    }

    public DefaultWebDavPropertySource( ResourceTypeHelper resourceTypeHelper, QuotaDataAccessor quotaDataAccessor ) {
        this.resourceTypeHelper = resourceTypeHelper;
        this.quotaDataAccessor = quotaDataAccessor;
        this.propertyMap = new PropertyMap( WebDavProtocol.NS_DAV );
        log.info( "DefaultWebDavPropertySource: resourceTypeHelper: " + resourceTypeHelper.getClass() );
        log.info( "DefaultWebDavPropertySource: quotaDataAccessor: " + quotaDataAccessor.getClass() );
        propertyMap.add( new ContentLengthPropertyWriter() );
        propertyMap.add( new ContentTypePropertyWriter() );
        propertyMap.add( new CreationDatePropertyWriter() );
        propertyMap.add( new DisplayNamePropertyWriter() );
        propertyMap.add( new LastModifiedDatePropertyWriter() );
        propertyMap.add( new ResourceTypePropertyWriter() );
        propertyMap.add( new EtagPropertyWriter() );

        propertyMap.add( new SupportedLockPropertyWriter() );
        propertyMap.add( new LockDiscoveryPropertyWriter() );

        propertyMap.add( new MSIsCollectionPropertyWriter() );
        propertyMap.add( new MSIsReadOnlyPropertyWriter() );
        propertyMap.add( new MSNamePropertyWriter() );

        propertyMap.add( new QuotaAvailableBytesPropertyWriter() );
        propertyMap.add( new QuotaUsedBytesPropertyWriter() );

    }

    public Object getProperty( QName name, Resource r ) {
        return propertyMap.getProperty( name, r );
    }

    public void setProperty( QName name, Object value, Resource r ) {
        throw new UnsupportedOperationException( "Not supported. Standard webdav properties are not writable" );
    }

    public PropertyMetaData getPropertyMetaData( QName name, Resource r ) {
        return propertyMap.getPropertyMetaData( name, r );
    }

    public void clearProperty( QName name, Resource r ) {
        throw new UnsupportedOperationException( "Not supported. Standard webdav properties are not writable" );
    }

    public List<QName> getAllPropertyNames( Resource r ) {
        return propertyMap.getAllPropertyNames( r );
    }

    class DisplayNamePropertyWriter implements StandardProperty<String> {
        public String getValue( PropFindableResource res ) {
            return res.getName();
        }

        public String fieldName() {
            return "displayname";
        }

        public Class<String> getValueClass() {
            return String.class;
        }
    }

    class CreationDatePropertyWriter implements StandardProperty<Date> {

        public String fieldName() {
            return "getcreated";
        }

        public Date getValue( PropFindableResource res ) {
            return res.getModifiedDate();
        }

        public Class<Date> getValueClass() {
            return Date.class;
        }
    }

    class LastModifiedDatePropertyWriter implements StandardProperty<Date> {

        public String fieldName() {
            return "getlastmodified";
        }

        public Date getValue( PropFindableResource res ) {
            return res.getModifiedDate();
        }

        public Class<Date> getValueClass() {
            return Date.class;
        }
    }

    class ResourceTypePropertyWriter implements StandardProperty<List<QName>> {

        public List<QName> getValue( PropFindableResource res ) {
            log.trace( "ResourceTypePropertyWriter:getValue" );
            return resourceTypeHelper.getResourceTypes( res );
        }

        public String fieldName() {
            return "resourcetype";
        }

        public Class getValueClass() {
            return List.class;
        }
    }

    class ContentTypePropertyWriter implements StandardProperty<String> {

        public String getValue( PropFindableResource res ) {
            if( res instanceof GetableResource ) {
                GetableResource getable = (GetableResource) res;
                return getable.getContentType( null );
            } else {
                return "";
            }
        }

        public String fieldName() {
            return "getcontenttype";
        }

        public Class getValueClass() {
            return String.class;
        }
    }

    class ContentLengthPropertyWriter implements StandardProperty<Long> {

        public Long getValue( PropFindableResource res ) {
            if( res instanceof GetableResource ) {
                GetableResource getable = (GetableResource) res;
                Long l = getable.getContentLength();
                return l;
            } else {
                return null;
            }
        }

        public String fieldName() {
            return "getcontentlength";
        }

        public Class getValueClass() {
            return Long.class;
        }
    }

    class QuotaUsedBytesPropertyWriter implements StandardProperty<Long> {

        public Long getValue( PropFindableResource res ) {
            return quotaDataAccessor.getQuotaUsed( res );
        }

        public String fieldName() {
            return "quota-used-bytes";
        }

        public Class getValueClass() {
            return Long.class;
        }
    }

    class QuotaAvailableBytesPropertyWriter implements StandardProperty<Long> {

        public Long getValue( PropFindableResource res ) {
            return quotaDataAccessor.getQuotaAvailable( res );
        }

        public String fieldName() {
            return "quota-available-bytes";
        }

        public Class getValueClass() {
            return Long.class;
        }
    }

    class EtagPropertyWriter implements StandardProperty<String> {

        public String getValue( PropFindableResource res ) {
            String etag = DefaultHttp11ResponseHandler.generateEtag( res );
            return etag;
        }

        public String fieldName() {
            return "getetag";
        }

        public Class getValueClass() {
            return String.class;
        }
    }

//    <D:supportedlock/><D:lockdiscovery/>
    class LockDiscoveryPropertyWriter implements StandardProperty<LockToken> {

        public LockToken getValue( PropFindableResource res ) {
            if( !( res instanceof LockableResource ) ) return null;
            LockableResource lr = (LockableResource) res;
            LockToken token = lr.getCurrentLock();
            return token;
        }

        public String fieldName() {
            return "supportedlock";
        }

        public Class getValueClass() {
            return LockToken.class;
        }
    }

    class SupportedLockPropertyWriter implements StandardProperty<SupportedLocks> {

        public SupportedLocks getValue( PropFindableResource res ) {
            if( res instanceof LockableResource ) {
                return new SupportedLocks();
            } else {
                return null;
            }
        }

        public String fieldName() {
            return "supportedlock";
        }

        public Class getValueClass() {
            return SupportedLocks.class;
        }
    }

    // MS specific fields
    class MSNamePropertyWriter extends DisplayNamePropertyWriter {

        @Override
        public String fieldName() {
            return "name";
        }
    }

    class MSIsCollectionPropertyWriter implements StandardProperty<Boolean> {

        @Override
        public String fieldName() {
            return "iscollection";
        }

        public Boolean getValue( PropFindableResource res ) {
            return ( res instanceof CollectionResource );
        }

        public Class getValueClass() {
            return Boolean.class;
        }
    }

    class MSIsReadOnlyPropertyWriter implements StandardProperty<Boolean> {

        @Override
        public String fieldName() {
            return "isreadonly";
        }

        public Boolean getValue( PropFindableResource res ) {
            return !( res instanceof PutableResource );
        }

        public Class getValueClass() {
            return Boolean.class;
        }
    }

    private String nameEncode( String s ) {
        //return Utils.encode(href, false); // see MIL-31
        return Utils.escapeXml( s );
        //return href.replaceAll("&", "&amp;");  // http://www.ettrema.com:8080/browse/MIL-24
    }

    protected void sendStringProp( XmlWriter writer, String name, String value ) {
        String s = value;
        if( s == null ) {
            writer.writeProperty( null, name );
        } else {
            writer.writeProperty( null, name, s );
        }
    }

    void sendDateProp( XmlWriter writer, String name, Date date ) {
        sendStringProp( writer, name, ( date == null ? null : DateUtils.formatDate( date ) ) );
    }
}
