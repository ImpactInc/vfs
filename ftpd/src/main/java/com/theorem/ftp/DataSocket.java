package com.theorem.ftp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


/**
 * Class to handle the data socket.
 * The data socket may be active or passive.
 */
public class DataSocket {
    
    ServerSocket _pasvSSocket = null;    // Server socket for PASV transfers (null when no pasv transfer)
    Socket _pasvSocket = null;    // socket for PASV transfers (null when no pasv transfer)
    Global global;    // local version of the global class for convenience
    
    /**
     * Constructor to make global information accessible.
     *
     * @param global Global information.
     */
    public DataSocket(Global global) {
        this.global = global;
        _pasvSocket = null;
        _pasvSSocket = null;
    }
    
    /**
     * Create the pasv server socket and wait for connection.
     * return false if something breaks.
     * handles PASV setup responses.
     *
     * @param curCon CurrentInfo object for this entity.
     * @return true if the socket was opened sucessfuly.
     */
    public boolean createPasvSocket(CurrentInfo curCon) {

        if (_pasvSSocket != null) {
            // some automated systems open and close sockets for fun.
            closeDataSocket(_pasvSocket);
        }
        
        int sport;
        try {
            _pasvSSocket = bind(curCon.localIP);
            _pasvSSocket.setSoTimeout(global.FTPTimeout);    // timeout on socket
            sport = _pasvSSocket.getLocalPort();
        } catch (IOException ioei) {
            global.log.logMsg("Can't create PASV socket");
            curCon.respond("425 Can't open data connection.");
            return false;
        }
        
        int p1 = sport >> 8;
        int p2 = sport & 0xff;
        
        String tmp = curCon.localIPName;
        curCon.respond("227 Entering Passive Mode. (" + tmp + "," + p1 + "," + p2 + ')');
        
        try {
            _pasvSocket = _pasvSSocket.accept();
        } catch (IOException ioei) {
            
            global.log.logMsg("Can't open PASV socket for " + _pasvSSocket + " Error: " + ioei);
            curCon.respond("425 Can't open data connection.");
            return false;
        }
        return true;
    }
    
    /**
     * Bind the passive FTP socket to a particular local address.
     *
     * @param bindAddr Address to bind.
     * @return server socket.
     * @throws SocketException if the socket can't be bound to the local address.
     */
    private ServerSocket bind(InetAddress bindAddr) throws SocketException {
        // Binding an address requires us to a selected address.
        // Just perform a linear search through likely ports.
        ServerSocket socket = null;
        int bindPort = 0;
        
        int loopCount = 0;    // Number of times we've searched for a port.
        
        if (bindPort == 0) {
            bindPort = 2048;
        }
        
        // Create the socket.
        // Loop through looking for a suitable port.
        // If we run out of ports, loop one more time from the bottom.
        for (; bindPort < 65536; bindPort++) {
            try {
                socket = new ServerSocket(bindPort, 1, bindAddr);
                break;
            } catch (Exception se) {
                // Hmm, this could take a while if there aren't any sockets available.
                // It would be a very busy machine if this happened.
                if (bindPort >= 65535) {
                    // If we've been around the block twice it's an error.
                    if (++loopCount >= 2) {
                        throw new SocketException("Can't find a suitable port to open.");
                    }
                    
                    bindPort = 2048;    // Start again with a different port.
                }
            }
        }
        
        return socket;
    }
    
    /**
     * Return the data transfer socket.
     * Either the pasv socket or a new outbound socket.
     *
     * @param curCon CurrentInfo object for this entity.
     * @return socket opened.
     */
    public Socket getDataSocket(CurrentInfo curCon) {
        if (_pasvSSocket == null) {
            try {
                return new Socket(curCon.dataIP, curCon.dataPort);
            } catch (IOException ioe) {
                // This could be caused by a NAT'd private address, try the actual remote address.
                try {
                    return new Socket(curCon.remoteIP, curCon.dataPort);
                } catch (IOException ioe2) {
                    System.err.println("Can't create DATA socket: " + ioe2);
                    global.log.logMsg("Can't create DATA socket: " + ioe2);
                    return null;
                }
            }
        }
        
        return _pasvSocket;
    }
    
    /**
     * Close the data socket.
     *
     * @param s Socket to close.
     */
    public void closeDataSocket(Socket s) {
        try {
            s.close();
            if (_pasvSSocket != null) {
                _pasvSSocket.close();
            }
            
        } catch (IOException ioe) {
            global.log.logMsg("Can't close DATA socket: " + ioe);
        }
        
        _pasvSSocket = null;
        _pasvSocket = null;
    }
    
}
