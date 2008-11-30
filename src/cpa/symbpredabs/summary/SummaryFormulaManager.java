package cpa.symbpredabs.summary;

import java.util.Map;

import cfa.objectmodel.CFANode;

import cpa.symbpredabs.Pair;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.UnrecognizedCFAEdgeException;

/**
 * Formula manager that understands summary locations
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public interface SummaryFormulaManager extends SymbolicFormulaManager {

    /**
     * computes the path formulas for each of the leaves of the inner subgraph
     * of the given summary location
     */
    public Map<CFANode, Pair<SymbolicFormula, SSAMap>> buildPathFormulas(
            SummaryCFANode summary) throws UnrecognizedCFAEdgeException;

}
