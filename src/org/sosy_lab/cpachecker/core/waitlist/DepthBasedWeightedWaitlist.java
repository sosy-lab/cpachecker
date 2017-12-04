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

import java.io.Serializable;
import java.util.Comparator;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DepthBasedWeightedWaitlist extends WeightedRandomWaitlist {

  public DepthBasedWeightedWaitlist(WaitlistFactory pFactory, Configuration pConfig)
      throws InvalidConfigurationException {
    super(new DepthBasedComparator(), pFactory, pConfig);
  }

  /**
   * Compares abstract states by their depth in the ARG.
   * A state that is deeper in the ARG is 'greater than' a state that is higher in the ARG
   */
  private static class DepthBasedComparator implements Comparator<AbstractState>, Serializable {

    private final static long serialVersionUID = 151646346L;

    @Override
    public int compare(AbstractState o1, AbstractState o2) {
      ARGState s1 = (ARGState) o1;
      ARGState s2 = (ARGState) o2;

      int id1 = AbstractStates.extractLocation(s1).getReversePostorderId();
      int id2 = AbstractStates.extractLocation(s2).getReversePostorderId();

      int callStackDepth1 = AbstractStates.extractStateByType(s1, CallstackState.class).getDepth();
      int callStackDepth2 = AbstractStates.extractStateByType(s2, CallstackState.class).getDepth();

      int comp = Integer.compare(id1, id2);

      if (comp == 0) {
        return Integer.compare(callStackDepth1, callStackDepth2);
      } else {
        return comp;
      }


    }
  }

  public static WaitlistFactory factory(WaitlistFactory pDelegate, Configuration pConfig) {
    return () -> {
      try {
        return new DepthBasedWeightedWaitlist(pDelegate, pConfig);

      } catch (InvalidConfigurationException pE) {
        throw new AssertionError(pE);
      }
    };
  }
}
