package com.impact.vfs.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Test;


/**
 * Created by knut on 2017/05/08.
 */
public class ReferenceTest {
    
    @Test
    public void testGetAttributesForNonExistentFile() {
        
        try {
            Path p = new File(System.getProperty("user.home"), "nonexistent").toPath();
            Files.readAttributes(p, BasicFileAttributes.class);
            fail("Should not exist");
        } catch (Exception e) {
            assertEquals(NoSuchFileException.class, e.getClass());
        }
    }
}
