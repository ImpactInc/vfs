package com.theorem.ftp;

import java.util.Enumeration;
import java.util.Hashtable;


/*
 * Convenience class to hold multiple classes as a single structure.
 * The classes are related to the the virtual directory permissions.
 */
public class VirtualDirectory {
    
    public String physDir;    // Physical Directory name
    Hashtable allow;    // Allowed people as the key, "r" or "rw" as the value.
    Hashtable deny;    // Denied people as the key, "r" or "rw" as the value.
    boolean publicAccess;    // If true, directory is public (still check deny)
    
    /**
     * Return a string interpretation of this class.
     *
     * @return class decription string.
     */
    public String toString() {
        Enumeration e;
        int counter;
        
        StringBuffer sb = new StringBuffer();
        sb.append("Physical directory=").append(physDir).append(", ");
        if (!publicAccess) {
            sb.append("No ");
        }
        sb.append("Public Access. ");
    
        if (allow.isEmpty()) {
            sb.append("No one in particular is allowed access. ");
        } else {
        
            sb.append("Allowed: ");
        
            // The allow and deny hashtables contain a name and permissions.
            counter = 0;
            for (e = allow.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                sb.append(key).append('=').append((String)allow.get(key)).append(", ");
                counter++;
            }
            if (counter > 0) {
                sb.setLength(sb.length() - 2);    // Remove last trailing ', '.
            } else {
                sb.append("anonymous=r");    // If the counter is 0 mention anonymous.
            }
        
            sb.append(". ");
        }
    
        if (deny.isEmpty()) {
            sb.append("No one in particular is denied access.");
        } else {
            sb.append("Denied: ");
        
            counter = 0;
            // The allow and deny hashtables contain a name and permissions.
            for (e = deny.keys(); e.hasMoreElements(); ) {
                String key = (String)e.nextElement();
                sb.append(key).append(", ");
                counter++;
            }
        
            if (counter > 0) {
                sb.setLength(sb.length() - 2);    // Remove last trailing ', '.
            } else {
                sb.append("anonymous");    // If the counter is 0 mention anonymous.
            }
        
            sb.append('.');
        }
        
        return sb.toString();
    }
    
}
