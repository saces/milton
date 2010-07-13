package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.ConflictException;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.http11.Http11ResponseHandler;

import freenet.log.Logger;

import java.net.URI;


public class MoveHandler implements ExistingEntityHandler {

    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final ResourceHandlerHelper resourceHandlerHelper;

    public MoveHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper, ResourceHandlerHelper resourceHandlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.resourceHandlerHelper = resourceHandlerHelper;
    }



    public String[] getMethods() {
        return new String[]{Method.MOVE.code};
    }
        
    public boolean isCompatible(Resource handler) {
        return (handler instanceof MoveableResource);
    }        
    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.processResource( manager, request, response, r, this );
    }

    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        resourceHandlerHelper.process( httpManager, request, response, this );
    }

    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException {
        MoveableResource r = (MoveableResource) resource;
        String sDest = request.getDestinationHeader();
        //sDest = HttpManager.decodeUrl(sDest);
        Logger.debug(this, "dest header1: " + sDest);
        URI destUri = URI.create(sDest);
        sDest = destUri.getPath();
        Logger.debug(this, "dest header2: " + sDest);
        Dest dest = new Dest(destUri.getHost(),sDest);
        Logger.debug(this, "looking for destination parent: " + dest.host + " - " + dest.url);
        Resource rDest = manager.getResourceFactory().getResource(dest.host, dest.url);        
        Logger.debug(this, "process: moving from: " + r.getName() + " -> " + dest.url + " with name: " + dest.name);
        if( rDest == null ) {
            Logger.debug(this, "process: destination parent does not exist: " + sDest);
            responseHandler.respondConflict(resource, response, request, "Destination parent does not exist: " + sDest);
        } else if( !(rDest instanceof CollectionResource) ) {
            Logger.debug(this, "process: destination exists but is not a collection");
            responseHandler.respondConflict(resource, response, request, "Destination exists but is not a collection: " + sDest);
        } else { 
            CollectionResource colDest = (CollectionResource) rDest;
            // check if the dest exists
            Resource rExisting = colDest.child( dest.name);
            if( rExisting != null ) {
                // check for overwrite header
                Boolean ow = request.getOverwriteHeader();
                if( ow == null || !request.getOverwriteHeader().booleanValue() ) {
                    Logger.debug(this, "destination resource exists, and overwrite header is not set");
                    responseHandler.respondConflict( resource, response, request, "A resource exists at the destination");
                    return ;
                } else {
                    if( rExisting instanceof DeletableResource) {
                        Logger.debug(this, "deleting existing resource");
                        DeletableResource drExisting = (DeletableResource) rExisting;
                        drExisting.delete();
                    } else {
                        Logger.warning(this, "destination exists, and overwrite header is set, but destination is not a DeletableResource");
                        responseHandler.respondConflict( resource, response, request, "A resource exists at the destination, and it cannot be deleted");
                        return ;
                    }
                }
            }
            Logger.debug(this, "process: moving resource to: " + rDest.getName());
            try {
                r.moveTo( (CollectionResource) rDest, dest.name );
                responseHandler.respondCreated(resource, response, request);
            } catch( ConflictException ex ) {
                Logger.warning(this, "conflict", ex);
                responseHandler.respondConflict( resource, response, request, sDest );
            }
        }
        Logger.debug(this, "process: finished");
    }


}