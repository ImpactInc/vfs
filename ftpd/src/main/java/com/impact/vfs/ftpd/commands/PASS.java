package com.impact.vfs.ftpd.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.DataSocket;
import com.impact.vfs.ftpd.Global;


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
        
        if (checkPassword(curCon, password) == false) {
            curCon.respond("530 Not logged in.");
            curCon.entity = null;
            curCon.authName = null;    // this has not been authorized, kill it.
            return;
        }

        note = " Logged in from " + curCon.remoteSite;
        global.log.logMsg(note);

        // setLogin also sets home directory/ CWD
        curCon.setLogin(curCon.entity, global.permSet);
        
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
        
        return global.getAuthenticator().authenticate(curCon.entity, password);
    }
    
    // Send the welcome message.
    //
    void sendWelcome(CurrentInfo curCon) {
        Global global = curCon.global;
    
        if (global.getWelcomeFile() == null) {
            return;
        }
        
        Path df = global.getWelcomeFile();
        if (!Files.isRegularFile(df)) {
            return;
        }
        try {
            BufferedReader in = Files.newBufferedReader(df);
            String line;
            while ((line = in.readLine()) != null) {
                curCon.respond("230- " + line);
            }
            in.close();
        } catch (IOException ioe) {
            global.log.logMsg(ioe.getMessage());
        }
    }
    
}
