package com.theorem.ftp.commands;

import java.io.*;
import java.net.Socket;
import java.util.Date;

import com.theorem.ftp.*;


/**
* File retrieval.
*/
public class RETR
{
	public RETR(CurrentInfo curCon, String str)
	{
		Global global = curCon.global;
		String retrFileName;
		long start = new Date().getTime();	// aquire start time.
		long byteCount = 0;	// bytes retrieved
		FileReceiptImpl fri = null;

		// Get the file path:
		str = str.substring(4).trim();
		String arg = str;

		if (curCon.canReadFile(str) == false)
		{
			curCon.respond("553 Requested action not taken.");
			global.log.logMsg("RETR: Requested action not taken. No permission to read " + str);
			return;
		}

		if (curCon.curFile == null)
			retrFileName = new FCorrectPath().fixit(curCon.curPDir);
		else
			retrFileName = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);

		File retrFile = new File(retrFileName);

		if (!retrFile.isFile())
		{
			curCon.respond("553 Requested action not taken.");
			global.log.logMsg("RETR: Requested action not taken. " + str + " isn't a file.");
			return;
		}

		// If there's a reciept class run the external class.
		// The class's getStart method can refuse the file retrieval.
		if (global.fileClass != null)
		{
			fri = new FileReceiptImpl(curCon);
			String response = fri.getBefore(retrFileName);
			if (response != null)
			{
				curCon.respond(response);
				global.log.logMsg(response);
				return;
			}
		}

		RandomAccessFile outFile = null;

		try
		{
			outFile = new RandomAccessFile(retrFileName, "r");
		} catch (IOException ioeb)
		{
			global.log.logMsg("Can't open RETR file: " + curCon.curPDir + curCon.curFile + ": " + ioeb);
			curCon.respond("553 Requested action not taken.");
			return;
		}

		int amount;
		OutputStream out2 = null;
		Socket t = curCon.dataSocket.getDataSocket(curCon);
		if (t == null)
		{
			global.log.logMsg("Can't create RETR socket");
			curCon.respond("425 Can't open data connection. Try using passive (PASV) transfers.)");
			return;
		}
		curCon.respond("150 " + curCon.transferText() + " connection for " + arg + " (" + retrFile.length() + " bytes)");

		// If the session breaks during transmission save the virtual file name
		// for future restoration.
		Restore r = new Restore(curCon);

		try {
			// See if a REST was left behind. Check before adding an entry or
			// we'll overwrite the REST file position.
			long posn = r.getFilePosn(retrFileName);
			if (posn > 0)
			{
				// there was a pending REST
				outFile.seek(posn);
			}
			// Add the new entry now, since we're about to send the file.
			r.addRestInfo(retrFileName);

			out2 = t.getOutputStream();

			if (curCon.transferType == curCon.ATYPE)
			{
				// ASCII file transfers are  going to be a bit slow 'cause we have to read
				// them a byte at a time to convert possible bare NL's or CRLF's to CRLF.
				// This could be a binary file so don't try to read lines.
				BufferedOutputStream outs = new BufferedOutputStream(out2);

				int ci;
				while ((ci = outFile.read()) != -1)
				{
					byte c = (byte)ci;
					if (c == global.CRLFb[0])
						continue;	// Ignore CR's
					else if (c == global.CRLFb[1])
						outs.write(global.CRLFb, 0, 2);
					else
						outs.write((int)c);	// Write the byte.
				}
				outs.flush();

			} else {
				// Binary transfer - quite fast.
				byte bb[] = new byte[10240];
				while((amount = outFile.read(bb)) != -1)
				{
					byteCount += amount;
					out2.write(bb, 0, amount);
				}
			}
			out2.flush();
			curCon.respond("226 transfer complete");
			outFile.close();
			curCon.dataSocket.closeDataSocket(t);

		} catch(IOException e)
		{
			try {	outFile.close(); } catch (IOException c) {;}	// make sure file is closed.
			global.log.logMsg("Error communicating with RETR socket/file: " + e);
			curCon.respond("426 Connection closed; transfer aborted.");
			curCon.dataSocket.closeDataSocket(t);
			return;
		}

		// Delete information for REST command
		r.removeRestInfo(retrFileName);

		if (global.fileClass != null)
			fri.getAfter(retrFileName, byteCount);

		// log the successful transaction.
		global.fLog.logMsg(global, curCon, start, byteCount, "o");
	}
}
