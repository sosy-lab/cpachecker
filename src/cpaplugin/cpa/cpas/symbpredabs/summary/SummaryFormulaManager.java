package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Map;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;

public interface SummaryFormulaManager extends SymbolicFormulaManager {
    
    public Map<CFANode, Pair<SymbolicFormula, SSAMap>> buildPathFormulas(
            SummaryCFANode summary) throws UnrecognizedCFAEdgeException;

}
