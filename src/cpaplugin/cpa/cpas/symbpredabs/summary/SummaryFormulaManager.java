package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Map;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;

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
