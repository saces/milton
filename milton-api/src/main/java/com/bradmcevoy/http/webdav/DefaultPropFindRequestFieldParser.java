package com.bradmcevoy.http.webdav;

import com.bradmcevoy.io.StreamUtils;

import freenet.log.Logger;

import java.io.ByteArrayInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.xml.namespace.QName;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Simple implmentation which just parses the request body. If no xml is present
 * it will return an empty set.
 *
 * Note this generally shouldnt be used directly, but should be wrapped by
 * MSPropFindRequestFieldParser to support windows clients.
 *
 * @author brad
 */
public class DefaultPropFindRequestFieldParser implements PropFindRequestFieldParser {

    public DefaultPropFindRequestFieldParser() {
    }

    public ParseResult getRequestedFields( InputStream in ) {
        try {
            final Set<QName> set = new LinkedHashSet<QName>();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            StreamUtils.readTo( in, bout, false, true );
            byte[] arr = bout.toByteArray();
            if( arr.length > 1 ) {
                ByteArrayInputStream bin = new ByteArrayInputStream( arr );
                XMLReader reader = XMLReaderFactory.createXMLReader();
                PropFindSaxHandler handler = new PropFindSaxHandler();
                reader.setContentHandler( handler );
                try {
                    reader.parse( new InputSource( bin ) );
                    if( handler.isAllProp() ) {
                        return new ParseResult( true, set );
                    } else {
                        set.addAll( handler.getAttributes().keySet() );
                    }
                } catch( IOException e ) {
                    Logger.warning(this, "exception parsing request body", e );
                    // ignore
                } catch( SAXException e ) {
                    Logger.warning(this, "exception parsing request body", e );
                    // ignore
                }
            }
            return new ParseResult( false, set );
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }
}
