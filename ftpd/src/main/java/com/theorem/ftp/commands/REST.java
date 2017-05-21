package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Restore;


/**
 * RESTore previous session (if any)
 * <p>
 * The system keeps track of what file is currently being downloaded
 * and it's progress - command line gives position.
 */
public class REST {
    
    public REST(CurrentInfo curCon, String str) {
        str = str.substring(4).trim();
        //global.log.logMsg("REST: command is " + str);
        
        // Set the file position for the RESTore
        Restore r = new Restore(curCon);
        if (r.setFilePosn(str) == false) {
            curCon.respond("501 " + str + " bad parameter.");
            return;
        }
        
        curCon.respond("350 Requested file action pending further information.");
    }
}
