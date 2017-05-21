package com.theorem.ftp.commands;

import java.io.File;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FCorrectPath;
import com.theorem.ftp.Global;


// Rename to - get the new name of the file and perform the action.
public class RNTO {
    
    public RNTO(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        // Get the file path:
        str = str.substring(4).trim();
        
        if (curCon.canWriteFile(str) == false) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg("RNTO: No write permission for file " + str + " in directory " + curCon.curWD);
            return;
        }
        
        // RNFR got the file to rename (from) and stored it in curCon
        String renameFile = curCon.getRenameFile();

        String tmp = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);
        File rntoFile = new File(tmp);
        File rnfrFile = new File(renameFile);
        
        if (rnfrFile.renameTo(rntoFile)) {
            global.log.logMsg("Renamed file " + renameFile + " to " + tmp);
            curCon.respond("250 RNTO completed.");
        } else {
            global.log.logMsg("RNTO: Rename failed for " + renameFile + " -> " + tmp);
            curCon.respond("553 Requested action not taken.");
        }
    }
}
