/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.multigoal;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAEdgesGoal {

  private List<CFAEdge> edges;

  public CFAEdgesGoal(List<CFAEdge> pEdges) {
    edges = pEdges;
  }

  // returns true, if a change of state happened due to processing edge
  public boolean acceptsEdge(CFAEdge edge, int index) {
    if (index < edges.size()) {
      // if (edge.equals(edges.get(index))) {
      // use object comparison instead of equals for performance reasons
      if (edge == edges.get(index)) {
        return true;
      }
    }
    return false;
  }

  public List<CFAEdge> getEdges() {
    return edges;
  }

  public void replaceEdges(List<CFAEdge> pNewEdges) {
    edges = pNewEdges;
  }

  public boolean coveredByPath(List<CFAEdge> pPath) {
    int index = 0;
    for (CFAEdge edge : pPath) {
      if (index >= edges.size()) {
        break;
      }
      if (edge != null) {
        if (edge.equals(edges.get(0))) {
          index++;
        }
      }
    }
    return index >= edges.size();
  }

  public void addEdge(CFAEdge pEdge) {
    edges.add(pEdge);
  }

}
