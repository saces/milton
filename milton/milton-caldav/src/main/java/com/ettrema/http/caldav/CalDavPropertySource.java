package com.ettrema.http.caldav;

import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.property.PropertySource;
import java.util.List;
import javax.xml.namespace.QName;

/**
 *
 * @author alex
 */
public class CalDavPropertySource implements PropertySource{

    private final PropertyMap propertyMap;

    public CalDavPropertySource() {
        propertyMap = new PropertyMap( "whatever-the-caldav-namespace-is---might-be-DAV");
    }

    public Object getProperty( QName name, Resource r ) {
        return propertyMap.getProperty( name, r );
    }

    public void setProperty( QName name, Object value, Resource r ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public PropertyMetaData getPropertyMetaData( QName name, Resource r ) {
        return propertyMap.getPropertyMetaData( name, r );
    }

    public void clearProperty( QName name, Resource r ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<QName> getAllPropertyNames( Resource r ) {
        return propertyMap.getAllPropertyNames( r );
    }

}
