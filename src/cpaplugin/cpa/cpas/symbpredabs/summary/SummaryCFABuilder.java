package cpaplugin.cpa.cpas.symbpredabs.summary;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cpaplugin.cfa.CFAMap;
import cpaplugin.cfa.objectmodel.BlankEdge;
import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFAErrorNode;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.AssumeEdge;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.FunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cpa.cpas.symbpredabs.logging.LazyLogger;

/**
 * Manipulates the original CFA(s) of the program, to build the "summary" CFAs,
 * in which each node summarizes a loop-free subpart of the original program
 * @author alb
 */
public class SummaryCFABuilder {
    // maps each original node to its summary location
    private Map<CFANode, SummaryCFANode> summaryMap;
    // the original CFAs (one per function) of the program
    private CFAMap cfas;
    // the summarized CFAs
    private CFAMap summarized;
    // maps each original node to its copy used in the summary CFA
    private Map<CFANode, CFANode> nodeMap;
    
    public SummaryCFABuilder(CFAMap cfas) {
        this.cfas = cfas;
        summaryMap = new HashMap<CFANode, SummaryCFANode>();
        summarized = null;
        nodeMap = new HashMap<CFANode, CFANode>();
    }
    
    public CFAMap buildSummary() {
        if (summarized != null) {
            return summarized;
        }
        summarized = new CFAMap();
        for (CFAFunctionDefinitionNode cfa : cfas.cfaMapIterator()) {
            summarized.addCFA(cfa.getFunctionName(), buildSummary(cfa));
        }
        return summarized;
    }
    
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
            ret = new DeclarationEdge(d.getRawStatement(), d.getDeclarators());
        } else if (tp == CFAEdgeType.FunctionCallEdge) {
            // TODO - handle functions!
            assert(false);
        } else if (tp == CFAEdgeType.MultiDeclarationEdge) {
            assert(false);
        } else if (tp == CFAEdgeType.MultiStatementEdge) {
            assert(false);
        } else if (tp == CFAEdgeType.ReturnEdge) {
            // TODO - handle functions!
            assert(false);
        } else if (tp == CFAEdgeType.StatementEdge) {
            StatementEdge s = (StatementEdge)orig;
            ret = new StatementEdge(s.getRawStatement(), s.getExpression());
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
            nodeMap.put(orig, ret);
            return ret;
        }
    }
    
//    private boolean shouldStartSummary(CFANode n, CFAEdge e) {
//        return shouldStartSummary(n);
//    }
    
    private boolean shouldStartSummary(CFANode n) {
        if (n.isLoopStart() || n instanceof CFAErrorNode ||
                n.getNumLeavingEdges() == 0) {
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
//                        System.out.println("ERROR!: n: " + n.getNodeNumber() +
//                                ", p: " + p.getNodeNumber() + 
//                                ", e: " + e.getRawStatement());
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
            return false;
        }
    }

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
    }
    
