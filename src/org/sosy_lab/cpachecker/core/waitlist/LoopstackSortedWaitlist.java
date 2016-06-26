/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Waitlist implementation that sorts the abstract states by the depth of
 * their loopstack.
 * States with a larger/smaller (depending on the used factory method)
 * loopstack are considered first.
 */
public class LoopstackSortedWaitlist extends AbstractSortedWaitlist<Integer>{
  private final int multiplier;

  private LoopstackSortedWaitlist(WaitlistFactory pSecondaryStrategy,
      int pMultiplier) {
    super(pSecondaryStrategy);
    multiplier = pMultiplier;
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    LoopstackState loopstackState =
        AbstractStates.extractStateByType(pState, LoopstackState.class);
    return (loopstackState != null) ? (multiplier * loopstackState.getDepth()) : 0;
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new LoopstackSortedWaitlist(pSecondaryStrategy, 1);
  }

  public static WaitlistFactory reversedFactory(
      final WaitlistFactory pSecondaryStrategy) {
    return () -> new LoopstackSortedWaitlist(pSecondaryStrategy, -1);
  }
}
