package com.impact.vfs.ftpd.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;


// Rename to - get the new name of the file and perform the action.
public class RNTO {
    
    public RNTO(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        // Get the file path:
        str = str.substring(4).trim();
        
        if (curCon.canWriteFile(str) == false) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg("RNTO: No write permission for file " + str + " in directory " + curCon.getCwd());
            return;
        }
        
        // RNFR got the file to rename (from) and stored it in curCon
        Path renameFile = curCon.getRenameFile();
        try {
            Path renameTo = curCon.virtToPhys(str);
    
            Files.move(renameFile, renameTo);
            curCon.respond("250 RNTO completed.");
            global.log.logMsg("Renamed file " + renameFile + " to " + renameTo);
        } catch (IOException e) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("RNTO: Rename failed for " + renameFile + " -> " + str);
        }
    }
}
