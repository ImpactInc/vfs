package com.theorem.ftp;

import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;


// Deal with the home directory issue.
// There will be several possible places to search.
// Each will have been set up as a VirtualDirectory - partially filled out.
// A homedir will be created for each user (including anonymous, if practical)
// when they log in and will be deleted when they drop out.
// We assume the login name matches the directory name.
// In the case of people using other RADIUS servers (proxy servers)
// Remove everything from the '@' beyond. (should be done in login)
//
// This entry will be automatically removed when the FTP Thread dies.
//
public class HomeDirectory {
    
    final String VIRTUAL_DIR = "/user/";    // always the virtual directory.
    
    Global global;
    CurrentInfo curCon;
    
    public HomeDirectory(Global global, CurrentInfo curCon) {
        this.global = global;
        this.curCon = curCon;
    }
    
    public void setup() {
        //global.log.logMsg("Seaching for home directory");
        
        // Search for the home directory for the given name
        for (Enumeration e = global.personalDir.keys(); e.hasMoreElements(); ) {
            String propName, value, physDir, virtDir, subdir, path;
            
            propName = (String)e.nextElement();
            value = (String)global.personalDir.get(propName);
            
            // Split the file path at the {}
            int ind;
            if ((ind = value.indexOf("{}")) > -1) {
                physDir = value.substring(0, ind);
                subdir = value.substring(ind + 2);
            } else {
                physDir = value;
                subdir = "";
            }
            
            //global.log.logMsg("physDir: " + physDir);
            //global.log.logMsg("subdir: " + subdir);
    
            if (physDir == null || physDir.equals("")) {
                continue;
            }
            
            path = new FCorrectPath().fixit(physDir + "/" + curCon.entity + subdir);
            //global.log.logMsg("looking under path: " + path);
            
            File f = new File(path);
            
            if (f.isDirectory() & f.canRead()) {
                virtDir = VIRTUAL_DIR + curCon.entity + subdir;
                //global.log.logMsg("virtDir is " + virtDir);
                
                VirtualDirectory vdir = new VirtualDirectory();
                
                //global.log.logMsg("setting up virtdir = " + virtDir + " as physdir " + path);
                vdir.physDir = path;
                vdir.allow = new Hashtable();
                vdir.allow.put(curCon.entity, "rw");
                
                vdir.deny = new Hashtable();    // empty - no denials
                vdir.publicAccess = false;
                
                global.permSet.addVirtualDirectory(virtDir, vdir);
            }
        }
    }
    
}
