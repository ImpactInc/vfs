package com.theorem.ftp.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class MDTM {
    
    public MDTM(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        // Get the file path.  Some clients include a time in front of the file name (ws_ftp)
        str = str.substring(4);
        str = str.trim();
        
        if (curCon.canReadFile(str) == false) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg("MDTM: No permission to read file " + str);
            return;
        }
        
        try {
            Path mdtmFile = curCon.virtToPhys(str);
            FileTime modTime = Files.getLastModifiedTime(mdtmFile);
            // This should turn into yyyyMMddHHmmss and in GMT.
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            curCon.respond("213 " + sdf.format(new Date(modTime.toMillis())));
        } catch (IOException e) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg("SIZE: The file does not exist " + str);
        }
    }
}
