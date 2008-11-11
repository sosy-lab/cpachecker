package symbpredabstraction;

import java.util.Map;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.exceptions.UnrecognizedCFAEdgeException;

public interface SymbPredAbsFormulaManager extends SymbolicFormulaManager {

    /**
     * computes the path formulas for each of the leaves of the inner subgraph
     * of the given summary location
     */
    public Map<CFANode, PathFormula> buildPathFormulas(
            SummaryCFANode summary) throws UnrecognizedCFAEdgeException;

}
