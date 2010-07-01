package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.DigestResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

import freenet.log.Logger;

/**
 *
 * @author brad
 */
public class DigestAuthenticationHandler implements AuthenticationHandler {

    private final NonceProvider nonceProvider;
    private final DigestHelper digestHelper;


    public DigestAuthenticationHandler( NonceProvider nonceProvider ) {
        this.nonceProvider = nonceProvider;
        this.digestHelper = new DigestHelper(nonceProvider);
    }

    public DigestAuthenticationHandler() {
        this.nonceProvider = new SimpleMemoryNonceProvider( 60*60*24 ); // one day
        this.digestHelper = new DigestHelper(nonceProvider);
    }

    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        boolean b;
        if( r instanceof DigestResource ) {
            b = Auth.Scheme.DIGEST.equals( auth.getScheme() );
        } else {
            Logger.debug(this, "resource is not an instanceof " + DigestResource.class );
            b = false;
        }
        return b;
    }

    public Object authenticate( Resource r, Request request ) {
        DigestResource digestResource = (DigestResource) r;
        Auth auth = request.getAuthorization();
        DigestResponse resp = digestHelper.calculateResponse(auth, r.getRealm(), request.getMethod());
        if( resp == null ) {
            Logger.debug(this, "requested digest authentication is invalid or incorrectly formatted");
            return null;
        } else {
            Object o = digestResource.authenticate( resp );
            return o;
        }
    }

    public String getChallenge( Resource resource, Request request ) {

        String nonceValue = nonceProvider.createNonce( resource, request );
        return digestHelper.getChallenge(nonceValue, request.getAuthorization(), resource.getRealm());
    }

    public boolean isCompatible( Resource resource ) {
        return ( resource instanceof DigestResource );
    }
}

