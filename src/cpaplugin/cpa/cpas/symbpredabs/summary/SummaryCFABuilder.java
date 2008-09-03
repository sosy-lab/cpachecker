package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;

import cpaplugin.cfa.objectmodel.BlankEdge;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.CallToReturnEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.FunctionCallEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.ReturnEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cmdline.CPAMain;
import cpaplugin.cpa.cpas.symbpredabs.GlobalDeclarationEdge;
import cpaplugin.logging.LazyLogger;

/**
 * Manipulates the original CFA(s) of the program, to build the "summary" CFAs,
 * in which each node summarizes a loop-free subpart of the original program
 * 
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class SummaryCFABuilder {
    // maps each original node to its summary location
    private Map<CFANode, SummaryCFANode> summaryMap;
    private Map<SummaryCFANode, Integer> summarySizeMap;
    // entry point of the program
    private CFAFunctionDefinitionNode mainFunction;
    // maps each original node to its copy used in the summary CFA
    private Map<CFANode, CFANode> nodeMap;
    // list of global variables. At the moment, we simply initialize them
    // at the beginning of mainFunction
    private List<IASTDeclaration> globalVars;
    
    public SummaryCFABuilder(CFAFunctionDefinitionNode mainFunction,
                             List<IASTDeclaration> globalVars) {
        this.mainFunction = mainFunction;
        this.globalVars = globalVars;
        summaryMap = new HashMap<CFANode, SummaryCFANode>();
        nodeMap = new HashMap<CFANode, CFANode>();
        summarySizeMap = new HashMap<SummaryCFANode, Integer>();
    }
    
    public CFAFunctionDefinitionNode buildSummary() {
//        mainFunction = 
//            (CFAFunctionDefinitionNode)removeIrrelevant(mainFunction);
        mainFunction = buildSummary(mainFunction);
        // ensure that the error location has a dummy successor - this is needed
        // by some of the CPAs (like the interpolation-based ones)
        for (SummaryCFANode s : summarySizeMap.keySet()) {
            if (s.getInnerNode() instanceof CFAErrorNode) {
                if (((CFANode)s).getNumLeavingEdges() == 0) {
                    InnerNode n = new InnerNode(0);
                    SummaryNode s2 = new SummaryNode(n);
                    n.setSummaryNode(s2);
                    CFAEdge e = new SummaryCFAEdge();
                    e.initialize((CFANode)s, s2);
                    e = new BlankEdge("DUMMY");
                    e.initialize(s.getInnerNode(), n);
                }
            }
        }
        return mainFunction;
//        SummaryCFAFunctionDefinitionNode s = 
//            (SummaryCFAFunctionDefinitionNode)buildSummary(mainFunction);
//        return removeIrrelevant(s);
    }

    // creates a copy of the "orig" edge to link "src" and "dest"
    private CFAEdge copyEdge(CFAEdge orig, CFANode src, CFANode dest) {
        CFAEdgeType tp = orig.getEdgeType();
        
        CFAEdge ret = null;
        if (tp == CFAEdgeType.AssumeEdge) {
            AssumeEdge a = (AssumeEdge)orig;
            String raw = a.getRawStatement();
            assert(raw.startsWith("[") && raw.endsWith("]"));
            raw = raw.substring(1, raw.length()-1);
            ret = new AssumeEdge(raw, a.getExpression(),
                                 a.getTruthAssumption());
        } else if (tp == CFAEdgeType.BlankEdge) {
            BlankEdge b = (BlankEdge)orig;
            ret = new BlankEdge(b.getRawStatement());
        } else if (tp == CFAEdgeType.CallToReturnEdge) {
            assert(false);
        } else if (tp == CFAEdgeType.DeclarationEdge) {
            DeclarationEdge d = (DeclarationEdge)orig;
            ret = new DeclarationEdge(
                    d.getRawStatement(), d.getDeclarators(), 
                    d.getDeclSpecifier());
        } else if (tp == CFAEdgeType.FunctionCallEdge) {
            FunctionCallEdge fc = (FunctionCallEdge)orig;
            ret = new FunctionCallEdge(fc.getRawStatement(), null);
            ((FunctionCallEdge)ret).setArguments(fc.getArguments());
        } else if (tp == CFAEdgeType.MultiDeclarationEdge) {
            assert(false);
        } else if (tp == CFAEdgeType.MultiStatementEdge) {
            assert(false);
        } else if (tp == CFAEdgeType.ReturnEdge) {
            ReturnEdge r = (ReturnEdge)orig;
            ret = new ReturnEdge(r.getRawStatement());
        } else if (tp == CFAEdgeType.StatementEdge) {
            StatementEdge s = (StatementEdge)orig;
            ret = new StatementEdge(s.getRawStatement(), s.getExpression());
            ((StatementEdge)ret).setIsJumpEdge(s.isJumpEdge());
        } else {
            assert(false); // should not happen
        }
        assert(ret != null);
        ret.setPredecessor(src);
        ret.setSuccessor(dest);
        
        LazyLogger.log(LazyLogger.DEBUG_3,
                "LINKING NODES: " + src.getNodeNumber() + "(", 
                ((InnerCFANode)src).getSummaryNode(), ") AND " + 
                 + dest.getNodeNumber() +
                "(", ((InnerCFANode)dest).getSummaryNode() + ")");
        LazyLogger.log(LazyLogger.DEBUG_3, 
                "  ORIGINAL: ", orig.getPredecessor().getNodeNumber(), " AND ",
                orig.getSuccessor().getNodeNumber());

        
        return ret;
    }
    
    // builds a copy of the input node, without any edge connected to it
    private CFANode copyNode(CFANode orig) {
        if (nodeMap.containsKey(orig)) {
            return nodeMap.get(orig);
        } else {
            CFANode ret = null;
            if (orig instanceof FunctionDefinitionNode) {
                FunctionDefinitionNode f = (FunctionDefinitionNode)orig;
                ret = new InnerFunctionDefinitionNode(
                        f.getLineNumber(), f.getFunctionDefinition());
            } else if (orig instanceof CFAErrorNode) {
                ret = new InnerCFAErrorNode(orig.getLineNumber());
            } else {
                ret = new InnerNode(orig.getLineNumber());
            }
            if (orig.isLoopStart()) {
                ret.setLoopStart();
            }
            if (orig.getEnteringSummaryEdge() != null) {
                CallToReturnEdge ce = orig.getEnteringSummaryEdge();
                CallToReturnEdge nce = new CallToReturnEdge(
                        ce.getRawStatement(), ce.getExpression());
                // copy the summary edge and connect also to the predecessor
                CFANode dest = ce.getPredecessor();
                nce.initializeSummaryEdge(copyNode(dest), ret);
            }
            ret.setFunctionName(orig.getFunctionName());
            nodeMap.put(orig, ret);
            return ret;
        }
    }

    // returns true if this node should be at the root of a new
    // summary. Examples include loops and function calls
    private boolean shouldStartSummary(CFANode n) {
        if (n.isLoopStart() || n instanceof CFAErrorNode ||
                n.getNumLeavingEdges() == 0) {
            return true;
        } else if (n instanceof CFAFunctionDefinitionNode) {
            return true;
        } else if (n.getEnteringSummaryEdge() != null) {
            return true;
        } else {
            SummaryCFANode cur = null;
            for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
                CFAEdge e = n.getEnteringEdge(i);
                if (!isLoopBack(e)) {
                    CFANode p = e.getPredecessor();
                    if (!summaryMap.containsKey(p)) {
                        // this might happen if this e is a jump edge: in this
                        // case, we ignore it...
                        assert(e instanceof BlankEdge);
                        continue;
                    }
                    assert(summaryMap.containsKey(p));
                    SummaryCFANode s = summaryMap.get(p);
                    if (cur == null) {
                        cur = s;
                    } else if (cur != s) {
                        return true;
                    }
                }
            }
            // check if we have only blank incoming edges, and the current 
            // summary is already big TODO
            if (CPAMain.cpaConfig.getBooleanValue(
                    "cpas.symbpredabs.smallSummaries")) {
                if (n.getNumEnteringEdges() >= 1) {
                    for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
                        CFAEdge e = n.getEnteringEdge(i);
                        if (!(e instanceof BlankEdge)) break;
                        if (e instanceof BlankEdge && 
                            e.getRawStatement().startsWith(
                                    "Goto: BREAK_SUMMARY")) {
                            return true;
                        }
                    }
                }
            }
//            int summarySize = 0;
//            if (cur != null && summarySizeMap.containsKey(cur)) {
//                summarySize = summarySizeMap.get(cur);
//            }
//            final int MAX_SUMMARY_SIZE = 5;
//            if (summarySize > MAX_SUMMARY_SIZE) {
//                boolean allIncomingBlank = true;
//                for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
//                    CFAEdge e = n.getEnteringEdge(i);
//                    if (!isLoopBack(e) && !(e instanceof BlankEdge)) {
//                        allIncomingBlank = false;
//                        break;
//                    }
//                }
//                if (allIncomingBlank) return true;
//            }
            return false;
        }
    }
    
    private static final int DUPLICATE_NO = 0;
    private static final int DUPLICATE_FORWARD = 1;
    private static final int DUPLICATE_BACKWARD = -1;
    
    /*
     * Duplication of nodes. The idea is that we want to have only blank edges
     * between summary locations, as this simplifies things. But sometimes we
     * have edges with expressions between two nodes belonging to different 
     * summary locations. In this case, we duplicate one of these nodes, and
     * add a blank edge to link the two copies: this blank edge would be the one
     * that connects the two macro locations. 
     * 
     * We can decide whether to duplicate "forward" (meaning that the duplicate
     * node will be the successor of the edge) or "backward"
     */
    private int shouldDuplicate(CFANode n) {
        if (n instanceof CFAFunctionDefinitionNode) {
            return DUPLICATE_BACKWARD;
        } else if (n.getEnteringSummaryEdge() != null) {
            return DUPLICATE_FORWARD;
        } else {
            return DUPLICATE_NO;
        }
    }

    // links the summary nodes "s1" and "s2", by connecting them with a
    // SummaryCFAEdge
    private void linkSummaries(SummaryCFANode s1, SummaryCFANode s2) {
        CFANode n1 = (CFANode)s1;
        CFANode n2 = (CFANode)s2;
        
        boolean alreadyLinked = false;
        for (int i = 0; i < n1.getNumLeavingEdges(); ++i) {
            if (n1.getLeavingEdge(i).getSuccessor() == n2) {
                alreadyLinked = true;
                break;
            }
        }
        if (!alreadyLinked) {
            CFAEdge se = new SummaryCFAEdge();
            se.setPredecessor((CFANode)s1);
            se.setSuccessor((CFANode)s2);
            
            LazyLogger.log(LazyLogger.DEBUG_3,
                           "LINKING SUMMARIES: ", s1, " AND ", s2);
        }
    }
    
    private void setSummary(CFANode n, SummaryCFANode s) {
        ((InnerCFANode)n).setSummaryNode(s);
        int count = 0;
        if (summarySizeMap.containsKey(s)) {
            count = summarySizeMap.get(s);
        }
        summarySizeMap.put(s, count+1);
    }
        
    private boolean isLoopBack(CFAEdge e) {
        CFANode s = e.getSuccessor();
        boolean yes = s.isLoopStart() && !e.getRawStatement().equals("while");
        LazyLogger.log(LazyLogger.DEBUG_3,
                "CHECKING isLoopBack, e: ", e.getRawStatement(),
                ", s: ", s.getNodeNumber() + ", RESULT: " + yes);
        if (!yes) {
            // also return edges are loopbacks
            yes = e instanceof ReturnEdge;
        }
        return yes;
    }

    // sorts topologically the nodes in the given cfa, without considering
    // "loopback" edges (see isLoopBack method above)
    private List<CFANode> topologicalSort(CFAFunctionDefinitionNode cfa) {
        LinkedList<CFANode> order = new LinkedList<CFANode>();
        Stack<CFANode> toProcess = new Stack<CFANode>();
        Map<CFANode, Integer> visited = new HashMap<CFANode, Integer>();
        
        toProcess.push(cfa);
        while (!toProcess.empty()) {
            CFANode n = toProcess.peek();
            //assert(!visited.containsKey(n) || visited.get(n) != 1);
            if (visited.containsKey(n) && visited.get(n) == 1) {
                toProcess.pop();
                continue;
            }
            boolean finished = true;
            visited.put(n, -1);
            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                CFAEdge e = n.getLeavingEdge(i);
                CFANode s = e.getSuccessor();
                // check whether the edge is not a loop-back - we want to
                // exclude those
                if (!isLoopBack(e) && 
                    (!visited.containsKey(s) || visited.get(s) != 1)) {
                    toProcess.push(s);
                    finished = false;
                }
            }
            if (n.getLeavingSummaryEdge() != null) {
                CFANode s = n.getLeavingSummaryEdge().getSuccessor();
                if ((!visited.containsKey(s) || visited.get(s) != 1)) {
                    toProcess.push(s);
                    finished = false;
                }
            }
            if (finished) {
                toProcess.pop();
                LazyLogger.log(LazyLogger.DEBUG_3,
                               "FINISHED: ", n.getNodeNumber());
                visited.put(n, 1);
                if (n != cfa) {
                    order.addFirst(n);
                }
            }
        }
        
        return order;
    }

    // implementation of Algorithm 3.1 of the draft paper 
    // "SymbolicProgramAnalysis"
    private CFAFunctionDefinitionNode buildSummary(
            CFAFunctionDefinitionNode cfa) {
        List<CFANode> toProcess = topologicalSort(cfa);
        
        SummaryCFANode curSummary =
            new SummaryCFAFunctionDefinitionNode(copyNode(cfa),
                cfa.getLineNumber(), cfa.getFunctionName(),
                ""/* TODO - cfa.getContainingFileName()*/);
        summaryMap.put(cfa, curSummary);
        setSummary(copyNode(cfa), curSummary);               
        
        SummaryCFAFunctionDefinitionNode ret = 
            (SummaryCFAFunctionDefinitionNode)curSummary;
        
        List<CFAEdge> loopbacks = new LinkedList<CFAEdge>();
        List<CFANode> loopbacksDup = new LinkedList<CFANode>();
        
        for (CFANode n : toProcess) {
            assert(!summaryMap.containsKey(n));
            
            LazyLogger.log(LazyLogger.DEBUG_3,
                    "PROCESSING: ", n.getNodeNumber());
            
            if (shouldStartSummary(n)) {
                curSummary = new SummaryNode(copyNode(n));
                setSummary(copyNode(n), curSummary);
                summaryMap.put(n, curSummary);
                int res = shouldDuplicate(n);
                if (res == DUPLICATE_BACKWARD) {
                    assert(n.getNumEnteringEdges() > 0);
                    CFANode dup = null;//duplicateNode(copyNode(n));
                    SummaryCFANode s = null;
                    for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
                        CFAEdge e = n.getEnteringEdge(i);
                        assert(!isLoopBack(e));
                        CFANode pred = e.getPredecessor();
                        if (!summaryMap.containsKey(pred)) {
                            assert(e instanceof BlankEdge);
                        } else {
                            if (dup == null) {
                                dup = duplicateNode(copyNode(n));
                                s = summaryMap.get(pred);
                                setSummary(dup, s);
                                // now link together n and the duplicate
                                BlankEdge be = new BlankEdge("");
                                be.setPredecessor(dup);
                                be.setSuccessor(copyNode(n));
                            } else {
                                SummaryCFANode curs = summaryMap.get(pred);
                                if (((InnerCFANode)dup).getSummaryNode() != 
                                    curs) {
                                    dup = duplicateNode(copyNode(n));
                                    s = curs;
                                    setSummary(dup, s);
                                    // now link together n and the duplicate
                                    BlankEdge be = new BlankEdge("");
                                    be.setPredecessor(dup);
                                    be.setSuccessor(copyNode(n));
                                }
                            }
                            linkSummaries(s, curSummary);
                            copyEdge(e, copyNode(pred), dup);
                        }
                    }
                    // now link together n and the duplicate
//                    BlankEdge e = new BlankEdge("");
//                    e.setPredecessor(dup);
//                    e.setSuccessor(copyNode(n));
                } else if (res == DUPLICATE_FORWARD) {
                    assert(n.getNumEnteringEdges() == 1);                    
                    CFAEdge e = n.getEnteringEdge(0);
                    if (!isLoopBack(e)) {
                        System.err.println("ERROR: " + e.getRawStatement());
                    }
                    assert(isLoopBack(e));
                    CFANode pred = e.getPredecessor();
                    CFANode dup = duplicateNode(copyNode(pred));
                    // in this case, the duplicate will be the first node of
                    // the new summary. So, we re-create the summary and
                    // re-set the information on (the copy of) n
                    curSummary = new SummaryNode(dup);                    
                    setSummary(dup, curSummary);
                    setSummary(copyNode(n), curSummary);
                    summaryMap.put(n, curSummary);
                    // this edge is a loopback, we have to process it later...
                    loopbacksDup.add(n);
//                    // copy the edge from pred to n, but using the duplicate
//                    // as predecessor
//                    copyEdge(e, dup, copyNode(n));
//                    // link the summaries
//                    assert(summaryMap.containsKey(pred));
//                    SummaryCFANode s = summaryMap.get(pred);
//                    linkSummaries(s, curSummary);
//                    // now link together pred and the duplicate
//                    BlankEdge be = new BlankEdge("");
//                    be.setPredecessor(copyNode(pred));
//                    be.setSuccessor(dup);
                } else {
                    for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
                        CFAEdge e = n.getEnteringEdge(i);
                        if (!isLoopBack(e)) {
                            CFANode pred = e.getPredecessor();
                            if (!summaryMap.containsKey(pred)) {
                                assert(e instanceof BlankEdge);
                            } else {
                                SummaryCFANode s = summaryMap.get(pred);
                                linkSummaries(s, curSummary);
                                copyEdge(e, copyNode(pred), copyNode(n));
                            }
                        } else {
                            loopbacks.add(e);
                        }
                    }
                }
            } else {
                SummaryCFANode sum = null;
                for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
                    CFAEdge e = n.getEnteringEdge(i);
                    CFANode pred = e.getPredecessor();
                    assert(!isLoopBack(e));
                    if (!summaryMap.containsKey(pred)) {
                        assert(e instanceof BlankEdge);
                        continue;
                    }
                    if (sum == null) {
                        assert(summaryMap.containsKey(pred));
                        sum = summaryMap.get(pred);
                        setSummary(copyNode(n), sum);
                        summaryMap.put(n, sum);
                    }
                    copyEdge(e, copyNode(pred), copyNode(n));
                }
            }
        }
        LazyLogger.log(LazyLogger.DEBUG_3, "NOW LINKING LOOPBACKS");
        for (CFAEdge e : loopbacks) {
            CFANode p = e.getPredecessor();
            CFANode s = e.getSuccessor();
            
            assert(summaryMap.containsKey(p));
            assert(summaryMap.containsKey(s));
            
            SummaryCFANode sp = summaryMap.get(p);
            SummaryCFANode ss = summaryMap.get(s);
            linkSummaries(sp, ss);
            copyEdge(e, copyNode(p), copyNode(s));
        }
        for (CFANode n : loopbacksDup) {
            CFAEdge e = n.getEnteringEdge(0);
            CFANode pred = e.getPredecessor();
            curSummary = summaryMap.get(n);
            CFANode dup = curSummary.getInnerNode(); //duplicateNode(copyNode(pred));
            // in this case, the duplicate will be the first node of
            // the new summary. So, we re-create the summary and
            // re-set the information on (the copy of) n
//            curSummary = new SummaryNode(dup);                    
//            setSummary(dup, curSummary);
//            setSummary(copyNode(n), curSummary);
//            summaryMap.put(n, curSummary);
            // copy the edge from pred to n, but using the duplicate
            // as predecessor
            copyEdge(e, dup, copyNode(n));
            // link the summaries
            assert(summaryMap.containsKey(pred));
            SummaryCFANode s = summaryMap.get(pred);
            linkSummaries(s, curSummary);
            // now link together pred and the duplicate
            BlankEdge be = new BlankEdge("");
            be.setPredecessor(copyNode(pred));
            be.setSuccessor(dup);
        }
        
        addGlobalDeclarations(copyNode(cfa));
        
        return ret;
    }

    private void addGlobalDeclarations(CFANode cfa) {
        if (globalVars.isEmpty()) {
            return;
        }
        // create a series of GlobalDeclarationEdges, one for each declaration,
        // and add them as successors of the input node
        List<CFANode> decls = new LinkedList<CFANode>();
        InnerNode cur = new InnerNode(0);
        cur.setSummaryNode(((InnerCFANode)cfa).getSummaryNode());
        cur.setFunctionName(cfa.getFunctionName());
        decls.add(cur);
        
        for (IASTDeclaration d : globalVars) {
            assert(d instanceof IASTSimpleDeclaration);
            IASTSimpleDeclaration sd = (IASTSimpleDeclaration)d;
            if (sd.getDeclarators().length == 1 && 
                    sd.getDeclarators()[0] instanceof IASTFunctionDeclarator) {
                continue;
            }
            GlobalDeclarationEdge e = new GlobalDeclarationEdge(
                    d.getRawSignature(),
                    ((IASTSimpleDeclaration)d).getDeclarators(),
                    ((IASTSimpleDeclaration)d).getDeclSpecifier());
            InnerNode n = new InnerNode(0);
            n.setSummaryNode(cur.getSummaryNode());
            n.setFunctionName(cur.getFunctionName());
            e.initialize(cur, n);
            decls.add(n);
            cur = n;
        }
        
        // now update the successors of cfa
        for (int i = 0; i < cfa.getNumLeavingEdges(); ++i) {
            CFAEdge e = cfa.getLeavingEdge(i);
            e.setPredecessor(cur);
        }
        if (cfa.getLeavingSummaryEdge() != null) {
            cfa.getLeavingSummaryEdge().setPredecessor(cur);
        }
        // and add a blank edge connecting the first node in decl with cfa
        BlankEdge be = new BlankEdge("INIT GLOBAL VARS");
        be.initialize(cfa, decls.get(0));
    }

    private CFANode duplicateNode(CFANode orig) {
        CFANode ret = null;
        if (orig instanceof FunctionDefinitionNode) {
            FunctionDefinitionNode f = (FunctionDefinitionNode)orig;
            ret = new InnerFunctionDefinitionNode(
                    f.getLineNumber(), f.getFunctionDefinition());
        } else {
            ret = new InnerNode(orig.getLineNumber());
            if (orig.getEnteringSummaryEdge() != null) {
                CallToReturnEdge ce = orig.getEnteringSummaryEdge();
                CallToReturnEdge nce = new CallToReturnEdge(
                        ce.getRawStatement(), ce.getExpression());
                // copy the summary edge 
                nce.initializeSummaryEdge(ce.getPredecessor(), ret);
            }
        }
        ret.setFunctionName(orig.getFunctionName());
        return ret;
    }

}
