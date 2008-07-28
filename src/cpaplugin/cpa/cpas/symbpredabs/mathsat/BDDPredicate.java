package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import cpaplugin.cpa.cpas.symbpredabs.Predicate;

public class BDDPredicate implements Predicate {
    private int bddVar;
    
    public BDDPredicate(int var) {
        bddVar = var;
    }
    
    public int getBDDVar() {
        return bddVar;
    }
}
