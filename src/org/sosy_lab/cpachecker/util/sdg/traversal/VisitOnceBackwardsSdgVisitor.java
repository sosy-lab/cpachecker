// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.traversal;

import java.util.Collection;
import org.sosy_lab.cpachecker.util.sdg.SdgEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgNode;
import org.sosy_lab.cpachecker.util.sdg.SystemDependenceGraph;

/**
 * Represents a visit-once-visitor (i.e., a visitor that guarantees that the delegate visitor visits
 * every node/edge at most once during a traversal) for backwards system dependence graph (SDG)
 * traversals. The visitor can forget all visited nodes by calling {@link #reset}. If {@link #reset}
 * isn't called between two SDG traversals, nodes and edges visited during the first traversal are
 * not visited again during the second traversal. Visit-once-visitors only work for the SDG they
 * were created for.
 *
 * @param <V> the variable type of the SDG
 * @param <N> the SDG node type of the SDG
 * @param <E> the SDG edge type of the SDG
 * @see SystemDependenceGraph#createVisitOnceVisitor(BackwardsSdgVisitor)
 * @see SystemDependenceGraph#traverse(Collection, BackwardsSdgVisitor)
 */
public final class VisitOnceBackwardsSdgVisitor<V, N extends SdgNode<?, ?, V>, E extends SdgEdge<V>>
    extends VisitOnceSdgVisitor<V, N, E> implements BackwardsSdgVisitor<V, N, E> {

  public VisitOnceBackwardsSdgVisitor(
      BackwardsSdgVisitor<V, N, E> pDelegateVisitor, int pNodeCount) {
    super(false, pDelegateVisitor, pNodeCount);
  }

  /**
   * Makes this visit-once-visitor forget all previously visited nodes.
   *
   * <p>If this method isn't called between two SDG traversals, nodes and edges visited during the
   * first traversal are not visited again during the second traversal.
   */
  @Override
  public void reset() {
    super.reset();
  }

  @Override
  public SdgVisitResult visitNode(N pNode) {
    return super.visitNode(pNode);
  }

  @Override
  public SdgVisitResult visitEdge(E pEdge, N pPredecessor, N pSuccessor) {
    return super.visitEdge(pEdge, pPredecessor, pSuccessor);
  }
}
