package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;


public class FEAT {
    
    public FEAT(CurrentInfo curCon, String str) {
        
        curCon.respond("211-Extensions supported:");
        curCon.respond(" EPSV");
        curCon.respond("211 END");
    }
}
