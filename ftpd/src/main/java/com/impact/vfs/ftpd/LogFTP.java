package com.impact.vfs.ftpd;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Log a message to an error log file.
//
//
// A record is kept of all the FTP activity that occurs on your VSERV.
// This record is kept in a file called xferlog, which is located in
// your ~/usr/log directory. Any time that a file is transferred through
// your FTP service, an entry is made in this file. Here's a sample entry
// from an xferlog file:
//
// Fri Oct 3 10:41:55 1997 1294 tide14.microsoft.com 4500828 /pub/demos/P952_256.ZIP b _ o a proxyuser@microsoft.com ftp 0 *
//
// Take a close look at each part of this log entry. All entries in the
// xferlog file will follow this same format.
//
// current-time: The first part of the entry indicates the time that the
// file transfer was completed. It takes the form "Day Month Date Hour
// Minute Second Year". In the example above, this section is "Fri Oct 3
// 10:41:55 1997"
//
// transfer-time: The next part indicates the total time in seconds that
// is took for the transfer to be completed. In this case, "1294" or just
// over 21 « minutes.
//
// remote-host: Next comes the IP address or host name of the individual
// who performed the transfer. In this case, it's
// "tide14.microsoft.com."
//
// file-size: This indicates the size of the transfered file in bytes.
// The size of the file transferred in this example is "4500828," bytes
// or about 4.5 MB.
//
// path and filename: The path and name of the file that was transferred
// is displayed next. In this example, it's "/pub/demos/P952_256.ZIP,"
// or more precisely, the file was called P952_256.ZIP and it was located
// in the /pub/demos subdirectory of the VSERV's FTP root.
//
// transfer-type: This next part of the entry is a single character
// indicating the type of transfer that was performed. It is either an
// "a" if it was an ascii (text) transfer, or "b" if it was a binary
// file was transferred. In this case, a binary file was retreived.
//
// special-action-flag: Next comes one or more single character flags
// indicating additional information about the file that was transferred.
// These characters can be are "C" if the file was compressed, "U" if
// the file was uncompressed, "T" if the file was tar'd (archived), or
// "_" if no special action was taken. No special action was taken in
// this example.
//
// direction: The direction of the transfer is indicated next with either
// an "o" if the transfer was outgoing (sent from your VSERV), or "I" if
// the transfer was incoming (received by your VSERV). The file in this
// example was retreived from the VSERV.
//
// access-mode: Next, an indication of the type of FTP access that the
// user was using is given. An "a" indicates that the user was using
// anonymous FTP, while "r" indicates real (user authenticated) FTP
// access. The user in this example was an anonymous FTP user.
//
// username: The username comes next, if the user had user-authenticated
// access. If the transfer was performed by an anonymous user, the Email
// address they entered as a password is used instead. In our example,
// the username is "proxyuser@microsoft.com."
//
// service-name: Next comes the name of the service being invoked for the
// transfer. In this example, as in most cases in this log file, it is
// "ftp."
//
// authentication-method: Next to last comes a number which indicates
// the authentication method being used for the transfer. In most cases
// this is a "0," as it is in this case.
//
// authenticated-user-id: At the very end of the entry comes the user id
// returned by the authentication method. A "*" indicates that the userid
// is not available, as is the case in this example.
//
//
public class LogFTP {
    
    static Logger log = LoggerFactory.getLogger("xfer");

    public synchronized void logMsg(Global global, CurrentInfo curCon, Path fName, long startTime, long size, String direction) {
        
        // Special action ("_"): These characters can be are
        // "C" if the file was compressed, "U" if the file was uncompressed,
        // "T" if the file was tar'd (archived), or "_" if no special action was taken.
        
        // Get time to run in seconds from start time to now.
        long diffTime = (new Date().getTime() - startTime) / 1000;
        String accessMode = curCon.entity.equals("anonymous") ? "a" : "r";
        String transferType = curCon.transferType == curCon.ATYPE ? "a" : "b";
    
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        String dateStr = sdf.format(new Date());
        
        StringBuffer ds = new StringBuffer(dateStr);
        ds.append(' ').append(diffTime).append(' ');
        ds.append(curCon.remoteSite).append(' ');
        ds.append(size).append(' ');
        ds.append(fName);
        ds.append(' ').append(transferType).append(" _ ");
        ds.append(direction).append(' ');
        ds.append(accessMode).append(' ');
        ds.append(curCon.entity).append(' ');
        ds.append("ftp 0 ");
        
        // Log the authenticated name or '*' if not authenticated.
        if (curCon.authName == null || curCon.authName.equals("")) {
            ds.append(global.AnonymousC);
        } else {
            ds.append(curCon.authName);
        }
        
        log.info(ds.toString());
    }
}
