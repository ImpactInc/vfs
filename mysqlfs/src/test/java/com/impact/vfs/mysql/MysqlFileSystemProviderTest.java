/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.impact.vfs.mysql;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;


public class MysqlFileSystemProviderTest {

    static Path root;
    static SingleConnectionDataSource pool;
    
    
    @BeforeClass
    public static void mount() {
        
        pool = new SingleConnectionDataSource();
        pool.setDriverClassName("com.mysql.cj.jdbc.Driver");
        pool.setUrl("jdbc:mysql://localhost/test");
        pool.setUsername("root");
        pool.setPassword("");
    
        Properties props = new Properties();
        props.setProperty("useSSL", "false");
        pool.setConnectionProperties(props);
    
        MysqlFileSystemProvider provider = new MysqlFileSystemProvider();
        provider.setDataSource(pool);
        
        root = provider.getFileSystemRoot("testroot");
    }
    

    @AfterClass
    public static void umount() throws IOException {
        root.getFileSystem().close();
        pool.destroy();
    }


    @Test
    public void testPathsGetAndDirectoryStream() throws IOException {
    
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    System.out.println(p.toString());
                    
                    if (p.toString().endsWith(".txt")) {
                        Files.copy(p, System.out);
                    }
                } else {
                    System.out.println(p.toString() + "/");
                }
            }
        }
    }

    
    @Test
    public void testWriteSmallFile() throws IOException {
    
        assertEquals(3263, lengthOfwrittenFile("elg.png"));
    }
    
    @Test
    public void testWriteLargeFile() throws IOException {
    
        assertEquals(15150, lengthOfwrittenFile("gravatar.jpeg"));
    }
    
    public int lengthOfwrittenFile(String name) throws IOException {
        
        InputStream in = getClass().getResourceAsStream("/" + name);
        
        Path dst = root.resolve(name);
        Files.copy(in, dst);
        in.close();
        
        return (int)Files.size(dst);
    }
}
