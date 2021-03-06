package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Auth.Scheme;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

import freenet.log.Logger;

/**
 *
 * @author brad
 */
public class BasicAuthHandler implements AuthenticationHandler {

    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        Logger.debug(this, "supports: " + auth.getScheme() );
        return auth.getScheme().equals( Scheme.BASIC );
    }

    public Object authenticate( Resource resource, Request request ) {
        Logger.debug(this, "authenticate" );
        Auth auth = request.getAuthorization();
        Object o = resource.authenticate( auth.getUser(), auth.getPassword() );
        Logger.debug(this, "result: " + o);
        return o;
    }

    public String getChallenge( Resource resource, Request request ) {
        return "Basic realm=\"" + resource.getRealm() + "\"";
    }

    public boolean isCompatible( Resource resource ) {
        return true;
    }
}
