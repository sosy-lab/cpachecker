/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.waitlist;

import org.sosy_lab.cpachecker.core.defaults.AnyCFAEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.NoEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ThreadModularSortedWaitlist extends AbstractSortedWaitlist<Integer> {

  protected ThreadModularSortedWaitlist(WaitlistFactory pSecondaryStrategy) {
    super(pSecondaryStrategy);
  }

  @Override
  protected Integer getSortKey(AbstractState pState) {
    AbstractStateWithLocations loc =
        AbstractStates.extractStateByType(pState, AbstractStateWithLocations.class);
    if (loc instanceof AbstractStateWithEdge) {
      AbstractEdge edge = ((AbstractStateWithEdge) loc).getAbstractEdge();
      if (edge instanceof WrapperCFAEdge
          || edge == AnyCFAEdge.getInstance()
          || edge == NoEdge.getInstance()) {
        // Thread transition or projection, do not delay them as they are empty
        return 1;
      } else if (edge == EmptyEdge.getInstance()) {
        // Environment transition
        return -((ARGState) pState).getStateId();
      } else {
        throw new UnsupportedOperationException("Unknown edge: " + edge.getClass());
      }
    } else {
      throw new UnsupportedOperationException("ThreadModularWaitlist expects states with edges");
    }
  }

  public static WaitlistFactory factory(final WaitlistFactory pSecondaryStrategy) {
    return () -> new ThreadModularSortedWaitlist(pSecondaryStrategy);
  }
}
