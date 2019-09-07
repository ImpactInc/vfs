package com.impact.vfs.examples;


import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.apache.jackrabbit.webdav.lock.SimpleLockManager;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SessionManager;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import com.impact.vfs.mysql.MysqlFileSystemProvider;
import com.impact.vfs.webdav.SimpleDavLocatorFactory;
import com.impact.vfs.webdav.SimpleDavResourceFactory;
import com.impact.vfs.webdav.SimpleDavSessionProvider;
import com.impact.vfs.webdav.SimpleWebdavServlet;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;

import com.impact.vfs.ftpd.Authenticator;
import com.impact.vfs.ftpd.Ftpd;


public class Main {
 
    Path root;
    DataSource dataSource;
    
    @Option(name = "--jdbcurl", usage = "JDBC URL, ex: jdbc:mysql://localhost/test")
    public String jdbcUrl = "jdbc:mysql://localhost/test";
    
    @Option(name = "--jdbcusername", aliases = { "--jdbcuser" }, usage = "JDBC username")
    public String jdbcUsername = "root";
    
    @Option(name = "--jdbcpassword", aliases = { "--jdbcpwd" }, usage = "JDBC password")
    public String jdbcPassword = "";
    
    @Option(name = "--rootdir", usage = "Use native file system, with specified directory as root")
    public String useFilesystemRoot;
    
    @Option(name = "--zip", usage = "Use a ZIP archive as root")
    public String zipFile;
    
    @Option(name = "--s3bucket", usage = "Use an AWS S3 bucket as root ()")
    public String s3Bucket;
    
    @Option(name = "--help", usage = "Print help message")
    public boolean help = false;

    public static void main(String... args) throws Exception {

        Main main = new Main();
        main.run(args);
    }


    public void run(String... args) throws Exception {
    
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            help = true;
        }
        if (help) {
            help = false;
            parser.printUsage(System.err);
            System.exit(1);
        }
        

        if (useFilesystemRoot != null) {
            root = mountFile(useFilesystemRoot);
        } else if (zipFile != null) {
            root = mountZip(zipFile);
        } else if (s3Bucket != null) {
            root = mountS3(s3Bucket);
        } else {
            dataSource = dbConnect();
            new DbMigration().ensureSchemaPresent("testroot");
            root = mountMysqlfs("testroot");
        }

        Server server = startWebServer();
        Ftpd ftpd = startFtpServer();
    
        System.out.println("Feel free to connect with a DAV client to http://localhost:8080/webdav");
        System.out.println("Feel free to connect with an FTP client to ftp://localhost:12121/");
        System.out.println("Hit Ctrl+C or type \"quit\"and hit return when done.");

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
        try {
            root.getFileSystem().close();
        } catch (UnsupportedOperationException e) {
            // ignore
        }
        if (dataSource != null) {
            dataSource.close();
        }
        System.out.println("halt");
    }
    
    
    

    
    
    Server startWebServer() throws Exception {

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
        SimpleDavLocatorFactory simpleLocatorFactory = new SimpleDavLocatorFactory("/webdav", root);

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
        return server;
    }
    
    
    
    static Path mountFile(String rootPath) {
        return new File(rootPath).toPath();
    }
    
    static Path mountZip(String zipPath) throws IOException {
        return single(FileSystems.newFileSystem(URI.create("jar:file:" + zipPath + "!/"), Collections.emptyMap()).getRootDirectories());
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
    
    
    public Ftpd startFtpServer() {
    
        Ftpd instance = new Ftpd();

        instance.initialize(12121, new Authenticator() {
    
            @Override
            public boolean authenticate(String name, String password) {
                return true;
            }
    
            @Override
            public Path getPhysicalRoot(String name) {
                return root;
            }
    
            @Override
            public Path getVirtualRoot(String name) {
                return root;
            }
    
        });
        
        Thread thread = new Thread(instance);
        thread.setDaemon(true);
        thread.start();
        return instance;
    }



    DataSource dbConnect() {
    
    
        if (jdbcUrl == null) {
            jdbcUrl = "jdbc:mysql://localhost/test";
        }
    
        if (jdbcUsername == null) {
            jdbcUsername = "root";
        }
        
        if (jdbcPassword == null) {
            jdbcPassword = "";
        }
        
        DataSource pool = new DataSource();
        pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
        pool.setUrl(jdbcUrl);
        pool.setUsername(jdbcUsername);
        pool.setPassword(jdbcPassword);
    
        Properties props = new Properties();
        props.setProperty("useSSL", "false");
        pool.setDbProperties(props);
        return pool;
    }
    
    Path mountMysqlfs(String rootName) {
        
        MysqlFileSystemProvider provider = new MysqlFileSystemProvider();
        provider.setDataSource(dataSource);
        
        return provider.getFileSystemRoot(rootName);
    }
    
    
    class DbMigration {
    
        JdbcTemplate tmpl = new JdbcTemplate(dataSource);
        
        boolean missingTable(String name) {
            List<Boolean> present = tmpl.query("show tables like ?",
                    (rs, rowNum) -> name.equalsIgnoreCase(rs.getString(1)), name);
            return present.isEmpty() || !present.stream().allMatch(p -> p);
        }
    
        
        void applyResource(String name) throws IOException {
    
            InputStream in = getClass().getResourceAsStream("/" + name);
            String statement = new String(FileCopyUtils.copyToByteArray(in), StandardCharsets.UTF_8);
            in.close();
            tmpl.update(statement);
        }
    

        long coalesce(List<Long> in) {
            if (in == null || in.isEmpty()) {
                return 0L;
            }
            return in.get(0).longValue();
        }

        void ensureSchemaPresent(String rootName) throws IOException {
            
            if (missingTable("direntry")) {
                System.out.println("Creating direntry");
                applyResource("mysqlfs-schema-direntry.sql");
            }
    
            if (missingTable("blocks")) {
                System.out.println("Creating blocks");
                applyResource("mysqlfs-schema-blocks.sql");
            }

            long rootId = coalesce(tmpl.queryForList("select id from direntry where parent = 0 and name = ?", Long.class, rootName));
            if (rootId == 0L) {
                System.out.println("Creating " + rootName);
                tmpl.update("insert into direntry set parent = 0, type = 'dir', name = ?, size = 0, ctime = now(), mtime = now(), atime= now()", rootName);
            }
            // We have to have a directory within the root, so that the WebDAV mount can access something
            // This is a limitation of the WebDAV front end
            String dirname = "demo";
            long demoId = coalesce(tmpl.queryForList("select id from direntry where parent = ? and name = ?", Long.class, rootId, dirname));
            if (demoId == 0L) {
                System.out.println("Creating " + dirname);
                tmpl.update("insert into direntry (parent, type, name, size, ctime, mtime, atime) "
                        + "select id, 'dir', ?, 0, now(), now(), now() from direntry where parent = 0 and name = ?", dirname, rootName);
            }
        }
    }
}
