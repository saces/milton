package com.ettrema.http.caldav;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HandlerHelper;
import com.bradmcevoy.http.HttpExtension;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.ettrema.http.acl.ACLHandler;
import com.ettrema.http.report.ReportHandler;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author brad
 */
public class CalDavProtocol implements HttpExtension {

    private final Set<Handler> handlers;

    public CalDavProtocol( Set<Handler> handlers ) {
        this.handlers = handlers;
    }

    public CalDavProtocol( WebDavResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        handlers = new HashSet<Handler>();
        handlers.add( new ACLHandler( responseHandler, handlerHelper ) );
        handlers.add( new ReportHandler( responseHandler, handlerHelper ) );
    }

    public Set<Handler> getHandlers() {
        return Collections.unmodifiableSet( handlers );
    }
}
