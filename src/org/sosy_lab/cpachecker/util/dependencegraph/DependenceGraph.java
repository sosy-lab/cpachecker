// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.dependencegraph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    return new DependenceGraphBuilder(pCfa, pConfig, pLogger, pShutdownNotifier);
  }

  Table<DGNode, DGNode, DependenceType> getMatrix() {
    return adjacencyMatrix;
  }

  public Collection<DGNode> getAllNodes() {
    return nodes.getAllNodes();
  }

  public ReachedSet getReachable(CFAEdge pStart, TraversalDirection pDirection)
      throws InterruptedException {
    return getReachable(pStart, pDirection, ImmutableSet.of());
  }

  /**
   * Returns all summary edges that must be reached during backward traversal because they are
   * callers (or the caller's caller, etc.) of the function that contains pStart.
   */
  private Set<CFunctionSummaryEdge> getRelevantCallerSummaryEdges(CFAEdge pStart) {

    Deque<CFAEdge> waitlist = new ArrayDeque<>();
    Set<CFAEdge> seen = new HashSet<>();
    Set<CFunctionSummaryEdge> relevantSummaryEdges = new HashSet<>();

    waitlist.add(pStart);
    seen.add(pStart);

    while (!waitlist.isEmpty()) {

      CFAEdge edge = waitlist.remove();

      if (edge instanceof CFunctionCallEdge) {
        relevantSummaryEdges.add(((CFunctionCallEdge) edge).getSummaryEdge());
      }

      if (!(edge instanceof CFunctionReturnEdge)) {
        for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(edge.getPredecessor())) {
          if (seen.add(enteringEdge)) { // if not previously seen -> add to waitlist
            waitlist.add(enteringEdge);
          }
        }
      }
    }

    return relevantSummaryEdges;
  }

  /**
   * Return the reachable dependences of the given {@link CFAEdge} ignoring a set of given edges.
   *
   * @param pStart edge to get reachable dependences for
   * @param pDirection direction of the search for reachability
   * @param pEdgesToIgnore edges to ignore on the search. Edges in this collection are ignored in
   *     the search.
   */
  public ReachedSet getReachable(
      CFAEdge pStart, TraversalDirection pDirection, Collection<CFAEdge> pEdgesToIgnore)
      throws InterruptedException {

    // reachable.get(cfaEdge) == null
    //   => cfaEdge not reachable
    // reachable.get(cfaEdge) == optional && optional.isEmpty()
    //   => edge reachable, all its causes reachable
    // reachable.get(cfaEdge) == optional && optional.isPresent()
    //   => edge reachable, all its causes in optional.get() are reachable
    Map<CFAEdge, Optional<Set<MemoryLocation>>> reachable = new HashMap<>();

    Set<CFunctionSummaryEdge> relevantSummaryEdges = new HashSet<>();
    Multimap<AFunctionDeclaration, CFunctionCallEdge> ignoredCallEdges = HashMultimap.create();

    Collection<DGNode> seen = new HashSet<>();
    Queue<DGNode> waitlist = new ArrayDeque<>();

    for (DGNode dgNode : nodes.getNodesForEdge(pStart)) {
      seen.add(dgNode);
      waitlist.add(dgNode);
    }

    if (pDirection == TraversalDirection.BACKWARD) {
      relevantSummaryEdges.addAll(getRelevantCallerSummaryEdges(pStart));
    }

    while (!waitlist.isEmpty()) {

      shutdownNotifier.shutdownIfNecessary();
      DGNode current = waitlist.remove();

      // FIXME: this is a strong overapproximation: If an unknown pointer is used,
      // we don't know anything, so we use the full program as slice
      if (current.isUnknownPointerNode()) {

        reachable.clear();
        for (CFAEdge edge : nodes.nodesForEdges.keySet()) {
          reachable.put(edge, Optional.empty());
        }
        break;

      } else if (!pEdgesToIgnore.contains(current.getCfaEdge())) {

        CFAEdge edge = current.getCfaEdge();

        // Only consider relevant call edges during backward traversal, but remember irrelevant
        // call edges as they can become relevant later on.
        if (pDirection == TraversalDirection.BACKWARD && edge instanceof CFunctionCallEdge) {
          CFunctionCallEdge callEdge = (CFunctionCallEdge) edge;
          if (!relevantSummaryEdges.contains(callEdge.getSummaryEdge())) {
            ignoredCallEdges.put(edge.getSuccessor().getFunction(), callEdge);
            continue;
          }
        }

        MemoryLocation cause = current.getCause();
        int dgNodeCount = nodes.nodesForEdges.get(edge).size();
        // for edges that have DG-nodes with causes, a set for these causes must be added
        if (dgNodeCount > 1 || (dgNodeCount == 1 && cause != null)) {
          Set<MemoryLocation> causes =
              reachable.computeIfAbsent(edge, key -> Optional.of(new HashSet<>())).orElseThrow();
          if (cause != null) {
            causes.add(cause);
          }
        } else {
          reachable.put(edge, Optional.empty());
        }

        // Additional call edges become relevant during backward traversal, when the corresponding
        // return edge is visited. Previously seen, but ignored, call edges must be checked again.
        if (pDirection == TraversalDirection.BACKWARD && edge instanceof CFunctionReturnEdge) {
          relevantSummaryEdges.add(((CFunctionReturnEdge) edge).getSummaryEdge());
          ignoredCallEdges.removeAll(edge.getPredecessor().getFunction());
          nodes.getNodesForEdge(pStart).forEach(waitlist::add);
        }

        for (DGNode dgNode : getAdjacentNeighbors(current, pDirection)) {
          if (seen.add(dgNode)) {
            waitlist.add(dgNode);
          }
        }
      }
    }

    return ReachedSet.fromMutable(reachable);
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

    private final ImmutableListMultimap<CFAEdge, DGNode> nodesForEdges;
    private final ImmutableSet<DGNode> specialNodes;

    public ImmutableNodeMap(NodeMap pNodeMap) {
      // FIXME avoid iteration in O(n) here, there may be lots of nodes
      ImmutableListMultimap.Builder<CFAEdge, DGNode> mapBuilder = ImmutableListMultimap.builder();
      for (Cell<CFAEdge, Optional<MemoryLocation>, DGNode> c :
          pNodeMap.getNodesForEdges().cellSet()) {
        mapBuilder.put(checkNotNull(c.getRowKey()), checkNotNull(c.getValue()));
      }
      nodesForEdges = mapBuilder.build();
      specialNodes = ImmutableSet.copyOf(pNodeMap.getSpecialNodes());
    }

    public ImmutableList<DGNode> getNodesForEdge(CFAEdge pEdge) {
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

  /** Contains reached CFA edges as well as their relevant causes. */
  public static final class ReachedSet {

    private final ImmutableMap<CFAEdge, Optional<ImmutableSet<MemoryLocation>>> reachable;

    private ReachedSet(ImmutableMap<CFAEdge, Optional<ImmutableSet<MemoryLocation>>> pReachable) {
      reachable = pReachable;
    }

    private static ReachedSet fromMutable(Map<CFAEdge, Optional<Set<MemoryLocation>>> pReachable) {

      ImmutableMap.Builder<CFAEdge, Optional<ImmutableSet<MemoryLocation>>> builder =
          ImmutableMap.builderWithExpectedSize(pReachable.size());

      for (Map.Entry<CFAEdge, Optional<Set<MemoryLocation>>> entry : pReachable.entrySet()) {

        CFAEdge cfaEdge = entry.getKey();
        Optional<Set<MemoryLocation>> optionalCauseSet = entry.getValue();

        if (optionalCauseSet.isPresent()) {
          Set<MemoryLocation> causeSet = optionalCauseSet.orElseThrow();
          builder.put(cfaEdge, Optional.of(ImmutableSet.copyOf(causeSet)));
        } else {
          builder.put(cfaEdge, Optional.empty());
        }
      }

      return new ReachedSet(builder.build());
    }

    private static Optional<Set<MemoryLocation>> combine(
        Optional<Set<MemoryLocation>> pMutableCauseSet,
        Optional<ImmutableSet<MemoryLocation>> pImmutableCauseSet) {

      if (pMutableCauseSet.isPresent() && pImmutableCauseSet.isPresent()) {
        Set<MemoryLocation> mutableCauseSet = pMutableCauseSet.orElseThrow();
        mutableCauseSet.addAll(pImmutableCauseSet.orElseThrow());
        return Optional.of(mutableCauseSet);
      } else {
        return Optional.empty();
      }
    }

    public static ReachedSet empty() {
      return new ReachedSet(ImmutableMap.of());
    }

    public static ReachedSet combine(ReachedSet pSomeReachedSet, ReachedSet pOtherReachedSet) {

      Map<CFAEdge, Optional<Set<MemoryLocation>>> reachable = new HashMap<>();

      for (ReachedSet reachedSet : ImmutableList.of(pSomeReachedSet, pOtherReachedSet)) {
        for (Map.Entry<CFAEdge, Optional<ImmutableSet<MemoryLocation>>> entry :
            reachedSet.reachable.entrySet()) {

          CFAEdge cfaEdge = entry.getKey();
          Optional<ImmutableSet<MemoryLocation>> optionalCauseSet = entry.getValue();

          Optional<Set<MemoryLocation>> optionalCauses;

          if (reachable.containsKey(cfaEdge)) {
            optionalCauses = combine(reachable.get(cfaEdge), optionalCauseSet);
          } else if (optionalCauseSet.isPresent()) {
            Set<MemoryLocation> mutableCauseSet = new HashSet<>(optionalCauseSet.orElseThrow());
            optionalCauses = Optional.of(mutableCauseSet);
          } else {
            optionalCauses = Optional.empty();
          }

          reachable.put(cfaEdge, optionalCauses);
        }
      }

      return fromMutable(reachable);
    }

    public ImmutableSet<CFAEdge> getReachedCfaEdges() {
      return reachable.keySet();
    }

    public boolean contains(CFAEdge pEdge, MemoryLocation pCause) {

      Optional<ImmutableSet<MemoryLocation>> causes = reachable.get(pEdge);

      if (causes == null) {
        return false;
      } else if (causes.isEmpty()) {
        return true;
      } else {
        return causes.orElseThrow().contains(pCause);
      }
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
