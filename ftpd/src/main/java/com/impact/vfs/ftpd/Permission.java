package com.impact.vfs.ftpd;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;


/**
 * Permission class to determine if something can be accessed by an  entity.
 */
public class Permission {
    
    Hashtable<String, VirtualDirectory> _permTable = null;
    Global global;    // convenience copy of the global data structure
    
    /**
     * Constructor
     *
     * @param global CurrentInfo object for this entity.
     */
    Permission(Global global) {
        this.global = global;
    }
    
    
    /**
     * Return the virtual home directory
     *
     * @return the home directory.
     */
    public String getHomeDir() {
        return global.homeDirectory;
    }
    
    /**
     * Return an enumeration of the virtual directory list.
     * <p>
     * Some things like this list.  The resulting list is not
     * checked for permissions here.
     *
     * @return an enumeration of the virtual directory table's keys.
     */
    public Enumeration<String> enumVirtDirList() {
        return _permTable.keys();
    }
    
    /**
     * Return the list of virtual directories.
     *
     * @return table of virtual directories.
     */
    public Hashtable<String, VirtualDirectory> getVirDirList() {
        return _permTable;
    }
    
    /**
     * Return the physical path from the virtual path
     * Assumes the mapping exists.  If there's a problem it's
     * resolved later when a bad mapping is used.
     *
     * @param vDir Virtual directory.
     * @return physical directory.
     */
    public String virtToPhys(String vDir) {

        String vDirRoot = longestVirtDir(vDir);
        String physDir = _permTable.get(vDirRoot).physDir;
        
        //global.log.logMsg("Permission:virtToPhys: getting vRootDir: " + vDirRoot + " vDir: " + vDir + " physDir: " + physDir);
        
        if (vDirRoot.equals("/")) {
            vDir = vDir.substring(vDirRoot.length() - 1);
            //global.log.logMsg("Permission:virtToPhys: vDirRoot is '/', vDir = " + vDir);
        } else {
            vDir = vDir.substring(vDirRoot.length());
            //global.log.logMsg("Permission:virtToPhys: vDirRoot is NOT '/', vDir = " + vDir);
        }
        
        //global.log.logMsg("vDir set to: " + vDir);
        //global.log.logMsg("Permission:virtToPhys:Returning: " + physDir + global.permSet.resolve(vDir));
        return physDir + global.permSet.resolve(vDir);
    }
    
    /**
     * Check permissions.
     * See if they're denied access first.
     * See if they just want to read a public area.
     * Finally see if they have the required specific permission to
     * the area.  PermReqd is a one letter permission.
     * A key of '@' is for all real logged in entities (everyone except
     * anoymous).
     *
     * @param vDir     Virtual directory.
     * @param entity   Entity name.
     * @param permReqd a permission letter.
     * @return true if the entity has permission.
     */
    private boolean checkPerms(String vDir, String entity, String permReqd) {

        VirtualDirectory vD = _permTable.get(vDir);
        //global.log.logMsg("checkperms: vD = " + vD);
        if (vD == null) {
            return false;    // terrible error so we'll say no permission.
        }
        
        // See if real logged in people are denied AND the entity isn't in the allow list.
        // This is the reverse of All Logged in Denied and is the entity permitted.
        if (vD.deny.contains(global.AllUsers) && !vD.allow.contains(entity)) {
            return false;
        }
        
        // Strongest permission is "deny".
        //System.out.println("checkPerms: vd = " + vDir + " entity: " +entity + " deny value = " + vD.deny.containsKey(entity));
        if (vD.deny.containsKey(entity)) {
            return false;    // immediate return with the bad news
        }
        
        // If they just want read permission and there's public access
        // all is well.
        if (vD.publicAccess == true && permReqd.equals("r")) {
            return true;    // they have public access at this level - continue
        }
        
        // get the specific permission for the entity (if any)
        if (vD.allow.containsKey(entity)) {
            if (((String)vD.allow.get(entity)).indexOf(permReqd) >= 0) {
                return true;    // they have the right permission
            }
        }
        
        // See if real logged in people have access.
        if (vD.allow.containsKey(global.AllUsers) && !entity.equals("anonymous")) {
            if (((String)vD.allow.get(global.AllUsers)).indexOf(permReqd) >= 0) {
                return true;    // they have the right permission
            }
        }
        
        if (vD.deny.contains(global.AllUsers) && !entity.equals("anonymous")) {
            if (((String)vD.allow.get(global.AllUsers)).indexOf(permReqd) >= 0) {
                return true;    // they have the right permission
            }
        }
        
        return false;    // fell through and didn't get permission
    }
    
