// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryMergeJoin implements MergeOperator {

  private MergeOperator mergeOperator;

  public LoopSummaryMergeJoin(MergeOperator pWrappedMerge) {
    mergeOperator = pWrappedMerge;
  }

  @Override
  public AbstractState merge(AbstractState pElement1, AbstractState pElement2, Precision pPrecision)
      throws CPAException, InterruptedException {
    return mergeOperator.merge(
        pElement1, pElement2, ((LoopSummaryPrecision) pPrecision).getPrecision());
  }
}
