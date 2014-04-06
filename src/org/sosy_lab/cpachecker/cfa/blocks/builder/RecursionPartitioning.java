/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * <code>PartitioningHeuristic</code> that creates a block for only those function-bodies,that are called recursively.
 */
public class RecursionPartitioning extends PartitioningHeuristic {

  private static final CFATraversal TRAVERSE_CFA_INSIDE_FUNCTION = CFATraversal.dfs().ignoreFunctionCalls();
  private final Set<FunctionEntryNode> recursiveFunctions;

  /** Do not change signature! Constructor will be created with Reflections. */
  public RecursionPartitioning(LogManager pLogger, CFA pCfa) {
    super(pLogger, pCfa);

    // build call graph
    final Multimap<String, String> callgraph = getCallgraph();

    // get recursive functions
    recursiveFunctions = getRecursionPoints(callgraph);
  }

  // get a simple structure with information about callees and called functions
  private Multimap<String, String> getCallgraph() {
    final Multimap<String,String> callgraph = HashMultimap.create();
    for (CFANode node : cfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
        if (edge.getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          callgraph.put(edge.getPredecessor().getFunctionName(), edge.getSuccessor().getFunctionName());
          assert edge.getSuccessor() instanceof FunctionEntryNode;
        }
      }
    }
    return callgraph;
  }

  // search for "backward-edges", the targets of backward-edges are recursion-points
  private Set<FunctionEntryNode> getRecursionPoints(final Multimap<String, String> callgraph) {
    final Set<FunctionEntryNode> recursionPoints = new HashSet<>();
    final Deque<String> waitlist = new ArrayDeque<>();
    final Set<String> finished = new HashSet<>();
    waitlist.add(cfa.getMainFunction().getFunctionName());
    while (!waitlist.isEmpty()) {
      String function = waitlist.pop();
      if (!finished.add(function)) { continue; }
      for (String calledFunction : callgraph.get(function)) {
        if (finished.contains(calledFunction)) {
          // forwards-, backwards- or sidewards-edge found
          if (isReachableFrom(calledFunction, function, callgraph)) {
            // recursion found
            recursionPoints.add(cfa.getFunctionHead(calledFunction));
          }
        } else {
          waitlist.add(calledFunction);
        }
      }
    }
    return recursionPoints;
  }

  // BFS search, iff there is a path from source to target
  private boolean isReachableFrom(final String source, final String target, final Multimap<String,String> callgraph) {
    final Deque<String> waitlist = new ArrayDeque<>();
    final Set<String> finished = new HashSet<>();
    waitlist.add(source);
    while (!waitlist.isEmpty()) {
      String function = waitlist.pop();
      if (target.equals(function)) { return true;}
      if (finished.add(function)) {
        waitlist.addAll(callgraph.get(function));
      }
    }
    return false;
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    return isMainFunction(pNode) || recursiveFunctions.contains(pNode);
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pNode) {
    Preconditions.checkArgument(shouldBeCached(pNode));
    Set<CFANode> blockNodes = TRAVERSE_CFA_INSIDE_FUNCTION.collectNodesReachableFrom(pNode);
    return blockNodes;
  }

  private boolean isMainFunction(CFANode pNode) {
    return pNode instanceof FunctionEntryNode && pNode.getNumEnteringEdges() == 0;
  }
}
