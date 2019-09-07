package com.theorem.ftp.commands;

import java.net.SocketException;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


/**
 * Site class handles site specific requests.
 * Implement your own here.
 */
public class SITE {
    
    public SITE(CurrentInfo curCon, String str) {
        
        // Get the UNIX command (CHMOD, UMASK, IDLE)
        // Currently Java only lets us alter IDLE.
        
        if (curCon.authName == null)    // must be logged in as a real entity (not anonymous).
        {
            curCon.respond("522 Requested action not taken.");
            return;
        }
        
        String subcmd = str.substring(4).trim();
        
        if (subcmd.startsWith("idle")) {
            try {
                int idleTime = Integer.parseInt(subcmd.substring(4).trim());
                
                if (idleTime > 0)    // Don't allow infinite or negative timeouts.
                {
                    try {
                        
                        curCon.clientSocket.setSoTimeout(idleTime);
                        curCon.respond("200 SITE IDLE set to " + idleTime);
                        
                    } catch (SocketException se) {
                        curCon.respond("522 Requested action not taken.");
                    }
                } else {
                    curCon.respond("501 Syntax error in arguments.");
                }
            } catch (NumberFormatException nfe) {
                curCon.respond("501 Syntax error in arguments.");
            }
        } else {
            new UNKNOWN(curCon, str);
        }
    }
}
