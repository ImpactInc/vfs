package com.theorem.ftp.util;

/**
 * Sort a list of indexed items.
 */
public class QSort {
    
    /**
     * Sort the elements of o using the sort comparison class.
     * No return value since the object will be sorted in place.
     *
     * @param o Collection object to be sorted.
     * @parem sc sort comparison class.
     */
    public QSort(Object o, int nItems, SortFilter sc) {
        int i;
        
        // Do various sorts depending on the number of elements.
    
        if (o == null || nItems == 1) {
            return;            // Nothing to sort;
        }
        
        sc.set(o);    // set up the object to sort.
        
        if (nItems == 2)    // Simple exchange.
        {
            // Since compare() compares something to Temp, set temp up.
            sc.setTemp(1);
            if (sc.compare(0)) {
                sc.setTemp(1);
                sc.copy(0, 1);
                sc.tempSet(0);
            }
        } else if (nItems > 2)    // more complex sort from "Progamming Perls".
        {
            int j;
            int t;
            
            for (i = 1; i < nItems; i++) {
                j = i;
                sc.setTemp(j);
                
                while (j > 0 && sc.compare(j - 1)) {
                    sc.copy(j - 1, j);
                    //					preference[j] = preference[j-1];
                    j--;
                }
                sc.tempSet(j);
                // preference[j] = t;
            }
        }
    }
    
}
