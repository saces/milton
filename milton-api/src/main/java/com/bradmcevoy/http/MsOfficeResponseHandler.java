package com.bradmcevoy.http;

import com.bradmcevoy.http.http11.DefaultHttp11ResponseHandler;

import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;

/**
 * Disables locking, as required for MS office support
 *
 */
public class MsOfficeResponseHandler extends AbstractWrappingResponseHandler {

    public MsOfficeResponseHandler(WebDavResponseHandler wrapped) {
        super(wrapped );
    }

    public MsOfficeResponseHandler(AuthenticationService authenticationService) {
        super( new DefaultWebDavResponseHandler(authenticationService));
    }





    /**
     * Overrides the default behaviour to set the status to Response.Status.SC_NOT_IMPLEMENTED
     * instead of NOT_ALLOWED, so that MS office applications are able to open
     * resources
     *
     * @param res
     * @param response
     * @param request
     */
    @Override
    public void respondMethodNotAllowed(Resource res, Response response, Request request) {
        wrapped.respondMethodNotImplemented( res, response, request );
    }
}
