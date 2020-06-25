/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 * <p>The transfer relation contains the transfer <code>a-g->a'</code> for a given {@link CFAEdge}
 * <code>g = (l, op, l')</code> and a current {@link SlicingPrecision} <code>π</code>, if one of the
 * following is true:
 *
 * <ol>
 *   <li><code>g</code> a relevant edge (i.e., <code>g ∈ π</code>), and the wrapped transfer
 *       relation contains <code>a-g->a'</code>
 *   <li><code>g</code> not a relevant edge, and the wrapped transfer relation contains <code>
 *       a-g'->a'</code> for noop-edge <code>g' = (l, noop, l')</code>.
 * </ol>
 */
public class SlicingTransferRelation extends SingleEdgeTransferRelation {

  private final TransferRelation delegate;

  public SlicingTransferRelation(final TransferRelation pDelegateTransferRelation) {
    delegate = pDelegateTransferRelation;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState, final Precision pPrecision, final CFAEdge pCfaEdge
  ) throws CPATransferException, InterruptedException {
    checkState(pPrecision instanceof SlicingPrecision, "Precision not of type " +
        SlicingPrecision.class.getSimpleName() + ", but " + pPrecision.getClass().getSimpleName());

    SlicingPrecision slicingPrecision = (SlicingPrecision) pPrecision;

    AbstractState wrappedState = pState;
    CFAEdge adjustedEdge = pCfaEdge;

    if (!slicingPrecision.isRelevant(pCfaEdge) && !isFunctionControlEdge(pCfaEdge)) {
      adjustedEdge = replaceWithNoop(pCfaEdge);
    }

    Precision wrappedPrecision = slicingPrecision.getWrappedPrec();
    return delegate
        .getAbstractSuccessorsForEdge(wrappedState, wrappedPrecision, adjustedEdge);
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
