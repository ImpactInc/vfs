package com.impact.vfs.webdav;

import javax.servlet.http.HttpSession;

import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;

/**
 * Created by knut on 15/02/05.
 */
public class SimpleDavSessionProvider implements DavSessionProvider {

    public static final String ATTR = SimpleDavSession.class.getCanonicalName();

    @Override
    public boolean attachSession(WebdavRequest request) {

        HttpSession httpSession = request.getSession();
        SimpleDavSession davSession = (SimpleDavSession)httpSession.getAttribute(ATTR);
        if (davSession == null) {
            davSession = new SimpleDavSession();
            httpSession.setAttribute(ATTR, davSession);
        }
        request.setDavSession(davSession);
        return true;
    }

    @Override
    public void releaseSession(WebdavRequest request) {
        HttpSession httpSession = request.getSession(false);
        if (httpSession == null) {
            return;
        }
        SimpleDavSession davSession = (SimpleDavSession)httpSession.getAttribute(ATTR);
        if (davSession == null) {
            return;
        }
        httpSession.removeAttribute(ATTR);
        request.setDavSession(null);
    }

}
