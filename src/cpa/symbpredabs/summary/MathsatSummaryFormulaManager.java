/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabs.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;

import cmdline.CPAMain;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import symbpredabstraction.interfaces.SymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormula;
import symbpredabstraction.mathsat.MathsatSymbolicFormulaManager;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import common.Pair;
import cpa.symbpredabs.BlockEdge;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * A SummaryFormulaManager for MathSAT formulas.
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class MathsatSummaryFormulaManager extends MathsatSymbolicFormulaManager
        implements SummaryFormulaManager {

    // Computes a topological order of the nodes in the subgraph corresponding
    // to the given Summary location
    private List<InnerCFANode> topologicalSort(SummaryCFANode summary) {
        LinkedList<InnerCFANode> order = new LinkedList<InnerCFANode>();
        Stack<CFANode> toProcess = new Stack<CFANode>();
        Map<CFANode, Integer> visited = new HashMap<CFANode, Integer>();

        toProcess.push(summary.getInnerNode());
        while (!toProcess.empty()) {
            CFANode n = toProcess.peek();
            assert(!visited.containsKey(n) || visited.get(n) != 1);
            boolean finished = true;
            visited.put(n, -1);
            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                CFAEdge e = n.getLeavingEdge(i);
                CFANode s = e.getSuccessor();
                InnerCFANode succ = (InnerCFANode)s;
                // Check whether the edge really belongs to the inner graph,
                // or whether it connects different summary nodes
                // (or is a self-loop in this summary node)
                if (succ.getSummaryNode().equals(summary) &&
                        !succ.equals(summary.getInnerNode()) &&
                        (!visited.containsKey(s) || visited.get(s) != 1)) {
                    toProcess.push(s);
                    finished = false;
                }
            }
            if (finished) {
                toProcess.pop();
                visited.put(n, 1);
                order.addFirst((InnerCFANode)n);
            }
        }

        return order;
    }

    /**
     * Construct the symbolic formulas corresponding to all the possible paths
     * that lead from the given summary location to another summary
     * location. The return value is a mapping from the leaves in the subgraph
     * corresponding to the summary, to the symbolic formula associated to
     * each leaf.
     */
    public Map<CFANode, Pair<SymbolicFormula, SSAMap>> buildPathFormulas(
            SummaryCFANode summary) throws UnrecognizedCFAEdgeException {
        // here we assume that all variables start with index 1 in the ssa
        // the ssa will be updated with the latest index of the variables

        // how we build the formula: we do a BFS traversal of the inner graph,
        // building one formula for each path from the root to a leaf. Then we
        // take the disjunction of the paths
        List<InnerCFANode> toProcess = topologicalSort(summary);
        // maps each node to the formula corresponding to all the paths from
        // the root to the node
        Map<InnerCFANode, MathsatSymbolicFormula> nodeToFormula =
            new HashMap<InnerCFANode, MathsatSymbolicFormula>();
        Map<InnerCFANode, SSAMap> nodeToSSA =
            new HashMap<InnerCFANode, SSAMap>();
        Collection<InnerCFANode> leaves = new Vector<InnerCFANode>();

        CPAMain.logManager.log(Level.ALL, "DEBUG_4", "TOPOLOGICAL SORT:", toProcess);

        nodeToFormula.put((InnerCFANode)summary.getInnerNode(),
                          (MathsatSymbolicFormula)makeTrue());
        nodeToSSA.put((InnerCFANode)summary.getInnerNode(), new SSAMap());

        for (InnerCFANode in : toProcess) {
            CFANode n = (CFANode)in;

            assert(nodeToFormula.containsKey(in));

            final SymbolicFormula t = nodeToFormula.get(in);
            final SSAMap ssa = nodeToSSA.get(in);
            //updateMaxIndex(ssa);

            boolean isLeaf = true;
            for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
                CFAEdge e = n.getLeavingEdge(i);
                InnerCFANode succ = (InnerCFANode)e.getSuccessor();
                // Check whether the edge really belongs to the inner graph,
                // or whether it connects different summary nodes
                // (or is a self-loop in this summary node)
                if (succ.getSummaryNode().equals(summary) &&
                        !succ.equals(summary.getInnerNode())) {
                    isLeaf = false;
                    
                    PathFormula p = null;
                    if (e instanceof BlockEdge) {
                      BlockEdge block = (BlockEdge)e;
                      SymbolicFormula f = t;
                      SSAMap fSSA = ssa;
                      for (CFAEdge edge : block.getEdges()) {
                        p = makeAnd(f, edge, fSSA, false);
                        f = p.getFirst();
                        fSSA = p.getSecond();
                      }
                      assert(p != null);
                    } else {
                      p = makeAnd(t, e, ssa, false);
                    }
                    
                    SymbolicFormula t1 = p.getFirst();
                    SSAMap ssa1 = p.getSecond();
                    if (nodeToFormula.containsKey(succ)) {
                        MathsatSymbolicFormula old =
                            nodeToFormula.get(succ);
                        SSAMap oldssa = nodeToSSA.get(succ);
                        Pair<Pair<SymbolicFormula, SymbolicFormula>,
                             SSAMap> pm = mergeSSAMaps(oldssa, ssa1, false);
                        old = (MathsatSymbolicFormula)makeAnd(
                                old, pm.getFirst().getFirst());
                        t1 = makeAnd(t1, pm.getFirst().getSecond());
                        t1 = makeOr(old, t1);
                        ssa1 = pm.getSecond();
                    }
                    nodeToFormula.put(succ, (MathsatSymbolicFormula)t1);

                    CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                                   " FORMULA FOR LOCATION ",
                                   ((CFANode)succ).getNodeNumber(), ": ",
                                   mathsat.api.msat_term_id(
                                           ((MathsatSymbolicFormula)
                                                   t1).getTerm()), " ", t1);

                    nodeToSSA.put(succ, ssa1);
                }
            }
            if (isLeaf) {
                // if this node is a leaf, remember it
              CPAMain.logManager.log(Level.ALL, "DEBUG_4",
                               "LEAF LOCATION ", n.getNodeNumber());
                leaves.add(in);
            }
        }

        Map<CFANode, Pair<SymbolicFormula, SSAMap>> ret =
            new TreeMap<CFANode, Pair<SymbolicFormula, SSAMap>>();
        for (InnerCFANode n : leaves) {
            assert(nodeToFormula.containsKey(n));
            assert(nodeToSSA.containsKey(n));

            SymbolicFormula f = nodeToFormula.get(n);
            SSAMap s = nodeToSSA.get(n);
            ret.put((CFANode)n, new Pair<SymbolicFormula, SSAMap>(f, s));

            CPAMain.logManager.log(Level.ALL, "DEBUG_3",
                    "FORMULA FOR LEAF: ", ((CFANode)n).getNodeNumber(),
                    " IS: ", f);
        }

        return ret;
    }
}
