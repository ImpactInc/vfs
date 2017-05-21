package com.theorem.ftp;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.StringTokenizer;


/**
 * Class to check a name against a simple text file.
 * The file is read sequentially and is in plain text.
 */
public class PasswordFile implements Authenticator {
    
    private String _passwordFile;
    private RandomAccessFile _passFile = null;    // file handle for password file.
    private long _passStart = 0;    // starting position of password file.
    
    /**
     * Constructor to set up the password file.
     *
     * @param passwordFile Name of the password file.
     */
    public PasswordFile(String passwordFile) {
        _passwordFile = passwordFile;
    }
    
    
    /**
     * Look up (in a file) the given name.
     *
     * @param name     Entiry name.
     * @param password Entity password.
     * @param log   Global data for this thread.
     * @return true if the  password matches, false for any other reason
     */
    @Override
    public boolean authenticate(String name, String password, String configDir, LogMsg log) {

        // Read sequentially through a file.  Naturally this isn't the most
        // efficient thing to do, but it works.  Apart from building
        // an index each time and checking for changes to the original
        // file, this is the simplest method.
        // The drawback is in sharing open files on some systems.
    
        // Only reopen if the file handle is null.
        if (_passFile == null) {
            try {
                _passFile = new RandomAccessFile(_passwordFile, "r");
                _passStart = _passFile.getFilePointer();
            
            } catch (IOException ioe) {
                log.logMsg("Can't open Password file: " + _passwordFile + ": " + ioe);
                return false;    // can't look this up
            }
        }
    
        try {
            //global.log.logMsg("checkNamePassword.checkNamePassword: starting to read");
            _passFile.seek(_passStart);    // start at the beginning of the file.
        
            String line;
            while ((line = _passFile.readLine()) != null) {
                // parse the file:
                //global.log.logMsg("checkNamePassword.checkNamePassword:  reading [" + line + "]");
                // ignore comments.
                if (line.startsWith("#") || line.startsWith(";") || line.length() == 0) {
                    continue;
                }
            
                // Split line into two parts, comma delimited, ignore spaces and tabs.
                StringTokenizer st = new StringTokenizer(line, ",");
            
                String v = st.nextToken().trim();
                //global.log.logMsg("checkNamePassword.checkNamePassword:  reading name [" + v + "] vs typed name [" + name + "]" );
                if (name.equals(v)) {
                    //global.log.logMsg("checkNamePassword.checkNamePassword: name found");
                    // found the name, check the password:
                    v = st.nextToken().trim();
                    //global.log.logMsg("checkNamePassword.checkNamePassword: checking found password: " + v + " vs. entered password " + password );
                    return (password.equals(v));    // plaintext check.
                }
            }
        } catch (IOException ioe) {
            log.logMsg("Error reading password file." + ioe);
            return false;    // can't look this up - error
        }
    
        return false;    // fell through without a name match.
    }
    
}
