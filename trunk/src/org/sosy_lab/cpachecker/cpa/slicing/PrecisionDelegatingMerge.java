// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slicing;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Merge operator of {@link SlicingCPA}. Uses the merge operator of the CPA wrapped by the
 * SlicingCPA, with the precision of the CPA wrapped by the SlicingCPA.
 */
// Since we have as precision type SlicingPrecision,
// we can not simply use the wrapped merge operator.
public class PrecisionDelegatingMerge implements MergeOperator {

  private final MergeOperator delegateMerge;

  public PrecisionDelegatingMerge(final MergeOperator pDelegateMerge) {
    delegateMerge = pDelegateMerge;
  }

  @Override
  public AbstractState merge(
      final AbstractState pState1, final AbstractState pState2, final Precision pPrecision)
      throws CPAException, InterruptedException {
    checkState(
        pPrecision instanceof SlicingPrecision,
        "Precision not of type %s, but %s",
        SlicingPrecision.class.getSimpleName(),
        pPrecision.getClass().getSimpleName());

    final Precision wrappedPrecision = ((SlicingPrecision) pPrecision).getWrappedPrec();
    return delegateMerge.merge(pState1, pState2, wrappedPrecision);
  }
}
