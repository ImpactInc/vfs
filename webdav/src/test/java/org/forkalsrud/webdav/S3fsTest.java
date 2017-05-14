package org.forkalsrud.webdav;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Created by knut on 2017/05/09.
 */
public class S3fsTest {
    
    static Path root;
    
    @BeforeClass
    public static void mount() throws IOException {
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
            if ("eventrouting-test".equals(p.getFileName().toString())) {
                root = p;
                break;
            }
        }
    }
    static <T> T single(Iterable<T> all) {
        return all.iterator().next();
    }
    
    @AfterClass
    public static void umount() throws IOException {
        root.getFileSystem().close();
    }
    
    @Test
    public void testListFiles() throws IOException {
    
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    System.out.println(p.toString());
                } else {
                    System.out.println(p.toString() + "/");
                }
            }
        }
    }

}
