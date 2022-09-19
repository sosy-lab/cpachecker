// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.blocking;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ReducedFunction {
  private final Table<ReducedNode, ReducedNode, Set<ReducedEdge>> cfaEdges;
  private final Multiset<ReducedNode> activeNodes;
  private final ReducedNode entryNode;
  private final ReducedNode exitNode;

  public ReducedFunction(ReducedNode pEntryNode, ReducedNode pExitNode) {
    assert pEntryNode != null;
    assert pExitNode != null;

    cfaEdges = HashBasedTable.create();
    activeNodes = HashMultiset.create();

    entryNode = pEntryNode;
    exitNode = pExitNode;
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

    assert (activeNodes.count(pFrom) > 0);
    assert (activeNodes.count(pTo) > 0);
  }

  public void removeEdge(ReducedNode pFrom, ReducedNode pTo, ReducedEdge pEdge) {
    assert cfaEdges.contains(pFrom, pTo);
    assert (activeNodes.count(pFrom) > 0);
    assert (activeNodes.count(pTo) > 0);
    assert (pEdge.getPointsTo() == pTo);

    Set<ReducedEdge> edges = cfaEdges.get(pFrom, pTo);
    if (edges != null) {
      edges.remove(pEdge);
      if (edges.isEmpty()) {
        cfaEdges.remove(pFrom, pTo);
      }
    }

    activeNodes.remove(pFrom);
    activeNodes.remove(pTo);
  }

  public void insertFunctionSum(ReducedFunction pToInline) {
    // Copy CFA edges.
    cfaEdges.putAll(pToInline.cfaEdges);
    activeNodes.addAll(pToInline.activeNodes);
  }

  public ReducedNode getEntryNode() {
    return entryNode;
  }

  public ReducedNode getExitNode() {
    return exitNode;
  }

  public Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> getInlinedCfa() {
    return cfaEdges.rowMap();
  }

  public List<ReducedEdge> getLeavingEdges(ReducedNode pOfNode) {
    List<ReducedEdge> result = new ArrayList<>();
    Collection<Set<ReducedEdge>> edges = cfaEdges.row(pOfNode).values();
    for (Set<ReducedEdge> edgesToNode : edges) {
      result.addAll(edgesToNode);
    }

    return result;
  }

  public int getNumEnteringEdges(ReducedNode pOfNode) {
    int result = 0;
    Collection<Set<ReducedEdge>> edges = cfaEdges.column(pOfNode).values();
    for (Set<ReducedEdge> edgesToNode : edges) {
      result += edgesToNode.size();
    }
    return result;
  }

  public int getNumLeavingEdges(ReducedNode pOfNode) {
    int result = 0;
    Collection<Set<ReducedEdge>> edges = cfaEdges.row(pOfNode).values();
    for (Set<ReducedEdge> edgesToNode : edges) {
      result += edgesToNode.size();
    }
    return result;
  }

  public int getNumOfActiveNodes() {
    return activeNodes.elementSet().size();
  }

  public Set<ReducedNode> getAllActiveNodes() {
    return activeNodes.elementSet();
  }
}
