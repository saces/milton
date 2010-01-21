package com.ettrema.http.report;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.io.ReadingException;
import com.bradmcevoy.io.StreamUtils;
import com.bradmcevoy.io.WritingException;
import com.ettrema.http.ReportableResource;
import java.io.InputStream;

/**
 *
 * @author alex
 */
public class ReportHandler implements Handler {

    public String[] getMethods() {
        return new String[]{Method.REPORT.code};
    }

    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        try {
            InputStream in = this.getClass().getResourceAsStream( "/caldav-report.sample.xml" );
            StreamUtils.readTo( in, response.getOutputStream(), true, false );
        } catch( ReadingException ex ) {
            throw new RuntimeException( ex );
        } catch( WritingException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public boolean isCompatible( Resource res ) {
        return ( res instanceof ReportableResource );
    }
}
