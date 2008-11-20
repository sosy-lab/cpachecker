package cpaplugin.cpa.cpas.symbpredabs;

import java.util.Vector;

public interface InterpolatingTheoremProver {
    public void init();
    public void reset();
    void addFormula(SymbolicFormula f);
    boolean isUnsat();
    SymbolicFormula getInterpolant(Vector<SymbolicFormula> formulasOfA);
}
