package com.bradmcevoy.http.webdav;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.IOException;

import org.xml.sax.SAXException;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Response.Status;

import freenet.log.Logger;

/**
 * Note that this is both a new entity handler and an existing entity handler
 * 
 * @author brad
 */
public class LockHandler implements ResourceHandler {

    private final WebDavResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;

    private LockWriterHelper lockWriterHelper;

    public LockHandler( WebDavResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        lockWriterHelper = new LockWriterHelper();
    }

    public LockWriterHelper getLockWriterHelper() {
        return lockWriterHelper;
    }

    public void setLockWriterHelper( LockWriterHelper lockWriterHelper ) {
        this.lockWriterHelper = lockWriterHelper;
    }


    public void processResource( HttpManager manager, Request request, Response response, Resource r ) throws NotAuthorizedException, ConflictException, BadRequestException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public String[] getMethods() {
        return new String[]{Method.LOCK.code};
    }

    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException {
        if( !handlerHelper.checkExpects( responseHandler, request, response ) ) {
            return;
        }

        String host = request.getHostHeader();
        String url = HttpManager.decodeUrl( request.getAbsolutePath() );

        // Find a resource if it exists
        Resource r = manager.getResourceFactory().getResource( host, url );
        if( r != null ) {
            Logger.debug(this, "locking existing resource: " + r.getName() );
            processExistingResource( manager, request, response, r );
        } else {
            Logger.debug(this, "lock target doesnt exist, attempting lock null.." );
            processNonExistingResource( manager, request, response, host, url );
        }
    }

    protected void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException {
        if( handlerHelper.isNotCompatible( resource, request.getMethod()) || !isCompatible( resource ) ) {
            responseHandler.respondMethodNotImplemented( resource, response, request );
            return;
        }
        if( !handlerHelper.checkAuthorisation( manager, resource, request ) ) {
            responseHandler.respondUnauthorised( resource, response, request );
            return;
        }

        LockableResource r = (LockableResource) resource;
        LockTimeout timeout = LockTimeout.parseTimeout( request );
        String ifHeader = request.getIfHeader();
        response.setContentTypeHeader( Response.XML );
        if( ifHeader == null || ifHeader.length() == 0 ) {
            processNewLock( manager, request, response, r, timeout );
        } else {
            processRefresh( manager, request, response, r, timeout, ifHeader );
        }
    }

    /**
     * (from the spec)
     * 7.4 Write Locks and Null Resources
     *
     * It is possible to assert a write lock on a null resource in order to lock the name.
     *
     * A write locked null resource, referred to as a lock-null resource, MUST respond with
     * a 404 (Not Found) or 405 (Method Not Allowed) to any HTTP/1.1 or DAV methods except
     * for PUT, MKCOL, OPTIONS, PROPFIND, LOCK, and UNLOCK. A lock-null resource MUST appear
     * as a member of its parent collection. Additionally the lock-null resource MUST have
     * defined on it all mandatory DAV properties. Most of these properties, such as all
     * the get* properties, will have no value as a lock-null resource does not support the GET method.
     * Lock-Null resources MUST have defined values for lockdiscovery and supportedlock properties.
     *
     * Until a method such as PUT or MKCOL is successfully executed on the lock-null resource the resource 
     * MUST stay in the lock-null state. However, once a PUT or MKCOL is successfully executed on
     * a lock-null resource the resource ceases to be in the lock-null state.
     *
     * If the resource is unlocked, for any reason, without a PUT, MKCOL, or 
     * similar method having been successfully executed upon it then the resource
     * MUST return to the null state.
     *
     *
     * @param manager
     * @param request
     * @param response
     * @param host
     * @param url
     */
    private void processNonExistingResource( HttpManager manager, Request request, Response response, String host, String url ) throws NotAuthorizedException {
        String name;

        Path parentPath = Path.path( url );
        name = parentPath.getName();
        parentPath = parentPath.getParent();
        url = parentPath.toString();

        Resource r = manager.getResourceFactory().getResource( host, url );
        if( r != null ) {
            if( !handlerHelper.checkAuthorisation( manager, r, request ) ) {
                responseHandler.respondUnauthorised( r, response, request );
                return;
            } else {
                processCreateAndLock( manager, request, response, r, name );
            }
        } else {
            Logger.debug(this, "couldnt find parent to execute lock-null, returning not found" );
            //respondNotFound(response,request);
            response.setStatus( Status.SC_CONFLICT );

        }
    }

