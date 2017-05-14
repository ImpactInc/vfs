package org.forkalsrud.webdav;


import java.io.*;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.forkalsrud.mysqlfs.MysqlFileSystemProvider;


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

        // Path fsRoot = mountFile(new File(System.getProperty("user.home"), "tmp").getAbsolutePath());
        // Path fsRoot = mountMysqlfs("testroot");
        // Path fsRoot = mountS3("eventrouting-test");
        Path fsRoot = mountZip(new File(System.getProperty("user.home"), "demo.zip").getAbsolutePath());


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
    
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String cmd;
        outer:
        while ((cmd = in.readLine()) != null) {
            switch (cmd) {
                case "quit":
                    break outer;
                default:
                    System.out.println("Not understood: " + cmd);
            }
        }
        System.out.println("shutting down");
        server.stop();
        server.join();
        System.out.println("unmounting file system");
        fsRoot.getFileSystem().close();
        System.out.println("halt");
    }
    
    
    static Path mountFile(String rootPath) throws IOException {
        return new File(rootPath).toPath();
    }
    
    static Path mountZip(String zipPath) throws IOException {
        return single(FileSystems.newFileSystem(URI.create("jar:file:" + zipPath + "!/"), Collections.emptyMap()).getRootDirectories());
    }


    static Path mountMysqlfs(String rootName) throws IOException {
        DataSource pool = new DataSource();
        pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
        pool.setUrl("jdbc:mysql://localhost/mysqlfs");
        pool.setUsername("root");
        pool.setPassword("");
    
        Properties props = new Properties();
        props.setProperty("useSSL", "false");
        pool.setDbProperties(props);
    
        MysqlFileSystemProvider provider = new MysqlFileSystemProvider();
        provider.setDataSource(pool);
    
        FileSystem fs = provider.newFileSystem(URI.create("mysqlfs:" + rootName + "/"), null);
        return single(fs.getRootDirectories());
    }

    static Path mountS3(String bucketName) throws IOException {
        Properties props = new Properties();
        Reader r = new FileReader(new File(System.getProperty("user.home"), ".s3fs"));
        props.load(r);
        r.close();
        HashMap<String, Object> env = new HashMap<>();
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            env.put(String.valueOf(e.getKey()), e.getValue());
        }
        FileSystem fs = FileSystems.newFileSystem(URI.create("s3:///"), env);
        for (Path p : fs.getRootDirectories()) {
            if (bucketName.equals(p.getFileName().toString())) {
                return p;
            }
        }
        throw new NoSuchFileException(bucketName);
    }


    static <T> T single(Iterable<T> all) {
        return all.iterator().next();
    }
    
}
