package com.bradmcevoy.http.http11.auth;

import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;

import freenet.log.Logger;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A very simple nonce provide that users a map to store issued nonces.
 *
 * If the map is constructed internally it will be a ConcurrentHashMap, which
 * will restrict the application to a single machine, and nonces will not
 * be preserved across restarts.
 *
 * To improve reliability and scalability provide an alternative map implementation.
 * For example, it could be a cluster
 * aware map which synchonrises across a cluster. Or it could be a map which
 * persists entries to a database or file.
 *
 * @author brad
 */
public class SimpleMemoryNonceProvider implements NonceProvider {

    private final int nonceValiditySeconds;
    private Map<UUID, Nonce> nonces;
    private final ExpiredNonceRemover remover;
    private boolean enableNonceCountChecking;



    public SimpleMemoryNonceProvider( int nonceValiditySeconds ) {
        this.nonces = new ConcurrentHashMap<UUID, Nonce>();
        this.nonceValiditySeconds = nonceValiditySeconds;
        this.remover = new ExpiredNonceRemover( nonces, nonceValiditySeconds );
        Logger.debug(this, "created" );
    }

    public SimpleMemoryNonceProvider( int nonceValiditySeconds, ExpiredNonceRemover remover ) {
        this(nonceValiditySeconds,remover, new ConcurrentHashMap<UUID, Nonce>());
    }

    public SimpleMemoryNonceProvider( int nonceValiditySeconds, ExpiredNonceRemover remover, Map<UUID, Nonce> nonces ) {
        this.nonces = nonces;
        this.nonceValiditySeconds = nonceValiditySeconds;
        this.remover = remover;
    }

    public SimpleMemoryNonceProvider( int nonceValiditySeconds, Map<UUID, Nonce> nonces ) {
        this.nonces = nonces;
        this.nonceValiditySeconds = nonceValiditySeconds;
        this.remover = new ExpiredNonceRemover( nonces, nonceValiditySeconds );
    }

    public Nonce createNonceObject( Resource resource, Request request ) {
        UUID id = UUID.randomUUID();
        Date now = new Date();
        Nonce n = new Nonce( id, now );
        nonces.put( n.getValue(), n );
        return n;
    }

    public String createNonce( Resource resource, Request request ) {
        return createNonceObject(resource, request ).getValue().toString();
    }

    public NonceValidity getNonceValidity( String nonce, Long nc ) {
        Logger.debug(this, "getNonceValidity: " + nonce );
        UUID value = null;
        try {
            value = UUID.fromString( nonce );
        } catch( Exception e ) {
            Logger.debug(this, "couldnt parse nonce" );
            return NonceValidity.INVALID;
        }
        Nonce n = nonces.get( value );
        if( n == null ) {
            Logger.debug(this, "not found in map of size: " + nonces.size() );
            return NonceValidity.INVALID;
        } else {
            if( isExpired( n.getIssued() ) ) {
                Logger.debug(this, "nonce has expired" );
                return NonceValidity.EXPIRED;
            } else {
                if( nc == null ) {
                    Logger.debug(this, "nonce ok" );
                    return NonceValidity.OK;
                } else {
                    if( enableNonceCountChecking && nc <= n.getNonceCount() ) {
                        Logger.warning(this, "nonce-count was not greater then previous, possible replay attack. new: " + nc + " old:" + n.getNonceCount() );
                        return NonceValidity.INVALID;
                    } else {
                        Logger.debug(this, "nonce and nonce-count ok" );
                        Nonce newNonce = n.increaseNonceCount( nc );
                        nonces.put( newNonce.getValue(), newNonce );
                        return NonceValidity.OK;
                    }
                }
            }
        }
    }

    private boolean isExpired( Date issued ) {
        long dif = ( System.currentTimeMillis() - issued.getTime() ) / 1000;
        return dif > nonceValiditySeconds;
    }

    /**
     * IE seems to send nc (nonce count) parameters out of order. To correctly
     * implement checking we need to record which nonces have been sent, and not
     * assume they will be sent in a monotonically increasing sequence.
     *
     * The quick fix here is to disable checking of the nc param, since other
     * common servers seem to do so to.
     *
     * Note that this will allow replay attacks.
     *
     * @return
     */
    public boolean isEnableNonceCountChecking() {
        return enableNonceCountChecking;
    }

    public void setEnableNonceCountChecking( boolean enableNonceCountChecking ) {
        this.enableNonceCountChecking = enableNonceCountChecking;
    }
}
