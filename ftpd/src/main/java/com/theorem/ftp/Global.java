package com.theorem.ftp;

import java.util.Hashtable;

// Global objects all sessions need to access.
// This file is updated in main and doled out to each login.
//
public class Global implements Cloneable {
    
    Authenticator authenticator;
    String serverIdentification;
    
    public final String AllUsers = "@";    // All users shotcut name.
    public final char AllUsersC = '@';    // All users shortcut name.
    final String Anonymous = "*";    // Public access to anonymous
    final char AnonymousC = '*';    // Public access to anonymous
    
    public static final char CRLFc[] = { (char)0x0d, (char)0x0a };    // CRLF for Telnet as char array
    public static final byte CRLFb[] = { (byte)0x0d, (byte)0x0a };    // CRLF for Telnet as char array
    
    static final String CRLF = new String(CRLFc);    // CRLF for Telnet.
    static final String LF = new String(CRLFc, 1, 1);
    
    public String configDir;
    public Permission permSet;
    
    int FTPPort;    // FTP command port number (data port is n-1)
    public int FTPTimeout;    // port timeout in milliseconds
    public LogMsg log;        // log file object - logs FTP functions & errors
    public LogFTP fLog;    // log file object - real FTP transfer log
    
    // Hashtable of search directories for /user/name directories
    // Key is the physical directory path, contents is the subdirectory, if any.
    Hashtable personalDir = null;
    
    String homeDirectory;    // default home directory
    
    String osName;        // name of the OS (for system dependent info)
    
    public String welcomeFile;    // An announcement file to be printed at each session.
    String displayFile;    // file to be displayed on a CWD/CDUP
    
    // Alternatives to RADIUS authentication:
    public String passwordFile;    // name of the password file
    public String passwordClass;    // name of a provided authentication class
    
    // Hook for tracking uploads and downloads.
    public String fileClass;        // Class to be called when a file is uploaded or downloaded.
    
    // Log the commands or not.  Passwords will be logged!
    boolean logCommands;
    
    Global newClone() {
        try {
            return (Global)clone();
        } catch (CloneNotSupportedException cnse) {
            // We never expect to get here, but -
            log.logMsg("FTP server can't clone global data - critical error, quitting.");
            System.err.println("FTP server can't clone global data - critical error, quitting.");
            
            System.exit(1);
            return null;    // The compiler likes this.
        }
    }
    
    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public Authenticator getAuthenticator() {
        return authenticator;
    }
    
    public void setServerIdentification(String s) {
        serverIdentification = s;
    }

    public String getServerIdentification() {
        return serverIdentification;
    }
}
