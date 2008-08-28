package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import cpaplugin.cpa.cpas.symbpredabs.Predicate;

/**
 * A predicate represented as a BDD variable
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDPredicate implements Predicate {
    private int bddVar;
    private int varindex;
    
    public BDDPredicate(int var, int idx) {
        bddVar = var;
        varindex = idx;
    }
    
    public int getBDDVar() {
        return bddVar;
    }
    
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BDDPredicate) {
            return varindex == ((BDDPredicate)o).varindex;
        } else {
            return false;
        }
    }
    
    public int hashCode() {
        return varindex;
    }
    
    public String toString() {
        return "BDD(" + varindex + ")";
    }
}
