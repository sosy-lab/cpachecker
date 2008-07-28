package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class SummaryTransferRelation implements TransferRelation {

    private SummaryAbstractDomain domain;
    
    public SummaryTransferRelation(SummaryAbstractDomain d) {
        domain = d;
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }
    
    private AbstractElement buildSuccessor(SummaryAbstractElement e,
                                           CFAEdge edge) {
        SummaryCPA cpa = domain.getCPA();
        SummaryCFANode succLoc = (SummaryCFANode)edge.getSuccessor();
        // check whether the successor is an error location: if so, we want
        // to check for feasibility of the path...
        
        Collection<Predicate> predicates = 
            cpa.getPredicateMap().getRelevantPredicates(
                    edge.getSuccessor());
        SummaryAbstractElement succ = new SummaryAbstractElement(succLoc);
        Map<CFANode, Pair<SymbolicFormula, SSAMap>> p = 
            cpa.getPathFormulas(succLoc);
        succ.setPathFormulas(p);
        SummaryAbstractFormulaManager amgr = cpa.getAbstractFormulaManager(); 
        AbstractFormula abstraction = amgr.buildAbstraction(
                cpa.getFormulaManager(), e, succ, predicates);
        succ.setAbstraction(abstraction);
        succ.setParent(e);
        
        Level lvl = CustomLogLevel.SpecificCPALevel;
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            SummaryFormulaManager mgr = cpa.getFormulaManager();
            LazyLogger.log(lvl, "COMPUTED ABSTRACTION: ", 
                           amgr.toConcrete(mgr, abstraction));
        }
        
        if (amgr.isFalse(abstraction)) {
            return domain.getBottomElement();
        } else {            
            // if we reach an error state, we want to log this...
            if (succ.getLocation().getInnerNode() instanceof CFAErrorNode) {
                LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                               "REACHED ERROR LOCATION!: ", succ, 
                               " RETURNING BOTTOM!");
                return domain.getBottomElement();
            } 
            return succ;
        }
    }

    @Override
    public AbstractElement getAbstractSuccessor(AbstractElement element,
            CFAEdge cfaEdge) {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Getting Abstract Successor of element: ", element, 
                       " on edge: ", cfaEdge);

        // To get the successor, we compute the predicate abstraction of the
        // formula of element plus all the edges that connect any of the 
        // inner nodes of the summary of element to any inner node of the  
        // destination
        SummaryAbstractElement e = (SummaryAbstractElement)element;
        CFANode src = (CFANode)e.getLocation();
        
        for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
            CFAEdge edge = src.getLeavingEdge(i);
            if (edge.equals(cfaEdge)) {
                AbstractElement ret = buildSuccessor(e, edge);

                LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                               "Successor is: ", ret);
                
                return ret;
            }
        }
        
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

        return domain.getBottomElement();
    }

    @Override
    public List<AbstractElement> getAllAbstractSuccessors(
            AbstractElement element) throws CPAException {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Getting ALL Abstract Successors of element: ", 
                       element);

        List<AbstractElement> allSucc = new Vector<AbstractElement>();
        SummaryAbstractElement e = (SummaryAbstractElement)element;
        CFANode src = (CFANode)e.getLocation();
        
        for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
            allSucc.add(buildSuccessor(e, src.getLeavingEdge(i)));
        }
        
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       allSucc.size(), " successors found");

        return allSucc;
    }

}
