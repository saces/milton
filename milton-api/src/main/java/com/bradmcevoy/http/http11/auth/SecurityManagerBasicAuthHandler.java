package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Auth.Scheme;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.SecurityManager;

import freenet.log.Logger;

/**
 *
 * @author brad
 */
public class SecurityManagerBasicAuthHandler implements AuthenticationHandler {

    private final com.bradmcevoy.http.SecurityManager securityManager;

    public SecurityManagerBasicAuthHandler(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        Logger.debug(this, "supports: " + auth.getScheme() );
        return auth.getScheme().equals( Scheme.BASIC );
    }

    public Object authenticate( Resource resource, Request request ) {
        Logger.debug(this, "authenticate" );
        Auth auth = request.getAuthorization();
        Object o = securityManager.authenticate(auth.getUser(), auth.getPassword());
        Logger.debug(this, "result: " + o);
        return o;
    }

    public String getChallenge( Resource resource, Request request ) {
        String realm = securityManager.getRealm(request.getHostHeader());
        return "Basic realm=\"" + realm + "\"";
    }

    public boolean isCompatible( Resource resource ) {
        return true;
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }
}
