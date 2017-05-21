package com.theorem.ftp;

import java.util.Enumeration;
import java.util.Hashtable;


public class Restore {

    // This hashtable will only grow to the maximum number of concurrent
    // connections plus the number of concurrent downloads.
    // It holds a key of site & entity information.  The payload is
    // another hashtable of a list of concurrent files being downloaded by
    // each entity.  Typically this would be one each.
    
    private static Hashtable restore = null;
    
    private CurrentInfo curCon;
    
    public Restore(CurrentInfo curCon) {
        if (restore == null) {
            restore = new Hashtable();
        }
        this.curCon = curCon;
    }
    
    // Add an entry when a file fails to complete.
    // Initially the position is 0 indicating it's just an entry of a failed
    // file.  The REST command filles this in.
    //
    public void addRestInfo(String fileName) {
        Hashtable fileList;
        
        // make a key from the entity and site name
        String key = makeKey();
    
        if ((fileList = (Hashtable)restore.get(key)) == null) {
            fileList = new Hashtable();
        }
        
        fileList.put(fileName, new Long(0L));
        restore.put(key, fileList);
    }
    
    // Remove an entry when a file successfuly completed
    //
    public void removeRestInfo(String fileName) {
        Hashtable fileList;
        
        String key = makeKey();
    
        if ((fileList = (Hashtable)restore.get(key)) == null) {
            return;
        }
        
        fileList.remove(fileName);
        if (fileList.isEmpty()) {
            restore.remove(key);
        }
        
    }
    
    // Returns the file position (set by REST) where the error occurred
    // in the file.
    //
    public long getFilePosn(String fileName) {
        Hashtable fileList;
        Long posn;
        String key = makeKey();
    
        if ((fileList = (Hashtable)restore.get(key)) == null) {
            return 0L;
        }
        
        // There may be multiple files in the list.  Get the position of the named file.
        if ((posn = (Long)fileList.get(fileName)) != null) {
            //global.log.logMsg("Retrieving file position for " + key + " posn " + ((Long)restore.get(key)).longValue());
            return posn.longValue();
        }
        
        //global.log.logMsg("No key, Retrieving file position for " + key + " posn " + 0);
        return 0L;    // If there's no key in the system restart the whole thing.
    }
    
    // Sets the file position (set by REST) where the error occurred in the file.
    // Since we don't yet know which file to position set them all to the same value.
    // This works because each file request will be preceded by a position request.
    //
    public boolean setFilePosn(String posn) {
        Hashtable fileList;
        String key = makeKey();
        long filePosn;
        
        try {
            filePosn = Long.parseLong(posn);
        } catch (NumberFormatException nfe) {
            return false;    // This is a parameter error.
        }
    
        if ((fileList = (Hashtable)restore.get(key)) == null) {
            return true;    // not an error, just no file(s) to set up.
        }
        
        for (Enumeration e = fileList.keys(); e.hasMoreElements(); ) {
            Object fileName = e.nextElement();
            fileList.put(fileName, new Long(filePosn));
        }
        
        return true;
    }
    
    // Create a key out of some personal information
    //
    private String makeKey() {
        return curCon.entity + curCon.remoteSite;
    }
    
}
