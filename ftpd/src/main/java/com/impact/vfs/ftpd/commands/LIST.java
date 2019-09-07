package com.impact.vfs.ftpd.commands;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;
import com.impact.vfs.ftpd.util.Text;


public class LIST {
    
    public LIST(CurrentInfo curCon, String str) {
        
        // LIST will return the current directories contents
        // NLST wants a directory listing from either a named DIR
        // or just a LIST of the current dir.
        
        String listDir;
        String ustr = str.toUpperCase();

        if (ustr.startsWith("LIST")) {
            showList(curCon, curCon.getCwd(), this::details);
        } else {
            // NLST
            String arg = str.substring(4).trim();
            Path dir = arg.length() > 0 ? curCon.virtToPhys(arg) : curCon.getCwd();
            showList(curCon, dir, this::nameOnly);
        }
    }
    
    // Display the contents of a directory
    // listDir is assumed to be tested for permission beforehand.
    //
    void showList(CurrentInfo curCon, String listDir, Function<Path, String> details) {
        Path dir = curCon.virtToPhys(listDir);
        showList(curCon, dir, details);
    }
    // Display the contents of a directory
    // listDir is assumed to be tested for permission beforehand.
    //
    void showList(CurrentInfo curCon, Path dir, Function<Path, String> details) {
    
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
            
            //global.log.logMsg("LIST: dir on " + listDir + " phys dir " + dir);
            Stream<Path> a = Files.list(dir);

            ArrayList<Path> list = a.sorted().collect(Collectors.toCollection(ArrayList::new));

            //global.log.logMsg("showList: before sort.");
            //for (i = 0; i < alen; i++)
            //global.log.logMsg("showList: file[" + i + "] " + a[i]);
            /*
            StringCompare slc = new StringCompare();
            new QSort(a, alen, slc);
            */
            //global.log.logMsg("showList: after sort.");
            //for (i = 0; i < alen; i++)
            //global.log.logMsg("showList: file[" + i + "] " + a[i]);
                
            for (Path p : list) {
    
                String line = details.apply(p);
                out2.print(line);
                out2.print(curCon.transferEOL());
                //global.log.logMsg("Showing line: " + dr + "rwxrwxrwx   1 owner    group " + Text.format(flen, 11) + " " + date + " " + a[i]);
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
    
    private String details(Path p) {
        try {
        BasicFileAttributes attr = Files.getFileAttributeView(p, BasicFileAttributeView.class).readAttributes();
        
        String fn = p.getFileName().toString();
        String dr = attr.isDirectory() ? "d" : "-";    // DIR
        long flen = attr.isDirectory() ? 0 : Files.size(p);
        String date = modTime(attr.lastModifiedTime());
        return dr + "rwxrwxrwx   1 owner    group " + Text.format(flen, 11) + " " + date + " " + fn;
        } catch (IOException e) {
            return e.getMessage();
        }
    }
    
    private String nameOnly(Path p) {
        return p.getFileName().toString();
    }


    // Display the contents of a directory as a list of names.
    // listDir is assumed to be tested for permission beforehand.
    //
    void showNList(CurrentInfo curCon, String listDir) {
    
        showList(curCon, listDir, this::nameOnly);
    }
    
    /**
     * Get the file modification time as a useful string.
     *
     * @param ts      FileTime object to examine.
     * @return Time string.
     */
    String modTime(FileTime ts) {
        // Time format: May 12 01:12
        //          or: May 27  1998
        // Written before the convenience of SimpleDateFormat.
        
        //global.log.logMsg("FileData.modTime: Mod Time: " + f.lastModified());
        Date df = new Date(ts.toMillis());
        
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
