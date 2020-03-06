/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cfa;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

public class RandomEdgeRemovalStrategy {

  private Set<CFANode> lastAnswer = new HashSet<>();

  public long countPossibleMutations(final ParseResult parseResult) {
    long possibleMutations = 0;

    for (CFANode node : parseResult.getCFANodes().values()) {
      if (canDeleteNodeSimply(node)) {
        possibleMutations += 1;
      }
    }

    return possibleMutations;
  }

  // can delete node with its only leaving edge and reconnect entering edge instead
  public boolean canDeleteNodeSimply(CFANode pNode) {
    if (pNode instanceof FunctionEntryNode
        || pNode instanceof FunctionExitNode
        || pNode instanceof CFATerminationNode) {
      return false;
    }
    if (pNode.getNumLeavingEdges() != 1) {
      return false;
    }
    if (pNode.getNumEnteringEdges() != 1) {
      return false;
    }

    CFANode successor = pNode.getLeavingEdge(0).getSuccessor();
    if (successor.getNumEnteringEdges() != 1) {
      return false;
    }
    CFANode predecessor = pNode.getEnteringEdge(0).getPredecessor();
    if (predecessor.getNumLeavingEdges() != 1) {
      return false;
    }

    return true;
  }

  public Collection<CFANode> chooseNodesToRemove(ParseResult parseResult) {
    List<CFANode> answer = new ArrayList<>();
    List<CFANode> preds = new ArrayList<>();
    List<CFANode> succs = new ArrayList<>();

    int nodesFound = 0;
    for (CFANode node : parseResult.getCFANodes().values()) {
      if (lastAnswer.contains(node)) {
        continue;
      }
      if (canDeleteNodeSimply(node)) {
        CFANode predecessor = node.getEnteringEdge(0).getPredecessor();
        CFANode successor = node.getLeavingEdge(0).getSuccessor();
        assert predecessor.getNumLeavingEdges() == 1;

        System.out.println("Choosing (p:" + predecessor + ") " + node + " (s:" + successor + ")");
        answer.add(node);
        preds.add(predecessor);
        succs.add(successor);

        if (++nodesFound > 4) {
          break;
        }
      }
    }

    lastAnswer = new HashSet<>(answer);
    return ImmutableList.copyOf(answer);
  }
}