    private void processCreateAndLock( HttpManager manager, Request request, Response response, Resource parentResource, String name ) throws NotAuthorizedException {
        if( parentResource instanceof LockingCollectionResource ) {
            Logger.debug(this, "parent supports lock-null. doing createAndLock" );
            LockingCollectionResource lockingParent = (LockingCollectionResource) parentResource;
            LockTimeout timeout = LockTimeout.parseTimeout( request );
            response.setContentTypeHeader( Response.XML );

            LockInfo lockInfo;
            try {
                lockInfo = LockInfo.parseLockInfo( request );
            } catch( SAXException ex ) {
                throw new RuntimeException( "Exception reading request body", ex );
            } catch( IOException ex ) {
                throw new RuntimeException( "Exception reading request body", ex );
            }

            // TODO: this should be refactored to return a LockResult as for existing entities

            Logger.debug(this, "Creating lock on unmapped resource: " + name );
            LockToken tok = lockingParent.createAndLock( name, timeout, lockInfo );
            response.setStatus( Status.SC_CREATED );
            response.setLockTokenHeader( "<opaquelocktoken:" + tok.tokenId + ">" );  // spec says to set response header. See 8.10.1
            respondWithToken( tok, request, response );

        } else {
            Logger.debug(this, "parent does not support lock-null, respondong method not allowed" );
            responseHandler.respondMethodNotImplemented( parentResource, response, request );
        }
    }

    public boolean isCompatible( Resource handler ) {
        return handler instanceof LockableResource;
    }

    protected void processNewLock( HttpManager milton, Request request, Response response, LockableResource r, LockTimeout timeout ) throws NotAuthorizedException {
        LockInfo lockInfo;
        try {
            lockInfo = LockInfo.parseLockInfo( request );
        } catch( SAXException ex ) {
            throw new RuntimeException( "Exception reading request body", ex );
        } catch( IOException ex ) {
            throw new RuntimeException( "Exception reading request body", ex );
        }

        if( handlerHelper.isLockedOut( request, r ) ) {
            this.responseHandler.respondLocked( request, response, r );
            return;
        }

        // todo: check if already locked and return 423 locked or 412-precondition failed
        // also must support multi-status. when locking a collection and a DEPTH > 1, must lock all
        // child elements
        Logger.debug(this, "locking: " + r.getName() );
        LockResult result = r.lock( timeout, lockInfo );
        if( result.isSuccessful() ) {
            LockToken tok = result.getLockToken();
            Logger.debug(this, "..locked ok: " + tok.tokenId );
            response.setLockTokenHeader( "<opaquelocktoken:" + tok.tokenId + ">" );  // spec says to set response header. See 8.10.1
            respondWithToken( tok, request, response );
        } else {
            responseWithLockFailure( result, request, response );
        }
    }

    protected void processRefresh( HttpManager milton, Request request, Response response, LockableResource r, LockTimeout timeout, String ifHeader ) throws NotAuthorizedException {
        String token = parseToken( ifHeader );
        Logger.debug(this, "refreshing lock: " + token );
        LockResult result = r.refreshLock( token );
        if( result.isSuccessful() ) {
            LockToken tok = result.getLockToken();
            respondWithToken( tok, request, response );
        } else {
            responseWithLockFailure( result, request, response );
        }
    }

    protected void respondWithToken( LockToken tok, Request request, Response response ) {
        response.setStatus( Status.SC_OK );
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XmlWriter writer = new XmlWriter( out );
        writer.writeXMLHeader();
        writer.open( "D:prop  xmlns:D=\"DAV:\"" );
        writer.newLine();
        writer.open( "D:lockdiscovery" );
        writer.newLine();
        writer.open( "D:activelock" );
        writer.newLine();
        lockWriterHelper.appendType( writer, tok.info.type );
        lockWriterHelper.appendScope( writer, tok.info.scope );
        lockWriterHelper.appendDepth( writer, tok.info.depth );
        lockWriterHelper.appendOwner( writer, tok.info.owner );
        lockWriterHelper.appendTimeout( writer, tok.timeout.getSeconds() );
        lockWriterHelper.appendTokenId( writer, tok.tokenId );
        lockWriterHelper.appendRoot( writer, request.getAbsoluteUrl() );
        writer.close( "D:activelock" );
        writer.close( "D:lockdiscovery" );
        writer.close( "D:prop" );
        writer.flush();

        Logger.debug(this, "lock response: " + out.toString() );
        try {
            response.getOutputStream().write( out.toByteArray() );
        } catch( IOException ex ) {
            Logger.warning(this, "exception writing to outputstream", ex );
        }
//        response.close();

    }

    static String parseToken( String ifHeader ) {
        String token = ifHeader;
        int pos = token.indexOf( ":" );
        if( pos >= 0 ) {
            token = token.substring( pos + 1 );
            pos = token.indexOf( ">" );
            if( pos >= 0 ) {
                token = token.substring( 0, pos );
            }
        }
        return token;
    }


    private void responseWithLockFailure( LockResult result, Request request, Response response ) {
        response.setStatus( result.getFailureReason().status );

    }
}
