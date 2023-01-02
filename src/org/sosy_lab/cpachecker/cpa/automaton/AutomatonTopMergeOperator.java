// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class AutomatonTopMergeOperator implements MergeOperator {

  private final AbstractDomain domain;
  private final AbstractState topState;

  public AutomatonTopMergeOperator(AbstractDomain pDomain, AbstractState pTopState) {
    domain = pDomain;
    topState = pTopState;
  }

  @Override
  public AbstractState merge(AbstractState el1, AbstractState el2, Precision p)
      throws CPAException, InterruptedException {

    boolean anyAutomatonTop =
        domain.isLessOrEqual(topState, el1) || domain.isLessOrEqual(topState, el2);

    if (anyAutomatonTop) {
      return topState;
    } else {
      return el2;
    }
  }
}
