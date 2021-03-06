package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.AuthenticationHandler;
import com.bradmcevoy.http.Filter;
import com.bradmcevoy.http.FilterChain;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.http11.Http11ResponseHandler;
import com.bradmcevoy.http.SecurityManager;

import freenet.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A filter to perform authentication before resource location.
 *
 * This allows the authenticated context to be available for resource location.
 *
 * Note that this filter contains a list of AuthenticationHandler. However,
 * these handlers MUST be designed to ignore the resource variable as it will
 * always be null when used with this filter. This approach allows these handlers
 * to be used with the post-resource-location approach.
 *
 */
public class PreAuthenticationFilter implements Filter {

    private static final ThreadLocal<Request> tlRequest = new ThreadLocal<Request>();

    private final Http11ResponseHandler responseHandler;
    private final List<AuthenticationHandler> authenticationHandlers;

    public static Request getCurrentRequest() {
        return tlRequest.get();
    }

    public PreAuthenticationFilter(Http11ResponseHandler responseHandler, List<AuthenticationHandler> authenticationHandlers) {
        this.responseHandler = responseHandler;
        this.authenticationHandlers = authenticationHandlers;
    }

    public PreAuthenticationFilter(Http11ResponseHandler responseHandler, SecurityManager securityManager) {
        this.responseHandler = responseHandler;
        this.authenticationHandlers = new ArrayList<AuthenticationHandler>();
        authenticationHandlers.add(new SecurityManagerBasicAuthHandler(securityManager));
        authenticationHandlers.add(new SecurityManagerDigestAuthenticationHandler(securityManager));
    }



    public void process(FilterChain chain, Request request, Response response) {
        Logger.debug(this, "process");
        try {
            tlRequest.set(request);
            Object authTag = authenticate(request);
            if( authTag != null ) {
                request.getAuthorization().setTag(authTag);
                chain.process(request, response);
            } else {
                responseHandler.respondUnauthorised(null, response, request);
            }
        } finally {
            tlRequest.remove();
        }
    }


    /**
     * Looks for an AuthenticationHandler which supports the given resource and
     * authorization header, and then returns the result of that handler's
     * authenticate method.
     *
     * Returns null if no handlers support the request
     *
     * @param request
     * @return
     */
    public Object authenticate( Request request ) {
        for( AuthenticationHandler h : authenticationHandlers ) {
            if( h.supports( null, request ) ) {
                Object o = h.authenticate( null, request );
                if( o == null ) {
                    Logger.warning(this, "authentication failed by AuthenticationHandler:" + h.getClass() );
                }
                return o;
            }
        }
        Logger.warning(this, "No AuthenticationHandler supports scheme:" + request.getAuthorization().getScheme()  );
        return null;
    }

    /**
     * Generates a list of http authentication challenges, one for each
     * supported authentication method, to be sent to the client.
     *
     * @param request - the current request
     * @return - a list of http challenges
     */
    public List<String> getChallenges( Request request ) {
        List<String> challenges = new ArrayList<String>();
        for( AuthenticationHandler h : authenticationHandlers ) {
            String ch = h.getChallenge( null, request );
            challenges.add( ch );
        }
        return challenges;
    }

}
