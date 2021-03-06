
package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.*;

import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.http11.Http11ResponseHandler;

import freenet.log.Logger;

public class UnlockHandler implements ExistingEntityHandler {

    private final ResourceHandlerHelper resourceHandlerHelper;

    private final Http11ResponseHandler responseHandler;

    public UnlockHandler( ResourceHandlerHelper resourceHandlerHelper, Http11ResponseHandler responseHandler ) {
        this.resourceHandlerHelper = resourceHandlerHelper;
        this.responseHandler = responseHandler;
    }


    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        resourceHandlerHelper.process( httpManager, request, response, this );
    }

    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        resourceHandlerHelper.processResource( manager, request, response, r, this );
    }

    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException {
        LockableResource r = (LockableResource) resource;
        String sToken = request.getLockTokenHeader();        
        sToken = LockHandler.parseToken(sToken);
        
        // this should be checked in processResource now
        
//       	if( r.getCurrentLock() != null &&
//       			!sToken.equals( r.getCurrentLock().tokenId) &&
//       			isLockedOut( request, resource ))
//    	{
//       		//Should this be unlocked easily? With other tokens?
//    		response.setStatus(Status.SC_LOCKED);
//    	    log.info("cant unlock with token: " + sToken);
//    		return;
//    	}

        
        Logger.debug(this, "unlocking token: " + sToken);
        r.unlock(sToken);
        responseHandler.respondNoContent( resource, response, request );
    }
    
    public String[] getMethods() {
        return new String[]{Method.UNLOCK.code};
    }
    
    public boolean isCompatible( Resource handler ) {
        return handler instanceof LockableResource;
    }

    
}
