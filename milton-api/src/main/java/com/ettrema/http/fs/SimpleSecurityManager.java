package com.ettrema.http.fs;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.http11.auth.DigestGenerator;
import com.bradmcevoy.http.http11.auth.DigestResponse;

import freenet.log.Logger;

import java.util.Map;

/**
 * Has a realm and a map where the keys are user names and the values are
 * passwords
 *
 * @author brad
 */
public class SimpleSecurityManager implements com.bradmcevoy.http.SecurityManager{

    private String realm;
    private Map<String,String> nameAndPasswords;

    public SimpleSecurityManager() {
    }

    public SimpleSecurityManager( String realm, Map<String,String> nameAndPasswords ) {
        this.realm = realm;
        this.nameAndPasswords = nameAndPasswords;
    }

    public Object getUserByName( String name ) {
        String actualPassword = nameAndPasswords.get( name );
        if( actualPassword != null ) return name;
        return null;
    }



    public Object authenticate( String user, String password ) {
        Logger.debug(this, "authenticate: " + user + " - " + password);
        // user name will include domain when coming form ftp. we just strip it off
        if( user.contains( "@")) {
            user = user.substring( 0, user.indexOf( "@"));
        }
        String actualPassword = nameAndPasswords.get( user );
        if( actualPassword == null ) {
            Logger.debug(this, "user not found: " + user);
            return null;
        } else {
            boolean ok;
            if( actualPassword == null ) {
                ok = password == null || password.length()==0;
            } else {
                ok = actualPassword.equals( password);
            }
            return ok ? user : null;
        }
    }

    public Object authenticate( DigestResponse digestRequest ) {
        DigestGenerator dg = new DigestGenerator();
        String actualPassword = nameAndPasswords.get( digestRequest.getUser() );
        String serverResponse = dg.generateDigest( digestRequest, actualPassword );
        String clientResponse = digestRequest.getResponseDigest();

        Logger.debug(this, "server resp: " + serverResponse );
        Logger.debug(this, "given response: " + clientResponse );

        if( serverResponse.equals( clientResponse ) ) {
            return "ok";
        } else {
            return null;
        }
    }



    public boolean authorise( Request request, Method method, Auth auth, Resource resource ) {
        Logger.debug(this, "authorise");
        return auth != null && auth.getTag() != null;
    }

    public String getRealm(String host) {
        return realm;
    }

    /**
     * @param realm the realm to set
     */
    public void setRealm( String realm ) {
        this.realm = realm;
    }

    public void setNameAndPasswords( Map<String, String> nameAndPasswords ) {
        this.nameAndPasswords = nameAndPasswords;
    }


//    public MiltonUser getUserByName( String name, String domain ) {
//        Logger.debug(this, "getUserByName: " + name + " - " + domain);
//        String actualPassword = nameAndPasswords.get( name );
//        if( actualPassword == null ) return null;
//        return new MiltonUser( name, name, domain );
//    }
}
