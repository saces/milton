package com.ettrema.http.caldav;

import com.ettrema.http.EventResource;
import com.ettrema.http.ICalResource;

/**
 *
 * @author brad
 */
public class ICalFormatter {

    public String format( ICalResource r ) {
        if( r instanceof EventResource ) {
            return formatEvent( (EventResource) r);
        } else {
            throw new RuntimeException( "Unsupported type: " + r.getClass() );
        }
    }

    private String formatEvent( EventResource r ) {
        // TODO, obviously
//        return "";
        return "BEGIN:VCALENDAR\n"
            + "VERSION:2.0\n"
            + "PRODID:-//MiltonCalDAV//EN\n"
            + "BEGIN:VEVENT\n"
            + "UID:" + r.getUniqueId() + "\n"
            + "DTSTAMP:19970714T170000Z\n" 
            + "SUMMARY:" + r.getSummary() + "\n"
            + "DTSTART:20100505T180000Z\n"
            + "DTEND:20100509T180000Z\n"
            + "END:VEVENT\n"
            + "END:VCALENDAR";
    }
}
