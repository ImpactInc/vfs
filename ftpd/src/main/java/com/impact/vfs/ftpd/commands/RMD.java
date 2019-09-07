package com.impact.vfs.ftpd.commands;

import java.nio.file.Files;
import java.nio.file.Path;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;


public class RMD {
    // Consider this: Even if we can't, say we did the job.
    // Some programs like to be able to delete dirs that don't exist.
    
    public RMD(CurrentInfo curCon, String str) {

        Global global = curCon.global;
        
        // Get the file path:
        str = str.substring(4).trim();
        try {
            Path path = curCon.virtToPhys(str);
            Files.deleteIfExists(path);
            global.log.logMsg("Removed directory " + str + "(" + path + ")");
            curCon.respond("250 RMD command succesful.");
        } catch (Exception e) {
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("RMD: failed to delete directory " + str);
        }
    }
}
