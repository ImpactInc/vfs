package org.forkalsrud.webdav;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.lock.LockManager;

/**
 * Created by knut on 15/02/05.
 */
public class SimpleDavResourceFactory implements DavResourceFactory {

    LockManager lockManager;

    @Override
    public DavResource createResource(DavResourceLocator locator, DavServletRequest request, DavServletResponse response) throws DavException {
        return createResource(locator, request.getDavSession());
    }

    @Override
    public DavResource createResource(DavResourceLocator locator, DavSession session) throws DavException {
        return new SimpleDavResource((SimpleResourceLocator)locator, this,  lockManager, session);
    }

    public void setLockManager(LockManager mgr) {
        this.lockManager = mgr;
    }
}
