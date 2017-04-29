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
package org.forkalsrud.mysqlfs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Properties;

import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;


public class MysqlFileSystemProviderTest {

    @Test
    public void testPathsGetAndDirectoryStream() throws IOException {
    
        final DriverManagerDataSource ds = new DriverManagerDataSource("jdbc:mysql://localhost/mysqlfs", "root", "");
        Properties props = new Properties();
        props.setProperty("useSSL", "false");
        props.setProperty("emulateLocators", "true");
        ds.setConnectionProperties(props);
        
        MysqlFileSystemProvider provider = new MysqlFileSystemProvider();
        provider.setDataSource(ds);
    
        FileSystem fs = provider.newFileSystem(URI.create("mysqlfs:testroot/"), null);
        Path root = single(fs.getRootDirectories());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    System.out.println(p.toString());
                    
                    Files.copy(p, System.out);
                } else {
                    System.out.println(p.toString() + "/");
                }
            }
        }

    }

    <T> T single(Iterable<T> all) {
        return all.iterator().next();
    }
}
