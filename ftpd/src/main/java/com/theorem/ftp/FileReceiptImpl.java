package com.theorem.ftp;

import java.io.CharArrayWriter;
import java.io.PrintWriter;


public class FileReceiptImpl {
    
    Global global;                // Global data holder.
    String entity;                // Entity's name (that which is logged in).
    Object newInst = null;    // Instance of the FileReciept class.
    
    public FileReceiptImpl(CurrentInfo curCon) {
        global = curCon.global;
        Class cls = null;
        newInst = null;
        
        // If there's no class, don't look for one.
        if (global.fileClass == null) {
            return;
        }
        
        entity = curCon.entity;    // Save the entity name.
        
        // Try to get the fqcn as a class, but this may fail if we're passed
        // a method.  Back out a level and try again.
        try {
            cls = Class.forName(global.fileClass);
            newInst = cls.newInstance();
        } catch (ClassNotFoundException cnfe) {
            global.log.logMsg("Can't find the file receipt class: " + global.fileClass + ": " + cnfe);
            global.fileClass = null;    // prevent future attempts at using this.
            return;
        } catch (IllegalAccessException iae) {
            global.log.logMsg("Can't access the file receipt class: " + global.fileClass + ": " + iae);
            global.fileClass = null;    // prevent future attempts at using this.
            return;
        } catch (InstantiationException ie) {
            global.log.logMsg("Can't instantiate the file receipt class: " + global.fileClass + ": " + ie);
            global.fileClass = null;    // prevent future attempts at using this.
            return;
        }
    }
    
    // Exceptions are caught by the fault catcher.
    //
    public String getBefore(String fileName) {
        if (newInst != null) {
            return ((FileReceipt)newInst).getBefore(entity, fileName, global.configDir, global.log);
        }
        
        return null;
    }
    
    // Exceptions are caught by the fault catcher.
    //
    public void getAfter(String fileName, long bytecount) {
        if (newInst != null) {
            ((FileReceipt)newInst).getAfter(entity, fileName, bytecount, global.configDir, global.log);
        }
    }
    
    // Exceptions are caught by the fault catcher.
    //
    public String putBefore(String fileName) {
        if (newInst != null) {
            ((FileReceipt)newInst).putBefore(entity, fileName, global.configDir, global.log);
        }
        
        return null;
    }
    
    // Exceptions are caught by the fault catcher.
    //
    public void putAfter(String fileName, long bytecount) {
        if (newInst != null) {
            ((FileReceipt)newInst).putAfter(entity, fileName, bytecount, global.configDir, global.log);
        }
    }
    
    // Exceptions are caught by the fault catcher.
    //
    public String enterDirectory(String physDirectory, String virtDirectory) {
        if (newInst != null) {
            return ((FileReceipt)newInst).enterDirectory(entity,
                    physDirectory,
                    virtDirectory,
                    global.configDir,
                    global.log);
        }
        
        return null;
    }
    
    /**
     * Create a useful and readable exception
     * Builds stack trace and captures exception.
     *
     * @param e Exception
     */
    void logExceptionString(Exception e) {
        String emsg;
        
        // Strangely, it's possible to end up with a null exception.
        // This is difficult to diagnose so we'll mention this specific error.
        
        CharArrayWriter ca = new CharArrayWriter();
        PrintWriter pwca = new PrintWriter(ca);
        
        e.printStackTrace(pwca);
        
        global.log.logMsg("Exception occurred:");
        
        emsg = global.fileClass + ": " + e.getMessage() + "\n" + ca.toString() + "\n";
        
        global.log.logMsg(emsg);
    }
    
}
