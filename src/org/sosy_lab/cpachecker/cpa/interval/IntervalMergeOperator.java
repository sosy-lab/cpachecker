// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.interval;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class IntervalMergeOperator implements MergeOperator {

  @Override
  public AbstractState merge(AbstractState state1, AbstractState state2, Precision precision)
      throws CPAException, InterruptedException {
    if (state1 instanceof IntervalAnalysisState intervalState1
        && state2 instanceof IntervalAnalysisState intervalState2) {
      if (!intervalState1.location().equals(intervalState2.location())) {
        // Do not merge if at different locations
        return intervalState2;
      }

      if (!intervalState1.location().isLoopStart()) {
        // Only merge at loop head
        return intervalState2;
      }

      return intervalState1.widen(intervalState2, intervalState2.location());
    }
    throw new CPAException("Can only merge interval states");
  }
}
