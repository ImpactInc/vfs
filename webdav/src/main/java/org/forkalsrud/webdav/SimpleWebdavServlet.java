package org.forkalsrud.webdav;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;


@SuppressWarnings("serial")
public class SimpleWebdavServlet extends AbstractWebdavServlet {

	DavSessionProvider sessionProvider;
	DavLocatorFactory locatorFactory;
	DavResourceFactory resourceFactory;


	@Override
	protected boolean isPreconditionValid(WebdavRequest request, DavResource resource) {
		return true;
	}

	@Override
	public DavSessionProvider getDavSessionProvider() {
		return sessionProvider;
	}

	@Override
	public void setDavSessionProvider(DavSessionProvider davSessionProvider) {
		this.sessionProvider = davSessionProvider;
	}

	@Override
	public DavLocatorFactory getLocatorFactory() {
		return locatorFactory;
	}

	@Override
	public void setLocatorFactory(DavLocatorFactory locatorFactory) {
		this.locatorFactory = locatorFactory;
	}

	@Override
	public DavResourceFactory getResourceFactory() {
		return resourceFactory;
	}

	@Override
	public void setResourceFactory(DavResourceFactory resourceFactory) {
		this.resourceFactory = resourceFactory;
	}



}
