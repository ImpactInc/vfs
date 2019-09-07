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

// Log messages to a file with time stamp.

package com.theorem.ftp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Log a message to an error log file.
//
public class LogMsg {
    
    static Logger log = LoggerFactory.getLogger("ftpd");
    
    public LogMsg() {
        // noop
    }
    
    public synchronized void logMsg(String msg) {
        log.info(msg);
    }
    
}

