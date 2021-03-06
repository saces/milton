package com.bradmcevoy.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import freenet.log.Logger;

public class LockInfo {

    public enum LockScope {

        NONE,
        SHARED,
        EXCLUSIVE
    }

    public enum LockType {

        READ,
        WRITE
    }

    public enum LockDepth {

        ZERO,
        INFINITY
    }

    public static LockInfo parseLockInfo( Request request ) throws IOException, FileNotFoundException, SAXException {
        InputStream in = request.getInputStream();

        XMLReader reader = XMLReaderFactory.createXMLReader();
        LockInfoSaxHandler handler = new LockInfoSaxHandler();
        reader.setContentHandler( handler );
        reader.parse( new InputSource( in ) );
        LockInfo info = handler.getInfo();
        info.depth = LockDepth.INFINITY; // todo
        info.lockedByUser = null;
        if( request.getAuthorization() != null ) {
            info.lockedByUser = request.getAuthorization().getUser();
        }
        if( info.lockedByUser == null ) {
            Logger.warning(LockInfo.class, "resource is being locked with a null user. This won't really be locked at all..." );
        }
        Logger.debug(LockInfo.class, "parsed lock info: " + info );
        return info;

    }
    public LockScope scope;
    public LockType type;
    /**
     * Contact details for the lock owner. E.g. phone number, website address, or
     * email address. Generally not used. Can be ignored.
     */
    public String owner;
    /**
     * The name of the user who has locked this resource.
     */
    public String lockedByUser;
    public LockDepth depth;

    public LockInfo( LockScope scope, LockType type, String owner, LockDepth depth ) {
        this.scope = scope;
        this.type = type;
        this.owner = owner;
        this.depth = depth;
    }

    public LockInfo() {
    }

    @Override
    public String toString() {
        return "scope: " + scope.name() + ", type: " + type.name() + ", owner: " + owner + ", depth:" + depth;
    }
}
