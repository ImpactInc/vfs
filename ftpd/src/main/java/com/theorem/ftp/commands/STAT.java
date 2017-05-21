package com.theorem.ftp.commands;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


public class STAT {
    // Not clear what this should return yet, if anything.
    
    public STAT(CurrentInfo curCon, String str) {
        // 211-xcasey Microsoft Windows NT FTP Server status:
        //      Version 4.0
        //      Connected to theorem.cooldog.com
        //      Logged in as michael
        //      TYPE: ASCII, FORM: Nonprint; STRUcture: File; transfer MODE: STREAM
        //      No data connection
        // 211 End of status.
        // Novell returns:
        // 211-nemesis FTP server status:
        //      Version wu-2.5.0(1) Fri Aug 20 11:06:17 MDT 1999
        //      Connected to 63.237.77.150
        //      Logged in anonymously
        //      TYPE: ASCII, FORM: Nonprint; STRUcture: File; transfer MODE: Stream
        //      No data connection
        //      0 data bytes received in 0 files
        //      0 data bytes transmitted in 0 files
        //      0 data bytes total in 0 files
        //      50 traffic bytes received in 0 transfers
        //      4427 traffic bytes transmitted in 0 transfers
        //      4527 traffic bytes total in 0 transfers
        // 211 End of status
        // FTP.sun.com returns:
        // 211-ftp FTP server status:
        //      ftpd Wed Oct 30 23:31:06 PST 1996
        //      Connected to dev1.latpro.com (63.237.77.150)
        //      Logged in anonymously
        //      TYPE: ASCII, FORM: Nonprint; STRUcture: File; transfer MODE: Stream
        //      No data connection
        // 211 End of status
        
        StringBuffer s = new StringBuffer();
        s.append("211- AXL FTP Server\n");
        
        try {
            s.append("211- Connected to ").append(InetAddress.getLocalHost().getHostName()).append('\n');
            s.append("211- Connected from ").append(curCon.remoteSite).append('\n');
            s.append("211- Logged in as ").append(curCon.entity).append('\n');
        } catch (UnknownHostException uhe) {
            ;
        }
        
        s.append("211 End of status.\n");
        curCon.respond(s.toString());
    }
}
