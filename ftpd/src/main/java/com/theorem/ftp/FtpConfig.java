package com.theorem.ftp;

import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import com.theorem.ftp.util.Text;


/**
 * Class to suspend FTP sessions (briefly) and reread the configuration when
 * it changes.
 */
class FtpConfig implements Runnable {
    
    Global global;    // Global information from the config table cloned
    // for each ftp connection.
    
    final long MINUTES = 0;
    final long SECONDS = 30;
    final long SLEEPTIME = (MINUTES * 60 + SECONDS) * 1000;
    
    private ThreadGroup _sessionGroup = null;
    
    FtpConfig(ThreadGroup sessionGroup, String configDir) {
        _sessionGroup = sessionGroup;
        
        this.global = new Global();
        
        synchronized (global) {
            //global.log.logMsg("FTPConfig: Configdir is " + configDir);
            readConfig(configDir);    // read the configuration files from this directory.
        }
    }
    
    // keep tabs on the configuration files - check for changes.
    //
    public void run() {
        // Reduce priority:
        int priority = Thread.currentThread().getPriority();
        priority = (priority + Thread.MIN_PRIORITY) / 2;
        Thread.currentThread().setPriority(priority);
        
        // Get the file handles.
        String iFile = global.configDir + "/ftp.cfg";
        iFile = new FCorrectPath().fixit(iFile);
        File cfgInfo = new File(iFile);
        long cfgModTime = cfgInfo.lastModified();
        
        iFile = global.configDir + "/dir.cfg";
        iFile = new FCorrectPath().fixit(iFile);
        File dirInfo = new File(iFile);
        long dirModTime = dirInfo.lastModified();
        
        while (true) {
            boolean readFile = false;    // assume we don't read the config file.
            long modTimeNow;
            
            try {
                Thread.sleep(SLEEPTIME);
            } catch (InterruptedException ie) {
                ;
            }
            
            // Check the file times to see if either configuration file changed.
            modTimeNow = cfgInfo.lastModified();
            if (modTimeNow > cfgModTime) {
                cfgModTime = modTimeNow;
                readFile = true;
            } else {
                modTimeNow = dirInfo.lastModified();
                if (modTimeNow > dirModTime) {
                    dirModTime = modTimeNow;
                    readFile = true;
                }
            }
            
            if (readFile == true) {
                
                synchronized (global) {
                    readConfig(global.configDir);    // read the configuration files from this directory.
                }
                
                String note = "Configuration changed";
                global.log.logMsg(note);
                checkDir();
            }
            
        }
    }
    
    /**
     * Return the current global data.
     * This is largely configuration information and other global needs.
     * Each ftp client thread should get one of these when it starts.
     * This procedure allows configurations to be updated for new threads.
     *
     * @return the global information class.
     */
    public Global getGlobal() {
        Global newGlobal;
        
        synchronized (global) {
            newGlobal = global.newClone();
        }
        
        return newGlobal;
    }
    
