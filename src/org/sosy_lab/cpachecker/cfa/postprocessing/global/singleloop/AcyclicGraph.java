// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Instances of this class are acyclic graphs. */
class AcyclicGraph {

  /** The set of nodes. */
  private final Set<CFANode> nodes = new HashSet<>();

  /** The set of edges. */
  private final Multimap<CFANode, CFAEdge> edges = LinkedHashMultimap.create();

  /** The set of uncommitted nodes. */
  private final Set<CFANode> uncommittedNodes = new HashSet<>();

  /** The set of uncommitted edges. */
  private final Multimap<CFANode, CFAEdge> uncommittedEdges = LinkedHashMultimap.create();

  /** A predicate that matches edges contained in the subgraph. */
  private boolean checkNullContainsEdge(CFAEdge pArg0) {
    return pArg0 != null && containsEdge(pArg0);
  }

  /** A function producing those edges leaving a node that are contained in this subgraph. */
  private Iterable<CFAEdge> getContainedLeavingEdges(@Nullable CFANode pArg0) {
    if (pArg0 == null) {
      return ImmutableSet.of();
    }
    return getLeavingEdges(pArg0)
        .filter(edge -> checkNullContainsEdge(edge))
        .transform(
            edge -> {
              if (edge instanceof FunctionCallEdge) {
                CFAEdge summaryEdge = ((FunctionCallEdge) edge).getSummaryEdge();
                return containsNode(summaryEdge.getSuccessor()) ? summaryEdge : null;
              }
              return edge;
            })
        .filter(notNull());
  }
  /**
   * Creates a new acyclic graph with the given root node and default growth strategy.
   *
   * @param pRoot the root node.
   */
  public AcyclicGraph(CFANode pRoot) {
    this.nodes.add(pRoot);
  }

  /**
   * Gets the nodes of the graph as an unmodifiable set.
   *
   * @return the nodes of the graph as an unmodifiable set.
   */
  public Iterable<CFANode> getNodes() {
    return Iterables.concat(
        Collections.unmodifiableSet(this.nodes),
        Collections.unmodifiableSet(this.uncommittedNodes));
  }

  /**
   * Gets the edges of the graph as an unmodifiable set.
   *
   * @return the edges of the graph as an unmodifiable set.
   */
  public Iterable<CFAEdge> getEdges() {
    return Iterables.concat(
        Collections.unmodifiableCollection(this.edges.values()),
        Collections.unmodifiableCollection(this.uncommittedEdges.values()));
  }

  /**
   * Checks if the given node is contained in this graph.
   *
   * @param pNode the node to look for.
   * @return {@code true} if the node is contained in the graph, {@code false} otherwise.
   */
  public boolean containsNode(CFANode pNode) {
    return this.nodes.contains(pNode) || this.uncommittedNodes.contains(pNode);
  }

  /**
   * Checks if the given edge is contained in this graph.
   *
   * @param pEdge the edge to look for.
   * @return {@code true} if the edge is contained in the graph, {@code false} otherwise.
   */
  public boolean containsEdge(CFAEdge pEdge) {
    return this.edges.containsValue(pEdge) || this.uncommittedEdges.containsValue(pEdge);
  }

  /**
   * Adds the given edge to the graph but does not commit the change.
   *
   * @param pEdge the edge to be added.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   * @throws InterruptedException if a shutdown has been requested by the given shutdown notifier.
   * @throws IllegalArgumentException if the edge cannot be added according to the employed growth
   *     strategy.
   */
  public void addEdge(CFAEdge pEdge, ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {
    Preconditions.checkArgument(offerEdge(pEdge, pShutdownNotifier));
  }

  /**
   * If the given edge may be added to the graph according to the growth strategy, it is added but
   * not committed.
   *
   * @param pEdge the candidate edge.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   * @return {@code true} if the edge was added, {@code false} otherwise.
   * @throws InterruptedException if a shutdown has been requested by the given shutdown notifier.
   */
  public boolean offerEdge(CFAEdge pEdge, ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {
    if (containsEdge(pEdge)) {
      return true;
    }
    if (!containsNode(pEdge.getPredecessor()) || introducesLoop(pEdge, pShutdownNotifier)) {
      return false;
    }
    this.uncommittedEdges.put(pEdge.getPredecessor(), pEdge);
    this.uncommittedNodes.add(pEdge.getSuccessor());
    return true;
  }

  /** Commits all changes. */
  public void commit() {
    this.nodes.addAll(uncommittedNodes);
    this.edges.putAll(uncommittedEdges);
    abort();
  }

  /** Aborts all changes. */
  public void abort() {
    this.uncommittedNodes.clear();
    this.uncommittedEdges.clear();
  }

  @Override
  public String toString() {
    return Iterables.toString(getEdges());
  }

  /**
   * Checks if the given control flow edge would introduce a loop to the graph if it was added.
   *
   * @param pEdge the edge to check.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   * @return {@code true} if adding the edge would introduce a loop to the graph, {@code false}
   *     otherwise.
   * @throws InterruptedException if a shutdown has been requested by the given shutdown notifier.
   */
  public boolean introducesLoop(CFAEdge pEdge, ShutdownNotifier pShutdownNotifier)
      throws InterruptedException {
    return CFAUtils.existsPath(
        pEdge.getSuccessor(),
        pEdge.getPredecessor(),
        this::getContainedLeavingEdges,
        pShutdownNotifier);
  }

  /**
   * Gets the edges leaving the given node.
   *
   * @param pNode the node.
   * @return the edges leaving the node.
   */
  private FluentIterable<CFAEdge> getLeavingEdges(CFANode pNode) {
    return from(Iterables.concat(this.edges.get(pNode), this.uncommittedEdges.get(pNode)));
  }

  /**
   * Resets the graph and aborts all changes.
   *
   * @param pNewRootNode the new root node.
   * @return this graph with all nodes and edges removed.
   */
  @CanIgnoreReturnValue
  public AcyclicGraph reset(CFANode pNewRootNode) {
    abort();
    this.edges.clear();
    this.nodes.clear();
    this.nodes.add(pNewRootNode);
    return this;
  }
}
