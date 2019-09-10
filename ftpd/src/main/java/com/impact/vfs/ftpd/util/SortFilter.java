package com.impact.vfs.ftpd.util;

/**
 * Abstract sort compare class
 */
public interface SortFilter {
    
    /**
     * Set the object to sort.
     *
     * @param o Object to sort.
     */
    void set(Object o);
    
    /**
     * Save the temporary object being sorted
     *
     * @param i index to object.
     */
    void setTemp(int i);
    
    /**
     * Copy element a to b.
     *
     * @param a Index recipient object in list.
     * @param b Index of donor object in list.
     */
    void copy(int a, int b);
    
    /**
     * Set object at index i to the temp value from setTemp().
     *
     * @param i Index to object that receives Temp.
     */
    void tempSet(int i);
    
    /**
     * Return true for a &gt; b.
     * Compare the indexed object against the temp object.
     *
     * @param i Index within object to compare.
     * @return integer result
     */
    boolean compare(int i);
    
}
