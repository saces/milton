package com.ettrema.httpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import com.ettrema.httpclient.PropFindMethod.Response;

/**
 *
 * @author mcevoyb
 */
public class Folder extends Resource {

    private boolean childrenLoaded = false;
    private final List<Resource> list = new ArrayList<Resource>();
    final List<FolderListener> folderListeners = new ArrayList<FolderListener>();

    /**
     *  Special constructor for Host
     */
    Folder() {
        super();
    }

    public Folder( Folder parent, Response resp ) {
        super( parent, resp );
    }

    public Folder( Folder parent, String name ) {
        super( parent, name );
    }

    public void addListener( FolderListener l ) {
        for( Resource r : this.children() ) {
            l.onChildAdded( r.parent, r );
        }
        folderListeners.add( l );
    }

    @Override
    public File downloadTo( File destFolder, ProgressListener listener ) throws FileNotFoundException {
        File thisDir = new File( destFolder, this.name );
        thisDir.mkdir();
        for( Resource r : this.children() ) {
            r.downloadTo( thisDir, listener );
        }
        return thisDir;
    }

    public void flush() {
        System.out.println( "Folder: flush" );
        if( list != null ) {
            for( Resource r : list ) {
                notifyOnChildRemoved( r );
            }
            list.clear();
            childrenLoaded = false;
        }
        children();
    }

    public List<? extends Resource> children() {
        System.out.println( "Folder: children: " + list.size() + " - loaded: " + childrenLoaded );
        if( childrenLoaded ) return list;

        List<Response> responses = host().doPropFind( href(), 1 );
        childrenLoaded = true;
        if( responses != null ) {
            System.out.println( "  responses: " + responses.size() );
            for( Response resp : responses ) {
                if( !resp.href.equals( this.href() ) ) {
                    Resource r = Resource.fromResponse( this, resp );
                    list.add( r );
                    this.notifyOnChildAdded( r );
                }
            }
        } else {
            System.out.println( "  null responses" );
        }
        return list;
    }

    public void removeListener( FolderListener folderListener ) {
        this.folderListeners.remove( folderListener );
    }

    @Override
    public String toString() {
        return href() + " (is a folder)";
    }

    public void upload( File f ) {
        upload( f, null );
    }

    public void upload( File f, ProgressListener listener ) {
        if( f.isDirectory() ) {
            uploadFolder( f, listener );
        } else {
            uploadFile( f, listener );
        }
    }

    protected void uploadFile( File f, ProgressListener listener ) {
        NotifyingFileInputStream in = null;
        try {
            in = new NotifyingFileInputStream( f, listener );
            upload( f.getName(), in, f.length() );
            listener.onComplete( in.fileName );
        } catch( Throwable ex ) {
            System.out.println( "Failed to upload: " + f.getAbsolutePath() );
            throw new RuntimeException( f.getAbsolutePath(), ex );
        } finally {
            Utils.close( in );
        }
    }

    protected void uploadFolder( File folder, ProgressListener listener ) {
        Folder newFolder = createFolder( folder.getName() );
        for( File f : folder.listFiles() ) {
            System.out.println( "newFolder.href: " + newFolder.href() );
            newFolder.upload( f, listener );
        }
    }

    public com.ettrema.httpclient.File upload( String name, InputStream content, Long contentLength ) {
        children(); // ensure children are loaded
        String newUri = href() + name;
        String contentType = URLConnection.guessContentTypeFromName( name );
        host().doPut( newUri, content, contentLength, contentType );
        com.ettrema.httpclient.File child = new com.ettrema.httpclient.File( this, name, contentType, contentLength );
        com.ettrema.httpclient.Resource oldChild = this.child( child.name );
        if( oldChild != null ) {
            this.list.remove( oldChild );
        }
        this.list.add( child );
        notifyOnChildAdded( child );
        return child;
    }

    public Folder createFolder( String name ) {
        System.out.println( "Folder: createFolder: " + name );
        children(); // ensure children are loaded
        String newUri = href() + name;
        host().doMkCol( newUri );
        Folder child = new Folder( this, name );
        this.list.add( child );
        notifyOnChildAdded( child );
        return child;
    }

    public Resource child( String childName ) {
        for( Resource r : children() ) {
            if( r.name.equals( childName ) ) return r;
        }
        return null;
    }

    void notifyOnChildAdded( Resource child ) {
        System.out.println( "Folder: notifyOnChildAdded" );
        List<FolderListener> l2 = new ArrayList<FolderListener>( folderListeners );
        for( FolderListener l : l2 ) {
            FolderListener fol = (FolderListener) l;
            System.out.println( "  notifying: " + fol );
            fol.onChildAdded( this, child );
        }
    }

    void notifyOnChildRemoved( Resource child ) {
        System.out.println( "Folder: notifyOnChildRemoved: " + child.name );
        List<FolderListener> l2 = new ArrayList<FolderListener>( folderListeners );
        for( FolderListener l : l2 ) {
            FolderListener fol = (FolderListener) l;
            fol.onChildRemoved( this, child );
        }
    }

    private class NotifyingFileInputStream extends FileInputStream {

        final ProgressListener listener;
        final String fileName;
        long pos;
        long totalLength;
        long nextNotify;

        public NotifyingFileInputStream( File f, ProgressListener listener ) throws FileNotFoundException {
            super( f );
            this.listener = listener;
            this.totalLength = f.length();
            this.fileName = f.getAbsolutePath();
        }

        @Override
        public int read() throws IOException {
            increment( 1 );
            return super.read();
        }

        @Override
        public int read( byte[] b ) throws IOException {
            increment( b.length );
            return super.read( b );
        }

        @Override
        public int read( byte[] b, int off, int len ) throws IOException {
            increment( len );
            return super.read( b, off, len );
        }

        private void increment( int len ) {
            pos += len;
            if( pos >= nextNotify ) {
                notifyListener();
            }
        }

        void notifyListener() {
            if( totalLength <= 0 ) {
                System.out.println( "warning, zero length resource" );
                listener.onProgress( 100, fileName );
                nextNotify += pos;
            } else {
                nextNotify = pos + totalLength / 50;
                if( nextNotify > totalLength ) nextNotify = totalLength;

                int percent = (int) ( ( pos * 100 / totalLength ) );
                if( percent > 100 ) percent = 100;
                listener.onProgress( percent, fileName );
            }
        }
    }

    @Override
    public String href() {
        return super.href() + "/";
    }
}
