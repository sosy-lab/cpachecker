// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

import java.util.Arrays;
import org.sosy_lab.cpachecker.util.sdg.SdgEdgeType;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;

/**
 * Implementation of a visit-once-visitor (i.e., a visitor that guarantees that the delegate visitor
 * visits every node/edge at most once during a traversal). Extended by {@link
 * VisitOnceForwardsSdgVisitor} and {@link VisitOnceBackwardsSdgVisitor}.
 */
abstract class VisitOnceSdgVisitor<N extends SystemDependenceGraph.Node<?, ?, ?>>
    implements SdgVisitor<N> {

  private final boolean forwards;
  private final SdgVisitor<N> delegateVisitor;

  private final byte[] visited;
  private byte visitedMarker;

  protected VisitOnceSdgVisitor(boolean pForwards, SdgVisitor<N> pDelegateVisitor, int pNodeCount) {

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
  public SdgVisitResult visitEdge(SdgEdgeType pType, N pPredecessor, N pSuccessor) {

    SdgVisitResult visitResult = delegateVisitor.visitEdge(pType, pPredecessor, pSuccessor);

    if (visitResult == SdgVisitResult.CONTINUE) {

      N nextNode = forwards ? pSuccessor : pPredecessor;

      if (isVisited(nextNode)) {
        return SdgVisitResult.SKIP;
      }
    }

    return visitResult;
  }
}
