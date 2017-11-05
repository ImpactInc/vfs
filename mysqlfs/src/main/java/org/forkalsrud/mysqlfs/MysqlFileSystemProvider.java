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
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.nio.file.spi.FileSystemProvider;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;


public class MysqlFileSystemProvider extends FileSystemProvider {

    private final Map<String, MysqlFileSystem> fileSystems = new HashMap<>();
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

    private MysqlFileSystem getFileSystem(URI uri, boolean create) {
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


    static class Opt {
        
        final Set<OpenOption> set = new HashSet<>();
        
        Opt(OpenOption... args) {
            set.addAll(Arrays.asList(args));
        }
    
        boolean contains(OpenOption... any) {
            for (OpenOption x : any) {
                if (set.contains(x)) {
                    return true;
                }
            }
            return false;
        }
    }





    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
    
        long id = resolve(path);
        if (id == 0L) {
            throw new IOException("Not found: " + path);
        }
        return newInputStream(id);
    }
    
    
    private InputStream newInputStream(final long id) {
    
        return new InputStream() {

            int pos = 0;
            int bufNo = 0;
            int bufPos = 0;
            byte[] buf;

            {
                next();
            }
            
            void next() {
                bufPos = 0;
                buf = queryFor(byte[].class,
                        "SELECT data FROM blocks WHERE dir = ? AND seq = ?",
                        id, ++bufNo);
                tmpl.update("UPDATE direntry SET atime = ? WHERE id = ?", new Date(), id);
            }
            
            void copy(byte[] dst, int dstPos, int length) {
                System.arraycopy(buf, bufPos, dst, dstPos, length);
                bufPos += length;
                pos += length;
            }

            @Override
            public int read(byte[] dst, int pos, int len) throws IOException {
                if (buf == null) {
                    return -1;
                }
                int dstPos = pos;
                int remainingToRead = len;
                int bytesRead = 0;
                while (buf != null && remainingToRead > 0) {
                    int l = Math.min(buf.length - bufPos, remainingToRead);
                    copy(dst, dstPos, l);
                    dstPos += l;
                    remainingToRead -= l;
                    bytesRead += l;
                    if (bufPos == buf.length) {
                        next();
                    }
                }
                return bytesRead > 0 ? bytesRead : -1;
            }

            @Override
            public int read() throws IOException {
                byte[] single = new byte[1];
                int bytesRead = read(single);
                return bytesRead > 0 ? ((int)single[0]) & 0xff : -1;
            }
        };
    }
    
    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {

        long id = resolve(path);
        Opt opt = new Opt(options);
    /*
        READ,
        WRITE,
        APPEND,
        TRUNCATE_EXISTING,
        CREATE,
        CREATE_NEW,
        DELETE_ON_CLOSE,
        SPARSE,
        SYNC,
        DSYNC;
    */
        if (id == 0L) {
            if (opt.contains(StandardOpenOption.CREATE, StandardOpenOption.CREATE_NEW)) {
                id = create(path, "file");
            } else {
                throw new IOException("Not found: " + path);
            }
        } else {
            if (opt.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
                truncate(id);
            }
        }
        return newOutputStream(id);
    }
    
    private OutputStream newOutputStream(final long id) {

        return new OutputStream() {
    
            int pos = 0;
            int bufNo = 0;
            int bufPos = 0;
            final byte[] buf = new byte[8192];
            boolean flushed = false;
            
            {
                next();
            }
    
            @Override
            public void flush() {
                byte[] data;
                if (bufPos == 0 || flushed) {
                    return;
                } else if (bufPos < buf.length) {
                    // MySQL only stores entire byte arrays
                    data = new byte[bufPos];
                    System.arraycopy(buf, 0, data, 0, bufPos);
                } else {
                    data = buf;
                }
                tmpl.update("REPLACE INTO blocks SET data = ?, dir = ?, seq = ?", data, id, bufNo);
                tmpl.update("UPDATE direntry SET size = ?, mtime = ? WHERE id = ?", pos, new Date(), id);
                flushed = true;
            }
    
            @Override
            public void close() throws IOException {
                flush();
                super.close();
            }

            void next() {
                flush();
                bufPos = 0;
                bufNo++;
            }

            void copy(byte[] src, int srcPos, int length) {
                System.arraycopy(src, srcPos, buf, bufPos, length);
                bufPos += length;
                pos += length;
                flushed = false;
            }
        
            public void write(byte src[], int off, int len) throws IOException {
                
                int remainingToWrite = len;
                int srcPos = off;
                while (remainingToWrite > 0) {
                    int l = Math.min(buf.length - bufPos, remainingToWrite);
                    copy(src, srcPos, l);
                    srcPos += l;
                    remainingToWrite -= l;
                    if (bufPos == buf.length) {
                        next();
                    }
                }
            }

            @Override
            public void write(int b) throws IOException {
                byte[] single = new byte[] { (byte)(b & 0xff) };
                write(single);
            }
        };
    }



