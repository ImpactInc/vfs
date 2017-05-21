package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FileReceiptImpl;
import com.theorem.ftp.Global;
import com.theorem.ftp.ShowDisplayFile;


// Also handles CDUP.
//
public class CWD {

	public CWD(CurrentInfo curCon, String str) {

		String cmd;	// Actual command name.
		Global global = curCon.global;

		// CDUP is handled here as well.
		if (str.equalsIgnoreCase("CDUP"))
		{
			cmd = "CDUP";
			str = "..";
		} else {
			// Get the file path from the CWD command:
			cmd = "CWD";
			str = str.substring(3).trim();
		}

		if (str.length() == 0)
		{
			// no direction, do nothing.
			curCon.respond("250 " + cmd + " command succesful.");
			return;
		}

		// Save the last directory we were in before performing the chdir.
		// This allows us to backout if the FileReceipt class fails to allow us in.
		String lastDir = curCon.curWD;

		if (curCon.chdir(str) == false)
		{
			// In the case where we're at the root, but not at the root
			// Go to the root. What this strangeness says is that if we're
			// at something like /www.theorem and we do a "go to /".
			if (str.equals(".."))
			{
				if (curCon.chdir("/") == false)
				{
					curCon.respond("450 Requested file action not taken.");
					global.log.logMsg("CDUP: Can't CHDIR to " + str + " from " + curCon.curWD);
					return;
				}

			} else {
				curCon.respond("450 Requested file action not taken.");
				return;
			}
		}

		// If there's a reciept class run the external class.
		// The class's getStart method can refuse the file retrieval.
		if (global.fileClass != null)
		{
			FileReceiptImpl fri = new FileReceiptImpl(curCon);
			String response = fri.enterDirectory(curCon.virtToPhys(curCon.curWD), curCon.curWD);
			if (response != null)
			{
				curCon.respond(response);
				global.log.logMsg(response);

				curCon.chdir(lastDir);	// Restore ourselves to our old directory.

				return;
			}
		}

		new ShowDisplayFile(global, curCon.curPDir, curCon.out);
		curCon.respond("250 " + cmd + " command succesful.");
	}
}
