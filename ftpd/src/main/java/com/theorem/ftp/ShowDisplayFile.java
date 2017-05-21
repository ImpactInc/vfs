package com.theorem.ftp;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;


/**
* Show a display file in the current directory, if any.
* Applies to CDUP and CWD.  Open the file and write it out
* as continuations.  We expect this to be followed by a
* 2?? xxx line.
* What about sharing the file? (some os's won't do this, even with JAVA - Early Novell JVM, anyway)
*/
public class ShowDisplayFile {

	public ShowDisplayFile(Global global, String cwd, PrintWriter out) {

		if (global.displayFile == null)
			return;

		String dispFile = cwd + "/" + global.displayFile;
		dispFile = new FCorrectPath().fixit(dispFile);
		File df = new File(dispFile);
		if (df.exists() && df.isFile())
		{
			RandomAccessFile fi;
			try {
				fi = new RandomAccessFile(df, "r");

				String line;
				while ((line = fi.readLine()) != null)
				{
					line.replace('\n', ' ').replace('\r', ' ');
					out.println("250- " + line);
				}
				out.flush();
				fi.close();
			} catch (IOException ioe) {
				global.log.logMsg("Failed to read display file " + dispFile);
				return;
			}
		}
	}
}