    //
    // Read the configuration file for valuable information and prizes.
    //
    void readConfig(String configDir) {
        String value;
        
        global.configDir = configDir;
        String cfgFile = global.configDir + "/ftp.cfg";
        
        global.welcomeFile = configDir + "/welcome.txt";
        
        Properties config = new Properties();
        
        try {
            BufferedInputStream cfgin = new BufferedInputStream(new FileInputStream(cfgFile));
            
            config.load(cfgin);
            cfgin.close();
        } catch (IOException ioe) {
            System.err.println("Can't find or read config file - quiting: " + ioe);
        }
        
        value = config.getProperty("server.logfile", "ftpsys.log");
        global.log = new LogMsg(value);
        global.log.logMsg("Configuring server:");
        
        value = config.getProperty("server.xferlog", "ftp.log");
        global.fLog = new LogFTP(value);
        
        // get the port number
        // Due to a documentation problem the properties was variously described as
        // 'server.port' and 'ftp.port'.  Accept either.
        value = config.getProperty("server.port", "21");
        if (value.equals("21")) {
            value = config.getProperty("ftp.port", "21");
        }
        try {
            global.FTPPort = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.println("Can't read ftp.port: invalid number: " + value);
            System.exit(1);
        }
        global.log.logMsg("Command Port: " + global.FTPPort);
        
        value = config.getProperty("server.timeout", "10");
        try {
            global.FTPTimeout = Integer.parseInt(value) * 1000 * 60;
        } catch (NumberFormatException e) {
            System.err.println("Can't read server.timeout: invalid number: " + value);
            System.exit(1);
        }
        global.log.logMsg("Timeout: " + value + " seconds");
        
        value = config.getProperty("anonymous.allow", "no");
        
        // See if anonymous is wanted.  Default is no. Default count is 10.
        Clients.allowAnonymous(value.equals("yes") ? true : false);
        global.log.logMsg("Anonymous clients: " + Clients.allowAnonymous);
        
        value = config.getProperty("anonymous.max", "0");
        try {
            int max = Integer.parseInt(value);
            Clients.setMaxAnonymous(max);
        } catch (NumberFormatException e) {
            System.err.println("Can't read anonymous.max: invalid number: " + value);
            System.exit(1);
        }
        if (Clients.allowAnonymous) {
            global.log.logMsg("Maximum Anonymous logins: " + value);
        }
        
        // unlimited connections is the default - set to 0.
        value = config.getProperty("connections.max", "0");
        try {
            int max = Integer.parseInt(value);
            Clients.setMaxConnections(max);
        } catch (NumberFormatException e) {
            System.err.println("Can't read connections.max: invalid number: " + value);
            System.exit(1);
        }
        global.log.logMsg("Maximum connections: " + value);
        
        global.permSet = new Permission(global);
        
        // read the directory configuration file.
        value = configDir + "/dir.cfg";
        global.permSet._permTable = new Hashtable();
        readDirTable(value);
        
        // This is dependent on the permission table being read.
        global.homeDirectory = "/";    // always this, used to be configurable.
        
        // get the OS and set up some flags.  Required for determining
        // dates for files.
        Properties prop = System.getProperties();
        if ((global.osName = prop.getProperty("os.name")) == null) {
            global.osName = "UNKNOWN";
        } else {
            global.log.logMsg("OS Name to display: " + global.osName);
        }
        
        // Get some alternative ways to determing authentication.
        global.passwordClass = config.getProperty("password.class");
        global.passwordFile = config.getProperty("password.file");
    
        if (global.passwordClass != null) {
            global.log.logMsg("Using password class: " + global.passwordClass + ".");
        }
        
        if (global.passwordFile != null) {
            global.passwordFile = configDir + "/" + global.passwordFile;
            global.log.logMsg("Using password file: " + global.passwordFile);
        }
        
        // Get the file upload and download notification classes
        global.fileClass = config.getProperty("filereceipt.class");
        if (global.fileClass != null) {
            global.log.logMsg("Using filereceipt class: " + global.fileClass);
        }
        
        // get the display file name - this will be displayed if it appears
        // in a directory after a chdir.
        global.displayFile = config.getProperty("server.display");
        if (global.displayFile != null) {
            global.log.logMsg("Directory display file: " + global.displayFile);
        }
        
        // Determine if we should log server commands.
        value = config.getProperty("server.logcommands", "no");
        global.logCommands = value.equalsIgnoreCase("yes") ? true : false;
        global.log.logMsg("Server will display all commands:" + global.logCommands);
        
        // Look for N.user properties
        // This may be set up either in the properties or the dir.cfg
        if (global.personalDir == null) {
            global.personalDir = new Hashtable();
        }
        
        final String sname = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int slen = sname.length();
        for (int nindex = 0; nindex < slen; nindex++) {
            String prefix, propName;
            
            // This prefix is used to generate the server names for the properties.
            prefix = sname.charAt(nindex) + ".";
            propName = prefix + "user";
            value = config.getProperty(propName);
            //System.out.println("Looking at prop name " + propName);
    
            if (value == null) {
                continue;
            }
            
            global.personalDir.put(propName, value);
            //System.out.println("Physdir = " + propName + " value = " + value);
        }
    
        global.setAuthenticator(new PasswordFile(global.passwordFile));
    }
    
    
    /**
     * Read a table of Directory mappings & permissions into a structure.
     *
     * @param fpath file path to table.  return the created vector or
     * @return a Hashtable (<code>extends Vector</code>) of directory maps.
     * or <code>null</code> if the file doesn't exist.
     */
    private void readDirTable(String fpath) {
        RandomAccessFile in = null;
        String line, token1;
        Hashtable groupTable = new Hashtable();
        int linecount = 0;
        int errorcount = 0;
        int i;
        
        fpath = new FCorrectPath().fixit(fpath);
        
        try {
            in = new RandomAccessFile(fpath, "r");
        } catch (IOException e) {
            System.err.println(fpath + ": File not found.  This file will be ignored.");
            return;
        }
        while (true)    // <<----- Note - this odd style only saves indenting.
        {
            try {
                if ((line = in.readLine()) == null) {
                    break;
                }
            
                line.trim();    // get rid of excess white space
                //System.out.println("Reading " + line + " from " + fpath);
                linecount++;
            
                // skip comments.
                if (line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
            
                // Split line into two (or more parts).
                StringTokenizer st = new StringTokenizer(line);
                int tcount = st.countTokens();
            
                if (tcount == 0) {
                    continue;    // empty line.
                }
            
                if (tcount < 2) {
                    if (errorcount++ == 0) {
                        System.err.println(fpath + ": Error:");
                    }
                    System.err.println("Line " + linecount + ": incomplete specification [" + line + "]");
                    continue;
                }
            
                token1 = st.nextToken();
            
                // If the token is the word "group" it's a group thing.
                if (token1.equals("group")) {
                    //System.out.println("Found a group: " + token1);
                    String cname;
                
                    if (tcount < 3) {
                        if (errorcount++ == 0) {
                            System.err.println(fpath + ": Error:");
                        }
                        System.err.println(fpath + ": Error: group line ignored.");
                        continue;
                    }
                
                    cname = st.nextToken();    // get the group name
                
                    // Check for interesting cnames: '*'
                    if (cname.equals(global.Anonymous)) {
                        if (errorcount++ == 0) {
                            System.err.println(fpath + ": Error:");
                        }
                        System.err.println(fpath + ": Error: bad group name, group ignored.");
                        continue;
                    }
                    if (errorcount > 0) {
                        continue;
                    }
                
                    String clist = "";
                    // Collect the rest of the names for the list.
                    for (i = 0; i < tcount - 2; i++) {
                        clist += st.nextToken() + ' ';
                    }
                
                    //System.out.println("Saving cname " + cname + " as clist " + clist);
                    groupTable.put(cname, clist);
                    continue;
                }
            
                // First token is the virtual directory.
                // Second token is the actual directory.
                // Remaining tokens are permissions.
                // Modifiers on permissions are '!' (deny) and '+' (write)
                // Default (no modifier) is for read.
                // /test SYS:/tmp * !mjl +gina
                // Virtual directory "/test" is really "SYS:/tmp" and
                // anyone may read (except mjl) and gina may write.
            
                VirtualDirectory virtual = new VirtualDirectory();
            
                String virtDir = token1;
            
                // See if the directory is a user definition directory.
                // It'll look like ?.user where ? is a letter.
                if (virtDir.regionMatches(false, 1, ".user", 0, 5)) {
                    // matched a user definition
                    if (global.personalDir == null) {
                        global.personalDir = new Hashtable();
                    }
                
                    String userDir = st.nextToken();
                
                    global.personalDir.put(virtDir, userDir);
                    continue;
                }
            
                // It wasn't a user directory, it's a virtual one.
            
                virtual.physDir = st.nextToken();
                virtual.allow = new Hashtable();
                virtual.deny = new Hashtable();
                virtual.publicAccess = false;    // assume no public access
            
                while (st.hasMoreTokens()) {
                    // check for permissions.
                    // They look like any of the following:
                    // * - public - anonymous
                    // xxx - read permission
                    // +xxx - read+write permissions
                    // !xxx - deny permission
                
                    String perm = st.nextToken();
                
                    // If the name is a group list, expand the list as a substitution.
                    // If there's no group list just use the current name.
                    // The special designation '@' is for any logged in user.
                
                    String name;
                    char permChar = perm.charAt(0);
                    if (permChar == '!' || permChar == '+') {
                        name = perm.substring(1);
                    } else {
                        name = perm;
                    }
                
                    String groupList = (String)groupTable.get(name);
                    String nameArray[];
                
                    if (groupList != null) {
                        //System.out.println("getting name array from group list: " + groupList);
                        nameArray = Text.split(groupList);
                    } else {
                        nameArray = Text.split(name);
                    }
                
                    // Go through the list and create permissions.
                    for (i = 0; i < nameArray.length; i++) {
                        name = nameArray[i];
                        //System.out.println("Name = " + nameArray[i]);
                    
                        switch (permChar) {
                            case '!':
                                virtual.deny.put(name, "");
                                //System.out.println("dir.cfg: denying " + name);
                                break;
                        
                            case '+':
                                virtual.allow.put(name, "rw");    // allow read & write
                                //System.out.println("dir.cfg: allowing write " + name);
                                break;
                        
                            default:
                                //System.out.println("dir.cfg: allowing read " + name);
                                virtual.allow.put(name, "r");    // allow read
                                break;
                        }
                    }
                }
            
                // Consolidation: If someone is on both lists, they're removed from the allow list.
                for (Enumeration e = virtual.deny.keys(); e.hasMoreElements(); ) {
                    String key = (String)e.nextElement();
                    if (virtual.allow.containsKey(key)) {
                        if (key.equals(global.Anonymous)) {
                            virtual.publicAccess = false;
                        }
                        virtual.allow.remove(key);
                    }
                }
            
                // Determine if the directory has public access.
                if (virtual.allow.containsKey(global.Anonymous)) {
                    virtual.publicAccess = true;
                }
            
                global.permSet.addVirtualDirectory(virtDir, virtual);
            
            } catch (IOException ioel) {
                System.err.println("Error Reading Directory mappings: " + ioel);
                global.log.logMsg("Error Reading Directory mappings: " + ioel);
                return;
            }
        }
        try {
            in.close();
        } catch (IOException ee) {
            ;
        }
        
        // Check to see if there's a root directory set up.
        if (global.permSet._permTable.get("/") == null) {
            String msg1 = "Warning: No root ('/') directory has been defined.";
            String msg2 = "Please create one in your dir.cfg file and make it publicly accessible (Eg: '/ /tmp *').";
            System.err.println(msg1);
            System.err.println(msg2);
            global.log.logMsg(msg1);
            global.log.logMsg(msg2);
        }
        
        return;
    }
    
    /**
     * Check the directories for access.
     * If they can't be accessed report this.
     */
    void checkDir() {
        String msg;
        Permission p = global.permSet;
        Hashtable vList = p.getVirDirList();
        VirtualDirectory vroot;
        
        // Check specifically for a virtual root and make sure it has a non-root directory.
        vroot = (VirtualDirectory)vList.get("/");
        if (vroot == null) {
            msg = "Virtual Directory for root (/)  is missing - Can't run server.";
            global.log.logMsg(msg);
            System.err.println(msg);
            System.exit(1);
        }
        
        if (vroot.physDir.endsWith("/")) {
            msg = "Virtual Directory for root (/) is set to a root device - Can't run server.";
            global.log.logMsg(msg);
            System.err.println(msg);
            System.exit(1);
        }
        
        
        for (Enumeration e = p.enumVirtDirList(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            VirtualDirectory v = (VirtualDirectory)vList.get(key);
            
            //System.out.println("chkdir: checking vdir " + key + " path " + key + " answer: " + p.physDirExists(key, key));
            if (p.physDirExists(key, key) == false) {
                msg = "Virtual Directory " + key + " can't access physical directory " + v.physDir;
                global.log.logMsg(msg);
            } else {
                msg = "Virtual Directory " + key + ": " + v;
                global.log.logMsg(msg);
            }
        }
    }
    
}
