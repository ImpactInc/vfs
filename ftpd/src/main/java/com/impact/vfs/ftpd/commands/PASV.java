package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.DataSocket;


public class PASV {
    
    public PASV(CurrentInfo curCon, String str) {
        
        curCon.dataSocket.createPasvSocket(curCon, new DataSocket.PortFormatter() {
    
            @Override
            public String format(int sport) {
                int p1 = sport >> 8;
                int p2 = sport & 0xff;
    
                String tmp = curCon.localIPName;
                return "227 Entering Passive Mode. (" + tmp + "," + p1 + "," + p2 + ')';
            }
        });
    }
}
