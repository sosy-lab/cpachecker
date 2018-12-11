/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slab;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

public class EdgeSet implements Serializable {

  private static final long serialVersionUID = 1L;

  private List<CFAEdge> edges;
  private CFAEdge selected;

  public EdgeSet(List<CFAEdge> edges) {
    checkArgument(edges.size() > 0);
    this.edges = edges;
    this.selected = null;
  }

  public EdgeSet(EdgeSet other) {
    checkArgument(other.edges.size() > 0);
    this.edges = new ArrayList<>(other.edges);
  }

  public List<CFAEdge> getEdges() {
    return new ImmutableList.Builder<CFAEdge>().addAll(edges).build();
  }

  public void removeEdge(CFAEdge pEdge) {
    edges.remove(pEdge);
    selected = null;
  }

  public void select(CFAEdge pEdge) {
    selected = pEdge;
  }

  public CFAEdge choose() {
    if (!edges.isEmpty()) {
      if (selected == null) {
        return edges.get(0);
      } else {
        if (edges.contains(selected)) {
          return selected;
        }
      }
    }
    // assert false;
    return null;
  }

  public CFAEdge onlyornull() {
    if (edges.size() == 1) {
      return edges.get(0);
    } else {
      return null;
    }
  }

  public boolean isEmpty() {
    return edges.isEmpty();
  }

  public boolean isSingleton() {
    return edges.size() == 1;
  }
}
