package com.impact.vfs.ftpd.commands;

import com.impact.vfs.ftpd.CurrentInfo;
import com.impact.vfs.ftpd.Global;


// ACCT is used in conjunction with other commands.
//
// ACCOUNT (ACCT)
//  The argument field is a Telnet string identifying the user's
//  account.  The command is not necessarily related to the USER
//  command, as some sites may require an account for login and
//  others only for specific access, such as storing files.  In
//  the latter case the command may arrive at any time.
//
//  There are reply codes to differentiate these cases for the
//  automation: when account information is required for login,
//  the response to a successful PASSword command is reply code
//  332.  On the other hand, if account information is NOT
//  required for login, the reply to a successful PASSword
//  command is 230; and if the account information is needed for
//  a command issued later in the dialogue, the server should
//  return a 332 or 532 reply depending on whether it stores
//  (pending receipt of the ACCounT command) or discards the
//  command, respectively.
//
public class ACCT {

	ACCT(CurrentInfo curCon, String str)
	{
		Global global = curCon.global;

		// Get the account name.  This is stored in a vector because
		// more than one account may be used in a session.
		// There should probably be some sort of verification.

		str = str.substring(4).trim();
		curCon.acct.addElement(str);
	}
}
