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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

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

  private final ImmutableMap<CFAEdge, DGNode> nodes;
  private final ImmutableSet<DGEdge> edges;

  DependenceGraph(final Map<CFAEdge, DGNode> pNodes, final Set<DGEdge> pEdges) {
    edges = ImmutableSet.copyOf(pEdges);
    nodes = ImmutableMap.copyOf(pNodes);
  }

  public static DGBuilder builder(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new DGBuilder(pCfa, pConfig, pLogger, pShutdownNotifier);
  }

  public boolean contains(final CFAEdge pNode) {
    return nodes.containsKey(pNode);
  }

  Collection<DGEdge> getEdges() {
    return edges;
  }

  public Collection<DGNode> getNodes() {
    return nodes.values();
  }

  public Collection<CFAEdge> getReachable(CFAEdge pStart, TraversalDirection pDirection) {
    Collection<CFAEdge> reachable = new HashSet<>();
    Queue<DGNode> waitlist = new ArrayDeque<>();

    waitlist.offer(nodes.get(pStart));

    while (!waitlist.isEmpty()) {
      DGNode current = waitlist.poll();

      if (!reachable.contains(current.getCfaEdge())) {
        reachable.add(current.getCfaEdge());
        Collection<DGNode> adjacent = getAdjacentNodes(current, pDirection);
        waitlist.addAll(adjacent);
      }
    }
    return reachable;
  }

  private Collection<DGNode> getAdjacentNodes(DGNode pNode, TraversalDirection pDirection) {
    Function<? super DGEdge, Stream<? extends DGNode>> getNextNode;
    switch (pDirection) {
      case FORWARD:
        getNextNode = (g) -> Stream.of(g.getEnd());
        break;
      case BACKWARD:
        getNextNode = (g) -> Stream.of(g.getStart());
        break;
      case BOTH:
        getNextNode = (g) -> Stream.of(g.getEnd(), g.getStart());
        break;
      default:
        throw new AssertionError("Unhandled direction " + pDirection);
    }

    return getAdjacentEdges(pNode, pDirection)
        .stream()
        .flatMap(getNextNode)
        .collect(Collectors.toSet());
  }

  private Collection<DGEdge> getAdjacentEdges(DGNode pNode, TraversalDirection pDirection) {
    Collection<DGEdge> adjacentEdges;
    switch (pDirection) {
      case FORWARD:
        adjacentEdges = pNode.getOutgoingEdges();
        break;
      case BACKWARD:
        adjacentEdges = pNode.getIncomingEdges();
        break;
      case BOTH:
        adjacentEdges = pNode.getOutgoingEdges();
        adjacentEdges.addAll(pNode.getIncomingEdges());
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
    return Objects.equals(nodes, that.nodes) && Objects.equals(edges, that.edges);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes, edges);
  }
}
