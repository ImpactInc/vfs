package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.Clients;
import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;


public class USER {
    
    public USER(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        String name = str.substring(4).trim();
        if (name.length() == 0) {
            curCon.respond("501 Syntax error in parameters or arguments.");
            return;
        }
        
        curCon.authName = name;
        curCon.entity = curCon.authName;
        
        global.log.logMsg(curCon.entity + ":" + curCon.sessionID + " Logging in.");
        
        Thread td = Thread.currentThread();
        
        if (curCon.authName.equals("anonymous")) {
            if (Clients.anonymousPermitted() == false) {
                curCon.respond("530 Anonymous logins are not accepted.");
                return;
            }
            if (Clients.checkAnonymous() == false) {
                curCon.respond("530 Too many Anonymous logins.");
                return;
            }
            
            curCon.respond("331 Anonymous login accepted: please use your email address as Password.");
            td.setName(curCon.entity + ":" + curCon.sessionID);
            return;
        }
        
        td.setName(curCon.entity + ":" + curCon.sessionID);
        curCon.respond("331 Password");
    }
}
