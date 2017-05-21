package com.theorem.ftp.commands;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

import com.theorem.ftp.*;
import com.theorem.ftp.util.QSort;
import com.theorem.ftp.util.StringCompare;
import com.theorem.ftp.util.Text;
import com.theorem.ftp.util.VectorCompare;


public class LIST {
    
    public LIST(CurrentInfo curCon, String str) {
        
        // LIST will return the current directories contents
        // NLST wants a directory listing from either a named DIR
        // or just a LIST of the current dir.
        
        String listDir;
        String ustr = str.toUpperCase();
        
        if (ustr.startsWith("LIST")) {
            showList(curCon, curCon.curWD);
        } else {
            // list the named dir (if any).
            StringTokenizer st = new StringTokenizer(ustr);
            // get the last token.
            int tcount = st.countTokens();
            if (tcount <= 1)    // only the NLST command
            {
                //System.out.println("NLST: only the nlist command.");
                showNList(curCon, curCon.curWD);
                return;
            }
    
            for (int i = 0; i < tcount - 1; i++) {
                st.nextToken();
            }
            listDir = st.nextToken();    // this is the final argument (skipping options)
            if (listDir.startsWith("-")) {
                // ignore options. Show current dir.
                showNList(curCon, curCon.curWD);
                return;
            }
            
            // Show the named dir: save our old working dir, chdir, and reStore.
            String origDir = curCon.curWD;
            if (curCon.chdir(listDir) == false) {
                // No such dir.
                curCon.respond("450 Requested file action not taken.");
                return;
            }
            showNList(curCon, curCon.curWD);
            curCon.chdir(origDir);
        }
    }
    
    // Display the contents of a directory
    // listDir is assumed to be tested for permission beforehand.
    //
    void showList(CurrentInfo curCon, String listDir) {
    
        final Global global = curCon.global;
        Socket t = null;
        int i;
        
        try {
            t = curCon.dataSocket.getDataSocket(curCon);
            if (t == null) {
                global.log.logMsg("Can't create LIST data socket");
                curCon.respond("425 Can't open data connection.");
                return;
            }
            curCon.respond("150 Opening " + curCon.transferText() + " data connection.");
            
            PrintWriter out2 = new PrintWriter(t.getOutputStream(), true);
            
            // If the directory is "/" show all the mapped directories the person is permitted
            // to see.  Only the directory known as '/' gets a file and other directory
            // list.
            
            String dir = curCon.virtToPhys(listDir);
            if (listDir.equals("/")) {
                String origDir = curCon.curWD;
                Permission p = global.permSet;
                Vector seen = new Vector();    // List of directories we've displayed - to avoid duplicates
                Vector list = new Vector();        // List of files or directories for sorting.
                
                // List the virtual directories that we have permission to visit
                for (Enumeration e = p.enumVirtDirList(); e.hasMoreElements(); ) {
                    String vdir = (String)e.nextElement();
    
                    if (vdir.equals(p.getHomeDir())) {
                        continue;    // skip the / dir itself - taken care of later.
                    }
    
                    if (seen.contains(vdir)) {
                        continue;
                    }
                    
                    seen.addElement(vdir);
    
                    if (curCon.chdir(vdir) == false) {
                        continue;
                    }
                    
                    // At this point sort the directory list.  Then we loop over it and do
                    // the following code:
                    
                    list.addElement(vdir);
                }
                
                new QSort(list, list.size(), new VectorCompare());
                
                // Get the virtual directory list to check physical directories vs virtal directories.
                Hashtable hvd = global.permSet.getVirDirList();
                
                int listLen = list.size();
                for (i = 0; i < listLen; i++) {
                    String vdir = (String)list.elementAt(i);
                    
                    // can't start with '/' or Netscape thinks it's an URL
                    String dispDir = (vdir.charAt(0) == '/') ? vdir.substring(1) : vdir;
                    
                    // Skip entries in the physical directory that match a virtual directory.
                    // This prevents duplicates from showing up in the root.
                    
                    VirtualDirectory vd = (VirtualDirectory)hvd.get(vdir);
                    
                    //if (vd != null)
                    //global.log.logMsg("listDir = [" + listDir + "] listDir.phys] = [" + dir + "] physdir = [" + vd.physDir + "] virtual dir = [" + vdir + "] vd.physdir startswith vdir: " +   vd.physDir.startsWith(dir));
                    // Ignore physical directories that start with the listed directories
                    // to prevent duplicate listing in the root directory.
                    // This prevents the following directories from being listed twice.
                    //
                    // If the dir.cfg file is set up as follows the following code will prevent double display of
                    // music, linux, and temp.
                    // /        /testdir    *
                    // /music    /testdir/music   @
                    // /linux       /testdir/linux      @
                    // /temp      /testdir/temp     @
                    if (vd.physDir.startsWith(dir)) {
                        continue;
                    }
                    
                    File f = new File(curCon.virtToPhys(vdir));
                    long flen = (f.isDirectory()) ? 0 : f.length();
                    //System.out.println(f.getName() + " (vdir=" + vdir + ") - Mod Time: " + f.lastModified());
                    //global.log.logMsg(f.getName() + " (vdir=" + vdir + ") - Mod Time: " + f.lastModified());
                    String date = modTime(f);
                    
                    out2.print("drwxrwxrwx   1 owner    group " + Text.format(flen, 11) + " " + date + " " + dispDir);
                    out2.print(curCon.transferEOL());
                }
                curCon.curWD = origDir;    // restore orginal directory.
            }
            
            //global.log.logMsg("LIST: dir on " + listDir + " phys dir " + dir);
            File f = new File(dir);
            String a[];
            
            if ((a = f.list()) != null) {
                int alen = a.length;
                //global.log.logMsg("showList: before sort.");
                //for (i = 0; i < alen; i++)
                //global.log.logMsg("showList: file[" + i + "] " + a[i]);
                
                StringCompare slc = new StringCompare();
                new QSort(a, alen, slc);
                
                //global.log.logMsg("showList: after sort.");
                //for (i = 0; i < alen; i++)
                //global.log.logMsg("showList: file[" + i + "] " + a[i]);
                
                for (i = 0; i < alen; i++) {
                    String fn = new FCorrectPath().fixit(dir + "/" + a[i]);
                    File fnF = new File(fn);
                    String dr = (fnF.isDirectory()) ? "d" : "-";    // DIR
                    //global.log.logMsg(fn + " - Mod Time: " + fnF.lastModified());
                    long flen = (fnF.isDirectory()) ? 0 : fnF.length();
                    String date = modTime(fnF);
                    out2.print(dr + "rwxrwxrwx   1 owner    group " + Text.format(flen, 11) + " " + date + " " + a[i]);
                    out2.print(curCon.transferEOL());
                    //global.log.logMsg("Showing line: " + dr + "rwxrwxrwx   1 owner    group " + Text.format(flen, 11) + " " + date + " " + a[i]);
                }
            }
            out2.flush();
            curCon.respond("226 Transfer complete.");
            curCon.dataSocket.closeDataSocket(t);
        } catch (IOException e) {
            global.log.logMsg("Error transfering file: " + e);
            curCon.respond("450 Requested file action not taken.");
            curCon.dataSocket.closeDataSocket(t);
        }
    }
    
