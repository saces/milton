////////////////////////////////////////////////////////////////////////////////
//
// Copyright (c) 2009, Suncorp Metway Limited. All rights reserved.
//
// This is unpublished proprietary source code of Suncorp Metway Limited.
// The copyright notice above does not evidence any actual or intended
// publication of such source code.
//
////////////////////////////////////////////////////////////////////////////////
package com.ettrema.ftp;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author u370681
 */
public class MiltonFtpFile implements FtpFile {

    private static final Logger log = LoggerFactory.getLogger(MiltonFtpFile.class);

    private final Path path;

    private CollectionResource parent;

    private final MiltonFsView ftpFactory;

    private Resource r;

    public MiltonFtpFile(MiltonFsView resourceFactory, Path path, Resource r) {
        this.path = path;
        this.r = r;
        this.parent = null;
        this.ftpFactory = resourceFactory;
    }

    public MiltonFtpFile(MiltonFsView resourceFactory, Path path, CollectionResource parent, Resource r) {
        this.path = path;
        this.r = null;
        this.parent = parent;
        this.ftpFactory = resourceFactory;
    }

    public String getAbsolutePath() {
        return path.toString();
    }

    public String getName() {
        return r.getName();
    }

    public boolean isHidden() {
        return false;
    }

    public boolean isDirectory() {
        return (r instanceof CollectionResource);
    }

    public boolean isFile() {
        return !isDirectory();
    }

    public boolean doesExist() {
        return r != null;
    }

    public boolean isReadable() {
        return true;
    }

    public boolean isWritable() {
        return true;
    }

    public boolean isRemovable() {
        return true;
    }

    public String getOwnerName() {
        return "anyone";
    }

    public String getGroupName() {
        return "anygroup";
    }

    public int getLinkCount() {
        return 0;
    }

    public long getLastModified() {
        return r.getModifiedDate().getTime();
    }

    public boolean setLastModified(long arg0) {
        return false;
    }

    public long getSize() {
        if (r instanceof GetableResource) {
            GetableResource gr = (GetableResource) r;
            return gr.getContentLength();
        } else {
            return 0;
        }
    }

    public boolean mkdir() {
        log.debug("mkdir: " + this.path);
        if (parent != null) {
            if (parent instanceof MakeCollectionableResource) {
                MakeCollectionableResource mcr = (MakeCollectionableResource) parent;
                try {
                    r = mcr.createCollection(path.getName());
                    return true;
                } catch (NotAuthorizedException ex) {
                    log.debug("no authorised");
                    return false;
                } catch (ConflictException ex) {
                    log.debug("conflict");
                    return false;
                }
            } else {
                log.debug("parent does not support creating collection");
                return false;
            }
        } else {
            throw new RuntimeException("no parent");
        }
    }

    public boolean delete() {
        if (r instanceof DeletableResource) {
            DeletableResource dr = (DeletableResource) r;
            dr.delete();
            return true;
        } else {
            return false;
        }
    }

    public boolean move(FtpFile newFile) {
        if (r == null) {
            throw new RuntimeException("resource not saved yet");
        } else if (r instanceof MoveableResource) {
            MoveableResource src = (MoveableResource) r;
            MiltonFtpFile dest = (MiltonFtpFile) newFile;
            CollectionResource crDest;
            try {
                crDest = dest.getParent();
            } catch( FtpException ex ) {
                log.error( "move",ex);
                return false;
            }
            String newName = dest.path.getName();
            src.moveTo(crDest, newName);
            return true;
        } else {
            log.debug("not moveable: " + this.getName());
            return false;
        }
    }

    public List<FtpFile> listFiles() {
        List<FtpFile> list = new ArrayList<FtpFile>();
        if (r instanceof CollectionResource) {
            CollectionResource cr = (CollectionResource) r;
            for (Resource child : cr.getChildren()) {
                list.add(ftpFactory.wrap(path.child(child.getName()), child));
            }
        }
        return list;
    }

    public OutputStream createOutputStream(long offset) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream createInputStream(long offset) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private CollectionResource getParent() throws FtpException {
        if (parent == null) {
            parent = (CollectionResource) ftpFactory.getResource(path.getParent());
        }
        return parent;
    }
}
