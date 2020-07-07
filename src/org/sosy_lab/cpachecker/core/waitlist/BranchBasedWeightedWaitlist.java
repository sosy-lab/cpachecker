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
import org.sosy_lab.cpachecker.cpa.conditions.path.AssumeEdgesInPathConditionState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BranchBasedWeightedWaitlist extends WeightedRandomWaitlist {

  private static final Comparator<AbstractState> BRANCHING_DEPTH_COMPARATOR =
      Comparator.comparingInt(
          s ->
              AbstractStates.extractStateByType(s, AssumeEdgesInPathConditionState.class)
                  .getPathLength());

  public BranchBasedWeightedWaitlist(
      WaitlistFactory pFactory, WeightedRandomWaitlist.WaitlistOptions pConfig) {
    super(BRANCHING_DEPTH_COMPARATOR, pFactory, pConfig);
  }

  public static WaitlistFactory factory(
      WaitlistFactory pDelegate, WeightedRandomWaitlist.WaitlistOptions pConfig) {
    return () -> new BranchBasedWeightedWaitlist(pDelegate, pConfig);
  }
}
