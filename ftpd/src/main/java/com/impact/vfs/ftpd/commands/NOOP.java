package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;


public class NOOP {
    
    public NOOP(CurrentInfo curCon, String str) {
        
        curCon.respond("200 Nothing Done.");
    }
}
