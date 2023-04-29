package org.etieskrill.injection.util;

import java.util.Set;
import java.util.TreeSet;

public final class UniqueIDManager {
    
    Set<Integer> usedIDs;
    
    public UniqueIDManager() {
        this.usedIDs = new TreeSet<>();
    }
    
    public int request() {
        int i = 0;
        
        while (i < Integer.MAX_VALUE) {
            if (!usedIDs.contains(i++)) {
                usedIDs.add(i);
                return i;
            }
        }
        
        throw new IllegalStateException("ID range has been exhausted");
    }
    
    public boolean revoke(int i) {
        return usedIDs.remove(i);
    }
    
}
