// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.validvars;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ValidVarsDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    if (pState1 instanceof ValidVarsState && pState2 instanceof ValidVarsState) {
      ValidVarsState v1 = (ValidVarsState) pState1;
      ValidVarsState v2 = (ValidVarsState) pState2;
      ValidVars newVars = v1.getValidVariables().mergeWith(v2.getValidVariables());

      if (newVars != v2.getValidVariables()) {
        return new ValidVarsState(newVars);
      }
    }
    return pState2;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2)
      throws CPAException, InterruptedException {
    if (pState1 instanceof ValidVarsState && pState2 instanceof ValidVarsState) {
      ValidVarsState v1 = (ValidVarsState) pState1;
      ValidVarsState v2 = (ValidVarsState) pState2;
      return v1.getValidVariables().isSubsetOf(v2.getValidVariables());
    }
    return false;
  }
}
