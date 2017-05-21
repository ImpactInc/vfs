package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


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
