package com.theorem.ftp.commands;

import com.theorem.ftp.CurrentInfo;
import com.theorem.ftp.Ftpd;


/**
 * Class to send reply for HELP request.
 */
public class HELP {

    public HELP(CurrentInfo curCon, String str) {

        String help = ""
                + "214-" + curCon.global.getServerIdentification() + "\n"
                + "214-Commands available:\n"
                + "214-ACCT\n"
                + "214-CDUP\n"
                + "214-CWD\n"
                + "214-DELE\n"
                + "214-LIST\n"
                + "214-MDTM\n"
                + "214-MKD\n"
                + "214-MODE\n"
                + "214-NOOP\n"
                + "214-PASS\n"
                + "214-PASV\n"
                + "214-EPSV\n"
                + "214-PORT\n"
                + "214-PWD\n"
                + "214-REST\n"
                + "214-RETR\n"
                + "214-RMD\n"
                + "214-RNFR\n"
                + "214-RNTO\n"
                + "214-SITE\n"
                + "214-SIZE\n"
                + "214-STAT\n"
                + "214-STOR\n"
                + "214-SYST\n"
                + "214-TYPE\n"
                + "214-USER\n"
                + "214 HELP command successful.\n";
        curCon.respond(help);
    }
}
