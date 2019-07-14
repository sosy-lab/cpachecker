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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class CFAEdgesGoal {

  private List<CFAEdge> edges;
  private Set<Set<CFAEdge>> negatedEdges;


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

  public void addNegatedEdges(Set<CFAEdge> pEdges) {
    if (negatedEdges == null) {
      negatedEdges = new HashSet<>();
    }
    negatedEdges.add(pEdges);
  }

  public Set<Set<CFAEdge>> getNegatedEdges() {
    if (negatedEdges == null) {
      return Collections.emptySet();
    }
    return negatedEdges;
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

  public boolean containsNegatedEdge(CFAEdge pCfaEdge) {
    for (Collection<CFAEdge> negatedEdges : getNegatedEdges()) {
      for (CFAEdge edge : negatedEdges) {
        if (edge.equals(pCfaEdge)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Edges:\n{");
    for (CFAEdge edge : edges) {
      builder.append(edge.toString());
      builder.append(",");
    }
    builder.append("}\n");

    if (negatedEdges != null) {
      builder.append("Negated Edges:\n");
      for (Set<CFAEdge> nedges : negatedEdges) {
        builder.append("{");
        for (CFAEdge edge : nedges) {
          builder.append(edge.toString());
          builder.append(",");
        }
        builder.append("}\n");
      }
    }
    return builder.toString();
  }

}
