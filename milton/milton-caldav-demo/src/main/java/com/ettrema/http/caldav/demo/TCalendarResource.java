package com.ettrema.http.caldav.demo;

import com.ettrema.http.AccessControlledResource;
import com.ettrema.http.CalendarResource;
import com.ettrema.http.ReportableResource;
import java.util.Date;

/**
 *
 * @author brad
 */
public class TCalendarResource extends TFolderResource implements CalendarResource, AccessControlledResource, ReportableResource{

    public TCalendarResource( TFolderResource parent, String name ) {
        super( parent, name );
    }

    @Override
    protected Object clone( TFolderResource newParent ) {
        return new TCalendarResource( newParent, name);
    }

    public TEvent addEvent(String name, Date start, Date end, String summary) {
        TEvent e = new TEvent( this, name, start, end, summary );
        this.children.add( e );
        return e;
    }
}
