package cpaplugin.cpa.cpas.symbpredabs.explicit;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.common.CPATransferException;
import cpaplugin.cpa.common.ErrorReachedException;
import cpaplugin.cpa.common.RefinementNeededException;
import cpaplugin.cpa.common.interfaces.AbstractDomain;
import cpaplugin.cpa.common.interfaces.AbstractElement;
import cpaplugin.cpa.common.interfaces.TransferRelation;
import cpaplugin.cpa.cpas.symbpredabs.AbstractFormula;
import cpaplugin.cpa.cpas.symbpredabs.CounterexampleTraceInfo;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormulaManager;
import cpaplugin.cpa.cpas.symbpredabs.UpdateablePredicateMap;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * TransferRelation for explicit-state lazy abstraction. This is the most
 * complex of the CPA-related classes, and where the analysis is actually
 * performed.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class ExplicitTransferRelation implements TransferRelation {

    // Abstract Reachability Tree of AbstractElements
    class ART {
        Map<AbstractElement, Vector<AbstractElement>> tree;
        AbstractElement root;
        
        public ART() {
            tree = new HashMap<AbstractElement, Vector<AbstractElement>>();
            root = null;
        }
        
        public AbstractElement getRoot() { return root; }
        
        public void addChild(AbstractElement parent, AbstractElement child) {
            if (root == null) {
                root = parent;
            }
            if (!tree.containsKey(parent)) {
                tree.put(parent, new Vector<AbstractElement>()); 
            }
            Collection<AbstractElement> c = tree.get(parent);
            c.add(child);
        }

        /**
         * Returns the elements in the subtree rooted at "root". If "remove"
         * is true, remove the elements from the ART (used in refinement). If
         * "includeRoot" is false, don't include "root" itself
         */
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

        // TODO - OBSOLETE and to be removed
        public Collection<AbstractElement> getRightSubtree(AbstractElement root,
                Collection<ExplicitAbstractElement> path, boolean includeRoot) {
            Vector<AbstractElement> ret = new Vector<AbstractElement>();
            
            Stack<AbstractElement> toProcess = new Stack<AbstractElement>();
            toProcess.push(root);
            Iterator<ExplicitAbstractElement> it = path.iterator();
            AbstractElement curInPath = null;//it.next();
            //assert(curInPath.equals(root));
            
            while (!toProcess.empty()) {
                AbstractElement cur = toProcess.pop();
                ret.add(cur);
                if (tree.containsKey(cur)) {
                    assert(it.hasNext());
                    curInPath = it.next();
                    Vector<AbstractElement> children = tree.get(cur);
                    for (int j = 0; j < children.size(); ++j) {
                        if (children.elementAt(j).equals(curInPath)) {
                            toProcess.add(children.remove(children.size()-1));
                            while (children.size() > j) {
                                Collection<AbstractElement> t = 
                                    getSubtree(children.remove(
                                            children.size()-1), true, true);
                                ret.addAll(t);
                            }
                        }
                    }                    
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
        
        public boolean contains(AbstractElement n) {
            return tree.containsKey(n);
        }
    }

    private ExplicitAbstractDomain domain;
    private ART abstractTree;
    
    private int numAbstractStates = 0; // for statistics
    private boolean errorReached = false;
    
    public ExplicitTransferRelation(ExplicitAbstractDomain d) {
        domain = d;
        abstractTree = new ART();
    }
    
    public int getNumAbstractStates() { return numAbstractStates; }
    public boolean hasReachedError() { return errorReached; }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    // isFunctionStart and isFunctionEnd are used to manage the call stack
    private boolean isFunctionStart(ExplicitAbstractElement elem) {
        return (elem.getLocation() instanceof FunctionDefinitionNode);
    }
    
    private boolean isFunctionEnd(ExplicitAbstractElement elem) {
        CFANode n = elem.getLocation();
        return (n.getNumLeavingEdges() == 1 &&
                n.getLeavingEdge(0) instanceof ReturnEdge);
    }

    // performs the abstract post operation
    private AbstractElement buildSuccessor(ExplicitAbstractElement e,
            CFAEdge edge) throws CPATransferException {
        ExplicitCPA cpa = domain.getCPA();
        CFANode succLoc = edge.getSuccessor();
        
        // check whether the successor is an error location: if so, we want
        // to check for feasibility of the path...
        
        Collection<Predicate> predicates = 
            cpa.getPredicateMap().getRelevantPredicates(
                    e.getLocation());
//        if (predicates.isEmpty() && e.getParent() != null) {
//            predicates = cpa.getPredicateMap().getRelevantPredicates(
//                    e.getParent().getLocation());
//        }
                
        ExplicitAbstractElement succ = new ExplicitAbstractElement(succLoc);
        
        // if e is the end of a function, we must find the correct return 
        // location
        if (isFunctionEnd(e)) {
            CFANode retNode = e.topContextLocation();
            if (!succLoc.equals(retNode)) {
                LazyLogger.log(LazyLogger.DEBUG_1,
                        "Return node for this call is: ", retNode,
                        ", but edge leads to: ", succLoc, ", returning BOTTOM");
                return domain.getBottomElement();
            }
        }
        
        succ.setContext(e.getContext(), false);
        if (isFunctionEnd(e)) {
            succ.popContext();
        }
        
        ExplicitAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
        AbstractFormula abstraction = amgr.buildAbstraction(
                cpa.getFormulaManager(), e, succ, edge, predicates);
        succ.setAbstraction(abstraction);
        succ.setParent(e);
        
        Level lvl = LazyLogger.DEBUG_1;
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            SymbolicFormulaManager mgr = cpa.getFormulaManager();
            LazyLogger.log(lvl, "COMPUTED ABSTRACTION: ", 
                           amgr.toConcrete(mgr, abstraction));
        }
        
        if (amgr.isFalse(abstraction)) {
            return domain.getBottomElement();
        } else {
            //++numAbstractStates;
            // if we reach an error state, we want to log this...
            if (succ.getLocation() instanceof CFAErrorNode) {
                if (CPAMain.cpaConfig.getBooleanValue(
                        "cpas.symbpredabs.abstraction.norefinement")) {
                    errorReached = true;
                    throw new ErrorReachedException(
                            "Reached error location, but refinement disabled");
                }
                // oh oh, reached error location. Let's check whether the 
                // trace is feasible or spurious, and in case refine the
                // abstraction
                //
                // first we build the abstract path
                Deque<ExplicitAbstractElement> path = 
                    new LinkedList<ExplicitAbstractElement>();
                path.addFirst(succ);
                ExplicitAbstractElement parent = succ.getParent();
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
                    performRefinement(path, info);
                } else {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                            "REACHED ERROR LOCATION!: ", succ, 
                            " RETURNING BOTTOM!");
                    errorReached = true;
                    throw new ErrorReachedException(
                            info.getConcreteTrace().toString());
                }
                return domain.getBottomElement();
            }
            
            if (isFunctionStart(succ)) {
                // we push into the context the return location, which is
                // the successor location of the summary edge
                assert(e.getLocation().getLeavingSummaryEdge() != null);
                CFANode retNode = null;
                retNode = 
                    e.getLocation().getLeavingSummaryEdge().getSuccessor();
                succ.pushContext(e.getAbstraction(), retNode);
//                for (CFANode l : e.getLeaves()) {  
//                    if (l instanceof FunctionDefinitionNode) {
//                        assert(l.getNumLeavingEdges() == 1);
//                        assert(l.getNumEnteringEdges() == 1);
//                        
//                        CFAEdge ee = l.getLeavingEdge(0);
//                        InnerCFANode n = (InnerCFANode)ee.getSuccessor();
//                        if (n.getSummaryNode().equals(succ.getLocation())) {
//                            CFANode pr = l.getEnteringEdge(0).getPredecessor();
//                            CallToReturnEdge ce = pr.getLeavingSummaryEdge();
//                            //assert(ce != null);
//                            if (ce != null) {
//                            retNode = ((InnerCFANode)ce.getSuccessor()).
//                                                               getSummaryNode();
//                            break;
//                            }
//                        }
//                    }
//                }
                //assert(retNode != null);
                if (retNode != null) {
//                LazyLogger.log(LazyLogger.DEBUG_3, "PUSHING CONTEXT TO ", succ,
//                        ": ", cpa.getAbstractFormulaManager().toConcrete(
//                                cpa.getFormulaManager(), 
//                                succ.getAbstraction()));
//                //succ.getContext().push(succ.getAbstraction());
//                succ.pushContext(succ.getAbstraction(), retNode);
                }
            }            
            
            return succ;
        }
    }

    // abstraction refinement is performed here
    private void performRefinement(Deque<ExplicitAbstractElement> path, 
            CounterexampleTraceInfo info) throws CPATransferException {
        LazyLogger.log(LazyLogger.DEBUG_1, "STARTING REFINEMENT");
        UpdateablePredicateMap curpmap =
            (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
        ExplicitAbstractElement root = null;
        ExplicitAbstractElement firstInterpolant = null;
        for (ExplicitAbstractElement e : path) {
            Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
            if (firstInterpolant == null && newpreds.size() > 0) {
                firstInterpolant = e;
            }
            if (curpmap.update((CFANode)e.getLocation(), newpreds)) {
                LazyLogger.log(LazyLogger.DEBUG_1, "REFINING LOCATION: ",
                        e.getLocation());
                if (root == null) {
//                    cur = e;
                    root = e.getParent();
                }
//                else if (root.getLocation().equals(e.getLocation())) {
//                    root = e;
//                }
            }
        }
        assert(root != null);// || firstInterpolant == path.getFirst());
        //root = firstInterpolant;
        //root = (ExplicitAbstractElement)abstractTree.getRoot();
        if (root == null) {
            assert(firstInterpolant != null);            
//            assert(CPAMain.cpaConfig.getBooleanValue(
//                    "cpas.symbpredabs.refinement.addPredicatesGlobally"));
            //root = abstractTree.getRoot();
            root = firstInterpolant;
        }
        assert(root != null);
        //root = path.getFirst();
        Collection<AbstractElement> toWaitlist = new HashSet<AbstractElement>();
        toWaitlist.add(root);
        Collection<AbstractElement> toUnreach = 
            abstractTree.getSubtree(root, true, false);
//        ExplicitCPA cpa = domain.getCPA();
//        for (AbstractElement e : toUnreach) {
//            Set<ExplicitAbstractElement> cov = cpa.getCoveredBy(
//                    (ExplicitAbstractElement)e);
//            for (AbstractElement c : cov) {
//                if (!((ExplicitAbstractElement)c).isDescendant(
//                        (ExplicitAbstractElement)root)) {
//                    toWaitlist.add(c);
//                }
//            }
//            cpa.uncoverAll((ExplicitAbstractElement)e);
//        }
        
//        Vector<ExplicitAbstractElement> pth = 
//            new Vector<ExplicitAbstractElement>();
//        boolean add = false;
//        for (ExplicitAbstractElement e : path) {
//            if (e == cur) {
//                add = true;
//            }
//            if (add) {
//                pth.add(e);
//            }
//        }
//        Collection<AbstractElement> toUnreach = 
//            abstractTree.getRightSubtree(root, pth, false);
        LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toWaitlist: ", 
                toWaitlist);
        LazyLogger.log(LazyLogger.DEBUG_1, "REFINEMENT - toUnreach: ", 
                toUnreach);
        throw new RefinementNeededException(toUnreach, toWaitlist);
    }

    @Override
    public AbstractElement getAbstractSuccessor(AbstractElement element,
            CFAEdge cfaEdge) throws CPATransferException {
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       "Getting Abstract Successor of element: ", element, 
                       " on edge: ", cfaEdge.getRawStatement());
        
        if (!abstractTree.contains(element)) {
            ++numAbstractStates;
        }

        // To get the successor, we compute the predicate abstraction of the
        // formula of element plus all the edges that connect any of the 
        // inner nodes of the summary of element to any inner node of the  
        // destination
        ExplicitAbstractElement e = (ExplicitAbstractElement)element;
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
        ExplicitAbstractElement e = (ExplicitAbstractElement)element;
        CFANode src = (CFANode)e.getLocation();
        
        for (int i = 0; i < src.getNumLeavingEdges(); ++i) {
            AbstractElement newe = 
                getAbstractSuccessor(e, src.getLeavingEdge(i));
            if (newe != domain.getBottomElement()) {
                allSucc.add(newe);
            }
        }
        
        LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                       allSucc.size(), " successors found");

        return allSucc;
    }
    
}
