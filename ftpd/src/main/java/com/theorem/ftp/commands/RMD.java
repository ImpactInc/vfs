package com.theorem.ftp.commands;

import java.io.File;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FCorrectPath;
import com.theorem.ftp.Global;


/**
 * Created by knut on 2017/05/14.
 */
public class RMD {
    // Consider this: Even if we can't, say we did the job.
    // Some programs like to be able to delete dirs that don't exist.
    
    public RMD(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        String saveDir = curCon.curWD;    // save working dir
        
        // Get the file path:
        str = str.substring(4).trim();
        if (curCon.chdir(str) == false)    // can we chdir?
        {
            //global.log.logMsg("curCOM.chkdir("+str+") failed");
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("RMD: Can't find directory " + str);
            return;
        }
        
        String rmdPath = new FCorrectPath().fixit(curCon.virtToPhys(curCon.curWD));
        curCon.chdir(saveDir);    // get back to our directory
        
        if ((new File(rmdPath).delete()) == false) {
            //global.log.logMsg("delete("+rmdPath+") failed");
            curCon.respond("450 Requested file action not taken.");
            global.log.logMsg("RMD: failed to delete directory " + rmdPath);
            return;
        }
        
        global.log.logMsg("Removed directory " + str + "(" + rmdPath + ")");
        curCon.respond("250 RMD command succesful.");
    }
}
