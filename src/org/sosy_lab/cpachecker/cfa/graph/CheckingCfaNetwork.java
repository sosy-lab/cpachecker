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

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * A {@link CfaNetwork} that forwards all calls to another {@link CfaNetwork} and performs
 * additional checks on parameters and return values.
 *
 * <p>The following checks are performed:
 *
 * <ul>
 *   <li>Checks whether all CFA nodes given as a method arguments actually belong to the CFA
 *       represented by a {@code CfaNetwork}.
 *   <li>Checks whether all CFA edges given as a method arguments actually belong to the CFA
 *       represented by a {@code CfaNetwork}.
 *   <li>Checks whether a returned set does indeed contain no duplicates. Using {@link
 *       UnmodifiableSetView} incorrectly may lead to duplicates in sets.
 * </ul>
 */
class CheckingCfaNetwork implements CfaNetwork {

  private final CfaNetwork delegate;

  private CheckingCfaNetwork(CfaNetwork pDelegate) {
    delegate = checkNotNull(pDelegate);
  }

  /**
   * Returns the specified {@link CfaNetwork} that is also wrapped in a {@link CheckingCfaNetwork}
   * if Java assertions are enabled.
   *
   * @param pCfaNetwork the {@link CfaNetwork} to wrap if Java assertions are enabled
   * @return If Java assertions are enabled, the specified {@link CfaNetwork} wrapped in a {@link
   *     CheckingCfaNetwork} is returned. Otherwise, if Java assertions are disabled, just the
   *     specified {@link CfaNetwork} is returned.
   * @throws NullPointerException if {@code pCfaNetwork == null}
   */
  static CfaNetwork wrapIfAssertionsEnabled(CfaNetwork pCfaNetwork) {
    CfaNetwork cfaNetwork = checkNotNull(pCfaNetwork);
    // Even though this is bad practice in general, the assert statement is used for its side-effect
    // (wrapping the specified `CfaNetwork` if evaluated).
    // The checks defined in this class can be rather expensive, so we only want to run them if Java
    // assertions are enabled.
    assert (cfaNetwork = new CheckingCfaNetwork(cfaNetwork)) != null;

    return cfaNetwork;
  }

  private static <E> Set<E> checkNoDuplicates(Set<E> pSet) {
    ImmutableSet<List<E>> duplicates = UnmodifiableSetView.duplicates(pSet);
    checkArgument(duplicates.isEmpty(), "Set contains duplicates: %s", duplicates);

    return pSet;
  }

  private <T extends CFANode> T checkContainsNode(T pNode) {
    checkArgument(
        delegate.nodes().contains(pNode),
        "`CfaNetwork` doesn't contain the specified CFA node: %s",
        pNode);

    return pNode;
  }

  private <T extends CFAEdge> T checkContainsEdge(T pEdge) {
    checkArgument(
        delegate.edges().contains(pEdge),
        "`CfaNetwork` doesn't contain the specified CFA edge: %s",
        pEdge);

    return pEdge;
  }

  // network-level accessors

  @Override
  public Set<CFANode> nodes() {
    return checkNoDuplicates(delegate.nodes());
  }

  @Override
  public Set<CFAEdge> edges() {
    return checkNoDuplicates(delegate.edges());
  }

  @Override
  public Graph<CFANode> asGraph() {
    return new Graph<>() {

      // graph-level accessors

      @Override
      public Set<CFANode> nodes() {
        return checkNoDuplicates(delegate.asGraph().nodes());
      }

      @Override
      public Set<EndpointPair<CFANode>> edges() {
        return checkNoDuplicates(delegate.asGraph().edges());
      }

      // graph properties

      @Override
      public boolean isDirected() {
        return delegate.asGraph().isDirected();
      }

      @Override
      public boolean allowsSelfLoops() {
        return delegate.asGraph().allowsSelfLoops();
      }

      @Override
      public ElementOrder<CFANode> nodeOrder() {
        return delegate.asGraph().nodeOrder();
      }

      @Override
      public ElementOrder<CFANode> incidentEdgeOrder() {
        return delegate.asGraph().incidentEdgeOrder();
      }

      // element-level accessors

      @Override
      public Set<CFANode> adjacentNodes(CFANode pNode) {
        return checkNoDuplicates(delegate.asGraph().adjacentNodes(checkContainsNode(pNode)));
      }

      @Override
      public Set<CFANode> predecessors(CFANode pNode) {
        return checkNoDuplicates(delegate.asGraph().predecessors(checkContainsNode(pNode)));
      }

      @Override
      public Set<CFANode> successors(CFANode pNode) {
        return checkNoDuplicates(delegate.asGraph().successors(checkContainsNode(pNode)));
      }

      @Override
      public Set<EndpointPair<CFANode>> incidentEdges(CFANode pNode) {
        return checkNoDuplicates(delegate.asGraph().incidentEdges(checkContainsNode(pNode)));
      }

      @Override
      public int degree(CFANode pNode) {
        return delegate.asGraph().degree(checkContainsNode(pNode));
      }

      @Override
      public int inDegree(CFANode pNode) {
        return delegate.asGraph().inDegree(checkContainsNode(pNode));
      }

      @Override
      public int outDegree(CFANode pNode) {
        return delegate.asGraph().outDegree(checkContainsNode(pNode));
      }

      @Override
      public boolean hasEdgeConnecting(CFANode pNodeU, CFANode pNodeV) {
        return delegate
            .asGraph()
            .hasEdgeConnecting(checkContainsNode(pNodeU), checkContainsNode(pNodeV));
      }

      @Override
      public boolean hasEdgeConnecting(EndpointPair<CFANode> pEndpoints) {
        checkContainsNode(pEndpoints.nodeU());
        checkContainsNode(pEndpoints.nodeV());

        return delegate.asGraph().hasEdgeConnecting(pEndpoints);
      }
    };
  }

