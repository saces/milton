package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import freenet.log.Logger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PostHandler implements ExistingEntityHandler {

    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ResourceHandlerHelper resourceHandlerHelper;

    public PostHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = new ResourceHandlerHelper( handlerHelper, responseHandler );
    }

    public String[] getMethods() {
        return new String[]{Request.Method.POST.code};
    }

    public boolean isCompatible( Resource handler ) {
        return ( handler instanceof PostableResource );
    }

    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException, BadRequestException {
        // need a linked hash map to preserve ordering of params
        Map<String, String> params = new LinkedHashMap<String, String>();
        Map<String, FileItem> files = new HashMap<String, FileItem>();
        try {
            request.parseRequestParameters( params, files );
        } catch( RequestParseException ex ) {
            Logger.warning(this, "exception parsing request. probably interrupted upload", ex );
            return;
        }

        request.getAttributes().put( "_params", params );
        request.getAttributes().put( "_files", files );

        this.resourceHandlerHelper.process( manager, request, response, this );
    }

    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        manager.onPost( request, response, r, request.getParams(), request.getFiles() );
        resourceHandlerHelper.processResource( manager, request, response, r, this, true, request.getParams(), request.getFiles() );
    }

    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException {
        PostableResource r = (PostableResource) resource;
        String url = r.processForm( request.getParams(), request.getFiles() );
        if( url != null ) {
            responseHandler.respondRedirect( response, request, url );
        } else {
            responseHandler.respondContent( resource, response, request, request.getParams() );
        }
    }
}
