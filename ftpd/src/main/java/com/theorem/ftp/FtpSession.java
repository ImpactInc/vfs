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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.theorem.ftp.commands.*;


/**
 * FTP Server.
 */
public class FtpSession implements Runnable {

    private Socket _incoming;
    private int _sessionID;
    static Global global;    // ftp daemon threads copy of global.
    
    /**
     * Constructor for running new session.
     *
     * @param insocket Socket connection
     * @param loginCounter current login count.
     * @param global Global information.
     */
    public FtpSession(Socket insocket, int loginCounter, Global global) {
        _incoming = insocket;
        _sessionID = loginCounter;
        this.global = global;
    }
    
    /**
     * Run the Session thread.
     */
    public void run() {

        CurrentInfo curCon = new CurrentInfo(global);        // current connection information.
        boolean loggedIn = false;                            // assume the newcomer isn't logged in yet.
        
        curCon.sessionID = _sessionID;                       // note which instance of an entity logged in.
        curCon.entity = "";                                  // none set up, but not null for other things
        curCon.clientSocket = _incoming;                     // set curCon's socket
        
        try {
            _incoming.setSoTimeout(global.FTPTimeout);
        } catch (IOException ioei) {
            // don't really care.
        }
        
        curCon.remoteIP = _incoming.getInetAddress();
        curCon.dataIP = curCon.remoteIP;
        curCon.localIP = _incoming.getLocalAddress();
        global.log.logMsg("curCon.localIP = " + curCon.localIP);
        curCon.localIPName = curCon.localIP.getHostAddress().replace('.', ',');
        
        global.log.logMsg("Accepting Connection from " + curCon.remoteIP);
        
        
        // Get default data port N-1 local -> N+1 remote
        curCon.dataPort = _incoming.getPort() + 1;
        curCon.localDataPort = _incoming.getLocalPort() - 1;
    
        try {
            curCon.in = new BufferedReader(new InputStreamReader(_incoming.getInputStream()));
            curCon.out = new PrintWriter(_incoming.getOutputStream(), true);
        } catch (IOException ioe) {
            global.log.logMsg("Can't open input & output to command socket.");
            System.err.println("Can't open input & output to command socket.");
            return;
        }
        
        // See if there are enough connections left.
        if (Clients.checkConnections() == false) {
            curCon.respond("530 Too many connections  Please try again later.");
            return;
        }
        
        curCon.respond("220 " + global.getServerIdentification());
        
        // loop forever waiting for commands.
        while (true) {
            
            // Note:
            // Mickeysoft sends this: [ÿôÿòABOR] to abort a transfer or something. (Don't chant
            // the brackets!)  Aborts aren't handled because we're a one client thread operation
            // right now.  Perhaps someday we'll thread the transfers themselves and support
            // aborts like a real FTP server.
            
            String str;
            
            try {
                str = curCon.in.readLine();
            } catch (IOException ioea) {
                // Error or timeout.  Close connection.
                curCon.respond("221 Service closing control connection.");
                curCon.out.flush();
                try {
                    _incoming.close();
                } catch (IOException ioek) {
                    // ignore
                }
                Clients.releaseConnection(curCon.entity);
                break;
            }
    
            // Quit on null commands (EOF)
            if (str == null) {
                break;
            }
            
            // Make a copy of a normalized command in upper case.
            String ustr = str.trim().toUpperCase();
    
            if (ustr.length() == 0) {
                continue;
            }
            
            // Log commands if asked.
            if (global.logCommands) {
                // If the command is PASS then we won't show the password.
                // If The command is a malformed PASS then the password will be shown.
                // In the day and age of good clients, hand typing commands though telnet is probably a lost art.
                // Anonymous login passwords are printed.
    
                if (curCon.entity != null && !curCon.entity.equals("anonymous")
                        && str.trim().toUpperCase().startsWith("PASS")) {
                    global.log.logMsg("Command: [PASS *************]");
                } else {
                    global.log.logMsg("Command: [" + str + "]");
                }
            }
            
            // Before we go to the general command set we force a login.
            // Accept only USER, PASS, and QUIT at this point.
            if (ustr.startsWith("USER")) {
                new USER(curCon, str);
                continue;
            } else if (ustr.startsWith("PASS")) {
                PASS pass = new PASS(curCon, str);
                loggedIn = pass.loggedIn();
                continue;
            } else if (ustr.startsWith("QUIT")) {
                // Can always quit!
                break;
            } else if (ustr.startsWith("HELP")) {
                // Can always get help.
                new HELP(curCon, str);
                continue;
            }
            
            // Always check to see if we're logged in before processing any commands.
            if (loggedIn == false) {
                curCon.respond("530 Not logged in.");
                continue;
            } else if (ustr.startsWith("LIST") || ustr.startsWith("NLST")) {
                new LIST(curCon, str);
            } else if (ustr.startsWith("RETR")) {
                new RETR(curCon, str);
            } else if (ustr.startsWith("SIZE")) {
                new SIZE(curCon, str);
            } else if (ustr.startsWith("MDTM")) {
                new MDTM(curCon, str);
            } else if (ustr.startsWith("PASV")) {
                new PASV(curCon, str);
            } else if (ustr.startsWith("RNFR")) {
                new RNFR(curCon, str);
            } else if (ustr.startsWith("RNTO")) {
                new RNTO(curCon, str);
            } else if (ustr.startsWith("STOR")) {
                // No Append, No generation of unique name
                new STOR(curCon, str, false);    // false means no append.
            } else if (ustr.startsWith("STOU")) {
                // No Append, Generation of unique name
                new STOR(curCon, str, true);    // false means no append.
            } else if (ustr.startsWith("TYPE")) {
                new TYPE(curCon, str);
            } else if (ustr.startsWith("ALLO")) {
                // ALLOcate space - treat like NOOP if not required.
                new NOOP(curCon, str);
            } else if (ustr.startsWith("NOOP")) {
                new NOOP(curCon, str);
            } else if (ustr.startsWith("STAT")) {
                new STAT(curCon, str);
            } else if (ustr.startsWith("DELE")) {
                new DELE(curCon, str);
            } else if (ustr.startsWith("REIN")) {
                // Reinitialize the entity.  Restore the connection
                // to pre-USER command state (they aren't logged in).
                new UNKNOWN(curCon, str);
            } else if (ustr.startsWith("CDUP") || ustr.startsWith("XCUP")) {
                new CDUP(curCon, str);
            } else if (ustr.startsWith("CWD") || ustr.startsWith("XCWD")) {
                new CWD(curCon, str);
            } else if (ustr.startsWith("PWD") || ustr.startsWith("XPWD")) {
                new PWD(curCon, str);
            } else if (ustr.startsWith("PORT")) {
                new PORT(curCon, str);
            } else if (ustr.startsWith("MKD") || ustr.startsWith("XMKD")) {
                new MKD(curCon, str);
            } else if (ustr.startsWith("RMD") || ustr.startsWith("XRMD")) {
                new RMD(curCon, str);
            } else if (ustr.startsWith("REST")) {
                new REST(curCon, str);
            } else if (ustr.startsWith("SITE")) {
                // SITE  The following nonstandard or UNIX-specific commands are supported by the SITE request:
                // UMASK Changes umask (SITE UMASK 002).
                // IDLE  Sets idler time (SITE IDLE 60).
                // CHMOD Changes mode of a file (SITE CHMOD 755 FileName).
                // These (except for idler time) can't be set within pure Java 2.x
                new SITE(curCon, str);
            } else if (ustr.startsWith("APPE")) {
                // APPEnds to the file
                // Append, No generation of unique name
                new STOR(curCon, str, true, false);
            } else if (ustr.startsWith("SYST")) {
                new SYST(curCon, str);
            } else {
                // Unknown command
                new UNKNOWN(curCon, str);
            }
        }
        
        // The command section falls through to here on a break from the long while loop.
        global.log.logMsg("Quit");
        
        curCon.respond("221 Service closing control connection.");
        curCon.out.flush();
        Clients.releaseConnection(Thread.currentThread().getName());
        try {
            _incoming.close();
        } catch (IOException ioed) {
            System.err.println("Can't close Socket: " + ioed);
        }
    }
    
}



