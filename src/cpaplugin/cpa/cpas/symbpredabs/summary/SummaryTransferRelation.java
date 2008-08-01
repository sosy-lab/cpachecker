package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.ErrorReachedExeption;
import cpaplugin.cpa.common.RefinementNeededException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.PredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.UpdateablePredicateMap;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;

public class SummaryTransferRelation implements TransferRelation {
    
    class ART {
        Map<AbstractElement, Collection<AbstractElement>> tree;
        
        public ART() {
            tree = new HashMap<AbstractElement, Collection<AbstractElement>>();
        }
        
        public void addChild(AbstractElement parent, AbstractElement child) {
            if (!tree.containsKey(parent)) {
                tree.put(parent, new Vector<AbstractElement>()); 
            }
            Collection<AbstractElement> c = tree.get(parent);
            c.add(child);
        }
        
        public Collection<AbstractElement> getSubtree(AbstractElement root, 
                boolean remove, boolean includeRoot) {
            Vector<AbstractElement> ret = new Vector<AbstractElement>();
            
            Stack<AbstractElement> toProcess = new Stack<AbstractElement>();
            toProcess.push(root);
            
            while (!toProcess.empty()) {
                AbstractElement cur = toProcess.pop();
                ret.add(cur);
                if (tree.containsKey(cur)) {
                    toProcess.addAll(remove ? tree.remove(cur) : tree.get(cur));
                }
            }
            if (!includeRoot) {
                AbstractElement tmp = ret.lastElement();
                assert(ret.firstElement() == root);
                ret.setElementAt(tmp, 0);
                ret.remove(ret.size()-1);
            }
            return ret;
        }
    }

    private SummaryAbstractDomain domain;
    private ART abstractTree;
    
    public SummaryTransferRelation(SummaryAbstractDomain d) {
        domain = d;
        abstractTree = new ART();
    }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }
    
    private AbstractElement buildSuccessor(SummaryAbstractElement e,
            CFAEdge edge) throws CPATransferException {
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
                // oh oh, reached error location. Let's check whether the 
                // trace is feasible or spurious, and in case refine the
                // abstraction
                //
                // first we build the abstract path
                Deque<SummaryAbstractElement> path = 
                    new LinkedList<SummaryAbstractElement>();
                path.addFirst(succ);
                SummaryAbstractElement parent = succ.getParent();
                while (parent != null) {
                    path.addFirst(parent);
                    parent = parent.getParent();
                }
                CounterexampleTraceInfo info = 
                    amgr.buildCounterexampleTrace(
                            cpa.getFormulaManager(), path);
                if (info.isSpurious()) {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel,
                            "Found spurious error trace, refining the ",
                            "abstraction");
                    performRefinement(path, info.getPredicatesForRefinement());
                } else {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                            "REACHED ERROR LOCATION!: ", succ, 
                            " RETURNING BOTTOM!");
                    throw new ErrorReachedExeption(
                            info.getConcreteTrace().toString());
                }
                return domain.getBottomElement();
            } 
            return succ;
        }
    }

    private void performRefinement(Deque<SummaryAbstractElement> path, 
            PredicateMap pmap) throws CPATransferException {
        // TODO Auto-generated method stub
        UpdateablePredicateMap curpmap =
            (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
        AbstractElement root = null;
        for (SummaryAbstractElement e : path) {
            Collection<Predicate> newpreds = pmap.getRelevantPredicates(
                    (CFANode)e.getLocation());
            if (curpmap.update((CFANode)e.getLocation(), newpreds)) {
                if (root == null) {
                    root = e.getParent();
                }
            }
        }
        assert(root != null);
        Collection<AbstractElement> toWaitlist = Collections.singleton(root);
        Collection<AbstractElement> toUnreach = 
            abstractTree.getSubtree(root, true, false);
//        Collection<AbstractElement> toUnreach = new Vector<AbstractElement>();
//        boolean add = false;
//        for (AbstractElement e : path) {
//            if (add) { 
//                toUnreach.add(e);
//            } else if (e == root) {
//                add = true;
//            }
//        }
        LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ", root);
        LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ", 
                toUnreach);
        throw new RefinementNeededException(toUnreach, toWaitlist);
    }

    @Override
    public AbstractElement getAbstractSuccessor(AbstractElement element,
            CFAEdge cfaEdge) throws CPATransferException {
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
                
                if (ret != domain.getBottomElement()) {
                    abstractTree.addChild(e, ret);
                }
                
                return ret;
            }
        }
        
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, "Successor is: BOTTOM");

        return domain.getBottomElement();
    }

    @Override
    public List<AbstractElement> getAllAbstractSuccessors(
            AbstractElement element) throws CPAException, CPATransferException {
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
