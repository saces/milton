package com.ettrema.http.caldav.demo;

import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.http.EventResource;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author brad
 */
public class TEvent extends TResource implements EventResource {

    private Date start;

    private Date end;

    private String summary;

    public TEvent( TCalendarResource parent, String name, Date start, Date end, String summary ) {
        super(parent, name);
        this.start = start;
        this.end = end;
        this.summary = summary;
    }


    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getSummary() {
        return summary;
    }

    /**
     * @param start the start to set
     */
    public void setStart( Date start ) {
        this.start = start;
    }

    /**
     * @param end the end to set
     */
    public void setEnd( Date end ) {
        this.end = end;
    }

    /**
     * @param summary the summary to set
     */
    public void setSummary( String summary ) {
        this.summary = summary;
    }

    @Override
    protected Object clone( TFolderResource newParent ) {
        return new TEvent( (TCalendarResource) newParent,name, start, end, summary);
    }

    public void sendContent( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException, BadRequestException {
        out.write( "hi i'm an event".getBytes());
    }

    public String getContentType( String accepts ) {
        return "text/calendar";
    }

}
