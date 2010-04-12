package com.ettrema.http.caldav;

import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.webdav.PropertyMap;
import com.bradmcevoy.http.webdav.PropertyMap.StandardProperty;
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
        propertyMap = new PropertyMap( "urn:ietf:params:xml:ns:caldav");
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
