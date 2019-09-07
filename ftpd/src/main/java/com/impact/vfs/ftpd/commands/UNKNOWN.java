package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;


// Unknown command response
//
public class UNKNOWN {
    
    public UNKNOWN(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        String note = " Unknown command [" + str + "]";
        global.log.logMsg(note);
        curCon.respond("500 " + str + " not understood.");
    }
}
