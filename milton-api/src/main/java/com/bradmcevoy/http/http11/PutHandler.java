package com.bradmcevoy.http.http11;

import com.bradmcevoy.http.*;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.quota.StorageChecker.StorageErrorReason;
import java.io.IOException;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Response.Status;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;
import com.bradmcevoy.io.FileUtils;
import com.bradmcevoy.io.RandomFileOutputStream;

import freenet.log.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class PutHandler implements Handler {
	private static volatile boolean logDEBUG;

	static {
		Logger.registerClass(PutHandler.class);
	}

    private final Http11ResponseHandler responseHandler;
    private final HandlerHelper handlerHelper;
    private final PutHelper putHelper;

    public PutHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.putHelper = new PutHelper();
        checkResponseHandler();
    }

    public PutHandler( Http11ResponseHandler responseHandler, HandlerHelper handlerHelper, PutHelper putHelper ) {
        this.responseHandler = responseHandler;
        this.handlerHelper = handlerHelper;
        this.putHelper = putHelper;
        checkResponseHandler();
    }

    private void checkResponseHandler() {
        if( !( responseHandler instanceof WebDavResponseHandler ) ) {
            Logger.warning(this, "response handler is not a WebDavResponseHandler, so locking and quota checking will not be enabled" );
        }
    }

    public String[] getMethods() {
        return new String[]{Method.PUT.code};
    }

    public boolean isCompatible( Resource handler ) {
        return ( handler instanceof PutableResource );
    }

    public void process( HttpManager manager, Request request, Response response ) throws NotAuthorizedException, ConflictException, BadRequestException {
        if( !handlerHelper.checkExpects( responseHandler, request, response ) ) {
            return;
        }

        String host = request.getHostHeader();
        String urlToCreateOrUpdate = HttpManager.decodeUrl( request.getAbsolutePath() );
        Logger.debug(this, "process request: host: " + host + " url: " + urlToCreateOrUpdate );

        Path path = Path.path( urlToCreateOrUpdate );
        urlToCreateOrUpdate = path.toString();

        Resource existingResource = manager.getResourceFactory().getResource( host, urlToCreateOrUpdate );
        ReplaceableResource replacee;

        StorageErrorReason storageErr = null;
        if( existingResource != null ) {
            //Make sure the parent collection is not locked by someone else
            if( handlerHelper.isLockedOut( request, existingResource ) ) {
                Logger.warning(this, "resource is locked, but not by the current user" );
                respondLocked( request, response, existingResource );
                return;
            }
            Resource parent = manager.getResourceFactory().getResource( host, path.getParent().toString() );
            if( parent instanceof CollectionResource ) {
                CollectionResource parentCol = (CollectionResource) parent;
                storageErr = handlerHelper.checkStorageOnReplace( request, parentCol, existingResource, host );
            } else {
                Logger.warning(this, "parent exists but is not a collection resource: " + path.getParent() );
            }
        } else {
            CollectionResource parentCol = putHelper.findNearestParent( manager, host, path );
            storageErr = handlerHelper.checkStorageOnAdd( request, parentCol, path.getParent(), host );
        }

        if( storageErr != null ) {
            respondInsufficientStorage( request, response, storageErr );
            return;
        }

        if( existingResource != null && existingResource instanceof ReplaceableResource ) {
            replacee = (ReplaceableResource) existingResource;
        } else {
            replacee = null;
        }

        if( replacee != null ) {
            long t = System.currentTimeMillis();
            try {
                manager.onProcessResourceStart( request, response, replacee );
                processReplace( manager, request, response, (ReplaceableResource) existingResource );
            } finally {
                t = System.currentTimeMillis() - t;
                manager.onProcessResourceFinish( request, response, replacee, t );
            }
        } else {
            // either no existing resource, or its not replaceable. check for folder
            String nameToCreate = path.getName();
            CollectionResource folderResource = findOrCreateFolders( manager, host, path.getParent() );
            if( folderResource != null ) {
                long t = System.currentTimeMillis();
                try {
                    if( folderResource instanceof PutableResource ) {

                        //Make sure the parent collection is not locked by someone else
                        if( handlerHelper.isLockedOut( request, folderResource ) ) {
                            respondLocked( request, response, folderResource );
                            return;
                        }

                        PutableResource putableResource = (PutableResource) folderResource;
                        processCreate( manager, request, response, putableResource, nameToCreate );
                    } else {
                        manager.getResponseHandler().respondMethodNotImplemented( folderResource, response, request );
                    }
                } finally {
                    t = System.currentTimeMillis() - t;
                    manager.onProcessResourceFinish( request, response, folderResource, t );
                }
            } else {
                responseHandler.respondNotFound( response, request );
            }
        }
    }

    private void processCreate( HttpManager manager, Request request, Response response, PutableResource folder, String newName ) throws ConflictException, BadRequestException {
        if( !handlerHelper.checkAuthorisation( manager, folder, request ) ) {
            responseHandler.respondUnauthorised( folder, response, request );
            return;
        }

        Logger.debug(this, "process: putting to: " + folder.getName() );
        try {
            Long l = putHelper.getContentLength( request );
            String ct = putHelper.findContentTypes( request, newName );
            Logger.debug(this, "PutHandler: creating resource of type: " + ct );
            folder.createNew( newName, request.getInputStream(), l, ct );
        } catch( IOException ex ) {
            Logger.warning(this, "IOException reading input stream. Probably interrupted upload: " + ex.getMessage() );
            return;
        }
        manager.getResponseHandler().respondCreated( folder, response, request );
    }

    private CollectionResource findOrCreateFolders( HttpManager manager, String host, Path path ) throws NotAuthorizedException, ConflictException {
        if( path == null ) return null;

        Resource thisResource = manager.getResourceFactory().getResource( host, path.toString() );
        if( thisResource != null ) {
            if( thisResource instanceof CollectionResource ) {
                return (CollectionResource) thisResource;
            } else {
                Logger.warning(this, "parent is not a collection: " + path );
                return null;
            }
        }

        CollectionResource parent = findOrCreateFolders( manager, host, path.getParent() );
        if( parent == null ) {
            Logger.warning(this, "couldnt find parent: " + path );
            return null;
        }

        Resource r = parent.child( path.getName() );
        if( r == null ) {
            if( parent instanceof MakeCollectionableResource ) {
                MakeCollectionableResource mkcol = (MakeCollectionableResource) parent;
                Logger.debug(this, "autocreating new folder: " + path.getName() );
                return mkcol.createCollection( path.getName() );
            } else {
                Logger.debug(this, "parent folder isnt a MakeCollectionableResource: " + parent.getName() );
                return null;
            }
        } else if( r instanceof CollectionResource ) {
            return (CollectionResource) r;
        } else {
            Logger.debug(this, "parent in URL is not a collection: " + r.getName() );
            return null;
        }
    }


    /**
     * "If an existing resource is modified, either the 200 (OK) or 204 (No Content) response codes SHOULD be sent to indicate successful completion of the request."
     * 
     * @param request
     * @param response
     * @param replacee
     */
    private void processReplace( HttpManager manager, Request request, Response response, ReplaceableResource replacee ) throws BadRequestException, NotAuthorizedException {
        if( !handlerHelper.checkAuthorisation( manager, replacee, request ) ) {
            responseHandler.respondUnauthorised( replacee, response, request );
            return;
        }
        try {
            Range range = putHelper.parseContentRange(replacee, request);
            if( range != null ) {
                Logger.debug(this, "partial put: " + range);
                if( replacee instanceof PartialllyUpdateableResource ) {
                    Logger.debug(this, "doing partial put on a PartialllyUpdateableResource");
                    PartialllyUpdateableResource partialllyUpdateableResource = (PartialllyUpdateableResource) replacee;
                    partialllyUpdateableResource.replacePartialContent(range, request.getInputStream());
                } else if( replacee instanceof GetableResource) {
                    Logger.debug(this, "doing partial put on a GetableResource");
                    File tempFile = File.createTempFile("milton-partial",null );
                    RandomAccessFile randomAccessFile = null;
                    
                    // The new length of the resource
                    long length;
                    try {
                        randomAccessFile = new RandomAccessFile(tempFile, "rw");
                        RandomFileOutputStream tempOut = new RandomFileOutputStream(tempFile);
                        GetableResource gr = (GetableResource) replacee;
                        // Update the content with the supplied partial content, and get the result as an inputstream
                        gr.sendContent(tempOut, null, null, null);

                        // Calculate new length, if the partial put is extending it
                        length = randomAccessFile.length();
                        if( range.getFinish()+1 > length ) {
                            length = range.getFinish()+1;
                        }

                        randomAccessFile.setLength(length);
                        randomAccessFile.seek(range.getStart());

                        int numBytesRead;
                        byte[] copyBuffer = new byte[1024];
                        InputStream newContent = request.getInputStream();

                        while ((numBytesRead = newContent.read(copyBuffer)) != -1) {
                            randomAccessFile.write(copyBuffer, 0, numBytesRead);
                        }
                    } finally {
                        FileUtils.close(randomAccessFile);
                    }
                    
                    InputStream updatedContent = new FileInputStream(tempFile);
                    BufferedInputStream bufin = new BufferedInputStream(updatedContent);

                    // Now, finally, we can just do a normal update
                    replacee.replaceContent(bufin, length);
                } else {
                    throw new BadRequestException(replacee, "Cant apply partial update. Resource does not support PartialllyUpdateableResource or GetableResource");
                }
            } else {
                Long l = request.getContentLengthHeader();
                replacee.replaceContent( request.getInputStream(), l );
            }
        } catch( IOException ex ) {
            Logger.warning(this, "IOException reading input stream. Probably interrupted upload: " + ex.getMessage() );
            return;
        }
        responseHandler.respondCreated( replacee, response, request );

        Logger.debug(this, "process: finished" );
    }

    public void processExistingResource( HttpManager manager, Request request, Response response, Resource resource ) throws NotAuthorizedException, BadRequestException, ConflictException {
        String host = request.getHostHeader();
        String urlToCreateOrUpdate = HttpManager.decodeUrl( request.getAbsolutePath() );
        Logger.debug(this, "process request: host: " + host + " url: " + urlToCreateOrUpdate );

        Path path = Path.path( urlToCreateOrUpdate );
        urlToCreateOrUpdate = path.toString();

        Resource existingResource = manager.getResourceFactory().getResource( host, urlToCreateOrUpdate );
        ReplaceableResource replacee;

        if( existingResource != null ) {
            //Make sure the parent collection is not locked by someone else
            if( handlerHelper.isLockedOut( request, existingResource ) ) {
                Logger.warning(this, "resource is locked, but not by the current user" );
                response.setStatus( Status.SC_LOCKED ); //423
                return;
            }

        }
        if( existingResource != null && existingResource instanceof ReplaceableResource ) {
            replacee = (ReplaceableResource) existingResource;
        } else {
            replacee = null;
        }

        if( replacee != null ) {
            processReplace( manager, request, response, (ReplaceableResource) existingResource );
        } else {
            // either no existing resource, or its not replaceable. check for folder
            String urlFolder = path.getParent().toString();
            String nameToCreate = path.getName();
            CollectionResource folderResource = findOrCreateFolders( manager, host, path.getParent() );
            if( folderResource != null ) {
                if(logDEBUG) {
                    Logger.debug(this, "found folder: " + urlFolder + " - " + folderResource.getClass() );
                }
                if( folderResource instanceof PutableResource ) {

                    //Make sure the parent collection is not locked by someone else
                    if( handlerHelper.isLockedOut( request, folderResource ) ) {
                        response.setStatus( Status.SC_LOCKED ); //423
                        return;
                    }

                    PutableResource putableResource = (PutableResource) folderResource;
                    processCreate( manager, request, response, putableResource, nameToCreate );
                } else {
                    responseHandler.respondMethodNotImplemented( folderResource, response, request );
                }
            } else {
                responseHandler.respondNotFound( response, request );
            }
        }

    }

    private void respondLocked( Request request, Response response, Resource existingResource ) {
        if( responseHandler instanceof WebDavResponseHandler ) {
            WebDavResponseHandler rh = (WebDavResponseHandler) responseHandler;
            rh.respondLocked( request, response, existingResource );
        } else {
            response.setStatus( Status.SC_LOCKED ); //423
        }
    }

    private void respondInsufficientStorage( Request request, Response response, StorageErrorReason storageErrorReason ) {
        if( responseHandler instanceof WebDavResponseHandler ) {
            WebDavResponseHandler rh = (WebDavResponseHandler) responseHandler;
            rh.respondInsufficientStorage( request, response, storageErrorReason );
        } else {
            response.setStatus( Status.SC_INSUFFICIENT_STORAGE );
        }
    }
}
