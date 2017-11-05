package com.theorem.ftp.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.theorem.ftp.CurrentInfo;
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
        
        try {
            Path rnfrFile = curCon.virtToPhys(str);
            if (!Files.isRegularFile(rnfrFile)) {
                curCon.respond("550 Requested action not taken.");
                global.log.logMsg("RNFR: " + rnfrFile + " isn't a file");
                return;
            }
            curCon.setRenameFile(rnfrFile);
            curCon.respond("350 RNFR accepted  Enter the new file name.");
        } catch (IOException e) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg(e.getMessage());
        }
    }
    
}