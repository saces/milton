package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.*;
import java.util.ArrayList;
import java.util.List;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsHandler implements ResourceHandler {

    private static final Logger log = LoggerFactory.getLogger( OptionsHandler.class );
    private final Http11ResponseHandler responseHandler;
    private final ResourceHandlerHelper resourceHandlerHelper;

    public OptionsHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
    }

    @Override
    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.process( manager, request, response, this );
    }

    public void processResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, ConflictException, BadRequestException {
        long t = System.currentTimeMillis();
        try {

            manager.onProcessResourceStart( request, response, resource );

            List<String> methodsAllowed = determineMethodsAllowed( manager, resource );
            responseHandler.respondWithOptions( resource, response, request, methodsAllowed );

        } finally {
            t = System.currentTimeMillis() - t;
            manager.onProcessResourceFinish( request, response, resource, t );
        }
    }

    public String[] getMethods() {
        return new String[]{Method.OPTIONS.code};
    }

    @Override
    public boolean isCompatible( Resource handler ) {
        return true;
    }

    private List<String> determineMethodsAllowed( HttpManager manager, Resource res ) {
        List<String> list = new ArrayList<String>();
        for( Handler f : manager.getAllHandlers() ) {
            if( f.isCompatible( res ) ) {
                for( String m : f.getMethods() ) {
                    list.add( m );
                }
            }
        }
        return list;
    }
}