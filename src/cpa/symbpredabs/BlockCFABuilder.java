package cpa.symbpredabs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import logging.LazyLogger;

import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;


import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;



/**
 * Manipulates the original CFA(s) of the program, to build the "block" CFAs,
 * in which each edge is a block of edges without branching
 * @author alb
 */
public class BlockCFABuilder {
    // entry point of the program
    private CFAFunctionDefinitionNode mainFunction;
    // list of global variables. At the moment, we simply initialize them
    // at the beginning of mainFunction
    private List<IASTDeclaration> globalVars;
    
    private Set<CFANode> marked = new HashSet<CFANode>();
    
    public BlockCFABuilder(CFAFunctionDefinitionNode mainFunction,
                           List<IASTDeclaration> globalVars) {
        this.mainFunction = mainFunction;
        this.globalVars = globalVars;
    }
    
    public CFAFunctionDefinitionNode buildBlocks() {
        mainFunction = buildBlocks(mainFunction);
        addGlobalDeclarations(mainFunction);
        return mainFunction;
    }
    
    public CFAFunctionDefinitionNode buildBlocks(
            CFAFunctionDefinitionNode cfa) {
        List<CFANode> toProcess = topologicalSort(cfa);
        Set<CFANode> cache = new HashSet<CFANode>();
        marked.add(cfa);
        LazyLogger.log(LazyLogger.DEBUG_3, "MARKING: ", cfa.getNodeNumber());
        for (CFANode n : toProcess) {
            if (cache.contains(n) || !marked.contains(n)) {
                continue;
            }
            cache.add(n);
            CFANode start = n;
            LazyLogger.log(LazyLogger.DEBUG_3, "PROCESSING: ",
                    n.getNodeNumber());
            Vector<CFAEdge> orig = new Vector<CFAEdge>();
            for (int i = 0; i < start.getNumLeavingEdges(); ++i) {
                CFAEdge e = start.getLeavingEdge(i);
                orig.add(e);
            }
            for (CFAEdge e : orig) {
                LazyLogger.log(LazyLogger.DEBUG_3, "  EDGE: ",
                        e.getRawStatement());
                BlockEdge curBlock = new BlockEdge();
                CFANode s = e.getSuccessor();
                CFAEdge ee = e;
                while (canAddToBlock(ee, s)) {
                    curBlock.addEdge(ee);
                    n.removeLeavingEdge(ee);
                    s.removeEnteringEdge(ee);
                    s = ee.getSuccessor();                    
                    n = s;
                    ee = s.getLeavingEdge(0);
                }
                if (start != n) {
                    start.removeLeavingEdge(e);
                    curBlock.initialize(start, n);
                }
            }
            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                CFAEdge e = n.getLeavingEdge(i);
                marked.add(e.getSuccessor());
            }
        }
        
        return cfa;
    }

    private boolean canAddToBlock(CFAEdge e, CFANode n) {
        if (e instanceof AssumeEdge) return false;
        if (e instanceof ReturnEdge) return false;
        return (n.getNumLeavingEdges() == 1 &&
                n.getNumEnteringEdges() <= 1 &&
                n.getLeavingSummaryEdge() == null &&
                n.getEnteringSummaryEdge() == null &&
                !(n instanceof CFAErrorNode) &&
                !(n instanceof CFAFunctionDefinitionNode));
    }

    private void addGlobalDeclarations(CFANode cfa) {
        if (globalVars.isEmpty()) {
            return;
        }
        // create a series of GlobalDeclarationEdges, one for each declaration,
        // and add them as successors of the input node
        BlockEdge curBlock = new BlockEdge();
        CFANode cur = new CFANode(0);
        cur.setFunctionName(cfa.getFunctionName());
        
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
            curBlock.addEdge(e);
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
        curBlock.initialize(cfa, cur);
    }

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
                //if (n != cfa) {
                    order.addFirst(n);
                //}
            }
        }
        
        return order;
    }

    private boolean isLoopBack(CFAEdge e) {
        CFANode s = e.getSuccessor();
        boolean yes = s.isLoopStart() && !e.getRawStatement().equals("while");
        LazyLogger.log(LazyLogger.DEBUG_3,
                "CHECKING isLoopBack, e: ", e.getRawStatement(),
                ", s: ", s.getNodeNumber() + ", RESULT: " + yes);
        if (!yes) {
            // self-loops are obviously loopbacks! :-) This happens because
            // in several Blast benchmarks the error function is defined as:
            // void errorFn() {
            //     ERROR: goto ERROR;
            // }
            if (e.getSuccessor() == e.getPredecessor()) {
                yes = true;
            } else {
                // also return edges are loopbacks
                yes = e instanceof ReturnEdge;
            }
        }
        return yes;
    }
    
    

}
