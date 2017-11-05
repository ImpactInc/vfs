package com.theorem.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.Vector;
import java.util.stream.Collectors;

import com.theorem.ftp.util.Text;


/**
 * Class to hold current information & requests for the current entity.
 * Tracks working directory and read/write permissions on files/directories.
 * Home directory, if supported, is a virtual dir entry held here
 * but known to Virtual class.
 */
public class CurrentInfo {
    
    // Transfer type codes.
    public static char ATYPE = 'A';    // Ascii transfer type.
    public static char ITYPE = 'I';        // Image trasfer type (Binary).
    
    // Transfer type Text.
    private static String AMODE = "ASCII mode";
    private static String IMODE = "Binary mode";
    
    // Who's who:
    public String entity;    // person or machine name
    public String authName;    // login name as it arrived - if anonymous it's null
    public String remoteSite;    // remote site name
    public int sessionID;    // Login identifier for multiple logins of a single entity
    // Used for tracking which 'x' we're logging.
    
    // Internet connection info.
    public InetAddress remoteIP = null;
    public InetAddress localIP = null;
    public String localIPName = null;
    public PrintWriter out = null;
    public BufferedReader in = null;
    public Socket clientSocket;        // incoming client socket
    public DataSocket dataSocket = null;    // data socket (active or PASV)
    public int dataPort;                        // current data port
    public int localDataPort;                   // current data port
    public InetAddress dataIP = null;    // Current data IP address
    public Vector acct;            // Account names (if used) from ACCT command.
    
    public char transferType = ATYPE;    // Type of transfer, Ascii  (ATYPE) or Image (ITYPE).
    
    /*
    */
    // These are side effects based on what's currently being constructed as a directory and / or file.
    public String curVDir;    // current virtual root directory
    public String curFile;    // side effect of getDir();

    private Path root;
    private Path cwd;



    public Global global;    // Convience copy of global data - every function/class should use this copy.
    
    private Permission _perms;    // Permission info
    

    private Path renameFile;  // The rename is two commands, the file to rename has to be remembered on the session
    
    public final byte[] eol;
    
    /**
     * Constructor to assign the global information to this class.
     *
     * @param global Shared global information.
     */
    CurrentInfo(Global global) {
        this.global = global;
        eol = System.getProperty("line.separator").getBytes();
    }
    
    /**
     * Set up the login information after the user is authenticated.
     *
     * @param name  Entity name.
     * @param perms Permission class.
     */
    public void setLogin(String name, Permission perms) {
        entity = name;
        _perms = perms;
        root = global.authenticator.getDirectory(name);
        cwd = root;
    }
    
    /**
     * Convert a virtual directory to a physical directory.
     *
     * @param path Virtual directory path.
     * @return the physical directory path.
     */
    public Path virtToPhys(String path) throws IOException {
        
        return root.resolve(createAbsolutePath(path));
        /*
        path = createAbsolutePath(path);
        //global.log.logMsg("CurrentInfo.virtToPhys returns: " + _perms.virtToPhys(path));
        return _perms.virtToPhys(path);
        */
    }
    
    
    public Path getCwd() {
        return cwd;
    }
    
    public Path getRoot() {
        return root;
    }

    /**
     * check for read permision on directory for a file path
     * curVDir, curPDir, and curFile are set up.
     *
     * @param filePath path to check read permission for.
     * @return true if read permission is granted.
     */
    public boolean canReadFile(String filePath) {

        return true;
        /*
        //global.log.logMsg("CurrentInfo.canRead: before creatAbspath = <" + filePath + ">");
        
        filePath = createAbsolutePath(filePath);
        //global.log.logMsg("CurrentInfo.canRead: after creatAbspath = <" + filePath + ">");
        
        String pathV = getDir(filePath);
        //global.log.logMsg("CurrentInfo:canRead: getdir returned " + pathV);
    
        if (_perms.canRead(entity, pathV) == false) {
            return false;
        }
        
        setDir(filePath);
        
        return true;
        */
    }
    

