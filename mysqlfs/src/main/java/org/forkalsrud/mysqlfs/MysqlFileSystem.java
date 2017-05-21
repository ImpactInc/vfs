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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import javax.sql.DataSource;
import javax.xml.bind.DatatypeConverter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;


public class MysqlFileSystem extends FileSystem {

    private final MysqlFileSystemProvider fileSystemProvider;
    final String root;
    private final Charset CS = StandardCharsets.UTF_8;

    public MysqlFileSystem(MysqlFileSystemProvider fileSystemProvider, DataSource dataSource, String rootName) throws IOException {
        this.root = rootName;
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public FileSystemProvider provider() {
        return fileSystemProvider;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return Collections.singleton(new MysqlPath(this, "/".getBytes(CS)));
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return null;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return null;
    }

    @Override
    public Path getPath(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new MysqlPath(this, path.getBytes(CS));
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        return new GeneralPathMatcher(syntaxAndPattern);
    }


    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

}
