package org.forkalsrud.mysqlfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Test;


/**
 * Created by knut on 2017/05/08.
 */
public class ReferenceTest {
    
    @Test
    public void testGetAttributesForNonExistentFile() throws IOException {
        
        Path p = new File(System.getProperty("user.home"), "nonexistent").toPath();
        BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
        assertFalse(attrs.isDirectory());
    }
}
