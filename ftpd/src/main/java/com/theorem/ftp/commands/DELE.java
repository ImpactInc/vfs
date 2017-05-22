package com.theorem.ftp.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class DELE {
    
    public DELE(CurrentInfo curCon, String str) {
        
        Global global = curCon.global;
        
        // Get the file path:
        str = str.substring(4).trim();
        
        if (curCon.canWriteFile(str) == false) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("DELE: No write permission for file " + str);
            return;
        }
        
        try {
            Path delpath = curCon.virtToPhys(str);
            Files.delete(delpath);
            global.log.logMsg("Deleted file " + delpath);
            curCon.respond("250 delete command successful.");
        } catch (IOException e) {
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("Failed to delete file " + str);
        }
    }
}
