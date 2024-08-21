// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.traceabstraction;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateMergeOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Merge operator of {@link TraceAbstractionCPA}. It is a wrapper MergeOperator containing the
 * {@link PredicateMergeOperator}, and its only functionality is to forward all incoming
 * method-calls to it.
 */
class TraceAbstractionMergeOperator implements MergeOperator {

  private final MergeOperator deletageMergeOperator;

  TraceAbstractionMergeOperator(MergeOperator pDeletageMergeOperator) {
    deletageMergeOperator = pDeletageMergeOperator;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    TraceAbstractionState taState1 = (TraceAbstractionState) pState1;
    TraceAbstractionState taState2 = (TraceAbstractionState) pState2;

    AbstractState wrappedState2 = taState2.getWrappedState();

    AbstractState mergeResult =
        deletageMergeOperator.merge(taState1.getWrappedState(), wrappedState2, pPrecision);

    if (mergeResult == wrappedState2) {
      // Wrapped predicate state is an abstraction location, hence no merging was done
      return pState2;
    }

    // TODO: double-check that stored predicates are correctly merged
    // (for now the behavior of MergeSepOp is taken w.r.t predicates)
    return new TraceAbstractionState(mergeResult, taState2.getActivePredicates());
  }
}
