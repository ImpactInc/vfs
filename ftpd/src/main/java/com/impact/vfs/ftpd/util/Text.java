/*
 * The contents of this file are subject to the Iridium FTP Server License,
 * and available in  the license.txt file that should accompany this distribution
 * and available at http://www.theorem.com.
 * You  may not use this file except  in compliance with the License.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 *  This code is Copyright (C) 2000 Michael Lecuyer. All Rights reserved.
 */

package com.theorem.ftp.util;

import java.util.*;


/**
 * Some text methods.
 */
public class Text {
    
    /**
     * Replace all occurences of a with b.
     *
     * @param s original string.
     * @param a string to change.
     * @param b replacement string.
     * @return string with replacements.
     */
    static public String replace(String s, String a, String b) {
        // If both parts of the replacement are just 1 char
        // use the String method.
        if (a.length() == 1 && b.length() == 1) {
            return s.replace(a.charAt(0), b.charAt(0));
        }
        
        // Otherwise do it the long way.
        
        int idx;
        int span = a.length();
        int lidx = 0;
        StringBuffer sb = new StringBuffer();
        
        while ((idx = s.indexOf(a, lidx)) >= 0) {
            // Copy where we started to this point.
            sb.append(s.substring(lidx, idx));
            sb.append(b);
            lidx = idx + span;
        }
    
        if (lidx < 0)    // Optimization - most strings won't need replacement.
        {
            return s;
        }
        
        sb.append(s.substring(lidx));
        
        return sb.toString();
    }
    
    
    
    
    
    
    /**
     * Split a line into an array based on white space.
     *
     * @param s String to split
     * @return array of strings.
     */
    static public String[] split(String s) {
        return split(s, " \t\n\r");
    }
    
    /**
     * Split a line into an array based on the delimiter.
     *
     * @param s     String to split
     * @param delim list of delimiters
     * @return array of strings.
     */
    static public String[] split(String s, String delim) {
        StringTokenizer st = new StringTokenizer(s, delim);
        int count = st.countTokens();
        
        String r[] = new String[count];
    
        for (int i = 0; st.hasMoreTokens(); i++) {
            r[i] = st.nextToken();
        }
        
        return r;
    }
    
    // Hex conversion.
    private static char hex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
    /**
     * Convert a byte buffer to a hex string.
     *
     * @param buf Byte array.
     * @return Hexadecimal representation.
     */
    public static String toHexString(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        int buflen = buf.length;
        for (int i = 0; i < buflen; i++) {
            sb.append(hex[(buf[i] >>> 4) & 0xf]).append(hex[buf[i] & 0xf]);
        }
        return sb.toString();
    }
    
    /**
     * Dump out the current call stack to stdout.
     */
    public static void dumpStack() {
        java.io.CharArrayWriter ca = new java.io.CharArrayWriter();
        java.io.PrintWriter pwca = new java.io.PrintWriter(ca);
        
        (new Throwable()).printStackTrace(pwca);
        
        System.out.println(ca.toString() + "\n");
    }
    
    /**
     * Format a number to the given width, right justfied, blank filled.
     *
     * @param n     Number to format.
     * @param width Width of spaces + number.
     * @return formatted number.
     */
    public static String format(int n, int width) {
        StringBuffer num = new StringBuffer(Integer.toString(n));
        
        int spaces = width - num.length();
        for (int i = 0; i < spaces; i++) {
            num.insert(0, ' ');
        }
        
        return num.toString();
    }
    
    /**
     * Format a number to the given width, right justfied, blank filled.
     *
     * @param n     Number to format.
     * @param width Width of spaces + number.
     * @return formatted number.
     */
    public static String format(long n, int width) {
        StringBuffer num = new StringBuffer(Long.toString(n));
        
        int spaces = width - num.length();
        for (int i = 0; i < spaces; i++) {
            num.insert(0, ' ');
        }
        
        return num.toString();
    }
}
