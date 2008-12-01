package cpa.symbpredabs.mathsat;

import cpa.symbpredabs.SymbolicFormula;


/**
 * A SymbolicFormula represented as a MathSAT term.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class MathsatSymbolicFormula implements SymbolicFormula {

    private long msatTerm;
    
    public MathsatSymbolicFormula(long t) {
        msatTerm = t;
    }
    
    public boolean isFalse() {
        return mathsat.api.msat_term_is_false(msatTerm) != 0;
    }

    public boolean isTrue() {
        return mathsat.api.msat_term_is_true(msatTerm) != 0;
    }
    
    @Override
    public String toString() {
        return mathsat.api.msat_term_repr(msatTerm);
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MathsatSymbolicFormula)) return false;
        return msatTerm == ((MathsatSymbolicFormula)o).msatTerm;
    }
    
    public long getTerm() { return msatTerm; }
    
    public void setTerm(long t) { msatTerm = t; }
    
    @Override
    public int hashCode() {
        return (int)msatTerm;
    }
}
