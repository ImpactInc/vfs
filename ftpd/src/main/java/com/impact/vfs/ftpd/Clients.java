package com.impact.vfs.ftpd;

// Class to track connections and anonymous logins.
// Static so everyone can access it.
// Synchronized for the same reason.
//
public class Clients {
    
    static int maxAnonymous = 0;    // Maximum number anonymous clients
    static int maxConnections = 0;    // Maximum number of anonymous clients
    
    static boolean allowAnonymous = false;    // true if anonymous allowed, false default
    
    static int anonymous = 0;    // count of anonymous clients
    static int connections = 0;        // count of otherwise logged in clients
    
    // Allow anonymous logins
    static void allowAnonymous(boolean val) {
        allowAnonymous = val;
    }
    
    // Return permission to allow anonymous logins
    // True if permitted, false otherwise
    public static boolean anonymousPermitted() {
        return allowAnonymous;
    }
    
    // Set the maximum value
    //
    static void setMaxAnonymous(int count) {
        maxAnonymous = count;
    }
    
    // Set the maximum value
    //
    static void setMaxConnections(int count) {
        maxConnections = count;
    }
    
    // Checks for the limit on anonymous connections
    // If we're below the limit add one to the count and
    // return true.
    //
    public synchronized static boolean checkAnonymous() {
        //global.log.logMsg("Checking on anonymous logins: max is " + maxAnonymous + " anonymous count is " + anonymous);
        // Are there any anonymous slots left?
        if (anonymous >= maxAnonymous) {
            return false;
        }
        
        anonymous++;
        return true;
    }
    
    // Checks for the limit on anonymous connections
    // If we're below the limit add one to the count and
    // return true.
    //
    synchronized static boolean checkConnections() {
        if (maxConnections == 0)    // unlimited connections - everyone wins.
        {
            connections++;
            return true;    // unlimited permitted
        }
    
        if (connections >= maxConnections) {
            return false;
        }
        
        connections++;
        return true;
    }
    
    /**
     * Release a connection.  If it's anonymous release one of those too.
     *
     * @param name Entity name (may be null during login).
     */
    synchronized static void releaseConnection(String name) {
        if (name != null && name.startsWith("anonymous")) {
            anonymous--;
        }
    
        if (maxConnections == 0)    // unlimited connections - everyone wins.
        {
            return;    // no counts to fix
        }
        
        connections--;
    }
    
}
