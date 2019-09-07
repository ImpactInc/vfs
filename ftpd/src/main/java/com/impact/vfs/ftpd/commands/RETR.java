package com.impact.vfs.ftpd.commands;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import com.impact.vfs.ftpd.*;


/**
 * File retrieval.
 */
public class RETR {
    
    public RETR(CurrentInfo curCon, String str) {

        Global global = curCon.global;
        long start = new Date().getTime();    // aquire start time.
        long byteCount;    // bytes retrieved
        
        // Get the file path:
        str = str.substring(4).trim();
        String arg = str;
    
        Path file = curCon.virtToPhys(str);

        if (curCon.canReadFile(str) == false) {
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("RETR: Requested action not taken. No permission to read " + str);
            return;
        }
    
    
        Socket t = curCon.dataSocket.getDataSocket(curCon);
        try {
            
            if (!Files.isRegularFile(file)) {
                curCon.respond("553 Requested action not taken.");
                global.log.logMsg("RETR: Requested action not taken. " + str + " isn't a file.");
                return;
            }
        
            // If there's a receipt class run the external class.
            // The class's getStart method can refuse the file retrieval.
            if (global.getFileListener() != null) {
                String response = global.getFileListener().getBefore(curCon.entity, file);
                if (response != null) {
                    curCon.respond(response);
                    global.log.logMsg(response);
                    return;
                }
            }
            
            if (t == null) {
                global.log.logMsg("Can't create RETR socket");
                curCon.respond("425 Can't open data connection. Try using passive (PASV) transfers.)");
                return;
            }
    
            byteCount = Files.size(file);
            String connMsg = "150 " + curCon.transferText() + " connection for " + arg;
            curCon.respond(connMsg + " (" + byteCount + " bytes)");
    
            OutputStream out = t.getOutputStream();
            
            if (curCon.transferType == curCon.ATYPE) {
                // ASCII file transfers are  going to be a bit slow 'cause we have to read
                // them a byte at a time to convert possible bare NL's or CRLF's to CRLF.
                // This could be a binary file so don't try to read lines.
                BufferedOutputStream outs = new BufferedOutputStream(out);
    
                InputStream in = Files.newInputStream(file);
                BufferedInputStream src = new BufferedInputStream(in);
                
                int ci;
                while ((ci = src.read()) != -1) {
                    byte c = (byte)ci;
                    if (c == global.CRLFb[0]) {
                        continue;    // Ignore CR's
                    } else if (c == global.CRLFb[1]) {
                        outs.write(global.CRLFb, 0, 2);
                    } else {
                        outs.write((int)c);    // Write the byte.
                    }
                }
                outs.flush();
                src.close();
                
            } else {
                // Binary transfer - quite fast.
                Files.copy(file, out);
            }
            out.flush();
            curCon.respond("226 transfer complete");
    
            if (global.getFileListener() != null) {
                global.getFileListener().getAfter(curCon.entity, file, byteCount);
            }
        } catch (IOException e) {
            global.log.logMsg("Error communicating with RETR socket/file: " + e);
            curCon.respond("426 Connection closed; transfer aborted.");
            return;
        } finally {
            curCon.dataSocket.closeDataSocket(t);
        }
        
        // log the successful transaction.
        global.fLog.logMsg(global, curCon, file, start, byteCount, "o");
    }
}
