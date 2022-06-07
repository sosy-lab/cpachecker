// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

import java.util.Arrays;
import org.sosy_lab.cpachecker.util.sdg.SdgEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgNode;

/**
 * Implementation of a visit-once-visitor (i.e., a visitor that guarantees that the delegate visitor
 * visits every node/edge at most once during a traversal). Extended by {@link
 * VisitOnceForwardsSdgVisitor} and {@link VisitOnceBackwardsSdgVisitor}.
 *
 * @param <V> the variable type of the SDG
 * @param <N> the SDG node type of the SDG
 * @param <E> the SDG edge type of the SDG
 */
abstract class VisitOnceSdgVisitor<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
    implements SdgVisitor<V, N, E> {

  private final boolean forwards;
  private final SdgVisitor<V, N, E> delegateVisitor;

  private final byte[] visited;
  private byte visitedMarker;

  protected VisitOnceSdgVisitor(
      boolean pForwards, SdgVisitor<V, N, E> pDelegateVisitor, int pNodeCount) {

    forwards = pForwards;
    delegateVisitor = pDelegateVisitor;

    visited = new byte[pNodeCount];
    visitedMarker = 1;
  }

  protected void reset() {

    visitedMarker++;

    if (visitedMarker == 0) {

      Arrays.fill(visited, (byte) 0);
      visitedMarker = 1;
    }
  }

  private boolean isVisited(N pNode) {
    return visited[pNode.getId()] == visitedMarker;
  }

  @Override
  public SdgVisitResult visitNode(N pNode) {

    if (!isVisited(pNode)) {

      visited[pNode.getId()] = visitedMarker;

      return delegateVisitor.visitNode(pNode);
    }

    return SdgVisitResult.SKIP;
  }

  @Override
  public SdgVisitResult visitEdge(E pEdge, N pPredecessor, N pSuccessor) {

    SdgVisitResult visitResult = delegateVisitor.visitEdge(pEdge, pPredecessor, pSuccessor);

    if (visitResult == SdgVisitResult.CONTINUE) {

      N nextNode = forwards ? pSuccessor : pPredecessor;

      if (isVisited(nextNode)) {
        return SdgVisitResult.SKIP;
      }
    }

    return visitResult;
  }
}
