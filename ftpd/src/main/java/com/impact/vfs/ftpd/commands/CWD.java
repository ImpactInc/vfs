package com.theorem.ftp.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;
import com.theorem.ftp.ShowDisplayFile;


// Also handles CDUP.
//
public class CWD {
    
    public CWD(CurrentInfo curCon, String str) {
        
        String cmd;    // Actual command name.
        Global global = curCon.global;
        
        // CDUP is handled here as well.
        if (str.equalsIgnoreCase("CDUP")) {
            cmd = "CDUP";
            str = "..";
        } else {
            // Get the file path from the CWD command:
            cmd = "CWD";
            str = str.substring(3).trim();
        }
        
        if (str.length() == 0) {
            // no direction, do nothing.
            curCon.respond("250 " + cmd + " command succesful.");
            return;
        }
        
        Path nextDir = curCon.virtToPhys(str);
        if (!Files.isDirectory(nextDir)) {
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("CDUP: Can't CHDIR to " + str);
            return;
        }
        
        // If there's a reciept class run the external class.
        // The class's getStart method can refuse the file retrieval.
        if (global.getFileListener() != null) {
            String response = global.getFileListener().enterDirectory(curCon.entity, nextDir, str);
            if (response != null) {
                curCon.respond(response);
                global.log.logMsg(response);
                return;
            }
        }
        curCon.setCwd(nextDir);    // Restore ourselves to our old directory.
        new ShowDisplayFile(global, nextDir, curCon.out);
        curCon.respond("250 " + cmd + " command succesful.");
    }
}
