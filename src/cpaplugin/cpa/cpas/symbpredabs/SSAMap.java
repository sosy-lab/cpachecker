package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alb
 *
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable 
 */
public class SSAMap {
    private Map<String, Integer> repr = new HashMap<String, Integer>();
    private static int nextSSAIndex = 1;
    
    public int getIndex(String variable) { 
        if (repr.containsKey(variable)) {
            return repr.get(variable).intValue();
        } else {
            // no index found, return -1
            return -1;
        }
    }
    
    public void setIndex(String variable, int idx) {
        repr.put(variable, new Integer(idx));
    }
    
    public static int getNextSSAIndex() {
        return nextSSAIndex++;
    }
    
    public Collection<String> allVariables() {
        return repr.keySet();
    }
}
