package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.DataSocket;


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
