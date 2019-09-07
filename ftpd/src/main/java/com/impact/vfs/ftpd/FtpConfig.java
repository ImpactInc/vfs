package com.impact.vfs.ftpd;

import java.nio.file.Path;
import java.util.Hashtable;


public class FtpConfig {
    
    Global global;    // Global information from the config


    public FtpConfig() {
    
        global = new Global();
        global.log = new LogMsg();
        global.fLog = new LogFTP();
        global.FTPPort = 21;
        global.FTPTimeout = 10 * 60 * 1000;
    
        Clients.allowAnonymous(false);
        Clients.setMaxAnonymous(0);
    
        Clients.setMaxConnections(0);
    
    
        global.homeDirectory = "/";    // always this, used to be configurable.
    
        // TODO delegate the permissions stuff
        global.permSet = new Permission(global);
        global.permSet._permTable = new Hashtable();
        global.personalDir = new Hashtable();
    

        global.osName = System.getProperty("os.name");
    }
    
    
    public Global getGlobal() {
        return global;
    }
    
    public void setPort(int port) {
        global.FTPPort = port;
    }
    
    public void setTimeout(int seconds) {
        global.FTPTimeout = 1000 * seconds;
    }
    
    public void setWelcomeFile(Path file) {
        global.welcomeFile = file;
    }

    public void setMaxConnections(int max) {
        Clients.setMaxConnections(max);
    }
    
    
    public void setFileReceiptListener(FileReceipt listener) {
        global.fileListener = listener;
    }

    public void setDisplayFile(String name) {
        global.displayFile = name;
    }

    public void setLogCommands(boolean enable) {
        global.logCommands = enable;
    }
    
}
