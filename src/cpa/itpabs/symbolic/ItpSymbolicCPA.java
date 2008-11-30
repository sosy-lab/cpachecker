package cpa.itpabs.symbolic;

import java.util.HashMap;
import java.util.Map;

import logging.LazyLogger;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.ReturnEdge;

import cpa.itpabs.ItpAbstractElement;
import cpa.itpabs.ItpAbstractElementManager;
import cpa.itpabs.ItpCPA;
import cpa.itpabs.ItpCPAStatistics;
import cpaplugin.CPAStatistics;
import cpa.itpabs.symbolic.ItpSymbolicAbstractElement;
import cpa.itpabs.symbolic.ItpSymbolicCounterexampleRefiner;
import cpa.symbpredabs.Pair;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.UnrecognizedCFAEdgeException;
import cpa.symbpredabs.mathsat.summary.MathsatSummaryFormulaManager;
import cpa.symbpredabs.summary.InnerCFANode;
import cpa.symbpredabs.summary.SummaryCFANode;


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
        public boolean isFunctionEnd(ItpAbstractElement e, 
                                     ItpAbstractElement succ) {
            CFANode n = succ.getLocation();
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
            if (isFunctionEnd(e, succ)) {
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
            SummaryCFANode retNode = null;
            for (CFANode l : e.getLeaves()) {  
                if (l instanceof FunctionDefinitionNode) {
                    assert(l.getNumLeavingEdges() == 1);

                    CFAEdge ee = l.getLeavingEdge(0);
                    InnerCFANode n = (InnerCFANode)ee.getSuccessor();
                    if (n.getSummaryNode().equals(succ.getLocation())) {
                        CFANode pr = l.getEnteringEdge(0).getPredecessor();
                        CallToReturnEdge ce = pr.getLeavingSummaryEdge();
                        if (ce != null) {
                            retNode = ((InnerCFANode)ce.getSuccessor()).
                            getSummaryNode();
                            break;
                        }
                    }
                }
            }
            if (retNode != null) {
                LazyLogger.log(LazyLogger.DEBUG_3, "PUSHING CONTEXT TO ", succ);
                succ.pushContext(succ.getAbstraction(), (CFANode)retNode);
            }
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
