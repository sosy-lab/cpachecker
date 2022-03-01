// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BlockDomain extends FlatLatticeDomain {

  @Override
  public boolean isLessOrEqual(
      AbstractState state1, AbstractState state2) throws CPAException {
    assert state1 instanceof BlockState && state2 instanceof BlockState : "BlockDomain operates on BlockStates only: " + state1 + ", " + state2;
    return state1.equals(state2);
  }
}
