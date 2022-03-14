// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.alwaystop;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

enum AlwaysTopMergeOperator implements MergeOperator {
  INSTANCE;

  @Override
  public AbstractState merge(
      AbstractState pElement1, AbstractState pElement2, Precision pPrecision) {

    assert pElement1 == AlwaysTopState.INSTANCE;
    assert pElement2 == AlwaysTopState.INSTANCE;
    assert pPrecision == AlwaysTopPrecision.INSTANCE;
    return AlwaysTopState.INSTANCE;
  }
}
