package com.bradmcevoy.http;

import com.bradmcevoy.http.http11.DeleteHandler.CantDeleteException;

import freenet.log.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of DeleteHelper
 *
 * It will delegate to the resource if it implements DeletableCollectionResource,
 * otherwise it will walk the collection if its a CollectionResource, and finally
 * will just call handlerHelper.isLockedOut otherwise
 *
 */
public class DeleteHelperImpl implements DeleteHelper {

    private final HandlerHelper handlerHelper;

    public DeleteHelperImpl(HandlerHelper handlerHelper) {
        this.handlerHelper = handlerHelper;
    }

    public boolean isLockedOut(Request req, DeletableResource r) {
        if (r instanceof DeletableCollectionResource) {
            DeletableCollectionResource dcr = (DeletableCollectionResource) r;
            boolean locked = dcr.isLockedOutRecursive(req);
            if( locked /*&& log.isInfoEnabled()*/) {
                Logger.normal(this, "isLocked, as reported by DeletableCollectionResource: " + dcr.getName());
            }
            return locked;
            
        } else if (r instanceof CollectionResource) {
            CollectionResource col = (CollectionResource) r;
            List<Resource> list = new ArrayList<Resource>();
            list.addAll(col.getChildren());
            for (Resource rChild : list) {
                if (rChild instanceof DeletableResource) {
                    DeletableResource rChildDel = (DeletableResource) rChild;
                    if (isLockedOut(req, rChildDel)) {
                        //if( log.isInfoEnabled()) {
                            Logger.normal(this, "isLocked: " + rChild.getName() + " type:" + rChild.getClass());
                        //}
                        return true;
                    }
                } else {
                    //if( log.isInfoEnabled() ) {
                        Logger.normal(this, "a child resource is not deletable: " + rChild.getName() + " type: " + rChild.getClass());
                    //}
                    return true;
                }
            }
            return false;

        } else {
            boolean locked = handlerHelper.isLockedOut(req, r);
            if( locked /*&& log.isInfoEnabled()*/) {
                Logger.normal(this, "isLocked, as reported by handlerHelper on resource: " + r.getName());
            }
            return locked;
            
        }
    }

    public void delete(DeletableResource r) throws CantDeleteException {
        if (r instanceof DeletableCollectionResource) {
            r.delete();

        } else if (r instanceof CollectionResource) {
            CollectionResource col = (CollectionResource) r;
            List<Resource> list = new ArrayList<Resource>();
            list.addAll(col.getChildren());
            for (Resource rChild : list) {
                if (rChild instanceof DeletableResource) {
                    DeletableResource rChildDel = (DeletableResource) rChild;
                    delete(rChildDel);
                } else {
                    throw new CantDeleteException(rChild, Response.Status.SC_LOCKED);
                }
            }
            r.delete();
            
        } else {
            r.delete();
        }
    }
}
