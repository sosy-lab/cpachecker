// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slicing;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * {@link TransferRelation} of the {@link SlicingCPA}. Wraps the transfer relation of the CPA
 * wrapped by the slicing CPA.
 *
 * <p>The transfer relation contains the transfer <code>{@code a-g->a'}</code> for a given {@link
 * CFAEdge} <code>g = (l, op, l')</code> and a current {@link SlicingPrecision} <code>π</code>, if
 * one of the following is true:
 *
 * <ol>
 *   <li><code>g</code> a relevant edge (i.e., <code>g ∈ π</code>), and the wrapped transfer
 *       relation contains <code>{@code a-g->a'}</code>
 *   <li><code>g</code> not a relevant edge, and the wrapped transfer relation contains <code>{@code
 *       a-g'->a'}</code> for noop-edge <code>g' = (l, noop, l')</code>.
 * </ol>
 */
public class SlicingTransferRelation extends SingleEdgeTransferRelation {

  private final TransferRelation delegate;

  public SlicingTransferRelation(final TransferRelation pDelegateTransferRelation) {
    delegate = pDelegateTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState, final Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    checkState(
        pPrecision instanceof SlicingPrecision,
        "Precision not of type %s, but %s",
        SlicingPrecision.class.getSimpleName(),
        pPrecision.getClass().getSimpleName());

    SlicingPrecision slicingPrecision = (SlicingPrecision) pPrecision;
    CFAEdge adjustedEdge = pCfaEdge;

    if (!slicingPrecision.isRelevant(pCfaEdge) && !isFunctionControlEdge(pCfaEdge)) {
      adjustedEdge = replaceWithNoop(pCfaEdge);
    }

    AbstractState wrappedState = pState;
    Precision wrappedPrecision = slicingPrecision.getWrappedPrec();

    return delegate.getAbstractSuccessorsForEdge(wrappedState, wrappedPrecision, adjustedEdge);
  }

  private boolean isFunctionControlEdge(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CFunctionSummaryEdge
        || pCfaEdge instanceof CFunctionSummaryStatementEdge) {
      return true;
    } else {
      switch (pCfaEdge.getEdgeType()) {
        case FunctionCallEdge:
        case FunctionReturnEdge:
        case CallToReturnEdge:
          return true;
        default:
          return false;
      }
    }
  }

  private CFAEdge replaceWithNoop(final CFAEdge pCfaEdge) {
    CFANode succ = pCfaEdge.getSuccessor();
    CFANode pred = pCfaEdge.getPredecessor();
    return new BlankEdge(
        pCfaEdge.getRawStatement(),
        pCfaEdge.getFileLocation(),
        pred,
        succ,
        pCfaEdge.getDescription());
  }
}
