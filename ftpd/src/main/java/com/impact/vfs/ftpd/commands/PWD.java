package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;


/**
 * Created by knut on 2017/05/14.
 */
public class PWD {
    
    public PWD(CurrentInfo curCon, String str) {
        
        curCon.respond("257 \"" + curCon.getCwd() + "\" is current directory.");
    }
}
