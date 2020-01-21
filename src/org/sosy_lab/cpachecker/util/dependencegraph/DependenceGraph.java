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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

/**
 * Dependence graph that describes flow dependence and control dependence between expressions and
 * assignments of a program.
 *
 * <p>A dependence graph G = (V, E) is a directed graph. His nodes V are CFA edges of the program.
 * Given two nodes i and j, if j is a dependence of i, a directed edge (j, i) from j to i is in E.
 */
public final class DependenceGraph implements Serializable {

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

  private final ImmutableNodeMap nodes;
  private ImmutableTable<DGNode, DGNode, DependenceType> adjacencyMatrix;

  private final transient ShutdownNotifier shutdownNotifier;

  DependenceGraph(
      final NodeMap pNodes,
      final Table<DGNode, DGNode, DependenceType> pEdges,
      final ShutdownNotifier pShutdownNotifier) {

    nodes = new ImmutableNodeMap(pNodes);
    adjacencyMatrix = ImmutableTable.copyOf(pEdges);
    shutdownNotifier = pShutdownNotifier;
  }

  public static DependenceGraphBuilder builder(
      final MutableCFA pCfa,
      final Optional<VariableClassification> pVarClassification,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new DependenceGraphBuilder(
        pCfa, pVarClassification, pConfig, pLogger, pShutdownNotifier);
  }

  Table<DGNode, DGNode, DependenceType> getMatrix() {
    return adjacencyMatrix;
  }

  public Collection<DGNode> getAllNodes() {
    return nodes.getAllNodes();
  }

  public Collection<CFAEdge> getReachable(CFAEdge pStart, TraversalDirection pDirection)
      throws InterruptedException {
    return getReachable(pStart, pDirection, ImmutableSet.of());
  }

  /**
   * Return the reachable dependences of the given {@link CFAEdge} ignoring a set of given edges.
   *
   * @param pStart edge to get reachable dependences for
   * @param pDirection direction of the search for reachability
   * @param pEdgesToIgnore edges to ignore on the search. Edges in this collection are ignored in
   *     the search.
   * @return the set of reachable CFA edges from the given edge, traversing through the graph in the
   *     given direction
   */
  public Collection<CFAEdge> getReachable(
      CFAEdge pStart, TraversalDirection pDirection, Collection<CFAEdge> pEdgesToIgnore)
      throws InterruptedException {
    Collection<CFAEdge> reachable = new HashSet<>();
    Collection<DGNode> visited = new HashSet<>();
    Queue<DGNode> waitlist = new ArrayDeque<>();
    nodes.getNodesForEdge(pStart).forEach(waitlist::offer);

    while (!waitlist.isEmpty()) {
      if (shutdownNotifier.shouldShutdown()) {
        throw new InterruptedException();
      }
      DGNode current = waitlist.poll();

      if (!visited.contains(current)) {
        visited.add(current);
        // FIXME: this is a strong overapproximation: If an unknown pointer is used,
        // we don't know anything, so we use the full program as slice
        if (current.isUnknownPointerNode()) {
          reachable.addAll(nodes.nodesForEdges.keys());
        } else if (!pEdgesToIgnore.contains(current.getCfaEdge())) {
          reachable.add(current.getCfaEdge());
          Collection<DGNode> adjacent = getAdjacentNeighbors(current, pDirection);
          waitlist.addAll(adjacent);
        }
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
    // If these equal, the root nodesForEdges have to equal, too.
    return Objects.equals(nodes, that.nodes)
        && Objects.equals(adjacencyMatrix, that.adjacencyMatrix);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes, adjacencyMatrix);
  }

  private static final class ImmutableNodeMap implements Serializable {

    private static final long serialVersionUID = 4311993821719514171L;

    private ImmutableMultimap<CFAEdge, DGNode> nodesForEdges;
    private ImmutableSet<DGNode> specialNodes;

    public ImmutableNodeMap(NodeMap pNodeMap) {
      // FIXME avoid iteration in O(n) here, there may be lots of nodes
      ImmutableMultimap.Builder<CFAEdge, DGNode> mapBuilder = ImmutableMultimap.builder();
      for (Cell<CFAEdge, Optional<MemoryLocation>, DGNode> c :
          pNodeMap.getNodesForEdges().cellSet()) {
        mapBuilder.put(checkNotNull(c.getRowKey()), checkNotNull(c.getValue()));
      }
      nodesForEdges = mapBuilder.build();
      specialNodes = ImmutableSet.copyOf(pNodeMap.getSpecialNodes());
    }

    public Collection<DGNode> getNodesForEdge(CFAEdge pEdge) {
      return nodesForEdges.get(pEdge);
    }

    public Collection<DGNode> getAllNodes() {
      // FIXME: It should be able to represent this as a basic union in O(1) (or is it?)
      return ImmutableSet.<DGNode>builder()
          .addAll(nodesForEdges.values())
          .addAll(specialNodes)
          .build();
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      ImmutableNodeMap that = (ImmutableNodeMap) pO;
      return Objects.equals(nodesForEdges, that.nodesForEdges)
          && Objects.equals(specialNodes, that.specialNodes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodesForEdges, specialNodes);
    }

    @Override
    public String toString() {
      return "ImmutableNodeMap{\n\t"
          + "Nodes per CFA edge="
          + nodesForEdges
          + ",\n\tspecial nodes="
          + specialNodes
          + "\n}";
    }
  }

  static final class NodeMap {
    private Table<CFAEdge, Optional<MemoryLocation>, DGNode> nodesForEdges =
        HashBasedTable.create();
    private Set<DGNode> specialNodes = new HashSet<>();

    /** Returns the mutable Multimap of CFA edges and their corresponding dependence graph nodes. */
    Table<CFAEdge, Optional<MemoryLocation>, DGNode> getNodesForEdges() {
      return nodesForEdges;
    }

    /**
     * Returns the mutable set special dependence graph nodes that are not specific to any CFA edge.
     */
    Set<DGNode> getSpecialNodes() {
      return specialNodes;
    }

    int size() {
      return nodesForEdges.size() + specialNodes.size();
    }

    public boolean containsANodeForEdge(CFAEdge pEdge) {
      return nodesForEdges.containsRow(pEdge);
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      NodeMap nodeMap = (NodeMap) pO;
      return Objects.equals(nodesForEdges, nodeMap.nodesForEdges)
          && Objects.equals(specialNodes, nodeMap.specialNodes);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodesForEdges, specialNodes);
    }

    @Override
    public String toString() {
      return "NodeMap{\n\t"
          + "Nodes per CFA edge="
          + nodesForEdges
          + ",\n\tspecial nodes="
          + specialNodes
          + "\n}";
    }
  }
}
