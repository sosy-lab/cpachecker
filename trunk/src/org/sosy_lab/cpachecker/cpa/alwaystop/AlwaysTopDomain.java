// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.alwaystop;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

enum AlwaysTopDomain implements AbstractDomain {
  INSTANCE;

  @Override
  public boolean isLessOrEqual(AbstractState pElement1, AbstractState pElement2) {

    assert pElement1 == AlwaysTopState.INSTANCE;
    assert pElement2 == AlwaysTopState.INSTANCE;
    return true;
  }

  @Override
  public AbstractState join(AbstractState pElement1, AbstractState pElement2) {

    assert pElement1 == AlwaysTopState.INSTANCE;
    assert pElement2 == AlwaysTopState.INSTANCE;
    return AlwaysTopState.INSTANCE;
  }
}
