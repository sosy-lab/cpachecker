package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;

public class BDDAbstractFormula implements AbstractFormula {
    // BDD representation of the formula, using the jdd BDD package
    private int jddRepr;
    
    public BDDAbstractFormula(int r) {
        jddRepr = r;
    }
    
    public int getBDD() {
        return jddRepr;
    }
}
