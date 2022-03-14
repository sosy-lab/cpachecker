// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.dominator;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DominatorDomain implements AbstractDomain {

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {

    assert pElement1 instanceof DominatorState && pElement2 instanceof DominatorState
        : "States given that are no dominator states! "
            + pElement1.getClass().getName()
            + ", "
            + pElement2.getClass().getName();

    return ((DominatorState) pElement1).isLessOrEqual((DominatorState) pElement2);
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {
    assert pElement1 instanceof DominatorState && pElement2 instanceof DominatorState
        : "States given that are no dominator states! "
            + pElement1.getClass().getName()
            + ", "
            + pElement2.getClass().getName();

    return ((DominatorState) pElement1).join((DominatorState) pElement2);
  }
}
