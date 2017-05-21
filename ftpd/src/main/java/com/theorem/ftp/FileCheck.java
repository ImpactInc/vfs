/*
* FTP Server Daemon
* Copyright (C) 2000 Michael Lecuyer. All Rights reserved.
* 
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* 
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*
* Or see [http://www.gnu.org/copyleft/lesser.html].
*/

/**
 * Demonstration implmentation of the FileReciept class.
 */

package com.theorem.ftp;

public class FileCheck implements FileReceipt {
    
    // Called before a file is retrieved from the server (a RETR command).
    //
    public String getBefore(String name, String fileName, String configDir, LogMsg log) {
        System.out.println(name + " Downloading the file " + fileName);
        if (name.equals("michael") && fileName.equals("/etc/passwd")) {
            return "553 Requested action not taken.";
        }
        
        return null;
    }
    
    // Called after a file is retrieved from the server (a RETR command).
    //
    public void getAfter(String name, String fileName, long bytes, String configDir, LogMsg log) {
        System.out.println(name + " Downloaded the file " + fileName + " (" + bytes + ").");
    }
    
    // Called before a file is stored on the server (a RETR command).
    //
    public String putBefore(String name, String fileName, String configDir, LogMsg log) {
        System.out.println(name + " Uploading the file " + fileName);
        if (fileName.equals("/etc/passwd")) {
            return "553 Requested action not taken.";
        }
        
        return null;
    }
    
    // Called after a file is stored on the server (a STOR command).
    //
    public void putAfter(String name, String fileName, long bytes, String configDir, LogMsg log) {
        System.out.println(name + " Uploaded the file " + fileName + " (" + bytes + ").");
    }
    
    /**
     * Method called when a directory is entered.
     *
     * @param entity Name of the entity uploading the file.
     * @param physDirName Physical path of the directory.
     * @param virtDirName Virtual path of the directory.
     * @configDir Configuration directory as supplied on the FTP server command line.
     * @log Log object for creating log entries, if necessary.  This writes to the main server log.
     *
     * @return Status string, or null if the directory may be entered.
     * This will be something like "450 Requested file action not taken." which will prevent
     * the command from completing or null to continue processing.
     */
    public String enterDirectory(String entity, String physDirName, String virtDirName, String configDir, LogMsg log) {
        System.out.println("Virtual dir: " + virtDirName + " Phys Dir " + physDirName);
        if (virtDirName.equals("/etc")) {
            return "450 Requested file action not taken.";
        }
        
        return null;
    }
}
