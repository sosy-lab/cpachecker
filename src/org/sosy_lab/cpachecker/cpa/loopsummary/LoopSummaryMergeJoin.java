// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGMergeJoin;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryMergeJoin extends ARGMergeJoin {

  public LoopSummaryMergeJoin(
      MergeOperator pWrappedMerge,
      AbstractDomain pWrappedDomain,
      boolean pMergeOnWrappedSubsumption) {
    super(pWrappedMerge, pWrappedDomain, pMergeOnWrappedSubsumption);
  }

  @Override
  public AbstractState merge(AbstractState pElement1, AbstractState pElement2, Precision pPrecision)
      throws CPAException, InterruptedException {
    return super.merge(pElement1, pElement2, ((LoopSummaryPrecision) pPrecision).getPrecision());
  }
}
