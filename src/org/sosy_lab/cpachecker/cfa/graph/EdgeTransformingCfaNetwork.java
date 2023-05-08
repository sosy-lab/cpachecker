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

import com.google.common.collect.Iterators;
import com.google.common.graph.EndpointPair;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;

/**
 * A {@link CfaNetwork} view in which all edges of the wrapped {@link CfaNetwork} are replaced
 * on-the-fly with their corresponding transformed edges.
 *
 * <p>The specified function returns the transformed edge for a given edge. The given edge and
 * transformed edge must have the same endpoints and the transformed edge must not be a summary
 * edge. The function is applied every time an edge is accessed, so the function may be called
 * multiple times for the same given edge.
 */
final class EdgeTransformingCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork delegate;
  private final Function<CFAEdge, CFAEdge> edgeTransformer;

  private EdgeTransformingCfaNetwork(
      CfaNetwork pDelegate, Function<CFAEdge, CFAEdge> pEdgeTransformer) {
    delegate = checkNotNull(pDelegate);
    edgeTransformer = checkNotNull(pEdgeTransformer);
  }

  static CfaNetwork of(CfaNetwork pDelegate, Function<CFAEdge, CFAEdge> pEdgeTransformer) {
    return CheckingCfaNetwork.wrapIfAssertionsEnabled(
        new EdgeTransformingCfaNetwork(pDelegate, pEdgeTransformer));
  }

  private static boolean haveSameEndpoints(CFAEdge pSomeEdge, CFAEdge pOtherEdge) {
    return pSomeEdge.getPredecessor().equals(pOtherEdge.getPredecessor())
        && pSomeEdge.getSuccessor().equals(pOtherEdge.getSuccessor());
  }

  private CFAEdge transformEdge(CFAEdge pEdge) {
    CFAEdge transformedEdge = edgeTransformer.apply(pEdge);
    checkArgument(
        haveSameEndpoints(pEdge, transformedEdge),
        "Endpoints of original edge and transformed edge are not the same: %s and %s",
        pEdge,
        transformedEdge);
    checkArgument(
        !(transformedEdge instanceof FunctionSummaryEdge),
        "A `CfaNetwork` must not contain any summary edges, but the transformed edge is a summary"
            + " edge: %s",
        transformedEdge);

    return transformedEdge;
  }

  @Override
  public Set<CFANode> nodes() {
    return delegate.nodes();
  }

  @Override
  public Set<CFAEdge> inEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return Iterators.transform(
            delegate.inEdges(pNode).iterator(), EdgeTransformingCfaNetwork.this::transformEdge);
      }
    };
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return Iterators.transform(
            delegate.outEdges(pNode).iterator(), EdgeTransformingCfaNetwork.this::transformEdge);
      }
    };
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(pEdge);
  }

  @Override
  public FunctionEntryNode functionEntryNode(FunctionExitNode pFunctionExitNode) {
    // The delegate may provide a more performant implementation that returns the same node as
    // `super.functionEntryNode`.
    return delegate.functionEntryNode(pFunctionExitNode);
  }

  @Override
  public Optional<FunctionExitNode> functionExitNode(FunctionEntryNode pFunctionEntryNode) {
    // The delegate may provide a more performant implementation that returns the same node as
    // `super.functionExitNode`.
    return delegate.functionExitNode(pFunctionEntryNode);
  }
}
