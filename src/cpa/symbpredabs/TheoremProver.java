package cpa.symbpredabs;

import java.util.Vector;


public interface TheoremProver {   
    public final static int CARTESIAN_ABSTRACTION = 0;
    public final static int COUNTEREXAMPLE_ANALYSIS = 2;
    public final static int ENTAILMENT_CHECK = 3;
    
    void init(int purpose);
    void push(SymbolicFormula f);
    void pop();
    boolean isUnsat(SymbolicFormula f);
    void reset();

    public interface AllSatCallback {
        public void modelFound(Vector<SymbolicFormula> model);
    }
    public int allSat(SymbolicFormula f,
            Vector<SymbolicFormula> important, AllSatCallback callback);
}
