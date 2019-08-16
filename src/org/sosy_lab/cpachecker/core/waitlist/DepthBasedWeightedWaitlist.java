/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.Comparator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DepthBasedWeightedWaitlist extends WeightedRandomWaitlist {

  /**
   * Compares abstract states by their depth in the ARG. A state that is deeper in the ARG is
   * 'greater than' a state that is higher in the ARG
   */
  private static final Comparator<AbstractState> DEPTH_BASED_STATE_COMPARATOR =
      Comparator.<AbstractState>comparingInt(
              s -> AbstractStates.extractLocation(s).getReversePostorderId())
          .thenComparingInt(
              s -> AbstractStates.extractStateByType(s, CallstackState.class).getDepth());

  public DepthBasedWeightedWaitlist(
      WaitlistFactory pFactory, WeightedRandomWaitlist.WaitlistOptions pConfig) {
    super(DEPTH_BASED_STATE_COMPARATOR, pFactory, pConfig);
  }

  public static WaitlistFactory factory(
      WaitlistFactory pDelegate, WeightedRandomWaitlist.WaitlistOptions pConfig) {
    return () -> new DepthBasedWeightedWaitlist(pDelegate, pConfig);
  }
}
