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

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Comparator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssumeEdgesInPathConditionState;
import org.sosy_lab.cpachecker.cpa.conditions.path.AssumeEdgesInPathConditionState.AssumeCountComparator;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BranchBasedWeightedWaitlist extends WeightedRandomWaitlist {

  public BranchBasedWeightedWaitlist(WaitlistFactory pFactory, Configuration pConfig)
      throws InvalidConfigurationException {
    super(new BranchingDepthComparator(), pFactory, pConfig);
  }

  public static class BranchingDepthComparator  implements Comparator<AbstractState>, Serializable {
    private final static long serialVersionUID = 121644346L;

    private Comparator<AssumeEdgesInPathConditionState> assumeCountComparator =
        new AssumeCountComparator();

    @Override
    public int compare(AbstractState o1, AbstractState o2) {
      AssumeEdgesInPathConditionState s1 =
          AbstractStates.extractStateByType(o1, AssumeEdgesInPathConditionState.class);
      AssumeEdgesInPathConditionState s2 =
          AbstractStates.extractStateByType(o2, AssumeEdgesInPathConditionState.class);

      Preconditions.checkState(s1 != null && s2 != null,
          "State not found: " + AssumeEdgesInPathConditionState.class.getSimpleName());

      return assumeCountComparator.compare(s1, s2);
    }
  }

  public static WaitlistFactory factory(WaitlistFactory pDelegate, Configuration pConfig) {
    return () -> {
      try {
        return new BranchBasedWeightedWaitlist(pDelegate, pConfig);

      } catch (InvalidConfigurationException pE) {
        throw new AssertionError(pE);
      }
    };
  }
}
