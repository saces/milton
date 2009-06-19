package com.bradmcevoy.http;

import com.bradmcevoy.http.Request.Method;

/**
 *
 */
public interface SecurityManager {

    /**
     *
     * @param user
     * @param password
     * @return - some object representing the user to associate with the request
     */
    Object authenticate( String user, String password );

    /**
     * Check that the authenticater user can access the given resource for the
     * given method.
     *
     * @param request - the request itself
     * @param method - the request method
     * @param auth - authentication object representing the current user
     * @param resource - the resource being operated on
     * @return - true to indicate that the user is allowed access
     */
    boolean authorise( Request request, Method method, Auth auth, Resource resource );

    /**
     * 
     * @return - the name of the security realm this is managing
     */
    String getRealm();

}
