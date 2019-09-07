package com.impact.vfs.ftpd.util;

import java.util.Vector;


/**
 * Compare vector elements (of strings)
 * for a sort
 */
public class VectorCompare implements SortFilter {
    
    Vector v;
    Object temp;
    
    public void set(Object o) {
        this.v = (Vector)o;
    }
    
    public void setTemp(int i) {
        temp = v.elementAt(i);
    }
    
    // Copy element i to element j.
    //
    public void copy(int i, int j) {
        v.set(j, v.elementAt(i));
    }
    
    public void tempSet(int i) {
        v.set(i, temp);
    }
    
    public boolean compare(int i) {
        return ((String)v.elementAt(i)).compareTo((String)temp) > 0;
    }
    
}
