package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class CDUP {
    
    public CDUP(CurrentInfo curCon, String str) {
        
        new CWD(curCon, str.trim());
    }
    
}
