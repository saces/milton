package com.ettrema.http.caldav.demo;

import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.bradmcevoy.io.StreamUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TFolderResource extends TResource implements PutableResource, MakeCollectionableResource {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TResource.class );
    ArrayList<TResource> children = new ArrayList<TResource>();

    public TFolderResource( TFolderResource parent, String name ) {
        super( parent, name );
        log.debug( "created new folder: " + name );
    }

    @Override
    protected Object clone( TFolderResource newParent ) {
        TFolderResource newFolder = new TFolderResource( newParent, name );
        for( Resource child : parent.getChildren() ) {
            TResource res = (TResource) child;
            res.clone( newFolder ); // will auto-add to folder
        }
        return newFolder;
    }

    @Override
    public Long getContentLength() {
        return null;
    }

    public String getContentType() {
        return null;
    }

    @Override
    public String checkRedirect( Request request ) {
        return null;
    }

    public List<? extends Resource> getChildren() {
        return children;
    }


    static ByteArrayOutputStream readStream( final InputStream in ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamUtils.readTo( in, bos );
        return bos;
    }

    public CollectionResource createCollection( String newName ) {
        log.debug( "createCollection: " + newName );
        TFolderResource r = new TFolderResource( this, newName );
        return r;
    }

    public Resource createNew( String newName, InputStream inputStream, Long length, String contentType ) throws IOException {
        ByteArrayOutputStream bos = readStream( inputStream );
        log.debug( "createNew: " + bos.size() + " - name: " + newName + " current child count: " + this.children.size() );
        TResource r = new TBinaryResource( this, newName, bos.toByteArray(), contentType );
        log.debug( "new child count: " + this.children.size() );
        return r;
    }

    public Resource child( String childName ) {
        for( Resource r : getChildren() ) {
            if( r.getName().equals( childName ) ) return r;
        }
        return null;
    }

    public void sendContent( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException, NotAuthorizedException, BadRequestException {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public String getContentType( String accepts ) {
        return "";
    }
}
