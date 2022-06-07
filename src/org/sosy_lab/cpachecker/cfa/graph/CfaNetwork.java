// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import com.google.common.graph.Network;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.PrepareNextIterator;
import org.sosy_lab.cpachecker.util.UnmodifiableSetView;

/**
 * Represents a {@link CFA} as a {@link Network}.
 *
 * <p>All connections between CFA nodes and/or edges are defined by a {@code CfaNetwork} and may
 * differ from the connections represented by its components (e.g., {@link CFAEdge#getSuccessor()},
 * {@link CFAUtils#allEnteringEdges(CFANode)}, {@link FunctionCallEdge#getSummaryEdge()}, {@link
 * FunctionEntryNode#getExitNode()}, etc). It's important to use methods provided by {@link
 * CfaNetwork}, if more than a single CFA node and/or edge is involved. For example, one should use
 * {@link CfaNetwork#outEdges(CFANode)} instead of {@link CFAUtils#allLeavingEdges(CFANode)} and
 * {@link #getFunctionSummaryEdge(FunctionCallEdge)} instead of {@link
 * FunctionCallEdge#getSummaryEdge()}.
 *
 * <p>For performance reasons, not all {@link CfaNetwork} implementations check whether CFA nodes
 * and edges given as method arguments actually belong to the CFA represented by a {@code
 * CfaNetwork}.
 *
 * <p>All returned sets are unmodifiable views, so attempts to modify such a set will throw an
 * exception, but modifications to the CFA represented by a {@code CfaNetwork} will be reflected in
 * the set. Don't try to modify the CFA represented by a {@code CfaNetwork} while iterating though
 * such a view as correctness of the iteration cannot be guaranteed anymore.
 */
public interface CfaNetwork extends Network<CFANode, CFAEdge> {

  /**
   * Returns a {@link CfaNetwork} view that represents the specified {@link CFA} as a {@link
   * Network}.
   *
   * <p>IMPORTANT: The specified CFA must not contain any parallel edges (i.e., edges that connect
   * the same nodes in the same order) and never add them in the future (if the CFA is mutable).
   * Additionally, the set returned by {@link CFA#getAllNodes()} must not contain any duplicates and
   * never add them in the future (if the CFA is mutable). Be aware that these requirements are not
   * enforced, so violating them may lead to unexpected results.
   *
   * @param pCfa the CFA to create a {@link CfaNetwork} view for
   * @return a {@link CfaNetwork} view that represents the specified {@link CFA} as a {@link
   *     Network}
   * @throws NullPointerException if {@code pCfa == null}
   */
  public static CfaNetwork wrap(CFA pCfa) {
    return new WrappingCfaNetwork(pCfa);
  }

  public static CfaNetwork filterEdges(CfaNetwork pNetwork, Predicate<CFAEdge> pFilter) {
    return new EdgeFilteringCfaNetwork(pNetwork, pFilter);
  }

  public static CfaNetwork of(CFA pCfa, Predicate<CFAEdge> pFilter) {
    return filterEdges(wrap(pCfa), pFilter);
  }

  public static CfaNetwork transformEdges(
      CfaNetwork pNetwork, Function<CFAEdge, CFAEdge> pTransformer) {
    return new EdgeTransformingCfaNetwork(pNetwork, pTransformer);
  }

  public static CfaNetwork of(CFA pCfa, Set<String> pFunctions) {
    return new FunctionFilteringCfaNetwork(pCfa, pFunctions);
  }

  public static CfaNetwork of(FunctionEntryNode pFunctionEntryNode) {
    return new SingleFunctionCfaNetwork(pFunctionEntryNode);
  }

  // in-edges / predecessors

  @Override
  default int inDegree(CFANode pNode) {
    return Iterables.size(inEdges(pNode));
  }

  /**
   * Returns the predecessor of the specified CFA edge.
   *
   * @param pEdge the CFA edge to get the predecessor for
   * @return the predecessor of the specified CFA edge
   * @throws NullPointerException if {@code pEdge == null}
   */
  default CFANode predecessor(CFAEdge pEdge) {
    return incidentNodes(pEdge).source();
  }