    private <T> Predicate<T> predicateOf(final DirectoryStream.Filter<T> filter) {
        return t -> {
            try {
                return filter.accept(t);
            } catch (Exception e) {
                return false;
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
    public SeekableByteChannel newByteChannel(final Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
    
        if (options.contains(StandardOpenOption.SYNC) || options.contains(StandardOpenOption.DSYNC)) {
            throw new IllegalArgumentException("No support for sync operations.");
        }
        long id = resolve(path);
        if (id == 0L) {
            if (options.contains(StandardOpenOption.CREATE) || options.contains(StandardOpenOption.CREATE_NEW)) {
                id = create(path, "file");
            } else {
                throw new NoSuchFileException(path.toString());
            }
        }
        final long resolvedId = id;
        return new LocalCopySeekableByteChannel(id, new LocalCopySeekableByteChannel.Sync() {
    
            @Override
            public InputStream read() throws IOException {
                return newInputStream(resolvedId);
            }
    
            @Override
            public OutputStream write() throws IOException {
                return newOutputStream(resolvedId);
            }
            
            @Override
            public void delete() throws IOException {
                MysqlFileSystemProvider.this.delete(path);
            }
        },
        options);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        create(dir, "dir");
    }

    @Override
    public void delete(Path path) throws IOException {
        long id = resolve(path);
        if (id > 0L) {
            delete(id);
        }
        // Update parent's mtime
        Path parent = path.getParent();
        long parentId = resolve(parent);
        if (parentId > 0) {
            tmpl.update("UPDATE direntry SET mtime = ? WHERE id = ?", new Date(), parentId);
        }
    }
    
    private void delete(long id) throws IOException {
        if ("file".equals(typeOf(id))) {
            tmpl.update("DELETE FROM blocks WHERE dir = ?", id);
        } else {
            Integer childCount = tmpl.queryForObject("SELECT count(1) FROM direntry WHERE parent = ?", Integer.class, id);
            if (childCount > 0) {
                throw new IOException("Directory not empty");
            }
        }
        tmpl.update("DELETE FROM direntry WHERE id = ?", id);
    }
    
    
    private String typeOf(long id) {
        return tmpl.queryForObject("SELECT type FROM direntry WHERE id = ?", new Object[] { id }, String.class);
    }
    
    private void truncate(long id) {
        tmpl.update("DELETE FROM blocks WHERE dir = ?", id);
        tmpl.update("UPDATE direntry SET size = 0, mtime = ? WHERE id = ?", new Date(), id);
    }
    
    
    static class CopyOpt {
    
        private final boolean overwrite;
        
        CopyOpt(CopyOption... options) {
            boolean overwrite = false;
            for (CopyOption o : options) {
                if (StandardCopyOption.REPLACE_EXISTING == o) {
                    overwrite = true;
                }
            }
            this.overwrite = overwrite;
        }
    }

    private void verifyEmpty(long dir) throws IOException {
        long count = tmpl.queryForObject("SELECT count(1) FROM direntry WHERE parent = ?",
                new Object[] { dir }, int.class);
        if (count > 0) {
            throw new IOException("Target not empty " + dir);
        }
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {

        CopyOpt opt = new CopyOpt(options);
        long sourceId = resolve(source);
        if (sourceId == 0L) {
            throw new IOException("Path does not exist: " + source);
        }
        String sourceType = typeOf(sourceId);
        long targetId = resolve(target);
        String targetType;
        if (targetId == 0L) {
            targetId = create(target, sourceType);
        } else if (targetId == sourceId) {
            return;
        } else {
            if (!opt.overwrite) {
                throw new IOException("Path already exists: " + target);
            }
            targetType = typeOf(targetId);
            if (!sourceType.equals(targetType)) {
                throw new IOException("Can't copy " + sourceType + " to " + targetType);
            }
            if ("dir".equals(targetType)) {
                verifyEmpty(targetId);
            }
            if ("file".equals(targetType)) {
                truncate(targetId);
            }
        }
        if ("file".equals(sourceType)) {
            tmpl.update("INSERT INTO blocks SELECT ?, seq, data FROM blocks WHERE dir = ?", targetId, sourceId);
        }
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
    
        CopyOpt opt = new CopyOpt(options);
        long sourceId = resolve(source);
        if (sourceId == 0L) {
            throw new IOException("Path does not exist: " + source);
        }
        Path oldParent = source.getParent();
        long oldParentId = resolve(oldParent);
        Path newParent = target.getParent();
        long newParentId = resolve(newParent);
        if (newParentId == 0L) {
            throw new IOException("Target not found: " + newParent);
        }
        String newParentType = typeOf(newParentId);
        if (!"dir".equals(newParentType)) {
            throw new IOException("Target not directory: " + newParent);
        }
        long targetId = resolve(target);
        if (targetId == sourceId) {
            return;
        }
        if (targetId > 0) {
            if (!opt.overwrite) {
                throw new IOException("Path already exists: " + target);
            }
            String targetType = typeOf(targetId);
            if ("dir".equals(targetType)) {
                verifyEmpty(targetId);
            }
            delete(targetId);
        }
        tmpl.update("UPDATE direntry SET parent = ?, name = ? WHERE id = ?",
                newParentId, target.getFileName().toString(), sourceId);
        
        // Update both old and new parent's mtime
        tmpl.update("UPDATE direntry SET mtime = ? WHERE id in (?, ?)",
                new Date(), oldParentId, newParentId);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return resolve(path) == resolve(path2);
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
        long id = resolve(path);
        if (id == 0L) {
            throw new NoSuchFileException(path.toString());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, Class<V> type, final LinkOption... options) {
    
        if (type == BasicFileAttributeView.class) {
            return (V)new BasicFileAttributeView() {
    
                @Override
                public String name() {
                    return "basic";
                }
    
                @Override
                public BasicFileAttributes readAttributes() throws IOException {
                    return MysqlFileSystemProvider.this.readAttributes(path, BasicFileAttributes.class, options);
                }
    
                @Override
                public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
                    MysqlFileSystemProvider.this.setTimes(path, lastModifiedTime, lastAccessTime, createTime);
                }
            };
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {

        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException();
        }
        long id = resolve(path);
        if (id == 0L) {
            throw new NoSuchFileException(path.toString());
        }
        return (A) tmpl.query("SELECT id, type, size, ctime, mtime, atime FROM direntry WHERE id = ?", rs -> {
            if (!rs.next()) {
                return null;
            }
            return new MysqlAttributes(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getLong(3),
                    rs.getTimestamp(4),
                    rs.getTimestamp(5),
                    rs.getTimestamp(6));
        }, id);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return null;
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw new ReadOnlyFileSystemException();
    }
    

    private void setTimes(Path path, FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) {
        long id = resolve(path);
        if (id == 0L) {
            return;
        }
        tmpl.update("UPDATE direntry SET mtime = ?, atime = ?, ctime = ? WHERE id = ?",
                new Date(lastModifiedTime.toMillis()),
                new Date(lastAccessTime.toMillis()),
                new Date(createTime.toMillis()),
                id);
    }
    

    private long resolve(Path path) {
        if (!(path.getFileSystem() instanceof MysqlFileSystem)) {
            throw new RuntimeException("Bad filesystem: " + path.getFileSystem());
        }
        Long id = queryFor(Long.class, "SELECT id FROM direntry WHERE parent = 0 AND name = ?",
                ((MysqlFileSystem)path.getFileSystem()).root);
        Iterator<Path> it = path.iterator();
        while (it.hasNext() && id != null) {
            id = queryFor(Long.class, "SELECT id FROM direntry WHERE parent = ? AND name = ?",
                    id, it.next().getFileName().toString());
        }
        return id != null ? id : 0L;
    }
    
    private List<Path> list(Path directory) {
        return tmpl.query("SELECT name FROM direntry WHERE parent = ?",
                new Object[] { resolve(directory) },
                (rs, rowNum) -> directory.resolve(rs.getString(1)));
    }
    
    
    
    private long create(Path path, String type) throws IOException {
        
        long parentId = resolve(path.getParent());
        String name = path.getFileName().toString();
        if (parentId == 0L) {
            throw new IOException("Create directory first! " + path);
        }
        String sql = "INSERT INTO direntry SET parent=?, type=?, name=?, size=0, ctime=?";
        PreparedStatementCreatorFactory factory = new PreparedStatementCreatorFactory(sql,
                Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP);
        factory.setReturnGeneratedKeys(true);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Object[] params = new Object[] {
                parentId,
                type,
                name,
                new Date()
        };
        tmpl.update(factory.newPreparedStatementCreator(params), keyHolder);
        long newId = keyHolder.getKey().longValue();
        // Update parent's mtime
        tmpl.update("UPDATE direntry SET mtime = ? WHERE id = ?", new Date(), parentId);
        return newId;
    }

    private <T> T queryFor(Class<T> clazz, String sql, Object... args) {
        return DataAccessUtils.singleResult(tmpl.queryForList(sql, args, clazz));
    }

}
