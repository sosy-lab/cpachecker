/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class CFATopologicalSort {

  private final Set<CFANode> visited   = new HashSet<CFANode>();
  private int                topSortId = 0;

  public void topologicalSort(CFANode node) {
    if (!visited.add(node)) {
      // already handled, do nothing
      return;
    }

    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFANode successor = node.getLeavingEdge(i).getSuccessor();

      topologicalSort(successor);
    }

    node.setTopologicalSortId(topSortId++);
  }
}