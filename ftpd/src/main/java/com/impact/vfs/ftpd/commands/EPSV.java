package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.DataSocket;


public class EPSV {
    
    public EPSV(CurrentInfo curCon, String str) {
        
        curCon.dataSocket.createPasvSocket(curCon, new DataSocket.PortFormatter() {
    
            @Override
            public String format(int sport) {
                return "229 Entering Extended Passive Mode (|||" + sport + "|)";
            }
        });
    }
}
