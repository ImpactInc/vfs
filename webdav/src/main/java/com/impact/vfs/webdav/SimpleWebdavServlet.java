package org.forkalsrud.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jackrabbit.webdav.*;
import org.apache.jackrabbit.webdav.io.InputContext;
import org.apache.jackrabbit.webdav.io.InputContextImpl;
import org.apache.jackrabbit.webdav.server.AbstractWebdavServlet;


@SuppressWarnings("serial")
public class SimpleWebdavServlet extends AbstractWebdavServlet {
    
    DavSessionProvider sessionProvider;
    DavLocatorFactory locatorFactory;
    DavResourceFactory resourceFactory;
    

    void dumpHeaders(HttpServletRequest request) {
    
        PrintStream out = System.out;
        out.println(request.getMethod() + " " + request.getRequestURI());
        Enumeration<String> en = request.getHeaderNames();
        while (en.hasMoreElements()) {
        
            String header = en.nextElement();
            String value = request.getHeader(header);
        
            out.println(header + ": " + value);
        }
        out.println();
    }

    /*
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        dumpHeaders(request);
        super.service(request, response);
        
    }
    */
    
    @Override
    protected void doPut(WebdavRequest request, WebdavResponse response,
            DavResource resource) throws IOException, DavException {
        dumpHeaders(request);
        request.setAttribute(request.getMethod(), Boolean.TRUE);
        super.doPut(request, response, resource);
    }

    @Override
    protected void doMkCol(WebdavRequest request, WebdavResponse response,
            DavResource resource) throws IOException, DavException {
        dumpHeaders(request);
        request.setAttribute(request.getMethod(), Boolean.TRUE);
        super.doMkCol(request, response, resource);
    }
    
    @Override
    protected InputContext getInputContext(DavServletRequest request, InputStream in) {
        final InputContext delegate = super.getInputContext(request, in);
        return (InputContext)Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[] { InputContext.class },
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("getProperty".equals(method.getName()) && "METHOD".equals(args[0])) {
                            return request.getMethod();
                        }
                        return method.invoke(delegate, args);
                    }
                });
    }


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
