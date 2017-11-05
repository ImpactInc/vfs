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
import java.nio.file.Path;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    
    /**
     * Initialize from config
     */
    public void initialize() {
    
        FtpConfig ftpcfg = new FtpConfig();
    
        // local copy for main() for printing messages.
        global = ftpcfg.getGlobal();
    
        global.setServerIdentification(copyMsg);
    }
    
    
    /**
     * Listens to socket and starts new sessions for each incomming connection.
     */
    @Override
    public void run() {
        
        global.log.logMsg(copyMsg + " Running on " + System.getProperty("os.name"));
    
        int loginCounter = 1;
    
        try {
            ServerSocket s = new ServerSocket(global.FTPPort);
        
            while (true) {
                Socket _incoming = s.accept();
                // Set the keep-alive option on the socket for the control channel to prevent it from being
                // disconnected during long transfers.
                _incoming.setKeepAlive(true);
                
                // update global for each ftp transaction thread.
                // global = ftpcfg.getGlobal();
            
                FtpSession fSession = new FtpSession(_incoming, loginCounter, global);
            
                String name = "FTP Session " + loginCounter;
                Thread ftpThread = new Thread(ftpSesGroup, fSession, name);
            
                ftpThread.start();
                loginCounter++;
            }
        } catch (IOException e) {
            global.log.logMsg(e.getMessage());
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
    
        Ftpd instance = new Ftpd();

        instance.initialize();
    
        instance.global.FTPPort = 12121;

        instance.global.setAuthenticator(new Authenticator() {
    
            Path root = new File(System.getProperty("user.home"), "tmp").toPath();
    
            @Override
            public boolean authenticate(String name, String password) {
                return true;
            }
    
            @Override
            public Path getPhysicalRoot(String name) {
                return root;
            }

            @Override
            public Path getVirtualRoot(String name) {
                return root;
            }

        });
        
        instance.run();
    }
    
    
}



