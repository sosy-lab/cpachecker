package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
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
import cpaplugin.cpa.cpas.symbpredabs.Pair;
import cpaplugin.cpa.cpas.symbpredabs.Predicate;
import cpaplugin.cpa.cpas.symbpredabs.SSAMap;
import cpaplugin.cpa.cpas.symbpredabs.SymbolicFormula;
import cpaplugin.cpa.cpas.symbpredabs.UpdateablePredicateMap;
import cpaplugin.exceptions.CPAException;
import cpaplugin.logging.CPACheckerLogger;
import cpaplugin.logging.CustomLogLevel;
import cpaplugin.logging.LazyLogger;


/**
 * Transfer relation for symbolic lazy abstraction with summaries
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryTransferRelation implements TransferRelation {

    // the Abstract Reachability Tree
    class ART {
        Map<AbstractElement, Collection<AbstractElement>> tree;
        AbstractElement root;
        
        public ART() {
            tree = new HashMap<AbstractElement, Collection<AbstractElement>>();
            root = null;
        }
        
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
        
        public AbstractElement findHighest(SummaryCFANode loc) {
            if (root == null) return null;
            
            Queue<AbstractElement> toProcess =
                new ArrayDeque<AbstractElement>();
            toProcess.add(root);
            
            while (!toProcess.isEmpty()) {
                SummaryAbstractElement e =
                    (SummaryAbstractElement)toProcess.remove();
                if (e.getLocation().equals(loc)) {
                    return e;
                }
                if (tree.containsKey(e)) {
                    toProcess.addAll(tree.get(e));
                }
            }
            System.out.println("ERROR, NOT FOUND: " + loc);
            //assert(false);
            //return null;
            return root; 
        }        
    }
    
    class Path {
        Vector<Integer> elemIds;
        
        public Path(Deque<SummaryAbstractElement> cex) {
            elemIds = new Vector<Integer>();
            elemIds.ensureCapacity(cex.size());
            for (SummaryAbstractElement e : cex) {
                elemIds.add(((CFANode)e.getLocation()).getNodeNumber());
            }
        }
        
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o instanceof Path) {
                return elemIds.equals(((Path)o).elemIds);
            }
            return false;
        }
        
        public int hashCode() {
            return elemIds.hashCode();
        }
    }    

    private SummaryAbstractDomain domain;
    private ART abstractTree;
    private Map<Path, Integer> seenAbstractCounterexamples;
    
    private int numAbstractStates = 0; // for statistics
    
    public SummaryTransferRelation(SummaryAbstractDomain d) {
        domain = d;
        abstractTree = new ART();
        seenAbstractCounterexamples = new HashMap<Path, Integer>();
    }
    
    public int getNumAbstractStates() { return numAbstractStates; }
    
    @Override
    public AbstractDomain getAbstractDomain() {
        return domain;
    }

    // isFunctionStart and isFunctionEnd are used for managing the context,
    // needed for handling function calls
    
    private boolean isFunctionStart(SummaryAbstractElement elem) {
        return (elem.getLocation().getInnerNode() instanceof 
                FunctionDefinitionNode);
    }
    
    private boolean isFunctionEnd(SummaryAbstractElement elem) {
        CFANode n = elem.getLocation().getInnerNode();
        return (n.getNumLeavingEdges() == 1 &&
                n.getLeavingEdge(0) instanceof ReturnEdge);
//        return (elem.getLocation().getInnerNode().getEnteringSummaryEdge() 
//                != null);
    }

    // abstract post operation
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
        
        // if e is the end of a function, we must find the correct return 
        // location
        if (isFunctionEnd(succ)) {
            SummaryCFANode retNode = e.topContextLocation();
            if (!succLoc.equals(retNode)) {
                LazyLogger.log(LazyLogger.DEBUG_1,
                        "Return node for this call is: ", retNode,
                        ", but edge leads to: ", succLoc, ", returning BOTTOM");
                return domain.getBottomElement();
            }
        }
        
//        Stack<AbstractFormula> context = 
//            (Stack<AbstractFormula>)e.getContext().clone();
//        if (isFunctionEnd(e)) {
//            context.pop();
//        }
//        succ.setContext(context);
        succ.setContext(e.getContext(), false);
        if (isFunctionEnd(succ)) {
            succ.popContext();
        }
        
        SummaryAbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
        AbstractFormula abstraction = amgr.buildAbstraction(
                cpa.getFormulaManager(), e, succ, predicates);
        succ.setAbstraction(abstraction);
        succ.setParent(e);
        
        Level lvl = LazyLogger.DEBUG_1;
        if (CPACheckerLogger.getLevel() <= lvl.intValue()) {
            SummaryFormulaManager mgr = cpa.getFormulaManager();
            LazyLogger.log(lvl, "COMPUTED ABSTRACTION: ", 
                           amgr.toConcrete(mgr, abstraction));
        }
        
        if (amgr.isFalse(abstraction)) {
            return domain.getBottomElement();
        } else {
            ++numAbstractStates;
            // if we reach an error state, we want to log this...
            if (succ.getLocation().getInnerNode() instanceof CFAErrorNode) {
                if (CPAMain.cpaConfig.getBooleanValue(
                        "cpas.symbpredabs.abstraction.norefinement")) {
                    CPAMain.cpaStats.setErrorReached(true);
                    throw new ErrorReachedException(
                            "Reached error location, but refinement disabled");
                }
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
                    performRefinement(path, info);
                } else {
                    LazyLogger.log(CustomLogLevel.SpecificCPALevel, 
                            "REACHED ERROR LOCATION!: ", succ, 
                            " RETURNING BOTTOM!");
                    CPAMain.cpaStats.setErrorReached(true);
                    throw new ErrorReachedException(
                            info.getConcreteTrace().toString());
                }
                return domain.getBottomElement();
            }
            
            if (isFunctionStart(succ)) {
                // we push into the context the return location, which is
                // the successor location of the summary edge
                SummaryCFANode retNode = null;
                for (CFANode l : e.getLeaves()) {  
                    if (l instanceof FunctionDefinitionNode) {
                        assert(l.getNumLeavingEdges() == 1);
                        //assert(l.getNumEnteringEdges() == 1);
                        
                        CFAEdge ee = l.getLeavingEdge(0);
                        InnerCFANode n = (InnerCFANode)ee.getSuccessor();
                        if (n.getSummaryNode().equals(succ.getLocation())) {
                            CFANode pr = l.getEnteringEdge(0).getPredecessor();
                            CallToReturnEdge ce = pr.getLeavingSummaryEdge();
                            //assert(ce != null);
                            if (ce != null) {
                                retNode = ((InnerCFANode)ce.getSuccessor()).
                                            getSummaryNode();
                                break;
                            }
                        }
                    }
                }
                //assert(retNode != null);
                if (retNode != null) {
                LazyLogger.log(LazyLogger.DEBUG_3, "PUSHING CONTEXT TO ", succ,
                        ": ", cpa.getAbstractFormulaManager().toConcrete(
                                cpa.getFormulaManager(), 
                                succ.getAbstraction()));
                //succ.getContext().push(succ.getAbstraction());
                succ.pushContext(succ.getAbstraction(), retNode);
                }
            }            
            
            return succ;
        }
    }


    // abstraction refinement and undoing of (part of) the ART
    private void performRefinement(Deque<SummaryAbstractElement> path, 
            CounterexampleTraceInfo info) throws CPATransferException {
        Path pth = new Path(path);
        int numSeen = 0;
        if (seenAbstractCounterexamples.containsKey(pth)) {
            numSeen = seenAbstractCounterexamples.get(pth);
        }
        seenAbstractCounterexamples.put(pth, numSeen+1);

        UpdateablePredicateMap curpmap =
            (UpdateablePredicateMap)domain.getCPA().getPredicateMap();
        AbstractElement root = null;
        AbstractElement firstInterpolant = null;
        for (SummaryAbstractElement e : path) {
            Collection<Predicate> newpreds = info.getPredicatesForRefinement(e);
            if (firstInterpolant == null && newpreds.size() > 0) {
                firstInterpolant = e;
            }
            if (curpmap.update((CFANode)e.getLocation(), newpreds)) {
                if (root == null) {
                    root = e.getParent();
                }
            }
        }
        if (root == null) {
            assert(firstInterpolant != null);
            if (numSeen > 1) {
//                assert(numSeen == 2);
            } else {
                assert(numSeen <= 1);
            }
            root = abstractTree.findHighest(
                    ((SummaryAbstractElement)firstInterpolant).getLocation());
        }
        assert(root != null);
        if (CPAMain.cpaConfig.getBooleanValue("analysis.bfs")) {
            // TODO When using bfs traversal, we would have to traverse the ART
            // computed so far, and check for each leaf whether to re-add it
            // to the waiting list or not, similarly to what Blast does
            // (file psrc/be/modelChecker/lazyModelChecker.ml, function
            // update_tree_after_refinment). But for now, for simplicity we 
            // just restart from scratch
            root = path.getFirst();
        }
        
        assert(root != null);
        //root = path.getFirst();
        Collection<AbstractElement> toWaitlist = new HashSet<AbstractElement>();
        toWaitlist.add(root);
        Collection<AbstractElement> toUnreach = 
            abstractTree.getSubtree(root, true, false);
        SummaryCPA cpa = domain.getCPA();
        for (AbstractElement e : toUnreach) {
            Set<SummaryAbstractElement> cov = cpa.getCoveredBy(
                    (SummaryAbstractElement)e);
            for (AbstractElement c : cov) {
                if (!((SummaryAbstractElement)c).isDescendant(
                        (SummaryAbstractElement)root)) {
                    toWaitlist.add(c);
                }
            }
            cpa.uncoverAll((SummaryAbstractElement)e);
        }
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
