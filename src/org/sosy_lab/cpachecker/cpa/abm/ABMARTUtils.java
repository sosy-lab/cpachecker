/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import static org.sosy_lab.cpachecker.util.AbstractElements.extractLocation;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGElement;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

class ABMARTUtils {
  private ABMARTUtils() {}

  public static Multimap<Block, ReachedSet> gatherReachedSets(ABMCPA cpa, ReachedSet finalReachedSet) {
    Multimap<Block, ReachedSet> result = HashMultimap.create();
    gatherReachedSets(cpa, cpa.getBlockPartitioning().getMainBlock(), finalReachedSet, result);
    return result;
  }

  private static void gatherReachedSets(ABMCPA cpa, Block block, ReachedSet reachedSet, Multimap<Block, ReachedSet> blockToReachedSet) {
    if(blockToReachedSet.containsEntry(block, reachedSet)) {
      return; //avoid looping in recursive block calls
    }

    blockToReachedSet.put(block, reachedSet);

    ARGElement firstElement = (ARGElement)reachedSet.getFirstElement();

    Deque<ARGElement> worklist = new LinkedList<ARGElement>();
    Set<ARGElement> processed = new HashSet<ARGElement>();

    worklist.add(firstElement);

    while(worklist.size() != 0){
      ARGElement currentElement = worklist.removeLast();

      assert reachedSet.contains(currentElement);

      if(processed.contains(currentElement)){
        continue;
      }
      processed.add(currentElement);

      for (ARGElement child : currentElement.getChildren()) {
        CFAEdge edge = getEdgeToChild(currentElement, child);
        if(edge == null) {
          //this is a summary edge
          Pair<Block,ReachedSet> pair = cpa.getTransferRelation().getCachedReachedSet(currentElement, reachedSet.getPrecision(currentElement));
          gatherReachedSets(cpa, pair.getFirst(), pair.getSecond(), blockToReachedSet);
        }
        if(!worklist.contains(child)){
          if(reachedSet.contains(child)) {
            worklist.add(child);
          }
        }
      }
    }
  }

  public static CFAEdge getEdgeToChild(ARGElement parent, ARGElement child) {
    CFANode currentLoc = extractLocation(parent);
    CFANode childNode = extractLocation(child);

    return getEdgeTo(currentLoc, childNode);
  }

  public static CFAEdge getEdgeTo(CFANode node1, CFANode node2) {
    for(int i = 0; i < node1.getNumLeavingEdges(); i++) {
      CFAEdge edge = node1.getLeavingEdge(i);
      if(edge.getSuccessor() == node2) {
        return edge;
      }
    }
    return null;
  }
}
