package org.forkalsrud.mysqlfs;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;


/**
 * Created by knut on 2017/04/29.
 */
public class MysqlAttributes implements BasicFileAttributes {
    
    final long id;
    final String type;
    final long size;
    final Date ctime;
    final Date mtime;
    final Date atime;
    
    public MysqlAttributes(long id, String type, long size, Date ctime, Date mtime, Date atime) {
        this.id = id;
        this.type = type;
        this.size = size;
        this.ctime = ctime;
        this.mtime = mtime;
        this.atime = atime;
    }
    
    
    private FileTime optional(Date in) {
        return in != null ? FileTime.fromMillis(in.getTime()) : null;
    }
    
    @Override
    public FileTime lastModifiedTime() {
        return mtime != null ? optional(mtime) : optional(ctime);
    }
    
    @Override
    public FileTime lastAccessTime() {
        return atime != null ? optional(atime) : optional(ctime);
    }
    
    @Override
    public FileTime creationTime() {
        return optional(ctime);
    }
    
    @Override
    public boolean isRegularFile() {
        return "file".equals(type);
    }
    
    @Override
    public boolean isDirectory() {
        return "dir".equals(type);
    }
    
    @Override
    public boolean isSymbolicLink() {
        return "symlink".equals(type);
    }
    
    @Override
    public boolean isOther() {
        return false;
    }
    
    @Override
    public long size() {
        return size;
    }
    
    @Override
    public Object fileKey() {
        return Long.valueOf(id);
    }
}