    // Display the contents of a directory as a list of names.
    // listDir is assumed to be tested for permission beforehand.
    //
    void showNList(CurrentInfo curCon, String listDir) {
    
        final Global global = curCon.global;
        Socket t = null;
        int i;
        
        try {
            t = curCon.dataSocket.getDataSocket(curCon);
            if (t == null) {
                global.log.logMsg("Can't create LIST data socket");
                curCon.respond("425 Can't open data connection.");
                return;
            }
            curCon.respond("150 Opening " + curCon.transferText() + " data connection for /bin/ls.");
            
            PrintWriter out2 = new PrintWriter(t.getOutputStream(), true);
            
            // Should sort the virtDirList enumeration
            // Directory names could be preceded by a 'D'
            // File name could be preceded by a 'F' to sort Dirs first and Files
            // last.  The first char would then be stripped off, noting what
            // it was in the listing.
            // This list (and the alen list) could be sorted together
            // (on filename and the first letter of the directory disegnation
            // char) - the actual list could be an array of strings, alhtough
            // this could become excessive.
            // I'd rather do it on the f.list.  Perhaps the file filter
            // could do it in two passes.
            
            // If the directory is "/" show all the mapped directories the person is permitted
            // to see.  Only the directory known as '/' gets a file and other directory
            // list.
            
            String dir = curCon.virtToPhys(listDir);
            //global.log.logMsg("Dir = " + dir + " listDir = " + listDir);
            if (listDir.equals("/")) {
                String origDir = curCon.curWD;
                Permission p = global.permSet;
                Vector seen = new Vector();    // list of directories we've displayed - to avoid duplicates
                
                // List the virtual directories that we have permission to visit
                for (Enumeration e = p.enumVirtDirList(); e.hasMoreElements(); ) {
                    String dispDir;
                    String vdir = (String)e.nextElement();
    
                    if (vdir.equals(p.getHomeDir())) {
                        continue;    // skip the / dir itself - taken care of later.
                    }
    
                    if (seen.contains(vdir)) {
                        continue;
                    }
                    seen.addElement(vdir);
    
                    if (curCon.chdir(vdir) == false) {
                        continue;
                    }
                    
                    // can't start with '/' or Netscape thinks it's an URL
                    dispDir = (vdir.charAt(0) == '/') ? vdir.substring(1) : vdir;
                    
                    File f = new File(curCon.virtToPhys(vdir));
                    out2.print(dispDir + curCon.transferEOL());
                }
                curCon.curWD = origDir;    // restore orginal directory.
            }
            
            //global.log.logMsg("LIST: dir on " + listDir + " phys dir " + dir);
            File f = new File(dir);
            String a[];
            
            if ((a = f.list()) != null) {
                int alen = a.length;
                
                for (i = 0; i < alen; i++) {
                    String fn = new FCorrectPath().fixit(dir + "/" + a[i]);
                    File fnF = new File(fn);
                    out2.print(fnF.getName() + curCon.transferEOL());
                    //global.log.logMsg("Printing [" + fnF.getName() + "]");
                }
            }
            out2.flush();
            curCon.respond("226 Transfer complete.");
            curCon.dataSocket.closeDataSocket(t);
        } catch (IOException e) {
            global.log.logMsg("Error transfering file: " + e);
            curCon.respond("450 Requested file action not taken.");
            curCon.dataSocket.closeDataSocket(t);
        }
    }
    
    /**
     * Get the file modification time as a useful string.
     *
     * @param f      File object to examine.
     * @return Time string.
     */
    String modTime(File f) {
        // Time format: May 12 01:12
        //          or: May 27  1998
        // Written before the convenience of SimpleDateFormat.
        
        //global.log.logMsg("FileData.modTime: Mod Time: " + f.lastModified());
        Date df = new Date(f.lastModified());
        
        // Get the current year from the GC
        GregorianCalendar dt = new GregorianCalendar();
        int thisYear = dt.get(dt.YEAR);
        dt.setTime(df);
        int fileYear = dt.get(dt.YEAR);
        
        String sdfs = (thisYear != fileYear) ? "MMM dd  yyyy" : "MMM dd HH:mm";
        
        SimpleDateFormat sdf = new SimpleDateFormat(sdfs);
        
        return sdf.format(df);
    }
    
}