//    // implementation of Algorithm 3.1 of the draft paper 
//    // "SymbolicProgramAnalysis"
//    private CFAFunctionDefinitionNode buildSummary_OLD(
//            CFAFunctionDefinitionNode cfa) {
//        Queue<CFANode> outerQueue = new LinkedList<CFANode>();
//
//        SummaryCFAFunctionDefinitionNode curSummary =
//            new SummaryCFAFunctionDefinitionNode(copyNode(cfa),
//                cfa.getLineNumber(), cfa.getFunctionName(),
//                ""/* TODO - cfa.getContainingFileName()*/);
//        summaryMap.put(cfa, curSummary);
//        setSummary(copyNode(cfa), curSummary);
//
//        outerQueue.add(cfa);
//        while (!outerQueue.isEmpty()) {
//            CFANode loc = outerQueue.remove();
//            SummaryCFANode sloc = summaryMap.get(loc);
//            
//            Queue<CFANode> innerQueue = new LinkedList<CFANode>();
//            for (int i = 0; i < loc.getNumLeavingEdges(); ++i) {
//                CFAEdge e = loc.getLeavingEdge(i);
//                CFANode l = e.getSuccessor();
//                if (!summaryMap.containsKey(l)) {
//                    if (shouldStartSummary(l, e)) {
//                        outerQueue.add(l);
//                        SummaryCFANode s = new SummaryNode(copyNode(l));
//                        linkSummaries(sloc, s);
//                        summaryMap.put(l, s);
//                        setSummary(copyNode(l), s);
//                        copyEdge(e, copyNode(loc), copyNode(l));
//                    } else {
//                        innerQueue.add(l);
//                        copyEdge(e, copyNode(loc), copyNode(l));
//                        summaryMap.put(l, sloc);
//                        setSummary(copyNode(l), sloc);
//                    }
//                } else {
//                    SummaryCFANode sl = summaryMap.get(l);
//                    linkSummaries(sloc, sl);
//                    copyEdge(e, copyNode(loc), copyNode(l));
//                }
//            }
//            
//            while (!innerQueue.isEmpty()) {
//                CFANode n = innerQueue.remove();
//                
//                assert(!n.isLoopStart());
//                assert(!(n instanceof CFAErrorNode));
//                
//                for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
//                    CFAEdge e = n.getLeavingEdge(i);
//                    CFANode succ = e.getSuccessor();
//                    if (!summaryMap.containsKey(succ)) {
//                        if (shouldStartSummary(succ, e)) {
//                            outerQueue.add(succ);
//                            SummaryCFANode s = new SummaryNode(copyNode(succ));
//                            linkSummaries(sloc, s);
//                            summaryMap.put(succ, s);
//                            setSummary(copyNode(succ), s);
//                            copyEdge(e, copyNode(n), copyNode(succ));
//                        } else {
//                            innerQueue.add(succ);
//                            copyEdge(e, copyNode(n), copyNode(succ));
//                            summaryMap.put(succ, sloc);
//                            setSummary(copyNode(succ), sloc);
//                        }
//                    } else {
//                        SummaryCFANode sl = summaryMap.get(succ);
//                        if (copyNode(succ).equals(sloc.getInnerNode())) {
//                            linkSummaries(sloc, sl);
//                        }
//                        copyEdge(e, copyNode(n), copyNode(succ));
//                    }
//                }
//            }
//        }
//        
//        return curSummary;
//    }
    
    private boolean isLoopBack(CFAEdge e) {
        CFANode s = e.getSuccessor();
        boolean yes = s.isLoopStart() && !e.getRawStatement().equals("while");
        LazyLogger.log(LazyLogger.DEBUG_4,
                "CHECKING isLoopBack, e: ", e.getRawStatement(),
                ", s: ", s.getNodeNumber() + ", RESULT: " + yes);
        return yes;
    }

    private List<CFANode> topologicalSort(CFAFunctionDefinitionNode cfa) {
        LinkedList<CFANode> order = new LinkedList<CFANode>();
        Stack<CFANode> toProcess = new Stack<CFANode>();
        Map<CFANode, Integer> visited = new HashMap<CFANode, Integer>();
        
        toProcess.push(cfa);
        while (!toProcess.empty()) {
            CFANode n = toProcess.peek();
            assert(!visited.containsKey(n) || visited.get(n) != 1);
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
        
        for (CFANode n : toProcess) {
            assert(!summaryMap.containsKey(n));
            
            LazyLogger.log(LazyLogger.DEBUG_3,
                    "PROCESSING: ", n.getNodeNumber());
            
            if (shouldStartSummary(n)) {
                curSummary = new SummaryNode(copyNode(n));
                setSummary(copyNode(n), curSummary);
                summaryMap.put(n, curSummary);
                for (int i = 0; i < n.getNumEnteringEdges(); ++i) {
                    CFAEdge e = n.getEnteringEdge(i);
                    if (!isLoopBack(e)) {
                        CFANode pred = e.getPredecessor();
                        //assert(summaryMap.containsKey(pred));
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
        
        return ret;
    }
}
