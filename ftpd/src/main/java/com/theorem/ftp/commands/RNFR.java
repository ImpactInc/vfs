package com.theorem.ftp.commands;

import java.io.File;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FCorrectPath;
import com.theorem.ftp.Global;


/**
 * Created by knut on 2017/05/14.
 */
public class RNFR {
    
    public RNFR(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        // Rename from -- get the old name of the file.
        
        // Get the file path:
        str = str.substring(4).trim();
        
        if (curCon.canWriteFile(str) == false) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("RNFR: No write permission for file " + str);
            return;
        }
        
        String rnfrFileName = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);
        if (!new File(rnfrFileName).isFile()) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg("RNFR: " + rnfrFileName + " isn't a file");
            return;
        }
        curCon.setRenameFile(new FCorrectPath().fixit(rnfrFileName));
        curCon.respond("350 RNFR accepted  Enter the new file name.");
    }
    
}
