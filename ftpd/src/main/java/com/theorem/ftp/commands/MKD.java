package com.theorem.ftp.commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class MKD {
    
    public MKD(CurrentInfo curCon, String str) {
        
        Global global = curCon.global;
        
        // See if we're at the root of the current virtual directory.
        
        // Get the file path:
        String virt = str.substring(4).trim();
        try {
            virt = curCon.createAbsolutePath(virt);
            Path newDir = curCon.getRoot().resolve(virt);
            Files.createDirectories(newDir);
            global.log.logMsg("Created directory " + str + "(" + virt + ")");
            curCon.respond("250 MKD command succesful");
        } catch (IOException e) {
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("MKD: failed to create directory " + virt);
        } finally {
            curCon.out.flush();
        }
/*
            // See if we can write in the base directory (relative or absolute)
        String testDir;
        if (str.charAt(0) == '/') {
            testDir = global.permSet.resolve(str + "/..");
        } else {
            testDir = global.permSet.resolve(curCon.curWD);
        }
        //global.log.logMsg("MKDIR testing Dir: " + testDir);
        
        if (!curCon.canWriteDir(testDir)) {
            //global.log.logMsg("MKDIR test directory: failed");
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("MKD: No write permission for " + str);
            return;
        }
        
        // now set up the dir to be created:
        if (str.charAt(0) == '/') {
            testDir = global.permSet.resolve(str);
        } else {
            testDir = global.permSet.resolve(curCon.curWD + "/" + str);
        }
        //global.log.logMsg("MKDIR creating Dir: " + testDir);
        
        String physDir = curCon.virtToPhys(testDir);
        //global.log.logMsg("MKDIR creating physDir: " + physDir);
        
        String mkdPath = new FCorrectPath().fixit(physDir);
        
        //global.log.logMsg("mkdpath is : " + mkdPath);
        
        File fobj = new File(mkdPath);
        
        if (!fobj.isDirectory())    // if it doesn't exist try to create it.
        {
            if ((fobj.mkdir()) == false) {
                curCon.respond("450 Requested file action not taken.");
                global.log.logMsg("MKD: failed to create directory " + mkdPath);
                return;
            }
        }
        
        global.log.logMsg("Created directory " + str + "(" + mkdPath + ")");
        curCon.respond("250 MKD command succesful");
        curCon.out.flush();
        */
    }
}
