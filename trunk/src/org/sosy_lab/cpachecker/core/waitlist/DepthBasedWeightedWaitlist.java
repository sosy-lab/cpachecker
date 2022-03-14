// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
