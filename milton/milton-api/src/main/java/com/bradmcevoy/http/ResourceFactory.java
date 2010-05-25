package com.bradmcevoy.http;

/**
 * Implementations of ResourceFactory translate URLs to instances of Resource
 * 

 * 
 * @author brad
 */
public interface ResourceFactory {
    
    /**
     * Locate an instance of a resource at the given url and on the given host
     * 
     * The host argument can be used for applications which implement virtual
     * domain hosting. But portable applications (ie those which do not depend on the host
     * name) should ignore the host argument. 
     * 
     * Note that the host will include the port number if it was specified in
     * the request
     * 
     * The path argument is just the part of the request url with protocol, host, port
     * number, and request parameters removed
     * 
     * Eg for a request http://milton.ettrema.com:80/downloads/index.html?ABC=123
     * the corresponding arguments will be:
     *   host: milton.ettrema.com:80
     *   path: /downloads/index.html
     * 
     * Note that your implementation should not be sensitive to trailing slashes
     * Eg these paths should return the same resource /apath and /apath/
     *
     * You should generally avoid using any request information other then that
     * provided in the method arguments. But if you find you need to you can access the
     * request and response objects from HttpManager.request() and HttpManager.response()
     * 
     * @param host
     * @param path
     * @return
     */
    Resource getResource(String host, String path);
    
    
}
