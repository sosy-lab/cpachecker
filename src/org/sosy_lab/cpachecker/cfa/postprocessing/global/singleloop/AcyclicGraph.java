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
package org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Instances of this class are acyclic graphs.
 */
class AcyclicGraph {

  /**
   * The set of nodes.
   */
  private final Set<CFANode> nodes = new HashSet<>();

  /**
   * The set of edges.
   */
  private final Multimap<CFANode, CFAEdge> edges = LinkedHashMultimap.create();

  /**
   * The set of uncommitted nodes.
   */
  private final Set<CFANode> uncommittedNodes = new HashSet<>();

  /**
   * The set of uncommitted edges.
   */
  private final Multimap<CFANode, CFAEdge> uncommittedEdges = LinkedHashMultimap.create();

  /**
   * A predicate that matches edges contained in the subgraph.
   */
  private final Predicate<CFAEdge> CONTAINS_EDGE = pArg0 -> pArg0 != null && containsEdge(pArg0);

  /**
   * A function producing those edges leaving a node that are contained in
   * this subgraph.
   */
  private final Function<CFANode, Iterable<CFAEdge>> GET_CONTAINED_LEAVING_EDGES =
      new Function<CFANode, Iterable<CFAEdge>>() {

    @Override
    @Nullable
    public Iterable<CFAEdge> apply(@Nullable CFANode pArg0) {
      if (pArg0 == null) {
        return Collections.emptySet();
      }
      return getLeavingEdges(pArg0).filter(CONTAINS_EDGE).transform(new Function<CFAEdge, CFAEdge>() {

        @Override
        @Nullable
        public CFAEdge apply(@Nullable CFAEdge pArg0) {
          if (pArg0 instanceof FunctionCallEdge) {
            CFAEdge summaryEdge = ((FunctionCallEdge) pArg0).getSummaryEdge();
            return containsNode(summaryEdge.getSuccessor()) ? summaryEdge : null;
          }
          return pArg0;
        }

      }).filter(notNull());
    }

  };

  /**
   * Creates a new acyclic graph with the given root node and default growth
   * strategy.
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
    return Iterables.concat(Collections.unmodifiableSet(this.nodes), Collections.unmodifiableSet(this.uncommittedNodes));
  }

  /**
   * Gets the edges of the graph as an unmodifiable set.
   *
   * @return the edges of the graph as an unmodifiable set.
   */
  public Iterable<CFAEdge> getEdges() {
    return Iterables.concat(Collections.unmodifiableCollection(this.edges.values()), Collections.unmodifiableCollection(this.uncommittedEdges.values()));
  }

  /**
   * Checks if the given node is contained in this graph.
   *
   * @param pNode the node to look for.
   * @return @{code true} if the node is contained in the graph,
   * @{code false} otherwise.
   */
  public boolean containsNode(CFANode pNode) {
    return this.nodes.contains(pNode) || this.uncommittedNodes.contains(pNode);
  }

  /**
   * Checks if the given edge is contained in this graph.
   *
   * @param pEdge the edge to look for.
   * @return @{code true} if the edge is contained in the graph,
   * @{code false} otherwise.
   */
  public boolean containsEdge(CFAEdge pEdge) {
    return this.edges.containsValue(pEdge) || this.uncommittedEdges.containsValue(pEdge);
  }

  /**
   * Adds the given edge to the graph but does not commit the change.
   *
   * @param pEdge the edge to be added.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   *
   * @throws InterruptedException if a shutdown has been requested by the given
   * shutdown notifier.
   * @throws IllegalArgumentException if the edge cannot be added according
   * to the employed growth strategy.
   */
  public void addEdge(CFAEdge pEdge, ShutdownNotifier pShutdownNotifier) throws InterruptedException {
    Preconditions.checkArgument(offerEdge(pEdge, pShutdownNotifier));
  }

  /**
   * If the given edge may be added to the graph according to the growth
   * strategy, it is added but not committed.
   *
   * @param pEdge the candidate edge.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   *
   * @return {@code true} if the edge was added, {@code false} otherwise.
   *
   * @throws InterruptedException if a shutdown has been requested by the given
   * shutdown notifier.
   */
  public boolean offerEdge(CFAEdge pEdge, ShutdownNotifier pShutdownNotifier) throws InterruptedException {
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

  /**
   * Commits all changes.
   */
  public void commit() {
    this.nodes.addAll(uncommittedNodes);
    this.edges.putAll(uncommittedEdges);
    abort();
  }

  /**
   * Aborts all changes.
   */
  public void abort() {
    this.uncommittedNodes.clear();
    this.uncommittedEdges.clear();
  }

  @Override
  public String toString() {
    return Iterables.toString(getEdges());
  }

  /**
   * Checks if the given control flow edge would introduce a loop to the
   * graph if it was added.
   *
   * @param pEdge the edge to check.
   * @param pShutdownNotifier the shutdown notifier to be checked.
   *
   * @return {@code true} if adding the edge would introduce a loop to the
   * graph, {@code false} otherwise.
   *
   * @throws InterruptedException if a shutdown has been requested by the given
   * shutdown notifier.
   */
  public boolean introducesLoop(CFAEdge pEdge, ShutdownNotifier pShutdownNotifier) throws InterruptedException {
    return CFAUtils.existsPath(pEdge.getSuccessor(), pEdge.getPredecessor(), GET_CONTAINED_LEAVING_EDGES, pShutdownNotifier);
  }

  /**
   * Gets the edges leaving the given node.
   *
   * @param pNode the node.
   *
   * @return the edges leaving the node.
   */
  private FluentIterable<CFAEdge> getLeavingEdges(CFANode pNode) {
    return from(Iterables.concat(this.edges.get(pNode), this.uncommittedEdges.get(pNode)));
  }

  /**
   * Resets the graph and aborts all changes.
   *
   * @param pNewRootNode the new root node.
   *
   * @return this graph with all nodes and edges removed.
   */
  public AcyclicGraph reset(CFANode pNewRootNode) {
    abort();
    this.edges.clear();
    this.nodes.clear();
    this.nodes.add(pNewRootNode);
    return this;
  }
}