package cpaplugin.cpa.cpas.itpabs.symbolic;

import java.util.HashMap;
import java.util.Map;

import cpaplugin.CPAStatistics;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cpa.cpas.itpabs.ItpAbstractElement;
import cpaplugin.cpa.cpas.itpabs.ItpAbstractElementManager;
import cpaplugin.cpa.cpas.itpabs.ItpCPA;
import cpaplugin.cpa.cpas.itpabs.ItpCPAStatistics;
import cpaplugin.cpa.cpas.itpabs.ItpCounterexampleRefiner;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.UnrecognizedCFAEdgeException;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.cpa.cpas.symbpredabs.mathsat.summary.MathsatSummaryFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.summary.SummaryCFANode;


/**
 * Symbolic version (using summary locations) of the interpolation-based lazy
 * abstraction analysis
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ItpSymbolicCPA extends ItpCPA {
    
    class ItpSymbolicAbstractElementCreator 
        implements ItpAbstractElementManager {
        @Override
        public ItpAbstractElement create(CFANode location) {
            ItpSymbolicAbstractElement ret = new ItpSymbolicAbstractElement(
                    location);
            ret.setPathFormulas(getPathFormulas((SummaryCFANode)location));
            return ret;
        }

        @Override
        public boolean isFunctionEnd(ItpAbstractElement e) {
            CFANode n = e.getLocation();
            return (n.getNumLeavingEdges() > 0 &&
                    n.getLeavingEdge(0) instanceof ReturnEdge);
        }

        @Override
        public boolean isFunctionStart(ItpAbstractElement e) {
            return (e.getLocation() instanceof FunctionDefinitionNode);
        }

        @Override
        public boolean isRightEdge(ItpAbstractElement e, CFAEdge edge,
                ItpAbstractElement succ) {
            if (isFunctionEnd(e)) {
                CFANode retNode = e.topContextLocation();
                if (!succ.getLocation().equals(retNode)) {
                    LazyLogger.log(LazyLogger.DEBUG_1,
                            "Return node for this call is: ", retNode,
                            ", but edge leads to: ", succ.getLocation());
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public void pushContextFindRetNode(ItpAbstractElement e,
                ItpAbstractElement succ) {
            assert(e.getLocation().getLeavingSummaryEdge() != null);
            CFANode retNode = 
                e.getLocation().getLeavingSummaryEdge().getSuccessor();
            succ.pushContext(succ.getAbstraction(), retNode);            
        }
    }
    
    private ItpSymbolicAbstractElementCreator elemCreator;
    private ItpCPAStatistics stats;
    private Map<SummaryCFANode, Map<CFANode, Pair<SymbolicFormula, SSAMap>>> 
        summaryToFormulaMap;

    private ItpSymbolicCPA() {
        super();
        elemCreator = new ItpSymbolicAbstractElementCreator();
        stats = new ItpCPAStatistics(this,
                "Symbolic Interpolation-based Lazy Abstraction with Summaries");
        mgr = new MathsatSummaryFormulaManager();
        refiner = new ItpSymbolicCounterexampleRefiner();
        summaryToFormulaMap = 
            new HashMap<SummaryCFANode, 
                        Map<CFANode, Pair<SymbolicFormula, SSAMap>>>();        
    }
    
    /**
     * Constructor conforming to the "contract" in CompositeCPA. The two
     * arguments are ignored
     * @param s1
     * @param s2
     */
    public ItpSymbolicCPA(String s1, String s2) {
        this();
    }
    
    public CPAStatistics getStatistics() {
        return stats;
    }

    @Override
    public ItpAbstractElementManager getElementCreator() {
        return elemCreator;
    }
    
    public Map<CFANode, Pair<SymbolicFormula, SSAMap>> getPathFormulas(
            SummaryCFANode succLoc) {
        try {
            if (!summaryToFormulaMap.containsKey(succLoc)) {
                Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = 
                    ((MathsatSummaryFormulaManager)mgr).buildPathFormulas(
                            succLoc); 
                summaryToFormulaMap.put(succLoc, p);
                
//                CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
//                        "SYMBOLIC FORMULA FOR " + succLoc.toString() + ": " + 
//                        p.getFirst().toString());
                
            }
            return summaryToFormulaMap.get(succLoc);
        } catch (UnrecognizedCFAEdgeException e) {
            e.printStackTrace();
            return null;
        }
    }    
    
}
