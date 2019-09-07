package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;


public class SYST {
    
    public SYST(CurrentInfo curCon, String str) {
        
        // claim to be a UNIX system.
        curCon.respond("215 UNIX");
    }
}
