package com.impact.vfs.ftpd.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;


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
    
        try {
            Path sizeFile = curCon.virtToPhys(str);
            long size = Files.size(sizeFile);
            curCon.respond("213 " + size);
        } catch (IOException e) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("SIZE: Failed because " + str + " isn't a file");
        }
    }
}
