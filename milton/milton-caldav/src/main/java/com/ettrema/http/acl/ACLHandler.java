package com.ettrema.http.acl;

import com.bradmcevoy.http.Handler;
import com.bradmcevoy.http.HttpManager;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.ettrema.http.AccessControlledResource;

/**
 *
 * @author brad
 */
public class ACLHandler implements Handler{

    public String[] getMethods() {
        return new String[]{Method.ACL.code};
    }

    public void process( HttpManager httpManager, Request request, Response response ) throws ConflictException, NotAuthorizedException, BadRequestException {
        response.setStatus( Response.Status.SC_OK );
    }

    public boolean isCompatible( Resource res ) {
        return (res instanceof AccessControlledResource);
    }

}
