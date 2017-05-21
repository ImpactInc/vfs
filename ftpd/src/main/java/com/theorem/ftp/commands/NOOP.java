package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class NOOP {
    
    public NOOP(CurrentInfo curCon, String str) {
        
        curCon.respond("200 Nothing Done.");
    }
}
