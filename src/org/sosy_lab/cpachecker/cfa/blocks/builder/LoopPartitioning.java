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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.collect.Iterables;


/**
 * <code>PartitioningHeuristic</code> that creates blocks for each loop-body.
 */
public class LoopPartitioning extends PartitioningHeuristic {

  private Map<CFANode, Set<CFANode>> loopHeaderToLoopBody;

  public LoopPartitioning(LogManager pLogger, CFA pCfa) {
    super(pLogger, pCfa);
    loopHeaderToLoopBody = null;
  }

  private void initLoopMap() {
    loopHeaderToLoopBody = new HashMap<>();
    if (cfa.getLoopStructure().isPresent()) {
      for (String functionName : cfa.getLoopStructure().get().keySet()) {
        for (Loop loop : cfa.getLoopStructure().get().get(functionName)) {
          if (loop.getLoopHeads().size() == 1) {
            //currently only loops with single loop heads supported
            loopHeaderToLoopBody.put(Iterables.getOnlyElement(loop.getLoopHeads()), loop.getLoopNodes());
          }
        }
      }
    }
  }

  @Override
  protected boolean shouldBeCached(CFANode pNode) {
    if (isMainFunction(pNode)) {
      Preconditions.checkArgument(cfa.getMainFunction().getFunctionName().equals(pNode.getFunctionName()));
      //main function
      return true;
    }
    return isLoopHead(pNode) && !hasBlankEdgeFromLoop(pNode) && !selfLoop(pNode);
  }

  private boolean isMainFunction(CFANode pNode) {
    return pNode instanceof FunctionEntryNode && pNode.getNumEnteringEdges() == 0;
  }

  private boolean isLoopHead(CFANode pNode) {
    return cfa.getAllLoopHeads().get().contains(pNode);
  }

  private boolean hasBlankEdgeFromLoop(CFANode pNode) {
    for (int i = 0; i < pNode.getNumEnteringEdges(); i++) {
      CFAEdge edge = pNode.getEnteringEdge(i);
      if (edge instanceof BlankEdge && isLoopHead(edge.getPredecessor())) {
        return true;
      }
    }
    return false;
  }

  private static boolean selfLoop(CFANode pNode) {
    return pNode.getNumLeavingEdges() == 1 && pNode.getLeavingEdge(0).getSuccessor().equals(pNode);
  }

  @Override
  protected Set<CFANode> getBlockForNode(CFANode pNode) {
    Preconditions.checkArgument(shouldBeCached(pNode));

    if (isMainFunction(pNode)) {
      return CFATraversal.dfs().ignoreFunctionCalls().collectNodesReachableFrom(pNode);
    }

    if (loopHeaderToLoopBody == null) {
      initLoopMap();
    }

    if (!loopHeaderToLoopBody.containsKey(pNode)) {
      // loopStructure is missing in CFA or loop with multiple headers
      return null;
    }

    Set<CFANode> loopBody = new HashSet<>(loopHeaderToLoopBody.get(pNode));
    insertLoopStartState(loopBody, pNode);
    insertLoopReturnStates(loopBody);
    return loopBody;
  }

  private void insertLoopStartState(Set<CFANode> pLoopBody, CFANode pLoopHeader) {
    for (int i = 0; i < pLoopHeader.getNumEnteringEdges(); i++) {
      CFAEdge edge = pLoopHeader.getEnteringEdge(i);
      if (edge instanceof BlankEdge && !pLoopBody.contains(edge.getPredecessor())) {
        pLoopBody.add(edge.getPredecessor());
      }
    }
  }

  private void insertLoopReturnStates(Set<CFANode> pLoopBody) {
    List<CFANode> addNodes = new ArrayList<>();
    for (CFANode node : pLoopBody) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (!pLoopBody.contains(edge.getSuccessor()) && !(node.getLeavingEdge(i).getEdgeType() == CFAEdgeType.FunctionCallEdge))  {
          addNodes.add(edge.getSuccessor());
        }
      }
    }
    pLoopBody.addAll(addNodes);
  }
}
