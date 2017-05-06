package org.forkalsrud.webdav;


import java.io.File;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;


public class Main {
 
    public static void main(String... args) throws Exception {
        final Server server = new Server(8080);

        RequestLogHandler requestLogHandler = new RequestLogHandler();
        RequestLog accessLog = new NCSARequestLog();
        requestLogHandler.setRequestLog(accessLog);

        server.setHandler(requestLogHandler);

        final ServletHandler servletHandler = new ServletHandler();

        HashSessionIdManager hashSessionIdManager = new HashSessionIdManager();
        SessionHandler sessionHandler = new SessionHandler();
        SessionManager sessionManager = new HashSessionManager();
        sessionManager.setSessionIdManager(hashSessionIdManager);
        sessionHandler.setSessionManager(sessionManager);
        sessionHandler.setHandler(servletHandler);
        sessionHandler.setServer(server);
        server.setSessionIdManager(hashSessionIdManager);


        ServletContextHandler context = new ServletContextHandler(requestLogHandler, "/", sessionHandler, null, servletHandler, null, ServletContextHandler.SESSIONS);


        SimpleWebdavServlet webdavServlet = new SimpleWebdavServlet();
        SimpleDavSessionProvider sessionProvider = new SimpleDavSessionProvider();

        String fsRoot = new File(System.getProperty("user.home"), "tmp").getAbsolutePath();
        SimpleDavLocatorFactory simpleLocatorFactory = new SimpleDavLocatorFactory("/webdav", fsRoot);

        SimpleLockManager simpleLockManager = new SimpleLockManager();

        SimpleDavResourceFactory simpleResourceFactory = new SimpleDavResourceFactory();
        simpleResourceFactory.setLockManager(simpleLockManager);

        webdavServlet.setDavSessionProvider(sessionProvider);
        webdavServlet.setLocatorFactory(simpleLocatorFactory);
        webdavServlet.setResourceFactory(simpleResourceFactory);


        final ServletHolder holder = new ServletHolder("webdav", webdavServlet);
        context.addServlet(holder, "/webdav/*");

        webdavServlet.init(new ServletConfig() {
            @Override
            public String getServletName() {
                return holder.getName();
            }

            @Override
            public ServletContext getServletContext() {
                return servletHandler.getServletContext();
            }

            @Override
            public String getInitParameter(String name) {
                return holder.getInitParameter(name);
            }

            @Override
            public Enumeration getInitParameterNames() {
                return holder.getInitParameterNames();
            }
        });

        server.start();
        server.join();
    }


    
}

