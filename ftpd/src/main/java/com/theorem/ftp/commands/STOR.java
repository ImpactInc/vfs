package com.theorem.ftp.commands;

import java.io.*;
import java.net.Socket;
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
        FileReceiptImpl fri = null;    // File reciept class.
        
        // If the file name is to be generated create a new path and file
        // using the current directory.
        
        if (unique) {
            while (true) {
                SimpleDateFormat sdf = new SimpleDateFormat("DDDHHmmssSSS");
                uniqueFileName = sdf.format(new Date());
                String path = curCon.curPDir + uniqueFileName;
                str = "STOU " + uniqueFileName;
                //System.out.println("File path is " + path);
                if (!new File(path).exists()) {
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
        
        String storFileName = curCon.curPDir + curCon.curFile;
        
        if (new File(storFileName).isDirectory()) {
            //global.log.logMsg("STOR: it's a directoey.");
            curCon.respond("553 Requested action not taken.");
            global.log.logMsg("STOR: " + str + " is not a file");
            return;
        }
        
        // If there's a reciept class run the external class.
        // The class's getStart method can refuse the file retrieval.
        if (global.fileClass != null) {
            fri = new FileReceiptImpl(curCon);
            String response = fri.putBefore(storFileName);
            if (response != null) {
                curCon.respond(response);
                global.log.logMsg(response);
                return;
            }
        }
        
        Socket dataSocket = curCon.dataSocket.getDataSocket(curCon);
        if (dataSocket == null) {
            global.log.logMsg("Can't create STOR socket");
            curCon.respond("425 Can't open data connection. Try using passive (PASV) transfers.)");
            return;
        }
        curCon.respond("150 File status okay; about to open " + curCon.transferText() + " connection.");
        
        // This should be a separate thread that sends data over the data connection.
        // storFile StorFile = new StorFile(socket, t, curCon);
        // storFile.go();
        
        InputStream in2 = null;
        RandomAccessFile inFile = null;
        String storFile = null;
        try {
            storFile = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);
            //global.log.logMsg("STORing file in " + storFile);
            inFile = new RandomAccessFile(storFile, "rw");
            in2 = dataSocket.getInputStream();
        } catch (IOException ioe1) {
            global.log.logMsg("Can't open STOR file: " + ioe1);
            curCon.dataSocket.closeDataSocket(dataSocket);
            curCon.respond("426 Connection closed; transfer aborted.");
            return;
        }
        
        // If the session breaks during transmission save the virtual file name
        // for future restoration.
        Restore r = new Restore(curCon);
        
        int amount;
        try {
            
            // See if a REST was left behind. Check before adding an entry or
            // we'll overwrite the REST file position.
            // And it could be an append.  The append position is used if
            // there is no restored position.
            
            long posn = r.getFilePosn(storFile);
    
            if (posn == 0 && append)    // Not restoring session, append to the file.
            {
                posn = inFile.length();
            }
            
            if (posn > 0) {
                // there was a pending REST
                inFile.seek(posn);
            } else {
                // we have to kill file contents first.  Close the file, delete
                // it and write
                inFile.close();
                new File(storFile).delete();
                inFile = new RandomAccessFile(storFile, "rw");
            }
            // Add the new entry now, since we're about to send the file.
            r.addRestInfo(storFile);
            
            if (curCon.transferType == CurrentInfo.ATYPE) {
                // ASCII file transfers are  going to be a bit slow 'cause we have to read
                // them a byte at a time to convert EOL's to our native ways.
                BufferedInputStream ins = new BufferedInputStream(in2);
                
                int ci;
                while ((ci = ins.read()) != -1) {
                    byte c = (byte)ci;
                    if (c == global.CRLFb[0]) {
                        continue;                    // Toss away CR's
                    } else if (c == global.CRLFb[1]) {
                        inFile.writeBytes("\n");    // Write whatever our platform uses for a newline.
                    } else {
                        inFile.writeByte((int)c);    // Write the byte.
                    }
                }
                
            } else {
                BufferedInputStream ins = new BufferedInputStream(in2);
                byte bb[] = new byte[10240];
                
                // Binary transfer - much faster.
                while ((amount = ins.read(bb, 0, bb.length)) != -1) {
                    byteCount += amount;
                    inFile.write(bb, 0, amount);
                }
            }
            
            in2.close();
    
            if (unique)    // Report the file name if we're generating the name
            {
                curCon.respond("250 " + uniqueFileName);
            } else {
                curCon.respond("226 transfer complete");
            }
            inFile.close();
            global.log.logMsg("Stored file " + storFile);
            curCon.dataSocket.closeDataSocket(dataSocket);
        } catch (IOException ioec) {
            try {
                inFile.close();
            } catch (IOException c) {
                ;
            }    // make sure file is closed.
            global.log.logMsg("Error communicating with STOR socket/files: " + ioec);
            curCon.respond("426 Connection closed; transfer aborted.");
            curCon.dataSocket.closeDataSocket(dataSocket);
            return;
        }
        
        
        // Delete information for REST command since everything worked
        r.removeRestInfo(storFile);
        
        if (global.fileClass != null) {
            new FileReceiptImpl(curCon).putAfter(storFile, byteCount);
        }
        
        global.fLog.logMsg(global, curCon, start, byteCount, "i");
    }
}
