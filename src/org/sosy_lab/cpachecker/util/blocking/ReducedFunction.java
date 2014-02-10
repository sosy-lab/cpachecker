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
package org.sosy_lab.cpachecker.util.blocking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

class ReducedFunction {
  private final Table<ReducedNode, ReducedNode, Set<ReducedEdge>> cfaEdges;
  private final Multiset<ReducedNode> activeNodes;
  private final ReducedNode entryNode;
  private final ReducedNode exitNode;

  public ReducedFunction(ReducedNode pEntryNode, ReducedNode pExitNode) {
    assert (pEntryNode != null);
    assert (pExitNode != null);

    this.cfaEdges = HashBasedTable.create();
    this.activeNodes = HashMultiset.create();

    this.entryNode = pEntryNode;
    this.exitNode = pExitNode;
  }

  public ReducedEdge addEdge(ReducedNode pFrom, ReducedNode pTo) {
    ReducedEdge edge = new ReducedEdge(pTo);
    this.addEdge(pFrom, pTo, edge);
    return edge;
  }

  public void addEdge(ReducedNode pFrom, ReducedNode pTo, ReducedEdge pEdge) {
    Set<ReducedEdge> edges = cfaEdges.get(pFrom, pTo);
    if (edges == null) {
      edges = new HashSet<>();
    }
    edges.add(pEdge);
    cfaEdges.put(pFrom, pTo, edges);

    activeNodes.add(pFrom);
    activeNodes.add(pTo);

    //System.out.println(String.format("add: %d --> %d", pFrom.getWrapped().getLineNumber(), pTo.getWrapped().getLineNumber()));

    assert (activeNodes.count(pFrom) > 0);
    assert (activeNodes.count(pTo) > 0);
  }

  public void removeEdge(ReducedNode pFrom, ReducedNode pTo, ReducedEdge pEdge) {
    assert (cfaEdges.contains(pFrom, pTo));
    assert (activeNodes.count(pFrom) > 0);
    assert (activeNodes.count(pTo) > 0);
    assert (pEdge.getPointsTo() == pTo);

    //System.out.println(String.format("remove: %d --> %d", pFrom.getWrapped().getLineNumber(), pTo.getWrapped().getLineNumber()));

    Set<ReducedEdge> edges = cfaEdges.get(pFrom, pTo);
    if (edges != null) {
      edges.remove(pEdge);
      if (edges.size() == 0) {
        cfaEdges.remove(pFrom, pTo);
      }
    }

    activeNodes.remove(pFrom);
    activeNodes.remove(pTo);
  }

  public void insertFunctionSum(ReducedFunction pToInline) {
    // Copy CFA edges.
    this.cfaEdges.putAll(pToInline.cfaEdges);
    this.activeNodes.addAll(pToInline.activeNodes);
  }

  public ReducedNode getEntryNode() {
    return this.entryNode;
  }

  public ReducedNode getExitNode() {
    return this.exitNode;
  }

  public Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> getInlinedCfa() {
    return cfaEdges.rowMap();
  }

  public List<ReducedEdge> getLeavingEdges(ReducedNode pOfNode) {
    ArrayList<ReducedEdge> result = new ArrayList<>();
    Collection<Set<ReducedEdge>> edges = cfaEdges.row(pOfNode).values();
    for (Set<ReducedEdge> edgesToNode: edges) {
      for (ReducedEdge e: edgesToNode) {
        result.add(e);
      }
    }

    return result;
  }

  public int getNumEnteringEdges(ReducedNode pOfNode) {
    int result = 0;
    Collection<Set<ReducedEdge>> edges = cfaEdges.column(pOfNode).values();
    for (Set<ReducedEdge> edgesToNode: edges) {
      result += edgesToNode.size();
    }
    return result;
  }

  public int getNumLeavingEdges(ReducedNode pOfNode) {
    int result = 0;
    Collection<Set<ReducedEdge>> edges = cfaEdges.row(pOfNode).values();
    for (Set<ReducedEdge> edgesToNode: edges) {
      result += edgesToNode.size();
    }
    return result;
  }

  public int getNumOfActiveNodes() {
    return activeNodes.elementSet().size();
  }

  public Set<ReducedNode> getAllActiveNodes() {
    return this.activeNodes.elementSet();
  }
}
