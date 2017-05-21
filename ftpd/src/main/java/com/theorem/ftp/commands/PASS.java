package com.theorem.ftp.commands;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.theorem.ftp.*;


public class PASS {
    
    private Global global;
    private CurrentInfo curCon;
    boolean _loggedIn;
    
    public PASS(CurrentInfo curCon, String str) {
        this.global = curCon.global;
        this.curCon = curCon;
        
        _loggedIn = false;    // assume they won't be able to log on.
        
        if (curCon.entity == null) {
            curCon.respond("530 Not logged in.");
            curCon.out.flush();
            return;
        }
        String password = str.substring(4).trim();
        
        curCon.remoteSite = curCon.remoteIP.getHostName();
        
        String note;
        
        if (curCon.entity.equals("anonymous")) {
            if (password.indexOf(global.AllUsers) == -1) {
                curCon.respond("530 Not logged in. Please use your email address as your password.");
                curCon.entity = null;
                return;
            }
            note = "/" + password + " Logged in from " + curCon.remoteSite;
            global.log.logMsg(note);
            curCon.authName = null;    // this has not been authorized, kill authorization name.
        } else {
            if (checkPassword(curCon, password) == false) {
                curCon.respond("530 Not logged in.");
                curCon.entity = null;
                curCon.authName = null;    // this has not been authorized, kill it.
                
                return;
            }
            // The name may be a RADIUS proxy name, eg bob@xyz.com
            // If there's a '@' then remove it and everything beyond.
            int idx;
            if ((idx = curCon.entity.indexOf(global.AllUsersC)) > -1) {
                curCon.entity = curCon.entity.substring(0, idx);
            }
            
            note = " Logged in from " + curCon.remoteSite;
            global.log.logMsg(note);
        }
        curCon.setLogin(curCon.entity, global.permSet);
        new HomeDirectory(global, curCon).setup();
        
        sendWelcome(curCon);
        curCon.respond("230 User " + curCon.entity + " logged in from " + curCon.remoteIP.getHostName());
        curCon.dataSocket = new DataSocket(global);
        _loggedIn = true;    // mark this as successful
    }
    
    public boolean loggedIn() {
        return _loggedIn;
    }
    

    // Carefully check the password for correctness.
    //
    private boolean checkPassword(CurrentInfo curCon, String password) {

        Global global = curCon.global;
        
        return global.getAuthenticator().authenticate(curCon.entity, password, global.configDir, global.log);
    }
    
    // Send the welcome message.
    //
    void sendWelcome(CurrentInfo curCon) {
        Global global = curCon.global;
    
        if (global.welcomeFile == null) {
            return;
        }
        
        File df = new File(global.welcomeFile);
        if (df.exists() && df.isFile()) {
            RandomAccessFile fi;
            try {
                fi = new RandomAccessFile(df, "r");
                
                String line;
                while ((line = fi.readLine()) != null) {
                    line.replace('\n', ' ').replace('\r', ' ');
                    curCon.respond("230- " + line);
                }
                fi.close();
                // It is possible that the return will happen before
                // the file is closed.  We should do something about this someday.
            } catch (IOException ioe) {
                return;
            }
        }
    }
    
}
