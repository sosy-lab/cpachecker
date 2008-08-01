package cpaplugin.cpa.cpas.symbpredabs;

import java.util.ArrayList;
import java.util.List;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class SymbPredAbstTransfer implements TransferRelation {

    private SymbPredAbstDomain domain;
    
    public SymbPredAbstTransfer(SymbPredAbstDomain domain) {
        this.domain = domain;
    }
    
    public AbstractDomain getAbstractDomain() {
        return domain;
    }
    
    private AbstractElement buildSuccessor(SymbolicFormulaManager mgr,
                                           SymbPredAbstElement e,
                                           CFAEdge curEdge) {
        // Ok, found. What to do here depends on whether we have
        // computed the abstraction at the previous step or not. 
        // If yes, we have to set the right parent pointer
        SymbPredAbstElement parent = e.getParent();
        if (e.getConcreteFormula().isTrue()) {
            parent = e;
        }        
        try {
            Pair<SymbolicFormula, SSAMap> p = 
                mgr.makeAnd(e.getConcreteFormula(), curEdge, e.getSSAMap(), false, true); 
            SymbPredAbstElement ret = new SymbPredAbstElement(
                    curEdge.getSuccessor(), 
                    p.getFirst(), e.getAbstractFormula(),
                    parent, p.getSecond());
            // if the destination is an error location, we want to check for 
            // feasibility of the path
            if (curEdge.getSuccessor() instanceof CFAErrorNode) {
                CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
                                     "Edge leads to Error Location, checking " +
                                     "feasibility...");
                SymbolicFormula f = ret.getFormula();
                if (!mgr.entails(mgr.makeTrue(), f)) {
                    // if the path is infeasible, we return the bottom element
                    // as successor
                    CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
                                         "OK, Error Location UNREACHABLE");                    
                    return domain.getBottomElement();
                } else {
                    CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
                                         "ERROR LOCATION IS REACHABLE");
                }
            }
            return ret;
        } catch (UnrecognizedCFAEdgeException exc) {
            CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel,
                    "UNRECOGNIZED CFA EDGE: " + exc.toString());
            return null;
        }
    }

    public AbstractElement getAbstractSuccessor(AbstractElement element,
                                                CFAEdge cfaEdge) throws CPATransferException {
        SymbPredAbstElement e = (SymbPredAbstElement)element;

        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                "Getting Abstract Successor of element: " + e.toString() + 
                " on edge: " + cfaEdge.getRawStatement());
        
        CFANode node = e.getLocation();
        SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();

        for (int i = 0; i < node.getNumLeavingEdges(); ++i) {
            CFAEdge curEdge = node.getLeavingEdge(i);
            if (curEdge == cfaEdge) {
                AbstractElement ret = buildSuccessor(mgr, e, curEdge);

                CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                        "    Successor is: " + ret.toString());
                
                return ret;
            }
        }
        
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                "    Successor is: BOTTOM");
        
        return domain.getBottomElement();
    }

    public List<AbstractElement> getAllAbstractSuccessors(
            AbstractElement element) throws CPAException, CPATransferException {
        SymbPredAbstElement e = (SymbPredAbstElement)element;
        
        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                "Getting ALL Abstract Successors of element: " + e.toString());
        
        List<AbstractElement> allSucc = new ArrayList<AbstractElement>();
        CFANode n = e.getLocation();
        SymbolicFormulaManager mgr = domain.getCPA().getFormulaManager();

        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
            allSucc.add(buildSuccessor(mgr, e, n.getLeavingEdge(i)));
        }

        CPACheckerLogger.log(CustomLogLevel.SpecificCPALevel, 
                "    " + Integer.toString(allSucc.size()) + 
                " successors found");

        return allSucc;
    }

}
