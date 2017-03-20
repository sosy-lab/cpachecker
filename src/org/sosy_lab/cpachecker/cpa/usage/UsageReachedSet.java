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
package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class UsageReachedSet extends PartitionedReachedSet {
  //public static Timer addTimer = new Timer();

  public UsageReachedSet(WaitlistFactory waitlistFactory) {
    super(waitlistFactory);
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);
    UsageState ustate = AbstractStates.extractStateByType(pState, UsageState.class);
    ustate.getContainer().removeState(ustate);
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    UsageState USstate = AbstractStates.extractStateByType(pState, UsageState.class);
    USstate.saveUnsafesInContainerIfNecessary(pState);
  }

  @Override
  public void clear() {
    AbstractStates.extractStateByType(getFirstState(), UsageState.class).getContainer().resetUnrefinedUnsafes();
    super.clear();
  }
}
