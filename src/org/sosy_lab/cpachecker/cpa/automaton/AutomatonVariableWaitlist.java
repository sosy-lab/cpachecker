/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.waitlist.AbstractSortedWaitlist;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class AutomatonVariableWaitlist extends AbstractSortedWaitlist<Integer> {

  private final String variableId;

  private AutomatonVariableWaitlist(WaitlistFactory pSecondaryStrategy, String pVariableId) {
    super(pSecondaryStrategy);
    this.variableId = pVariableId;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    int sortKey = Integer.MIN_VALUE;
    for (AutomatonState automatonState : AbstractStates.asIterable(pState).filter(AutomatonState.class)) {
      AutomatonVariable variable = automatonState.getVars().get(variableId);
      if (variable != null) {
        sortKey = Math.max(sortKey, variable.getValue());
      }
    }

    return sortKey;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy, final String pVariableId) {
    return () -> new AutomatonVariableWaitlist(pSecondaryStrategy, pVariableId);
  }
}
