package com.bradmcevoy.http;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.http11.Http11ResponseHandler;
import com.bradmcevoy.http.quota.StorageChecker;
import com.bradmcevoy.http.quota.StorageChecker.StorageErrorReason;

import freenet.log.Logger;

import java.util.List;

/**
 *
 * @author brad
 */
public class HandlerHelper {

    private AuthenticationService authenticationService;
    private final List<StorageChecker> storageCheckers;

    public HandlerHelper( AuthenticationService authenticationService, List<StorageChecker> storageCheckers ) {
        this.authenticationService = authenticationService;
        this.storageCheckers = storageCheckers;
    }


    /**
     * Checks the expect header, and responds if necessary
     *
     * @param resource
     * @param request
     * @param response
     * @return - true if the expect header is ok
     */
    public boolean checkExpects( Http11ResponseHandler responseHandler, Request request, Response response ) {
        String s = request.getExpectHeader();
        if( s != null && s.length() > 0 ) {
            responseHandler.respondExpectationFailed( response, request );
            return false;
        } else {
            return true;
        }
    }

    public boolean checkAuthorisation( HttpManager manager, Resource resource, Request request ) {
        Auth auth = request.getAuthorization();
        if( auth != null ) {
            if( auth.getTag() == null ) {  // don't do double authentication
                Object authTag = authenticationService.authenticate( resource, request ); //handler.authenticate( auth.user, auth.password );
                if( authTag == null ) {
                    Logger.warning(this, "failed to authenticate - authenticationService:" + authenticationService.getClass() + " resource type:" + resource.getClass() );
                    return false;
                } else {
                    Logger.debug(this, "got authenticated tag: " + authTag.getClass() );
                    auth.setTag( authTag );
                }
            } else {
                Logger.debug(this, "request is pre-authenticated");
            }
        } else {
            auth = manager.getSessionAuthentication( request );
        }


        boolean authorised = resource.authorise( request, request.getMethod(), auth );
        if( !authorised ) {
            //if( log.isWarnEnabled()) {
                Logger.warning(this, "authorisation declined, requesting authentication: " + request.getAbsolutePath() + ". resource type: " + resource.getClass().getCanonicalName());
                if( auth != null ) {
                    Logger.warning(this, "auth: " + auth.getUser() + " tag: " + auth.getTag());
                }
            //}
            return false;
        } else {
            return true;
        }
    }

    public boolean doCheckRedirect( Http11ResponseHandler responseHandler, Request request, Response response, Resource resource ) {
        String redirectUrl = resource.checkRedirect( request );
        if( redirectUrl != null ) {
            responseHandler.respondRedirect( response, request, redirectUrl );
            return true;
        } else {
            return false;
        }
    }

    /**
     * TODO: move to webdav
     * 
     * @param inRequest
     * @param inResource
     * @return
     */
    public boolean isLockedOut( Request inRequest, Resource inResource ) {
        if( inResource == null || !( inResource instanceof LockableResource ) ) {
            return false;
        }
        LockableResource lr = (LockableResource) inResource;
        LockToken token = lr.getCurrentLock();
        if( token != null ) {
            Auth auth = inRequest.getAuthorization();
            String lockedByUser = token.info.lockedByUser;
            if( lockedByUser == null ) {
                Logger.warning(this, "Resource is locked with a null user. Ignoring the lock" );
                return false;
            } else if( !lockedByUser.equals( auth.getUser() ) ) {
                Logger.normal(this, "fail: lock owned by: " + lockedByUser + " not by " + auth.getUser() );
                String value = inRequest.getIfHeader();
                if( value != null ) {
                    if( value.contains( "opaquelocktoken:" + token.tokenId + ">" ) ) {
                        Logger.normal(this, "Contained valid token. so is unlocked" );
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean missingLock( Request inRequest, Resource inParentcol ) {
        //make sure we are not requiring a lock
        String value = inRequest.getHeaders().get( "If" );
        if( value != null ) {
            if( value.contains( "(<DAV:no-lock>)" ) ) {
                Logger.normal(this, "Contained valid token. so is unlocked");
                return true;
            }
        }

        return false;
    }

    public StorageErrorReason checkStorageOnReplace(Request request, CollectionResource parentCol, Resource replaced, String host) {
        for( StorageChecker sc : storageCheckers) {
            StorageErrorReason res = sc.checkStorageOnReplace( request, parentCol, replaced, host );
            if( res != null ) {
                Logger.warning(this, "insufficient storage reason: " + res + " reported by: " + sc.getClass() );
                return res;
            }
        }
        return null;
    }

    public StorageErrorReason checkStorageOnAdd(Request request, CollectionResource nearestParent, Path parentPath, String host) {
        for( StorageChecker sc : storageCheckers) {
            StorageErrorReason res = sc.checkStorageOnAdd( request, nearestParent, parentPath, host );
            if( res != null ) {
                Logger.warning(this, "insufficient storage reason: " + res + " reported by: " + sc.getClass() );
                return res;
            }
        }
        return null;
    }

    /**
     * Returns true to indicate that the given resource MUST NOT handle the 
     * given method.
     * 
     * A return value of false indicates that it might.
     * 
     * @param r - the resource to check
     * @param m - the HTTP request method
     * @return - true to indicate the resource must not handle method m
     */
    public boolean isNotCompatible(Resource r, Method m) {
        if( r instanceof ConditionalCompatibleResource ){
            ConditionalCompatibleResource  ccr = (ConditionalCompatibleResource) r;
            return !ccr.isCompatible( m );
        }
        return false;
    }
}
