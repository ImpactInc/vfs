package org.forkalsrud.webdav;

import java.util.HashSet;

import org.apache.jackrabbit.webdav.DavSession;

/**
 * Created by knut on 15/02/05.
 */
public class SimpleDavSession implements DavSession {

    HashSet<String> locks = new HashSet<String>();
    HashSet<Object> refs = new HashSet<Object>();

    @Override
    public void addReference(Object reference) {
        refs.add(reference);
    }

    @Override
    public void removeReference(Object reference) {
        refs.remove(reference);
    }

    @Override
    public void addLockToken(String token) {
        locks.add(token);
    }

    @Override
    public String[] getLockTokens() {
        return locks.toArray(new String[locks.size()]);
    }

    @Override
    public void removeLockToken(String token) {
        locks.remove(token);
    }
}
