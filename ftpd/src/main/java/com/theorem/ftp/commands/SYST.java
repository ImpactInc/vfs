package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class SYST {
    
    public SYST(CurrentInfo curCon, String str) {
        
        // claim to be a UNIX system.
        curCon.respond("215 UNIX");
    }
}
