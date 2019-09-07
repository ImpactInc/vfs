package org.forkalsrud.mysqlfs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.Date;

import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.JdbcTemplate;


/**
 */
public class MysqSeekableChannel implements SeekableByteChannel {
    
    private JdbcTemplate tmpl;
    private int fd;
    boolean isOpen = true;
    
    private long filePos = 0;
    private long fileSize = 0;
    private long bufNo = 0;
    private int bufSize = 0;
    private int bufPos = 0;
    private byte[] buf = new byte[8192];
    private boolean dirty = false;
    
    
    
    @Override
    public int read(ByteBuffer dst) throws IOException {

        int remainingToRead = dst.remaining();
        int bytesRead = 0;
        while (filePos < fileSize && remainingToRead > 0) {
            int l = Math.min(bufSize - bufPos, remainingToRead);
            readCopy(dst, l);
            remainingToRead -= l;
            bytesRead += l;
            if (bufPos == bufSize) {
                load(bufNo + 1);
            }
        }
        return bytesRead > 0 ? bytesRead : -1;
    }

    void readCopy(ByteBuffer dst, int length) {
        dst.put(buf, bufPos, length);
        bufPos += length;
        filePos += length;
    }
    
    
    @Override
    public int write(ByteBuffer src) throws IOException {

        int remainingToWrite = src.remaining();
        int written = 0;
        while (remainingToWrite > 0) {

            int l = Math.min(8192 - bufPos, remainingToWrite);
            writeCopy(src, l);
            remainingToWrite -= l;
            written += l;
            if (bufPos == 8192) {
                load(bufNo + 1);
            }
        }
        return written;
    }

    void writeCopy(ByteBuffer src, int length) {
    
        src.get(buf, bufPos, length);
        bufPos += length;
        if (bufSize < bufPos) {
            bufSize = bufPos;
        }
        filePos += length;
        dirty = true;
    }
    
    
    @Override
    public long position() throws IOException {
        return filePos;
    }
    
    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
    
        long nextBufNo = newPosition / 8192;
        int nextBufPos = (int)(newPosition % 8192);
        if (nextBufNo != bufNo) {
            flush();
            load(nextBufNo);
            bufNo = nextBufNo;
        }
        bufPos = nextBufPos;
        return this;
    }
    
    @Override
    public long size() throws IOException {
        return fileSize;
    }
    
    
    void extend() throws IOException {

        if (filePos > fileSize) {
    
            long lastBufNo = fileSize / 8192;
            int lastBufPos = (int)(fileSize % 8192);
    
            if (bufNo != lastBufNo) {
                flush();
                load(lastBufNo);
            }
            bufPos = lastBufPos;
            long newFileSize = filePos;
            long remainingFill = newFileSize - fileSize;
            while (remainingFill > 0) {
                int len = (int)Math.min(8192 - bufPos, remainingFill);
                Arrays.fill(buf, bufPos, bufPos + len, (byte)0);
                bufPos += len;
                if (bufSize < bufPos) {
                    bufSize = bufPos;
                }
                remainingFill -= len;
                if (bufPos == 8192) {
                    flush();
                    load(bufNo + 1);
                }
            }
        }
    }
    
    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
    
        long lastBufNo = size / 8192;
        int lastBufPos = (int)(size % 8192);

        if (filePos > size) {
            // TODO flush?
            filePos = size;
        }
        if (bufNo != lastBufNo) {
            flush();
            load(lastBufNo);
        }
        Arrays.fill(buf, lastBufPos, 8192, (byte)0);
        bufPos = lastBufPos;
        flush();
        long oldBufCount = fileSize / 8192;
        for (long bufId = lastBufNo + 1; bufId <= oldBufCount; bufId++) {
            tmpl.update("DELETE FROM blocks WHERE dir = ? AND seq = ?", fd, bufId);
        }
        return this;
    }
    
    @Override
    public boolean isOpen() {
        return isOpen;
    }
    
    @Override
    public void close() throws IOException {
        flush();
        isOpen = false;
    }
    
    
    
    public void flush() {

        if (bufPos == 0 || !dirty) {
            return;
        }

        byte[] data;
        if (bufSize < 8192) {
            // MySQL only stores entire byte arrays
            data = new byte[bufSize];
            System.arraycopy(buf, 0, data, 0, bufSize);
        } else {
            data = buf;
        }
        tmpl.update("REPLACE INTO blocks SET data = ?, dir = ?, seq = ?", data, fd, bufNo);
        tmpl.update("UPDATE direntry SET size = ?, mtime = ? WHERE id = ?", fileSize, new Date(), fd);
        dirty = false;;
    }
    
    
    
    void load(long bufId) throws IOException {
        bufPos = 0;
        byte[] dbBuf = DataAccessUtils.singleResult(tmpl.queryForList(
                "SELECT data FROM blocks WHERE dir = ? AND seq = ?",
                new Long[] { Long.valueOf(fd), Long.valueOf(bufId) }, byte[].class));
        if (dbBuf != null && dbBuf.length == 8192) {
            buf = dbBuf;
            bufSize = 8192;
        } else if (dbBuf == null) {
            Arrays.fill(buf, (byte)0);
            bufSize = 0;
        } else {
            System.arraycopy(dbBuf, 0, buf, 0, dbBuf.length);
            Arrays.fill(buf, dbBuf.length, 8192, (byte)0);
            bufSize = dbBuf.length;
        }
        tmpl.update("UPDATE direntry SET atime = ? WHERE id = ?", new Date(), bufId);
        bufNo = bufId;
    }
}
