package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class PASV {
    
    public PASV(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        curCon.dataSocket.createPasvSocket(curCon);
    }
}
