package com.theorem.ftp.commands;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Global;


/**
 * Created by knut on 2017/05/14.
 */
public class PORT {

	public PORT(CurrentInfo curCon, String str) {

		Global global = curCon.global;

		StringTokenizer st = new StringTokenizer(str, " ,");
		if (st.countTokens() != 7)
		{
			curCon.respond("500 Wrong number of Parameters.");
			global.log.logMsg("PORT: Wrong number of Parameters: " + str);
			return;
		}

		// skip command.
		st.nextToken();

		StringBuffer sb = new StringBuffer();
		sb.append(st.nextToken()).append('.');
		sb.append(st.nextToken()).append('.');
		sb.append(st.nextToken()).append('.');
		sb.append(st.nextToken());

		try {
			curCon.dataIP = InetAddress.getByName(sb.toString());
		} catch (UnknownHostException uhe) {
			curCon.respond("500 Bad IP address [" + sb.toString() + "]");
			global.log.logMsg("PORT: Port number " + curCon.dataPort + " is bad: " + uhe);
		}

		try {
			int dp1 = Integer.parseInt(st.nextToken());
			int dp2 = Integer.parseInt(st.nextToken());
			curCon.dataPort = (dp1 << 8) + dp2;
		} catch (NumberFormatException e)
		{
			curCon.respond("500 Bad Port number.");
			global.log.logMsg("PORT: Port number " + curCon.dataPort + " is bad: " + e);
			return;
		}

		curCon.respond("200 PORT command successful.");
	}

}
