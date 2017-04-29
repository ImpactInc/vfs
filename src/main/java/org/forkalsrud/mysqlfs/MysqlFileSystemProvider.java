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
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.sql.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;


public class MysqlFileSystemProvider extends FileSystemProvider {

    final Map<String, MysqlFileSystem> fileSystems = new HashMap<>();
    private JdbcTemplate tmpl;

    public void setDataSource(DataSource ds) {
        this.tmpl = new JdbcTemplate(ds);
    }

    @Override
    public String getScheme() {
        return "mysqlfs";
    }

    @Override
    public MysqlFileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        synchronized (fileSystems) {
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            int i = schemeSpecificPart.indexOf("/");
            if (i >= 0) {
                schemeSpecificPart = schemeSpecificPart.substring(0, i);
            }
            MysqlFileSystem fileSystem = fileSystems.get(schemeSpecificPart);
            if (fileSystem != null) {
                throw new FileSystemAlreadyExistsException(schemeSpecificPart);
            }
            fileSystem = new MysqlFileSystem(this, null, schemeSpecificPart);
            fileSystems.put(schemeSpecificPart, fileSystem);
            return fileSystem;
        }
    }

    @Override
    public MysqlFileSystem getFileSystem(URI uri) {
        return getFileSystem(uri, false);
    }

    public MysqlFileSystem getFileSystem(URI uri, boolean create) {
        synchronized (fileSystems) {
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            int i = schemeSpecificPart.indexOf("/");
            if (i >= 0) {
                schemeSpecificPart = schemeSpecificPart.substring(0, i);
            }
            MysqlFileSystem fileSystem = fileSystems.get(schemeSpecificPart);
            if (fileSystem == null) {
                if (create) {
                    try {
                        fileSystem = newFileSystem(uri, null);
                    } catch (IOException e) {
                        throw (FileSystemNotFoundException) new FileSystemNotFoundException(schemeSpecificPart).initCause(e);
                    }
                } else {
                    throw new FileSystemNotFoundException(schemeSpecificPart);
                }
            }
            return fileSystem;
        }
    }


    @Override
    public Path getPath(URI uri) {
        String str = uri.getSchemeSpecificPart();
        int i = str.indexOf("/");
        if (i == -1) {
            throw new IllegalArgumentException("URI: " + uri + " does not contain path info ex. mysqlfs:root/path");
        }
        return getFileSystem(uri, true).getPath(str.substring(i + 1));
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
    
        Long fileHandle = tmpl.queryForObject("SELECT data FROM direntry WHERE id = ?", Long.class,
                resolve(path));
        try {
            Connection conn = tmpl.getDataSource().getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT id, 'data' AS data FROM filedata WHERE id = ?");
            stmt.setLong(1, fileHandle);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getBlob(2).getBinaryStream();
            } else {
                return null;
            }
        } catch (SQLException sqle) {
            throw new IOException(sqle);
        }
    }
    
    
    private <T> Predicate<T> predicateOf(final DirectoryStream.Filter<T> filter) {
        return new Predicate<T>() {
            
            @Override
            public boolean test(T t) {
                try {
                    return filter.accept(t);
                } catch (Exception e) {
                    return false;
                }
            }
        };
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final DirectoryStream.Filter<? super Path> filter) throws IOException {
        List<Path> list = list(dir);
        final Stream<Path> stream = list.stream().filter(predicateOf(filter));
        return new DirectoryStream<Path>() {
            @Override
            public Iterator<Path> iterator() {
                return stream.iterator();
            }
            
            @Override
            public void close() throws IOException {
                // noop
            }
        };
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void delete(Path path) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return path.toAbsolutePath().equals(path2.toAbsolutePath());
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return false;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return null;
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {

    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {

        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException();
        }
        return (A) tmpl.query("SELECT id, type, size, ctime, mtime, atime FROM direntry WHERE id = ?", new ResultSetExtractor<MysqlAttributes>() {

            public MysqlAttributes extractData(ResultSet rs) throws SQLException, DataAccessException {
                if (!rs.next()) {
                    return null;
                }
                return new MysqlAttributes(rs.getLong(1), rs.getString(2), rs.getLong(3), rs.getTimestamp(4), rs.getTimestamp(5), rs.getTimestamp(6));
            }
        }, resolve(path));
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
    
    
    long resolve(Path path) {
        if (!(path.getFileSystem() instanceof MysqlFileSystem)) {
            throw new RuntimeException("Bad filesystem: " + path.getFileSystem());
        }
        Long id = tmpl.queryForObject("SELECT id FROM direntry WHERE parent = 0 AND name = ?", Long.class,
                ((MysqlFileSystem)path.getFileSystem()).root);
        Iterator<Path> it = path.iterator();
        while (it.hasNext() && id != null) {
            id = tmpl.queryForObject("SELECT id FROM direntry WHERE parent = ? AND name = ?", Long.class,
                    id, it.next().getFileName().toString());
        }
        return id;
    }
    
    public List<Path> list(Path directory) {
        return tmpl.query("SELECT name FROM direntry WHERE parent = ?",
                new Object[] { resolve(directory) },
                (rs, rowNum) -> directory.resolve(rs.getString(1)));
    }
    

}
