// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LessEqualCoverageOperator implements CoverageOperator {

  private final ConfigurableProgramAnalysis cpa;

  public LessEqualCoverageOperator(ConfigurableProgramAnalysis pCpa) {
    cpa = pCpa;
  }

  @Override
  public boolean isSubsumed(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return cpa.getAbstractDomain().isLessOrEqual(state1, state2);
  }

  @Override
  public boolean isBasedOnEquality() {
    return false;
  }
}
