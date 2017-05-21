package com.theorem.ftp.commands;

import java.io.File;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.FCorrectPath;
import com.theorem.ftp.Global;


public class DELE {

	public DELE(CurrentInfo curCon, String str) {

		Global global = curCon.global;

		// Get the file path:
		str = str.substring(4).trim();

		if (curCon.canWriteFile(str) == false)
		{
			curCon.respond("553 Requested action not taken.");
			global.log.logMsg("DELE: No write permission for file " + str);
			return;
		}

		String delpath = new FCorrectPath().fixit(curCon.curPDir + curCon.curFile);
		File f = new File(delpath);
		if (f.delete())
		{
			global.log.logMsg("Deleted file " + delpath);
			curCon.respond("250 delete command successful.");
		}
		else
		{
			curCon.respond("450 Requested file action not taken.");
			global.log.logMsg("Failed to delete file " + delpath);
		}
	}
}
