/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.dependencegraph;

import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Dependence graph that describes flow dependence and control dependence between expressions and
 * assignments of a program.
 *
 * <p>A dependence graph G = (V, E) is a directed graph. His nodes V are CFA edges of the program.
 * Given two nodes i and j, if j is a dependence of i, a directed edge (j, i) from j to i is in E.
 */
public class DependenceGraph implements Serializable {

  private static final long serialVersionUID = -6721168496945584302L;

  public enum TraversalDirection {
    FORWARD,
    BACKWARD,
    BOTH
  }

  public enum DependenceType {
    CONTROL,
    FLOW
  }

  private final ImmutableTable<CFAEdge, Optional<MemoryLocation>, DGNode> nodes;
  private ImmutableTable<DGNode, DGNode, DependenceType> adjacencyMatrix;

  private final transient ShutdownNotifier shutdownNotifier;

  DependenceGraph(
      final Table<CFAEdge, Optional<MemoryLocation>, DGNode> pNodes,
      final Table<DGNode, DGNode, DependenceType> pEdges,
      final ShutdownNotifier pShutdownNotifier) {

    nodes = ImmutableTable.copyOf(pNodes);
    adjacencyMatrix = ImmutableTable.copyOf(pEdges);
    shutdownNotifier = pShutdownNotifier;
  }

  public static DGBuilder builder(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new DGBuilder(pCfa, pConfig, pLogger, pShutdownNotifier);
  }

  public boolean contains(final CFAEdge pNode, final Optional<MemoryLocation> pCause) {
    return nodes.contains(pNode, pCause);
  }

  Table<DGNode, DGNode, DependenceType> getMatrix() {
    return adjacencyMatrix;
  }

  public Collection<DGNode> getNodes() {
    return nodes.values();
  }

  public Collection<CFAEdge> getReachable(CFAEdge pStart, TraversalDirection pDirection)
      throws InterruptedException {
    Collection<CFAEdge> reachable = new HashSet<>();
    Collection<DGNode> visited = new HashSet<>();
    Queue<DGNode> waitlist = new ArrayDeque<>();
    nodes.row(pStart).values().forEach(waitlist::offer);

    while (!waitlist.isEmpty()) {
      if (shutdownNotifier.shouldShutdown()) {
        throw new InterruptedException();
      }
      DGNode current = waitlist.poll();

      if (!visited.contains(current)) {
        visited.add(current);
        reachable.add(current.getCfaEdge());
        Collection<DGNode> adjacent = getAdjacentNeighbors(current, pDirection);
        waitlist.addAll(adjacent);
      }
    }
    return reachable;
  }

  private Collection<DGNode> getAdjacentNeighbors(
      final DGNode pNode, final TraversalDirection pDirection) {
    return getAdjacentNeighbors(
        pNode, pDirection, dgEdge -> true);
  }

  private Collection<DGNode> getAdjacentNeighbors(
      DGNode pNode, TraversalDirection pDirection, Predicate<DependenceType> pIsEdgeOfInterest) {

    return getAdjacentNodes(pNode, pDirection)
        .entrySet()
        .stream()
        .filter(e -> pIsEdgeOfInterest.test(e.getValue()))
        .map(e -> e.getKey())
        .collect(Collectors.toSet());
  }

  private Map<DGNode, DependenceType> getAdjacentNodes(
      DGNode pNode, TraversalDirection pDirection) {
    Map<DGNode, DependenceType> adjacentEdges;
    switch (pDirection) {
      case FORWARD:
        adjacentEdges = adjacencyMatrix.row(pNode);
        break;
      case BACKWARD:
        adjacentEdges = adjacencyMatrix.column(pNode);
        break;
      case BOTH:
        adjacentEdges = adjacencyMatrix.row(pNode);
        adjacentEdges.putAll(adjacencyMatrix.column(pNode));
        break;
      default:
        throw new AssertionError("Unhandled direction " + pDirection);
    }
    return adjacentEdges;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    DependenceGraph that = (DependenceGraph) pO;
    // If these equal, the root nodes have to equal, too.
    return Objects.equals(nodes, that.nodes)
        && Objects.equals(adjacencyMatrix, that.adjacencyMatrix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes, adjacencyMatrix);
  }
}
