package com.ettrema.http.fs;

import com.bradmcevoy.common.ContentTypeUtils;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CopyableResource;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PropPatchableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.webdav.PropPatchHandler.Fields;

import freenet.log.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 *
 */
public class FsFileResource extends FsResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource, PropPatchableResource {

    /**
     *
     * @param host - the requested host. E.g. www.mycompany.com
     * @param factory
     * @param file
     */
    public FsFileResource( String host, FileSystemResourceFactory factory, File file ) {
        super( host, factory, file );
    }

    public Long getContentLength() {
        return file.length();
    }

    public String getContentType( String preferredList ) {
        String mime = ContentTypeUtils.findContentTypes( this.file );
        return ContentTypeUtils.findAcceptableContentType( mime, preferredList );
    }

    public String checkRedirect( Request arg0 ) {
        return null;
    }

    public void sendContent( OutputStream out, Range range, Map<String, String> params, String contentType ) throws IOException {
        Logger.debug(this, "send content: " + file.getAbsolutePath() );
        FileInputStream in = null;
        try {
            in = new FileInputStream( file );
            //        if( range != null ) {
            //            long start = range.getStart();
            //            if( start > 0 ) in.skip(start);
            //            long finish = range.getFinish();
            //            if( finish > 0 ) {
            //                StreamToStream.readTo(in, out);
            //            }
            //        } else {
            int bytes = IOUtils.copy( in, out );
            Logger.debug(this, "wrote bytes:  " + bytes );
            out.flush();
            //        }
        } finally {
            IOUtils.closeQuietly( in );
        }
    }

    /** @{@inheritDoc} */
    public Long getMaxAgeSeconds( Auth auth ) {
        return factory.maxAgeSeconds( this );
    }

    /** @{@inheritDoc} */
    @Override
    protected void doCopy( File dest ) {
        try {
            FileUtils.copyFile( file, dest );
        } catch( IOException ex ) {
            throw new RuntimeException( "Failed doing copy to: " + dest.getAbsolutePath(), ex );
        }
    }

    @Deprecated
    public void setProperties( Fields fields ) {
        // MIL-50
        // not implemented. Just to keep MS Office sweet
    }
}
