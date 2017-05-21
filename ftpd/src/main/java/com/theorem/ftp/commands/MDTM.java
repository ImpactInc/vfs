package com.theorem.ftp.commands;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FCorrectPath;
import com.theorem.ftp.Global;


/**
 * Created by knut on 2017/05/14.
 */
public class MDTM {
    
    public MDTM(CurrentInfo curCon, String str) {
        Global global = curCon.global;
        
        // Get the file path.  Some clients include a time in front of the file name (ws_ftp)
        str = str.substring(4);
        str = str.trim();
        
        // Strip off everything but the last token.
        StringTokenizer st = new StringTokenizer(str);
        int tcount = st.countTokens();
        for (int i = 0; i < tcount - 1; i++) {
            st.nextToken();
        }
        str = st.nextToken();    // Final token is file name.
        
        if (curCon.canReadFile(str) == false) {
            curCon.respond("550 Requested action not taken.");
            global.log.logMsg("MDTM: No permission to read file " + curCon.curVDir + curCon.curFile);
            return;
        }
        
        if (curCon.curFile == null) {
            curCon.respond("550 Requested action not taken.");
            // global.log.logMsg("SIZE: The file does not exist " + curCon.curVDir + curCon.curFile);
            return;
        }
        
        String mdtmFileName = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);
        long modTime = new File(mdtmFileName).lastModified();
        
        // This should turn into yyyyMMddHHmmss and in GMT.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        TimeZone tz = TimeZone.getDefault();
        
        // Adjust the file's mod time to GMT.
        modTime -= tz.getRawOffset();
        
        curCon.respond("213 " + sdf.format(new Date(modTime)));
        
    }
}
