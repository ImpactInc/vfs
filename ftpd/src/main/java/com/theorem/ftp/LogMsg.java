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

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

// Log a message to an error log file.
//
public class LogMsg
{
	private static final String DEFAULTLOG = "./error.log";	// default log file
	private String logFile;
	private SimpleDateFormat sdf;

	public LogMsg(String logfile)	// Constructor
	{
		logFile = logfile;
		sdf = new SimpleDateFormat("yyyyMMddHHmmss");
	}

	public LogMsg()	// Constructor
	{
        this(DEFAULTLOG);
	}

	// Clear the log file by deleting it.
	//
	public void clear()
	{
		File f = new File(logFile);
		if (f.exists() && f.isFile())
			f.delete();
	}

	// Methods:

	// open the file for each new message.
	//
	public synchronized void logMsg(String msg)
	{
		String fullMsg;

		fullMsg = sdf.format(new Date()) + " " + Thread.currentThread().getName() + ": " + msg;

		try {
			RandomAccessFile outf = new RandomAccessFile(logFile, "rw");
			outf.seek(outf.length());	// seek to end of file
			outf.writeBytes(fullMsg + "\r\n");
			try { outf.close(); } catch (IOException e) {}
		} catch (IOException e) {
			System.out.println(fullMsg);
		}
	}

}

