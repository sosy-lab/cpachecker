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
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states depending on the
 * content of the ExplicitState (if there is any).
 * Stas where less variables have a value assigned are considered first.
 * This states are expected to cover a bigger part of the state space,
 * so states with more variables will probably be covered later.
 */
public class ExplicitSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ExplicitSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    ValueAnalysisState explicitState =
      AbstractStates.extractStateByType(pState, ValueAnalysisState.class);

    // negate size so that the highest key corresponds to the smallest map
    return (explicitState != null) ? -explicitState.getSize() : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ExplicitSortedWaitlist(pSecondaryStrategy);
  }
}
