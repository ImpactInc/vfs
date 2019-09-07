package com.impact.vfs.ftpd;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;


/**
 * Created by knut on 2017/05/21.
 */
public class CurrentInfoTest {

    CurrentInfo info = new CurrentInfo(new Global());
    Path root;
    
    @Before
    public void setUp() {
        root = new File(System.getProperty("java.io.tmpdir")).toPath();
        info.setPhysRoot(root);
        info.setCwd(root.resolve("user"));
    }
    
    @Test
    public void virtToPhys() throws Exception {

        assertEquals(root.resolve("some/dir"), info.virtToPhys("/some/dir"));
        assertEquals(root.resolve("user/dir"), info.virtToPhys("dir"));
        assertEquals(root.resolve("dir"), info.virtToPhys("/some/../dir"));
    }
    
    @Test
    public void createAbsolutePath() throws Exception {
    
        assertEquals("user", info.createAbsolutePath("."));
        assertEquals("", info.createAbsolutePath(".."));
        assertEquals("", info.createAbsolutePath("../.."));
        assertEquals("", info.createAbsolutePath("~"));
        assertEquals("dir", info.createAbsolutePath("subdir/~/dir"));
    }
    
}
