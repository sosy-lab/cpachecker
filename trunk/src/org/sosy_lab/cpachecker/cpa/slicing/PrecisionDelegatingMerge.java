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

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Merge operator of {@link SlicingCPA}.
 * Uses the merge operator of the CPA wrapped by the SlicingCPA,
 * with the precision of the CPA wrapped by the SlicingCPA.
 */
// Since we have as precision type SlicingPrecision,
// we can not simply use the wrapped merge operator.
public class PrecisionDelegatingMerge
    implements MergeOperator {

  private final MergeOperator delegateMerge;

  public PrecisionDelegatingMerge(final MergeOperator pDelegateMerge) {
    delegateMerge = pDelegateMerge;
  }

  @Override
  public AbstractState merge(
      final AbstractState pState1, final AbstractState pState2, final Precision pPrecision)
      throws CPAException, InterruptedException {
    checkState(pPrecision instanceof SlicingPrecision, "Precision not of type " +
        SlicingPrecision.class.getSimpleName() + ", but " + pPrecision.getClass().getSimpleName());

    final Precision wrappedPrecision = ((SlicingPrecision) pPrecision).getWrappedPrec();
    return delegateMerge.merge(pState1, pState2, wrappedPrecision);
  }
}
