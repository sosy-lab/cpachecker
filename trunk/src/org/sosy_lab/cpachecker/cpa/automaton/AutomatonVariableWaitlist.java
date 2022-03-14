// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.AbstractSortedWaitlist;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class AutomatonVariableWaitlist extends AbstractSortedWaitlist<Integer> {

  private final String variableId;

  private AutomatonVariableWaitlist(WaitlistFactory pSecondaryStrategy, String pVariableId) {
    super(pSecondaryStrategy);
    variableId = pVariableId;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    int sortKey = Integer.MIN_VALUE;
    for (AutomatonState automatonState :
        AbstractStates.asIterable(pState).filter(AutomatonState.class)) {
      AutomatonVariable variable = automatonState.getVars().get(variableId);
      if (variable != null) {
        sortKey = Math.max(sortKey, variable.getValue());
      }
    }

    return sortKey;
  }

  public static WaitlistFactory factory(
      final WaitlistFactory pSecondaryStrategy, final String pVariableId) {
    return () -> new AutomatonVariableWaitlist(pSecondaryStrategy, pVariableId);
  }
}
