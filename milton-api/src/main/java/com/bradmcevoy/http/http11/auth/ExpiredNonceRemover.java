package com.bradmcevoy.http.http11.auth;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import freenet.log.Logger;

import static java.util.concurrent.TimeUnit.*;

/**
 * Periodically checks a map of Nonce's to remove those which
 * have expired.
 *
 * The map should be a reference to the live map in use by a NonceProvider
 *
 * @author brad
 */
public class ExpiredNonceRemover implements Runnable {

    private static final int INTERVAL = 10;

    private final Map<UUID, Nonce> nonces;
    private final int nonceValiditySeconds;
    private final ScheduledExecutorService scheduler;

    public ExpiredNonceRemover( Map<UUID, Nonce> nonces, int nonceValiditySeconds ) {
        this.nonces = nonces;
        this.nonceValiditySeconds = nonceValiditySeconds;
        Logger.debug(this, "scheduling checks for expired nonces every " + INTERVAL + " seconds");
        scheduler = Executors.newScheduledThreadPool( 1 );
        scheduler.scheduleAtFixedRate( this, 10, INTERVAL, SECONDS );
    }

    public void run() {
        Iterator<UUID> it = nonces.keySet().iterator();
        while( it.hasNext() ) {
            UUID key = it.next();
            Nonce n = nonces.get( key );
            if( isExpired( n.getIssued())) {
                Logger.debug(this, "removing expired nonce: " + key);
                it.remove();
            }
        }
    }

    private boolean isExpired( Date issued ) {
        long dif = (System.currentTimeMillis() - issued.getTime()) / 1000;
        return dif > nonceValiditySeconds;
    }

}
