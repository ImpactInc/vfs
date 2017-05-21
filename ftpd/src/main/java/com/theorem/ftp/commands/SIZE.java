package com.theorem.ftp.commands;

import java.io.File;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FCorrectPath;
import com.theorem.ftp.Global;


public class SIZE {
    
    public SIZE(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        // Get the file path:
        str = str.substring(4);
        str = str.trim();
        
        if (curCon.canReadFile(str) == false) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("SIZE: No permission to read " + str);
            return;
        }
        
        String sizeFileName;
        
        if (curCon.curFile == null) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("SIZE: The file does not exist " + curCon.curVDir + curCon.curFile);
            return;
        }
        
        sizeFileName = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);
        
        File fSize = new File(sizeFileName);
        if (!fSize.isFile()) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("SIZE: Failed because " + curCon.curVDir + curCon.curFile + " isn't a file");
            return;
        }
        curCon.respond("213 " + fSize.length());
    }
}
