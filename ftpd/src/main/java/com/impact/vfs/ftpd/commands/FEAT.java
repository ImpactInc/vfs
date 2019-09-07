package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.DataSocket;


public class FEAT {
    
    public FEAT(CurrentInfo curCon, String str) {
        
        curCon.respond("211-Extensions supported:");
        curCon.respond(" EPSV");
        curCon.respond("211 END");
    }
}
