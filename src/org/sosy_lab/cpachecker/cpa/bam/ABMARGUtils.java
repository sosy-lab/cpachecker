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
package org.sosy_lab.cpachecker.cpa.abm;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class ABMARGUtils {
  private ABMARGUtils() {}

  public static Multimap<Block, ReachedSet> gatherReachedSets(ABMCPA cpa, ReachedSet finalReachedSet) {
    Multimap<Block, ReachedSet> result = HashMultimap.create();
    gatherReachedSets(cpa, cpa.getBlockPartitioning().getMainBlock(), finalReachedSet, result);
    return result;
  }

  private static void gatherReachedSets(ABMCPA cpa, Block block, ReachedSet reachedSet, Multimap<Block, ReachedSet> blockToReachedSet) {
    if (blockToReachedSet.containsEntry(block, reachedSet)) {
      return; //avoid looping in recursive block calls
    }

    blockToReachedSet.put(block, reachedSet);

    ARGState firstElement = (ARGState)reachedSet.getFirstState();

    Deque<ARGState> worklist = new LinkedList<>();
    Set<ARGState> processed = new HashSet<>();

    worklist.add(firstElement);

    while (worklist.size() != 0) {
      ARGState currentElement = worklist.removeLast();

      assert reachedSet.contains(currentElement);

      if (processed.contains(currentElement)) {
        continue;
      }
      processed.add(currentElement);

      for (ARGState child : currentElement.getChildren()) {
        CFAEdge edge = getEdgeToChild(currentElement, child);
        if (edge == null) {
          //this is a summary edge
          Pair<Block, ReachedSet> pair = cpa.getTransferRelation().getCachedReachedSet(currentElement, reachedSet.getPrecision(currentElement));
          gatherReachedSets(cpa, pair.getFirst(), pair.getSecond(), blockToReachedSet);
        }
        if (!worklist.contains(child)) {
          if (reachedSet.contains(child)) {
            worklist.add(child);
          }
        }
      }
    }
  }

  public static CFAEdge getEdgeToChild(ARGState parent, ARGState child) {
    CFANode currentLoc = extractLocation(parent);
    CFANode childNode = extractLocation(child);

    return getEdgeTo(currentLoc, childNode);
  }

  public static CFAEdge getEdgeTo(CFANode node1, CFANode node2) {
    for (CFAEdge edge : leavingEdges(node1)) {
      if (edge.getSuccessor() == node2) {
        return edge;
      }
    }
    return null;
  }

  public static ARGState copyARG(ARGState pRoot) {
    HashMap<ARGState, ARGState> stateToCopyElem = new HashMap<>();
    HashSet<ARGState> visited = new HashSet<>();
    Stack<ARGState> toVisit = new Stack<>();
    ARGState current, copyState, copyStateInner;

    visited.add(pRoot);
    toVisit.add(pRoot);

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (stateToCopyElem.get(current) == null) {
        copyState = copyNode(current);
        stateToCopyElem.put(current, copyState);
      } else {
        copyState = stateToCopyElem.get(current);
      }

      for (ARGState c : current.getChildren()) {
        if (stateToCopyElem.get(c) == null) {
          copyStateInner = copyNode(c);
          stateToCopyElem.put(c, copyStateInner);
        } else {
          copyStateInner = stateToCopyElem.get(c);
        }
        copyStateInner.addParent(copyState);
        if (!visited.contains(c)) {
          visited.add(c);
          toVisit.add(c);
        }
      }

      if (current.isCovered()) {
        if (stateToCopyElem.get(current.getCoveringState()) == null) {
          copyStateInner = copyNode(current.getCoveringState());
          stateToCopyElem.put(current.getCoveringState(), copyStateInner);
        } else {
          copyStateInner = stateToCopyElem.get(current.getCoveringState());
        }
        if (!visited.contains(current.getCoveringState())) {
          visited.add(current.getCoveringState());
          toVisit.add(current.getCoveringState());
        }
        copyState.setCovered(copyStateInner);
      }
    }
    return stateToCopyElem.get(pRoot);
  }

  private static ARGState copyNode(ARGState toCopy) {
    ARGState copyState;
    if (toCopy instanceof ABMARGBlockStartState) {
      copyState = new ABMARGBlockStartState(toCopy.getWrappedState(), null);
      ((ABMARGBlockStartState) copyState).setAnalyzedBlock(((ABMARGBlockStartState) toCopy).getAnalyzedBlock());
    } else {
      copyState = new ARGState(toCopy.getWrappedState(), null);
    }
    return copyState;
  }
}
