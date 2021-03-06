package com.ettrema.http.fs;

import com.bradmcevoy.http.LockInfo;
import com.bradmcevoy.http.LockResult;
import com.bradmcevoy.http.LockTimeout;
import com.bradmcevoy.http.LockToken;
import com.bradmcevoy.http.LockableResource;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import freenet.log.Logger;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keys on getUniqueID of the locked resource.
 *
 */
public class SimpleLockManager implements LockManager {

    /**
     * maps current locks by the file associated with the resource
     */
    Map<String, CurrentLock> locksByUniqueId;
    Map<String, CurrentLock> locksByToken;

    public SimpleLockManager() {
        locksByUniqueId = new HashMap<String, CurrentLock>();
        locksByToken = new HashMap<String, CurrentLock>();
    }

    public synchronized LockResult lock( LockTimeout timeout, LockInfo lockInfo, LockableResource r ) {
        LockToken currentLock = currentLock( r );
        if( currentLock != null ) {
            return LockResult.failed( LockResult.FailureReason.ALREADY_LOCKED );
        }

        LockToken newToken = new LockToken( UUID.randomUUID().toString(), lockInfo, timeout );
        CurrentLock newLock = new CurrentLock( r.getUniqueId(), newToken, lockInfo.owner, lockInfo.lockedByUser );
        locksByUniqueId.put( r.getUniqueId(), newLock );
        locksByToken.put( newToken.tokenId, newLock );
        return LockResult.success( newToken );
    }

    public synchronized LockResult refresh( String tokenId, LockableResource resource ) {
        CurrentLock curLock = locksByToken.get( tokenId );
        curLock.token.setFrom( new Date() );
        return LockResult.success( curLock.token );
    }

    public synchronized void unlock( String tokenId, LockableResource r ) throws NotAuthorizedException {
        LockToken lockToken = currentLock( r );
        if( lockToken == null ) {
            Logger.debug(this, "not locked" );
            return;
        }
        if( lockToken.tokenId.equals( tokenId ) ) {
            removeLock( lockToken );
        } else {
            throw new NotAuthorizedException( r );
        }
    }

    private LockToken currentLock( LockableResource resource ) {
        CurrentLock curLock = locksByUniqueId.get( resource.getUniqueId() );
        if( curLock == null ) return null;
        LockToken token = curLock.token;
        if( token.isExpired() ) {
            removeLock( token );
            return null;
        } else {
            return token;
        }
    }

    private void removeLock( LockToken token ) {
        Logger.debug(this, "removeLock: " + token.tokenId );
        CurrentLock currentLock = locksByToken.get( token.tokenId );
        if( currentLock != null ) {
            locksByUniqueId.remove( currentLock.id );
            locksByToken.remove( currentLock.token.tokenId );
        } else {
            Logger.warning(this, "couldnt find lock: " + token.tokenId );
        }
    }

    public LockToken getCurrentToken( LockableResource r ) {
        CurrentLock lock = locksByUniqueId.get( r.getUniqueId() );
        if( lock == null ) return null;
        LockToken token = new LockToken();
        token.info = new LockInfo( LockInfo.LockScope.EXCLUSIVE, LockInfo.LockType.WRITE, lock.owner, LockInfo.LockDepth.ZERO );
        token.info.lockedByUser = lock.lockedByUser;
        token.timeout = lock.token.timeout;
        token.tokenId = lock.token.tokenId;
        return token;
    }

    class CurrentLock {

        final String id;
        final LockToken token;
        final String owner;
        final String lockedByUser;

        public CurrentLock( String id, LockToken token, String owner, String lockedByUser ) {
            this.id = id;
            this.token = token;
            this.owner = owner;
            this.lockedByUser = lockedByUser;
        }
    }
}
