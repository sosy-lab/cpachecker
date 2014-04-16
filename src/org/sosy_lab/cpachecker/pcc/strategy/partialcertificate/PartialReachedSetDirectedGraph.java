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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class PartialReachedSetDirectedGraph {

  // TODO set final if possible
  /* index of node is its position in <code>nodes</code>*/
  private AbstractState[] nodes;
  private int[][] adjacencyMatrix;
  private List<Integer>[] adjacencyList;


  public int getNumNodes(){
    return nodes.length; // TODO more efficient way?
  }
  public AbstractState[] getNodes() {
    return nodes;
  }

  public int[][] getAdjacencyMatrix() {
    return adjacencyMatrix;
  }

  public List<Integer>[] getAdjacencyList() {
    return adjacencyList;
  }

  public AbstractState[] getAdjacentNodesOutsideSet(Set<Integer> pNodeSetIndices) {
    List<AbstractState> listRes = new ArrayList<>();

    try {
      List<Integer> successors;
      for (Integer predecessor : pNodeSetIndices) {
        successors = adjacencyList[predecessor];
        for (int successor : successors) {
          if (!pNodeSetIndices.contains(successor)) {
            listRes.add(nodes[successor]);
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException | NullPointerException e) {
      // TODO Eingabe falsch
    }
    return listRes.toArray(new AbstractState[listRes.size()]);
  }

}
