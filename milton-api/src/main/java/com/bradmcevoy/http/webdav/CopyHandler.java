package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.http11.Http11ResponseHandler;

import freenet.log.Logger;

import java.net.URI;

public class CopyHandler implements ExistingEntityHandler {

    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ResourceHandlerHelper resourceHandlerHelper;

    public CopyHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper, ResourceHandlerHelper resourceHandlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = resourceHandlerHelper;
    }


    
    public String[] getMethods() {
        return new String[]{Method.COPY.code};
    }

    public boolean isCompatible(Resource handler) {
        return (handler instanceof CopyableResource);
    }

    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.processResource( manager, request, response, r, this );
    }

    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        resourceHandlerHelper.process( httpManager, request, response, this );
    }

    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException {
        CopyableResource r = (CopyableResource) resource;
        String sDest = request.getDestinationHeader();  
//        sDest = HttpManager.decodeUrl(sDest);
        URI destUri = URI.create(sDest);
        sDest = destUri.getPath();

        Dest dest = new Dest(destUri.getHost(),sDest);
        Resource rDest = manager.getResourceFactory().getResource(dest.host, dest.url);        
        Logger.debug(this, "process: copying from: " + r.getName() + " -> " + dest.url + "/" + dest.name);

        if( rDest == null ) {
            Logger.debug(this, "process: destination parent does not exist: " + sDest);
            responseHandler.respondConflict(resource, response, request, "Destination does not exist: " + sDest);
        } else if( !(rDest instanceof CollectionResource) ) {
            Logger.debug(this, "process: destination exists but is not a collection");
            responseHandler.respondConflict(resource, response,request, "Destination exists but is not a collection: " + sDest);
        } else { 
            Logger.debug(this, "process: moving resource to: " + rDest.getName());

            Resource fDest = manager.getResourceFactory().getResource(dest.host, dest.url + "/" + dest.name );        
           	if( handlerHelper.isLockedOut( request, fDest )) {
        		response.setStatus(Status.SC_LOCKED);
        		return;
        	}

            
            r.copyTo( (CollectionResource)rDest, dest.name );
            responseHandler.respondCreated(resource, response, request);
        }
        Logger.debug(this, "process: finished");
    }

    
}
