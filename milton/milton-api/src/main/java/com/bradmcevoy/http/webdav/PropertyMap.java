package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.property.PropertySource.PropertyAccessibility;
import com.bradmcevoy.property.PropertySource.PropertyMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

/**
 * Helper class for PropertySources. Consists of a map of StandardProperty
 * instances, keyed by property name
 *
 * @author brad
 */
public class PropertyMap {
    private final Map<String, StandardProperty> writersMap = new HashMap<String, StandardProperty>();

    private final String nameSpace;

    public PropertyMap( String nameSpace ) {
        this.nameSpace = nameSpace;
    }



    public Object getProperty( QName name, Resource r ) {
        if( !name.getNamespaceURI().equals( nameSpace ) ) {
            return null;
        }
        StandardProperty pa = writersMap.get( name.getLocalPart() );
        if( pa == null ) {
            return null;
        }
        if( r instanceof PropFindableResource ) {
            return pa.getValue( (PropFindableResource) r );
        } else {
            return null;
        }
    }

    public PropertyMetaData getPropertyMetaData( QName name, Resource r ) {
        if( !name.getNamespaceURI().equals( nameSpace ) ) {
            return PropertyMetaData.UNKNOWN;
        }
        StandardProperty pa = writersMap.get( name.getLocalPart() );
        if( pa == null ) {
            return PropertyMetaData.UNKNOWN;
        } else {
            if( r instanceof PropFindableResource ) {
                return new PropertyMetaData( PropertyAccessibility.READ_ONLY, pa.getValueClass() );
            } else {
                return PropertyMetaData.UNKNOWN;
            }
        }
    }

    public List<QName> getAllPropertyNames( Resource r ) {
        List<QName> list = new ArrayList<QName>();
        for( String nm : this.writersMap.keySet() ) {
            QName qname = new QName( WebDavProtocol.NS_DAV, nm );
            list.add( qname );
        }
        return list;
    }

    public void add( StandardProperty pw ) {
        writersMap.put( pw.fieldName(), pw );
    }

    public interface StandardProperty<T> {

        String fieldName();

        T getValue( PropFindableResource res );

        Class getValueClass();
    }

}