  // network properties

  @Override
  public boolean isDirected() {
    return delegate.isDirected();
  }

  @Override
  public boolean allowsParallelEdges() {
    return delegate.allowsParallelEdges();
  }

  @Override
  public boolean allowsSelfLoops() {
    return delegate.allowsSelfLoops();
  }

  @Override
  public ElementOrder<CFANode> nodeOrder() {
    return delegate.nodeOrder();
  }

  @Override
  public ElementOrder<CFAEdge> edgeOrder() {
    return delegate.edgeOrder();
  }

  // element-level accessors

  @Override
  public Set<CFANode> adjacentNodes(CFANode pNode) {
    return checkNoDuplicates(delegate.adjacentNodes(checkContainsNode(pNode)));
  }

  @Override
  public Set<CFANode> predecessors(CFANode pNode) {
    return checkNoDuplicates(delegate.predecessors(checkContainsNode(pNode)));
  }

  @Override
  public Set<CFANode> successors(CFANode pNode) {
    return checkNoDuplicates(delegate.successors(checkContainsNode(pNode)));
  }

  @Override
  public Set<CFAEdge> incidentEdges(CFANode pNode) {
    return checkNoDuplicates(delegate.incidentEdges(checkContainsNode(pNode)));
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return checkNoDuplicates(delegate.inEdges(checkContainsNode(pNode)));
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return checkNoDuplicates(delegate.outEdges(checkContainsNode(pNode)));
  }

  @Override
  public int degree(CFANode pNode) {
    return delegate.degree(checkContainsNode(pNode));
  }

  @Override
  public int inDegree(CFANode pNode) {
    return delegate.inDegree(checkContainsNode(pNode));
  }

  @Override
  public int outDegree(CFANode pNode) {
    return delegate.outDegree(checkContainsNode(pNode));
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(checkContainsEdge(pEdge));
  }

  @Override
  public Set<CFAEdge> adjacentEdges(CFAEdge pEdge) {
    return checkNoDuplicates(delegate.adjacentEdges(checkContainsEdge(pEdge)));
  }

  @Override
  public Set<CFAEdge> edgesConnecting(CFANode pNodeU, CFANode pNodeV) {
    return checkNoDuplicates(
        delegate.edgesConnecting(checkContainsNode(pNodeU), checkContainsNode(pNodeV)));
  }

  @Override
  public Set<CFAEdge> edgesConnecting(EndpointPair<CFANode> pEndpoints) {
    checkContainsNode(pEndpoints.nodeU());
    checkContainsNode(pEndpoints.nodeV());

    return checkNoDuplicates(delegate.edgesConnecting(pEndpoints));
  }

  @Override
  public Optional<CFAEdge> edgeConnecting(CFANode pNodeU, CFANode pNodeV) {
    return delegate.edgeConnecting(checkContainsNode(pNodeU), checkContainsNode(pNodeV));
  }

  @Override
  public Optional<CFAEdge> edgeConnecting(EndpointPair<CFANode> pEndpoints) {
    checkContainsNode(pEndpoints.nodeU());
    checkContainsNode(pEndpoints.nodeV());

    return delegate.edgeConnecting(pEndpoints);
  }

  @Override
  public CFAEdge edgeConnectingOrNull(CFANode pNodeU, CFANode pNodeV) {
    return delegate.edgeConnectingOrNull(checkContainsNode(pNodeU), checkContainsNode(pNodeV));
  }

  @Override
  public CFAEdge edgeConnectingOrNull(EndpointPair<CFANode> pEndpoints) {
    checkContainsNode(pEndpoints.nodeU());
    checkContainsNode(pEndpoints.nodeV());

    return delegate.edgeConnectingOrNull(pEndpoints);
  }

  @Override
  public boolean hasEdgeConnecting(CFANode pNodeU, CFANode pNodeV) {
    return delegate.hasEdgeConnecting(checkContainsNode(pNodeU), checkContainsNode(pNodeV));
  }

  @Override
  public boolean hasEdgeConnecting(EndpointPair<CFANode> pEndpoints) {
    checkContainsNode(pEndpoints.nodeU());
    checkContainsNode(pEndpoints.nodeV());

    return delegate.hasEdgeConnecting(pEndpoints);
  }

  // `CfaNetwork` specific

  @Override
  public Set<FunctionEntryNode> entryNodes() {
    return checkNoDuplicates(delegate.entryNodes());
  }

  @Override
  public CFANode predecessor(CFAEdge pEdge) {
    return delegate.predecessor(checkContainsEdge(pEdge));
  }

  @Override
  public CFANode successor(CFAEdge pEdge) {
    return delegate.successor(checkContainsEdge(pEdge));
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    return delegate.functionExitNode(checkContainsNode(pFunctionEntryNode));
  }
}