    /**
     * Search for the longest matching virtual directory in the given path.
     *
     * @param path
     * @return Longest matching virtual directory.
     */
    private String longestVirtDir(String path) {
        // do this by breaking up the virtual directory into
        // path parts and seeing finding the longest match by
        // seeing if the permission list contains the dir.
        // Take note when it does start and not when it
        // no longer does.
        
        //global.log.logMsg("perms.longestVirtDir; vdir = <" + path + ">");
        //global.log.logMsg("longestVirtDir: resolving  " + path);
        path = resolve(path);    // resolve path for ".." and "." entries.
        //global.log.logMsg("longestVirtDir: resolved as " + path);
        
        // If using Home dirs, the virtual directory is ~entity.
        // This always has a physdir of the physical directory.
        
        StringTokenizer st = new StringTokenizer(path, "/", true);
        int tcount = st.countTokens();
        int i;
        String test = "";
        
        // Part I, find the starting point for matching the longest.
        for (i = 0; i < tcount; i++) {
            test += st.nextToken();
            //global.log.logMsg("Test = " + test);
            if (_permTable.containsKey(test)) {
                break;    // stop, we've found the shortest match.
            }
        }
        if (i == tcount)    // gone too far, no match, this is an error!
        {
            //global.log.logMsg("longestVirtDir: _permTable doesn't contain " + path);
            return null;
        }
        i++;    // The break in the last "for" requires this side effect.
        String result = test;
        
        // Part II, find the longest match
        for (; i < tcount; i++) {
            test += st.nextToken();
            if (_permTable.containsKey(test)) {
                result = test;
                continue;    // this time continue as long as we match.
            }
        }
        
        //global.log.logMsg("longestVirtDir: returning " + result);
        return result;
    }
    
    
    
    /**
     * Resolve any ".." and "."'s in a virtual path.
     * Expects an absolute path.
     * Remove any entries with 3 or more .'s
     * This should probably cause an error later on - which is good.
     *
     * @param path Virtual directory as recieved from a client.
     * @return path without relative path indicators.
     */
    public String resolve(String path) {
        //global.log.logMsg("resolve: resolving arg " + path);
        char startChar = (path.length() > 0) ? path.charAt(0) : '/';
    
        if (path.length() == 0 || startChar != '/') {
            if (startChar != '~')    // special for home dirs
            {
                path = "/";
            }
        }
        //global.log.logMsg("resolve: First step path = " + path);
        //		Assert.that(path != null, "Permission.resolve: not absolute path - path null " + path);
        //		Assert.that(path.length() > 0, "Permission.resolve: not absolute path (0 length) - " + path);
        //		Assert.that(startChar == '/' && startChar != '~', "Permission.resolve: not absolute path first char not / - " + path);
        
        //		if (path.indexOf("/.") < 0)
        //			return path;	// nothing to resolve.
        
        if (path.equals("/"))    // special case.
        {
            //global.log.logMsg("resolve: returning original path = " + path);
            return path;
        }
        
        // create an array of strings from the path.
        // everytime we see a ".." replace it with null and
        // replace n-1 with null.
        // If we see ".", replace it with null.
        StringTokenizer st = new StringTokenizer(path, "/", false);
        int tcount = st.countTokens();
        int i;
        String test[] = new String[tcount];
        
        for (i = 0; i < tcount; i++)    // enumerate the path
        {
            test[i] = st.nextToken();
        }
        st = null;
        
        // Loop, resolving good and bad dots.
        for (i = 0; i < tcount; i++) {
            if (test[i].equals(".") || test[i].indexOf("...") > -1) {
                test[i] = null;
                continue;    // throw away single .'s and other funny stuff.
            }
            
            if (test[i].equals("..")) {
                test[i] = null;
    
                if (i == 0)    // if it's at the beginning, ignore ..
                {
                    continue;
                }
                
                int j;    // resolve things like "../.."
                for (j = i - 1; j >= 0; j--) {
                    if (test[j] == null) {
                        continue;    // skip portions already hit by a ".."
                    }
                    test[j] = null;
                    break;
                }
            }
        }
        
        // Reassemble the path.
        
        String truePath = "";
        
        for (i = 0; i < tcount; i++) {
            if (test[i] != null) {
                truePath += "/" + test[i];
            }
            //global.log.logMsg("resolve: truePath = " + truePath);
        }
        if (truePath.length() == 0)    // if nothing remains make it the root for convenience
        {
            if (startChar == '~') {
                truePath = "~";
                // get the first arg.
            }
            truePath = "/";
        } else if (startChar == '~') {
            // there's something but it was turned into /~michael
           truePath = truePath.substring(1);
        }
        
        // If the last char is '/', remove it.
        //global.log.logMsg("resolve: Dir: " + truePath);
        if (truePath.charAt(truePath.length() - 1) == '/') {
            //global.log.logMsg("resolve: Dir ended in /: " + truePath);
            truePath = truePath.substring(0, truePath.length() - 1);
            //global.log.logMsg("resolve: Dir ended in /, becomes: " + truePath);
        }
        //global.log.logMsg("resolve: returning truePath: " + truePath);
        return truePath;
    }
    
    
    // Add a virtual directory entry.
    //
    public void addVirtualDirectory(String virtDir, VirtualDirectory vde) {
        //global.log.logMsg("VirtDir = " + virtDir);
        //global.log.logMsg("physDir = " + vde.physDir);
        global.permSet._permTable.put(virtDir, vde);
    }
    
}
