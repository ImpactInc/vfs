package com.theorem.ftp.commands;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.theorem.ftp.*;


// STORe a file, could do STOU (store with unique file name).
// Can perform an append if append = true.
// Generate unique name if unique = true
//
public class STOR {
    
    public STOR(CurrentInfo curCon, String str, boolean append, boolean unique) {

        Global global = curCon.global;
        long start = new Date().getTime();    // aquire start time.
        long byteCount = 0;    // bytes retrieved
        String uniqueFileName = "";    // Used only if Unique is true, this is the file name, no ext.
        
        // If the file name is to be generated create a new path and file
        // using the current directory.
        
        if (unique) {
            while (true) {
                SimpleDateFormat sdf = new SimpleDateFormat("DDDHHmmssSSS");
                uniqueFileName = sdf.format(new Date());
                Path path = curCon.getCwd().resolve(uniqueFileName);
                str = "STOU " + uniqueFileName;
                //System.out.println("File path is " + path);
                if (!Files.exists(path)) {
                    break;    // quit if it's truly unique.
                }
            }
            //System.out.println("Command is now " + str);
        }
        
        // Get the file path.  Do it now (after unique) so we don't have an
        // error when the substring fails.
        str = str.substring(4).trim();
        
        if (curCon.canWriteFile(str) == false) {
            //global.log.logMsg("doesn't have permission to write file");
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("STOR: No write permission for file " + str);
            return;
        }
    
        Socket dataSocket = curCon.dataSocket.getDataSocket(curCon);

        try {
            Path storFile = curCon.virtToPhys(str);
        
            if (Files.isDirectory(storFile)) {
                //global.log.logMsg("STOR: it's a directory.");
                curCon.respond("553 Requested action not taken.");
                global.log.logMsg("STOR: " + str + " is not a file");
                return;
            }
        
            // If there's a reciept class run the external class.
            // The class's getStart method can refuse the file retrieval.
            if (global.getFileListener() != null) {
                String response = global.getFileListener().putBefore(curCon.entity, storFile);
                if (response != null) {
                    curCon.respond(response);
                    global.log.logMsg(response);
                    return;
                }
            }
        
            if (dataSocket == null) {
                global.log.logMsg("Can't create STOR socket");
                curCon.respond("425 Can't open data connection. Try using passive (PASV) transfers.)");
                return;
            }
            curCon.respond("150 File status okay; about to open " + curCon.transferText() + " connection.");
        
            // This should be a separate thread that sends data over the data connection.
            // storFile StorFile = new StorFile(socket, t, curCon);
            // storFile.go();
        
            InputStream in2 = dataSocket.getInputStream();
            
            if (curCon.transferType == CurrentInfo.ATYPE) {
                // ASCII file transfers are  going to be a bit slow 'cause we have to read
                // them a byte at a time to convert EOL's to our native ways.
                BufferedInputStream ins = new BufferedInputStream(in2);
                BufferedOutputStream os =
                        new BufferedOutputStream(Files.newOutputStream(storFile, StandardOpenOption.TRUNCATE_EXISTING));
                
                int ci;
                while ((ci = ins.read()) != -1) {
                    byte c = (byte)ci;
                    if (c == global.CRLFb[0]) {
                        continue;                    // Toss away CR's
                    } else if (c == global.CRLFb[1]) {
                        os.write(curCon.eol);    // Write whatever our platform uses for a newline.
                    }
                    os.write((int)c);    // Write the byte.
                }
                os.close();
                
            } else {
                
                Files.copy(in2, storFile);
            }
            
            in2.close();
            
            // Report the file name if we're generating the name
            if (unique) {
                curCon.respond("250 " + uniqueFileName);
            } else {
                curCon.respond("226 transfer complete");
            }
            global.log.logMsg("Stored file " + storFile);
    
            byteCount = Files.size(storFile);
            if (global.getFileListener() != null) {
                global.getFileListener().putAfter(curCon.entity, storFile, byteCount);
            }
        } catch (IOException ioec) {
            global.log.logMsg("Error communicating with STOR socket/files: " + ioec);
            curCon.respond("426 Connection closed; transfer aborted.");
            return;
        } finally {
            curCon.dataSocket.closeDataSocket(dataSocket);
        }
    
        global.fLog.logMsg(global, curCon, start, byteCount, "i");
    }
}
