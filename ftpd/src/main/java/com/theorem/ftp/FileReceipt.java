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
 * Interface for running external file receipt notification and entry to directories.
 * It provides a means to extend finer grade permissions as well as notify other applications
 * that a file has arrived.
 * <p>
 * A new instance of this class is created each time it's required.  That is to say
 * it has no persistance from one file retrieval to another unless you provide some
 * static fields.
 */

package com.theorem.ftp;

import java.nio.file.Path;


public interface FileReceipt {
    
    /**
     * Method called just before a file is retrieved from the FTP server.
     * <P>
     * The method may return a null indicating the file may be downloaded or
     * a status code and information if it cannot.  Typically a refusal looks like:
     * <BR><CODE> 553 Requested action not taken.</CODE>
     * <P>
     * @param entity Name of the entity downloading the file.
     * @param path Physical path of the file retrieved by the FTP server.
     *
     * @return Status string, or null if the file may be downloaded.
     */
    String getBefore(String entity, Path path);
    
    /**
     * Method called when a file is retrieved from the FTP server.
     * <P>
     * @param entity Name of the entity downloading the file.
     * @param path Physical path of the file retrieved by the FTP server.
     * @param byteCount Number of bytes downloaded.
     */
    void getAfter(String entity, Path path, long byteCount);
    
    /**
     * Method called just before a file is stored by the FTP server.
     * <P>
     * The method may return a null indicating the file may be uploaded or
     * a status code and information if it cannot.  Typically a refusal looks like:
     * <BR><CODE> 553 Requested action not taken.</CODE>
     * <P>
     * @param entity Name of the entity uploading the file.
     * @param path Physical path of the file stored by the FTP server.
     *
     * @return Status string, or null if the file may be downloaded.
     */
    String putBefore(String entity, Path path);
    
    /**
     * Method called when a file is stored by the FTP server.
     *
     * @param entity Name of the entity uploading the file.
     * @param path Physical path of the file stored by the FTP server.
     * @param byteCount Number of bytes uploaded.
     */
    void putAfter(String entity, Path path, long byteCount);
    
    /**
     * Method called when a directory is entered.
     *
     * @param entity Name of the entity uploading the file.
     * @param path Physical path of the directory.
     * @param virtDirName Virtual path of the directory.
     *
     * @return Status string, or null if the directory may be entered.
     * This will be something like "450 Requested file action not taken." which will prevent
     * the command from completing or null to continue processing.
     */
    String enterDirectory(
            String entity,
            Path path,
            String virtDirName);
}
