package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;


public class CDUP {
    
    public CDUP(CurrentInfo curCon, String str) {
        
        new CWD(curCon, str.trim());
    }
    
}
