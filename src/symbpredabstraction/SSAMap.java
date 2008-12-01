package symbpredabstraction;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable 
 */
public class SSAMap {
    private Map<String, Integer> repr = new HashMap<String, Integer>();
    private static int nextSSAIndex = 1;

    /**
     * returns the index of the variable in the map
     */
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

    /**
     * returns the next available global index. This method is not used anymore
     * (except in broken code :-) and should be removed.
     */
    public static int getNextSSAIndex() {
        return nextSSAIndex++;
    }
    
    public Collection<String> allVariables() {
        return repr.keySet();
    }
    
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{ ");
        for (String s : allVariables()) {
            buf.append(s);
            buf.append("@");
            buf.append(repr.get(s));
            buf.append(" ");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Explicit "copy constructor". I am not experienced enough with Java to
     * dare implementing a proper clone() :-)
     */
    public void copyFrom(SSAMap other) {
        for (String var : other.allVariables()) {
            setIndex(var, other.getIndex(var));
        }
    }

    /**
     * updates this map with the contents of other. That is, adds to this map
     * all the variables present in other but not in this
     */
    public void update(SSAMap other) {
        for (String var : other.allVariables()) {
            if (!repr.containsKey(var)) {
                setIndex(var, other.getIndex(var));
            }
        }
    }
}