    /**
     * Check for write permission on directory for a file path.
     *
     * @param dirPath Directory path to check.
     * @return true if able to write to directory.
     */
    public boolean canWriteDir(String dirPath) {

        /*
        dirPath = createAbsolutePath(dirPath);
        //global.log.logMsg("canWriteDir: trying to write to " + dirPath);
    
        if (_perms.canWrite(entity, dirPath) == false) {
            return false;
        }
        
        setDir(dirPath);
        */
        return true;
    }
    
    /**
     * Check for write permision on directory for a file path.
     *
     * @param filePath file path to be checked.
     * @return true if the file can be written.
     */
    public boolean canWriteFile(String filePath) {

        /*
        filePath = createAbsolutePath(filePath);
        
        String pathV = getDir(filePath);
        //global.log.logMsg("can write file: resulting dir is " + pathV);
    
        if (_perms.canWrite(entity, pathV) == false) {
            return false;
        }
        
        setDir(filePath);
        */
        return true;
    }
    
    
    /**
     * Extract a virtual directory from a file path.
     * We expect an absolute path.
     * Side effect - sets curFile.
     *
     * @param filePath path to a file.
     * @return virtual directory path to file.
     */
    public String getDir(Path filePath) {
    
        return root.relativize(filePath).toString();
    /*
        String pathV = null;    // full virtual path of file's directory.
        int sep;
        //global.log.logMsg("getDir: filepath arrived as: " + filePath);
        if (filePath.length() == 1) {
            //global.log.logMsg("getDir: length is 1, setting curFile to null, pathV to " + filePath);
            pathV = filePath;
            curFile = null;
        } else if ((sep = filePath.lastIndexOf('/')) >= 0) {
            // separate the file from the directory
            pathV = filePath.substring(0, sep);
            curFile = filePath.substring(sep);
            //global.log.logMsg("pathV = " + pathV + " curFile is " + curFile);
        } else {
            pathV = filePath;
        }
        
        return pathV;
      */
    }
    
    /**
     * Create an absolute virtual path. The incoming path may be relative or absolute.
     * This also resolves any ".."'s, but not past the root directory.
     * The path, if not absolute, is made from the CurWD (current working directory).
     *
     * @param path some path form.
     * @return absolute path.
     */
    public String createAbsolutePath(String path) throws IOException {
        
        if (path == null || path.length() == 0) {
            return "/";    // Sometimes this inanity comes from web browsers.
        }
        
        // Take CWD into account
        if (!path.startsWith("/")) {
            path = root.relativize(cwd).toString() + "/" + path;
        }
        ArrayList<String> stack = new ArrayList<>();
        for (String p : Arrays.asList(path.split("/"))) {
        
            if ("".equals(p)) {
                continue;
            }
            if ("~".equals(p)) {
                // We don't distinguish between home dire and root dir
                stack.clear();
                continue;
            }
            if (".".equals(p)) {
                continue;
            }
            if ("..".equals(p)) {
                if (!stack.isEmpty()) {
                    // pop
                    stack.remove(stack.size() - 1);
                }
                continue;
            }
            stack.add(p);
        }
        return stack.stream().collect(Collectors.joining("/"));
    }
    
    /**
     * Println replacement.
     * We need a CRLF TELNET sequence at the EOL.
     * We will do that with out.write.
     *
     * @param response to write to the client.
     */
    public void respond(String response) {
        out.write(response);
        out.write(global.CRLF);
        out.flush();
    }
    
    /**
     * Return a string reflecting the current transfer mode.
     *
     * @return text string.
     */
    public String transferText() {
        return (transferType == ATYPE) ? AMODE : IMODE;
    }
    
    /**
     * Return the appropriate line termination chars depending on type.
     * This is for text file information
     *
     * @return CFLF for ascii mode, LF otherwise.
     */
    public String transferEOL() {
        return (transferType == ATYPE) ? global.CRLF : global.LF;
    }


    public void setRenameFile(Path file) {
        this.renameFile = file;
    }
    
    public Path getRenameFile() {
        return renameFile;
    }


    public void setRoot(Path root) {
        this.root = root;
    }
    
    public void setCwd(Path cwd) {
        this.cwd = cwd;
    }

}
