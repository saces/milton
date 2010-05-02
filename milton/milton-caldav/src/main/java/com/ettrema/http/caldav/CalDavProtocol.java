package com.ettrema.http.caldav;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HandlerHelper;
import com.bradmcevoy.http.HttpExtension;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.ResourceHandlerHelper;
import com.bradmcevoy.http.values.ValueWriters;
import com.bradmcevoy.http.webdav.PropFindPropertyBuilder;
import com.bradmcevoy.http.webdav.PropFindXmlGenerator;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.bradmcevoy.property.PropertySource;
import com.ettrema.http.acl.ACLHandler;
import com.ettrema.http.report.Report;
import com.ettrema.http.report.ReportHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

    public CalDavProtocol( ResourceFactory resourceFactory, List<PropertySource> propertySources, WebDavResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        handlers = new HashSet<Handler>();
        handlers.add( new ACLHandler( responseHandler, handlerHelper ) );
        
        List<Report> reports = new ArrayList<Report>();
        ValueWriters valueWriters = new ValueWriters();
        PropFindXmlGenerator gen = new PropFindXmlGenerator( valueWriters );
        PropFindPropertyBuilder propertyBuilder = new PropFindPropertyBuilder( propertySources );
        reports.add( new MultiGetReport(resourceFactory, propertyBuilder, gen ));

        ResourceHandlerHelper resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
        handlers.add( new ReportHandler( responseHandler, resourceHandlerHelper, reports ) );
    }

    public Set<Handler> getHandlers() {
        return Collections.unmodifiableSet( handlers );
    }
}
