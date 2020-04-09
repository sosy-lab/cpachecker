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
package org.sosy_lab.cpachecker.cpa.multigoal;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class PartialPath {
  ImmutableList<CFAEdge> edges;

  public PartialPath(List<CFAEdge> edges) {
    this.edges = ImmutableList.copyOf(edges);
  }

  @Override
  public int hashCode() {
    return edges.hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (pObj == this) {
      return true;
    }
    if (pObj instanceof PartialPath) {
      return edges.equals(((PartialPath) pObj).edges);
    }
    return false;
  }

  public boolean acceptsEdge(CFAEdge edge, int index) {
    if (index < edges.size() && index >= 0) {
      // use object comparison instead of equals for performance reasons
      if (edge == edges.get(index)) {
        return true;
      }
    }
    return false;
  }

  public int size() {
    return edges.size();
  }

  public boolean contains(CFAEdge edge) {
    return edges.contains(edge);
  }

  public CFAEdge get(int pIndex) {
    return edges.get(pIndex);
  }

  public boolean isCoveredBy(List<CFAEdge> pPath) {
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

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("{");
    for (CFAEdge edge : edges) {
      builder.append(edge.toString());
      builder.append(",");
    }
    builder.deleteCharAt(builder.length() - 1);
    builder.append("}");
    return builder.toString();
  }

  public ImmutableList<CFAEdge> getEdges() {
    return edges;
  }

}
