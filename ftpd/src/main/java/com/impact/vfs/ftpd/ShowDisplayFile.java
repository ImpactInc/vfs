package com.impact.vfs.ftpd;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Show a display file in the current directory, if any.
 * Applies to CDUP and CWD.  Open the file and write it out
 * as continuations.  We expect this to be followed by a
 * 2?? xxx line.
 */
public class ShowDisplayFile {
    
    public ShowDisplayFile(Global global, Path cwd, PrintWriter out) {
        
        if (global.displayFile == null) {
            return;
        }
        
        Path file = cwd.resolve(global.displayFile);
        if (!Files.isReadable(file)) {
            return;
        }
    
        try {
            BufferedReader reader = Files.newBufferedReader(file);

            String line;
            while ((line = reader.readLine()) != null) {
                out.println("250- " + line);
            }
            out.flush();
            reader.close();
        } catch (IOException ioe) {
            global.log.logMsg("Failed to read display file " + file);
        }
    }
}
