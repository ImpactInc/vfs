/**
 * FTP Server Daemon
 * Copyright (C) 2000 Michael Lecuyer. All Rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * <p>
 * Or see [http://www.gnu.org/copyleft/lesser.html].
 */

package com.theorem.ftp;

import java.io.*;
import java.net.*;
import java.util.*;

import com.theorem.ftp.commands.*;


/**
 * FTP Server.
 */
public class Ftpd implements Runnable {
    
    /**
     * Copyright {@value}.
     */
    private String copyMsg = "AXL FTP Server (3.09) (C) 1998 - 2002 Michael Lecuyer";
    
    private Global global;    // ftp daemon threads copy of global.

    ThreadGroup ftpSesGroup = new ThreadGroup("FTP Session");
    
    private Authenticator authenticator;
    
    public void setAuthenticator(Authenticator authenticator) {
        this.authenticator = authenticator;
    }
    
    
    /**
     * Initialize from config files
     *
     * @param configDir
     */
    public void initialize(String configDir) {
    
        // Two thread groups.  One runs the sessions (ftpSesGroup)
        // The other waits to see if the config files change.  If so
        // it will call the ftp class with a special constructor (to re-read
        // the config files.  The intersting part about the FtpConfig proc
        // is that it will briefly suspend everyone until a new configuration
        // has been read.
        FtpConfig ftpcfg = new FtpConfig(ftpSesGroup, configDir);
    
        ThreadGroup FtpConfigGroup = new ThreadGroup("FTP Reconfigure");

        Thread FtpConfigT = new Thread(FtpConfigGroup, ftpcfg, "Configuration");
    
        // local copy for main() for printing messages.
        global = ftpcfg.getGlobal();
    
        FtpConfigT.start();
    
        ftpcfg.checkDir();
        
        if (authenticator != null) {
            global.setAuthenticator(authenticator);
        }
        global.setServerIdentification(copyMsg);
    }
    
    
    /**
     * Listens to socket and starts new sessions for each incomming connection.
     */
    @Override
    public void run() {
    
        String startMsg = copyMsg + " Running on " + global.osName;
        global.log.logMsg("");
        global.log.logMsg(startMsg);
        global.log.logMsg(copyMsg);
        global.log.logMsg("");
    
        System.out.println(startMsg);
    
        int loginCounter = 1;
    
        try {
            ServerSocket s = new ServerSocket(global.FTPPort);
        
            while (true) {
                Socket _incoming = s.accept();
            
                // update global for each ftp transaction thread.
                // global = ftpcfg.getGlobal();
            
                FtpSession fSession = new FtpSession(_incoming, loginCounter, global);
            
                String name = "FTP Session " + loginCounter;
                Thread ftpThread = new Thread(ftpSesGroup, fSession, name);
            
                ftpThread.start();
                loginCounter++;
            }
        } catch (IOException e) {
            // Don't care.
        }
    }
    
    
    /**
     * Main entry point.  Takes the configuration directory as the argument.
     *
     * @param args Command arguments.
     * The only argument supported is the name of the directory
     * where the configuration files (ftp.cfg and dir.cfg) are found.
     */
    public static void main(String[] args) {
    
        String configDir;
        if (args.length == 0) {
            configDir = ".";    // default directory.
        } else {
            configDir = args[0];
        }

        Ftpd instance = new Ftpd();

        instance.initialize(configDir);
        instance.run();
    }
    
    
}



