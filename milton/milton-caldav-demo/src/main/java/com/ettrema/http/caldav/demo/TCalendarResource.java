package com.ettrema.http.caldav.demo;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.http.AccessControlledResource;
import com.ettrema.http.CalendarResource;
import com.ettrema.http.ReportableResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 *
 * @author brad
 */
public class TCalendarResource extends TResource implements CalendarResource, AccessControlledResource, ReportableResource{

    public TCalendarResource( TFolderResource parent, String name ) {
        super( parent, name );
    }

    @Override
    protected Object clone( TFolderResource newParent ) {
        return new TCalendarResource( newParent, name);
    }

    public void sendContent( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException, BadRequestException {
        out.write( "hi there".getBytes());
    }

    public String getContentType( String accepts ) {
        return "text";
    }

}
