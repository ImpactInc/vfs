package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


/**
 * Created by knut on 2017/05/14.
 */
public class PWD {
    
    public PWD(CurrentInfo curCon, String str) {
        
        curCon.respond("257 \"" + curCon.getCwd() + "\" is current directory.");
    }
}
