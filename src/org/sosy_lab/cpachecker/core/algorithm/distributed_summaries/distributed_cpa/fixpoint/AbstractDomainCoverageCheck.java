// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.fixpoint;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AbstractDomainCoverageCheck implements CoverageCheck {

  private final AbstractDomain domain;

  public AbstractDomainCoverageCheck(AbstractDomain pDomain) {
    domain = pDomain;
  }

  @Override
  public boolean isCovered(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    return domain.isLessOrEqual(state1, state2);
  }
}
