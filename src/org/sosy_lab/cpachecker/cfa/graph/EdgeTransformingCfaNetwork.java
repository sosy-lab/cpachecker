// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterators;
import com.google.common.graph.EndpointPair;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A {@link CfaNetwork} that forwards all calls to another {@link CfaNetwork}, but also transforms
 * all returned edges using the specified function.
 *
 * <p>Before any edge is returned, the transformer function is applied. Only the edges returned by
 * the transformer function are part of the CFA represented by a {@link EdgeTransformingCfaNetwork}.
 */
final class EdgeTransformingCfaNetwork extends AbstractCfaNetwork {

  private final CfaNetwork delegate;
  private final Function<CFAEdge, CFAEdge> edgeTransformer;

  private EdgeTransformingCfaNetwork(
      CfaNetwork pDelegate, Function<CFAEdge, CFAEdge> pEdgeTransformer) {
    delegate = pDelegate;
    edgeTransformer = pEdgeTransformer;
  }

  static EdgeTransformingCfaNetwork of(
      CfaNetwork pDelegate, Function<CFAEdge, CFAEdge> pEdgeTransformer) {
    return new EdgeTransformingCfaNetwork(checkNotNull(pDelegate), checkNotNull(pEdgeTransformer));
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
        return Iterators.transform(delegate.inEdges(pNode).iterator(), edgeTransformer::apply);
      }
    };
  }

  @Override
  public Set<CFAEdge> outEdges(CFANode pNode) {
    return new UnmodifiableSetView<>() {

      @Override
      public Iterator<CFAEdge> iterator() {
        return Iterators.transform(delegate.outEdges(pNode).iterator(), edgeTransformer::apply);
      }
    };
  }

  @Override
  public EndpointPair<CFANode> incidentNodes(CFAEdge pEdge) {
    return delegate.incidentNodes(pEdge);
  }
}
