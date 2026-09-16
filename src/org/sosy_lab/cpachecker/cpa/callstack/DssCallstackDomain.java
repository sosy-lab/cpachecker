// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Local DSS coverage, including the behaviour of backwards callstack checking at the ghost edge.
 */
final class DssCallstackDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pFirst, AbstractState pSecond) {
    throw new UnsupportedOperationException("DSS keeps different callstack effects separate");
  }

  @Override
  public boolean isLessOrEqual(AbstractState pFirst, AbstractState pSecond) {
    return pFirst == pSecond
        || (pFirst instanceof DssCallstackState first
            && pSecond instanceof DssCallstackState second
            && first.canBeTopState() == second.canBeTopState()
            && first.getEffect().equals(second.getEffect())
            && first.sameStateInProofChecking(second));
  }
}
