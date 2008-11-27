package com.bradmcevoy.http;

import com.bradmcevoy.common.Path;


public class TResourceFactory implements ResourceFactory {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TResourceFactory.class);
    
    public static final TFolderResource ROOT = new TFolderResource((TFolderResource)null,"localhost:8084/MiltonTestWeb");
    
    static {
        TFolderResource folder;
        TResource file;
        file = new TResource(ROOT,"index.html");
        folder = new TFolderResource(ROOT,"folder1");
        folder = new TFolderResource(ROOT,"folder2");
        folder = new TFolderResource(ROOT,"folder3");
        file = new TResource(folder,"index.html");
        file = new TResource(folder,"stuff.html");
        folder = new TFolderResource(folder,"subfolder1");
        file = new TResource(folder,"index.html");
        folder = new TFolderResource(ROOT,"secure");
        folder.setSecure("test","pwd");
        file = new TResource(folder,"index.html");
        file.setSecure("test","pwd");
    }
    
    
    public Resource getResource(String host, String url) {
        log.debug("getResource: url: " + url );
        Path path = Path.path(url);
        return find(path);
    }

    private TResource find(Path path) {
        if( path==null || path.getParent().isRoot() ) return ROOT;
        TResource r = find(path.getParent());
        if( r == null ) return null;
        if( r instanceof TFolderResource ) {
            TFolderResource folder = (TFolderResource)r;
            for( Resource rChild : folder.getChildren() ) {
                TResource r2 = (TResource) rChild;
                if( r2.getName().equals(path.getName())) return r2;
            }
        }
        log.debug("not found: " + path);
        return null;
    }

    public String getSupportedLevels() {
        return "1";
    }

}