  @Override
  default Set<CFANode> predecessors(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.transform(inEdges(pNode).iterator(), CfaNetwork.this::predecessor);
      }

      @Override
      public int size() {
        return inDegree(pNode);
      }
    };
  }

  // out-edges / successors

  @Override
  default int outDegree(CFANode pNode) {
    return Iterables.size(outEdges(pNode));
  }

  /**
   * Returns the successor of the specified CFA edge.
   *
   * @param pEdge the CFA edge to get the successor for
   * @return the successor of the specified CFA edge
   * @throws NullPointerException if {@code pEdge == null}
   */
  default CFANode successor(CFAEdge pEdge) {
    return incidentNodes(pEdge).target();
  }

  @Override
  default Set<CFANode> successors(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return Iterators.transform(outEdges(pNode).iterator(), CfaNetwork.this::successor);
      }

      @Override
      public int size() {
        return outDegree(pNode);
      }
    };
  }

  // incident / adjacent

  @Override
  default int degree(CFANode pNode) {

    checkNotNull(pNode);

    return inDegree(pNode) + outDegree(pNode);
  }

  @Override
  default Set<CFAEdge> incidentEdges(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PrepareNextIterator<>() {

          private final Iterator<CFAEdge> inEdges = inEdges(pNode).iterator();
          private final Iterator<CFAEdge> outEdges = outEdges(pNode).iterator();

          @Override
          protected @Nullable CFAEdge prepareNext() {

            if (inEdges.hasNext()) {
              return inEdges.next();
            }

            while (outEdges.hasNext()) {

              CFAEdge edge = outEdges.next();

              // don't iterate over self-loop edges twice
              if (!predecessor(edge).equals(successor(edge))) {
                return edge;
              }
            }

            return null;
          }
        };
      }
    };
  }

  @Override
  default Set<CFANode> adjacentNodes(CFANode pNode) {

    checkNotNull(pNode);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFANode> iterator() {
        return new PrepareNextIterator<>() {

          private final Iterator<CFAEdge> inEdges = inEdges(pNode).iterator();
          private final Iterator<CFAEdge> outEdges = outEdges(pNode).iterator();

          @Override
          protected @Nullable CFANode prepareNext() {

            // predecessor iteration
            if (inEdges.hasNext()) {
              return predecessor(inEdges.next());
            }

            // successor iteration
            while (outEdges.hasNext()) {

              CFAEdge edge = outEdges.next();
              CFANode successor = successor(edge);

              // ignore nodes that were already iterated during predecessor iteration
              if (!successor.equals(pNode) && !successors(successor).contains(pNode)) {
                return successor;
              }
            }

            return null;
          }
        };
      }
    };
  }

  @Override
  default Set<CFAEdge> adjacentEdges(CFAEdge pEdge) {

    checkNotNull(pEdge);

    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {

        Iterator<CFAEdge> edges =
            Iterators.concat(
                incidentEdges(predecessor(pEdge)).iterator(),
                incidentEdges(successor(pEdge)).iterator());

        return Iterators.filter(edges, edge -> !edge.equals(pEdge));
      }

      @Override
      public int size() {
        // an edge is not considered adjacent to itself, so it has to be subtracted
        return (degree(predecessor(pEdge)) - 1) + (degree(successor(pEdge)) - 1);
      }
    };
  }

  // edge-connecting

  @Override
  default @Nullable CFAEdge edgeConnectingOrNull(CFANode pPredecessor, CFANode pSuccessor) {

    checkNotNull(pPredecessor);
    checkNotNull(pSuccessor);

    for (CFAEdge edge : outEdges(pPredecessor)) {
      if (successor(edge).equals(pSuccessor)) {
        return edge;
      }
    }

    return null;
  }

  @Override
  default @Nullable CFAEdge edgeConnectingOrNull(EndpointPair<CFANode> pEndpoints) {

    checkArgument(pEndpoints.isOrdered(), "endpoints must be ordered");

    return edgeConnectingOrNull(pEndpoints.source(), pEndpoints.target());
  }

  @Override
  default Optional<CFAEdge> edgeConnecting(CFANode pPredecessor, CFANode pSuccessor) {
    return Optional.ofNullable(edgeConnectingOrNull(pPredecessor, pSuccessor));
  }

  @Override
  default Optional<CFAEdge> edgeConnecting(EndpointPair<CFANode> pEndpoints) {
    return Optional.ofNullable(edgeConnectingOrNull(pEndpoints));
  }

  @Override
  default Set<CFAEdge> edgesConnecting(CFANode pPredecessor, CFANode pSuccessor) {

    checkNotNull(pPredecessor);
    checkNotNull(pSuccessor);

    return new UnmodifiableSetView<>() {

      private ImmutableSet<CFAEdge> createImmutableSet() {

        @Nullable CFAEdge edge = edgeConnectingOrNull(pPredecessor, pSuccessor);

        return edge == null ? ImmutableSet.of() : ImmutableSet.of(edge);
      }

      @Override
      public int size() {
        return createImmutableSet().size();
      }

      @Override
      public boolean contains(Object pObject) {
        return createImmutableSet().contains(pObject);
      }

      @Override
      public Iterator<CFAEdge> iterator() {
        return createImmutableSet().iterator();
      }
    };
  }

  @Override
  default Set<CFAEdge> edgesConnecting(EndpointPair<CFANode> pEndpoints) {

    checkArgument(pEndpoints.isOrdered(), "endpoints must be ordered");

    return edgesConnecting(pEndpoints.source(), pEndpoints.target());
  }

  @Override
  default boolean hasEdgeConnecting(CFANode pPredecessor, CFANode pSuccessor) {
    return edgeConnectingOrNull(pPredecessor, pSuccessor) != null;
  }

  @Override
  default boolean hasEdgeConnecting(EndpointPair<CFANode> pEndpoints) {

    if (!pEndpoints.isOrdered()) {
      return false; // see documentation of Network#hasEdgeConnecting(EndpointPair)
    }

    return hasEdgeConnecting(pEndpoints.source(), pEndpoints.target());
  }

  // entire network

  @Override
  default boolean isDirected() {
    return true;
  }

  @Override
  default boolean allowsSelfLoops() {
    return true;
  }

  @Override
  default boolean allowsParallelEdges() {
    return false;
  }

  @Override
  default ElementOrder<CFANode> nodeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  default Set<CFAEdge> edges() {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return new PrepareNextIterator<>() {

          private final Iterator<CFANode> nodeIterator = nodes().iterator();

          private Iterator<CFAEdge> outEdges = Collections.emptyIterator();

          @Override
          protected @Nullable CFAEdge prepareNext() {

            while (!outEdges.hasNext()) {
              if (nodeIterator.hasNext()) {
                CFANode node = nodeIterator.next();
                outEdges = outEdges(node).iterator();
              } else {
                return null;
              }
            }

            return outEdges.next();
          }
        };
      }
    };
  }

  @Override
  default ElementOrder<CFAEdge> edgeOrder() {
    return ElementOrder.unordered();
  }

  @Override
  default Graph<CFANode> asGraph() {
    return new Graph<>() {

      @Override
      public int inDegree(CFANode pNode) {
        return CfaNetwork.this.inDegree(pNode);
      }

      @Override
      public Set<CFANode> predecessors(CFANode pNode) {
        return CfaNetwork.this.predecessors(pNode);
      }

      @Override
      public int outDegree(CFANode pNode) {
        return CfaNetwork.this.outDegree(pNode);
      }

      @Override
      public Set<CFANode> successors(CFANode pNode) {
        return CfaNetwork.this.successors(pNode);
      }

      @Override
      public int degree(CFANode pNode) {
        return CfaNetwork.this.degree(pNode);
      }

      @Override
      public Set<EndpointPair<CFANode>> incidentEdges(CFANode pNode) {

        checkNotNull(pNode);

        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<EndpointPair<CFANode>> iterator() {
            return Iterators.transform(
                CfaNetwork.this.incidentEdges(pNode).iterator(), CfaNetwork.this::incidentNodes);
          }

          @Override
          public int size() {
            return CfaNetwork.this.degree(pNode);
          }
        };
      }

      @Override
      public ElementOrder<CFANode> incidentEdgeOrder() {
        return ElementOrder.unordered();
      }

      @Override
      public Set<CFANode> adjacentNodes(CFANode pNode) {
        return CfaNetwork.this.adjacentNodes(pNode);
      }

      @Override
      public boolean hasEdgeConnecting(CFANode pPredecessor, CFANode pSuccessor) {
        return CfaNetwork.this.hasEdgeConnecting(pPredecessor, pSuccessor);
      }

      @Override
      public boolean hasEdgeConnecting(EndpointPair<CFANode> pEndpoints) {
        return CfaNetwork.this.hasEdgeConnecting(pEndpoints);
      }

      @Override
      public boolean isDirected() {
        return CfaNetwork.this.isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return CfaNetwork.this.allowsSelfLoops();
      }

      @Override
      public Set<CFANode> nodes() {
        return CfaNetwork.this.nodes();
      }

      @Override
      public ElementOrder<CFANode> nodeOrder() {
        return CfaNetwork.this.nodeOrder();
      }

      @Override
      public Set<EndpointPair<CFANode>> edges() {
        return new UnmodifiableSetView<>() {

          @Override
          public Iterator<EndpointPair<CFANode>> iterator() {
            return Iterators.transform(
                CfaNetwork.this.edges().iterator(), CfaNetwork.this::incidentNodes);
          }

          @Override
          public int size() {
            return CfaNetwork.this.edges().size();
          }
        };
      }
    };
  }

  // CFA specific

  default Optional<FunctionExitNode> getFunctionExitNode(FunctionEntryNode pFunctionEntryNode) {

    Set<CFANode> waitlisted = new HashSet<>(ImmutableList.of(pFunctionEntryNode));
    Deque<CFANode> waitlist = new ArrayDeque<>(waitlisted);

    while (!waitlist.isEmpty()) {

      CFANode node = waitlist.remove();

      if (node instanceof FunctionExitNode) {
        return Optional.of((FunctionExitNode) node);
      }

      for (CFAEdge outEdge : outEdges(node)) {
        if (!(outEdge instanceof FunctionCallEdge)) {
          CFANode successor = incidentNodes(outEdge).target();
          if (waitlisted.add(successor)) {
            waitlist.add(successor);
          }
        }
      }
    }

    return Optional.empty();
  }

  default FunctionSummaryEdge getFunctionSummaryEdge(FunctionCallEdge pFunctionCallEdge) {

    CFANode functionCallEdgePredecessor = incidentNodes(pFunctionCallEdge).source();

    for (CFAEdge outEdge : outEdges(functionCallEdgePredecessor)) {
      if (outEdge instanceof FunctionSummaryEdge) {
        return (FunctionSummaryEdge) outEdge;
      }
    }

    throw new IllegalStateException(
        "Missing FunctionSummaryEdge for FunctionCallEdge: " + pFunctionCallEdge);
  }

  default FunctionSummaryEdge getFunctionSummaryEdge(FunctionReturnEdge pFunctionReturnEdge) {

    CFANode functionReturnEdgeSuccessor = incidentNodes(pFunctionReturnEdge).target();

    for (CFAEdge inEdge : inEdges(functionReturnEdgeSuccessor)) {
      if (inEdge instanceof FunctionSummaryEdge) {
        return (FunctionSummaryEdge) inEdge;
      }
    }

    throw new IllegalStateException(
        "Missing FunctionSummaryEdge for FunctionReturnEdge: " + pFunctionReturnEdge);
  }

  default FunctionEntryNode getFunctionEntryNode(FunctionSummaryEdge pFunctionSummaryEdge) {

    CFANode functionSummaryEdgePredecessor = incidentNodes(pFunctionSummaryEdge).source();

    for (CFANode successor : successors(functionSummaryEdgePredecessor)) {
      if (successor instanceof FunctionEntryNode) {
        return (FunctionEntryNode) successor;
      }
    }

    throw new IllegalStateException(
        "Missing FunctionEntryNode for FunctionSummaryEdge: " + pFunctionSummaryEdge);
  }
}
