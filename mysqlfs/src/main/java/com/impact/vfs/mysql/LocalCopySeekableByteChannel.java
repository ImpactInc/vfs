package com.impact.vfs.mysql;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by knut on 2017/05/05.
 */
class LocalCopySeekableByteChannel implements SeekableByteChannel {
    
    interface Sync {
        InputStream read() throws IOException;
        OutputStream write() throws IOException;
        void delete() throws IOException;
    }
    
    private final Sync sync;
    private final Set<? extends OpenOption> options;
    private final SeekableByteChannel seekable;
    private final Path tempFile;
    
    public LocalCopySeekableByteChannel(Sync sync, Set<? extends OpenOption> options) throws IOException {
        this.sync = sync;
        this.options = Collections.unmodifiableSet(new HashSet<>(options));
        
        tempFile = Files.createTempFile("mysqlfs-", null);
        if (!options.contains(StandardOpenOption.TRUNCATE_EXISTING)) {
            InputStream src = sync.read();
            Files.copy(src, tempFile);
            src.close();
        }

        Set<? extends OpenOption> seekOptions = new HashSet<>(this.options);
        seekOptions.remove(StandardOpenOption.CREATE_NEW);
        seekable = Files.newByteChannel(tempFile, seekOptions);
    }
    
    @Override
    public boolean isOpen() {
        return seekable.isOpen();
    }
    
    @Override
    public void close() throws IOException {
        try {
            if (!seekable.isOpen())
                return;
            
            seekable.close();
            
            if (options.contains(StandardOpenOption.DELETE_ON_CLOSE)) {
                sync.delete();
                return;
            }
            
            if (options.contains(StandardOpenOption.READ) && options.size() == 1) {
                return;
            }
            
            OutputStream dst = sync.write();
            Files.copy(tempFile, dst);
            dst.close();
            
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    
    @Override
    public int write(ByteBuffer src) throws IOException {
        return seekable.write(src);
    }
    
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        return seekable.truncate(size);
    }
    
    @Override
    public long size() throws IOException {
        return seekable.size();
    }
    
    @Override
    public int read(ByteBuffer dst) throws IOException {
        return seekable.read(dst);
    }
    
    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        return seekable.position(newPosition);
    }
    
    @Override
    public long position() throws IOException {
        return seekable.position();
    }
}
