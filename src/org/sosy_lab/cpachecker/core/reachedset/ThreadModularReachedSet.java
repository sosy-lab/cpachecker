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
package org.sosy_lab.cpachecker.core.reachedset;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.waitlist.Waitlist.WaitlistFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ThreadModularReachedSet extends PartitionedReachedSet {

  private static final long serialVersionUID = -1608561980102557646L;
  private final Set<AbstractState> threadTransitions;
  private final Set<AbstractState> projections;

  public ThreadModularReachedSet(WaitlistFactory pWaitlistFactory) {
    super(pWaitlistFactory);
    threadTransitions = new HashSet<>();
    projections = new HashSet<>();
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);

    AbstractStateWithLocations loc =
        AbstractStates.extractStateByType(pState, AbstractStateWithLocations.class);
    if (loc instanceof AbstractStateWithEdge) {
      AbstractStateWithEdge locEdge = (AbstractStateWithEdge) loc;
      if (locEdge.isProjection()) {
        projections.add(pState);
      } else if (locEdge.getAbstractEdge() instanceof WrapperCFAEdge) {
        threadTransitions.add(pState);
      } else {
        // applied state, skip
      }
    }
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);

    AbstractStateWithLocations loc =
        AbstractStates.extractStateByType(pState, AbstractStateWithLocations.class);
    if (loc instanceof AbstractStateWithEdge) {
      AbstractStateWithEdge locEdge = (AbstractStateWithEdge) loc;
      if (locEdge.isProjection()) {
        projections.remove(pState);
      } else if (locEdge.getAbstractEdge() instanceof WrapperCFAEdge) {
        threadTransitions.remove(pState);
      } else {
        // applied state, skip
      }
      Collection<ARGState> appliedStates = ((ARGState) pState).getAppliedTo();
      if (appliedStates != null) {
        for (AbstractState state : appliedStates) {
          super.removeOnlyFromWaitlist(state);
        }
      }
    }
  }

  public Collection<AbstractState> getStatesForApply(AbstractState pState) {
    AbstractStateWithLocations loc =
        AbstractStates.extractStateByType(pState, AbstractStateWithLocations.class);
    if (loc instanceof AbstractStateWithEdge) {
      AbstractStateWithEdge locEdge = (AbstractStateWithEdge) loc;
      if (locEdge.isProjection()) {
        return ImmutableSet.copyOf(threadTransitions);
      } else if (locEdge.getAbstractEdge() instanceof WrapperCFAEdge) {
        return ImmutableSet.copyOf(projections);
      } else {
        // applied state, skip
      }
    }
    return Collections.emptySet();
  }

  public int getProjectioinsNum() {
    return projections.size();
  }

  public int getThreadTransitionsNum() {
    return threadTransitions.size();
  }
}
