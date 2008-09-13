package symbpredabstraction;

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
public interface SymbPredAbsFormulaManager extends SymbolicFormulaManager {

    /**
     * computes the path formulas for each of the leaves of the inner subgraph
     * of the given summary location
     */
    public Pair<SymbolicFormula, SSAMap> buildPathFormula(
    		CFANode summary) throws UnrecognizedCFAEdgeException;

}
