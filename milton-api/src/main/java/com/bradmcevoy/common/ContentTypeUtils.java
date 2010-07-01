package com.bradmcevoy.common;

import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import freenet.log.Logger;

import java.io.File;
import java.util.Collection;

/**
 *
 * @author brad
 */
public class ContentTypeUtils {

    public static String findContentTypes( String name ) {
        Collection mimeTypes = MimeUtil.getMimeTypes( name );
        return buildContentTypeText(mimeTypes);
    }

    public static String findContentTypes( File file ) {
        Collection mimeTypes = null;
        try {
            mimeTypes = MimeUtil.getMimeTypes( file );
        } catch( MimeException e ) {
            Logger.warning(ContentTypeUtils.class, "exception retrieving content type for file: " + file.getAbsolutePath(),e);
            return "application/binary";
        }
        return buildContentTypeText(mimeTypes);
    }

    public static String findAcceptableContentType(String mime, String preferredList) {
        MimeType mt = MimeUtil.getPreferedMimeType(preferredList, mime);
        return mt.toString();

    }

    private static String buildContentTypeText( Collection mimeTypes ) {
        StringBuilder sb = null;
        for( Object o : mimeTypes ) {
            MimeType mt = (MimeType) o;
            if( sb == null ) {
                sb = new StringBuilder();
            } else {
                sb.append( "," );
            }
            sb.append( mt.toString() );
        }
        if( sb == null ) {
            return "";
        } else {
            return sb.toString();
        }
    }
}
