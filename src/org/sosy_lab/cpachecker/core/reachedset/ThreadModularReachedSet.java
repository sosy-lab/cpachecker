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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class ThreadModularReachedSet extends ForwardingReachedSet {

  // Do not optimize with canBeAnythingApplied(), very small effect
  private final Set<AbstractState> threadTransitions;
  private final Set<AbstractState> projections;

  public ThreadModularReachedSet(ReachedSet wrappedSet) {
    super(wrappedSet);
    threadTransitions = new TreeSet<>();
    projections = new TreeSet<>();
  }

  @Override
  public void add(AbstractState pState, Precision pPrecision) {
    super.add(pState, pPrecision);
    auxiliaryAdd(pState);
  }

  @Override
  public void addAll(Iterable<Pair<AbstractState, Precision>> pToAdd) {
    super.addAll(pToAdd);
    pToAdd.forEach(p -> this.auxiliaryAdd(p.getFirst()));
  }

  @Override
  public void remove(AbstractState pState) {
    super.remove(pState);
    auxiliaryRemove(pState);
  }

  @Override
  public void removeAll(Iterable<? extends AbstractState> pToRemove) {
    super.removeAll(pToRemove);
    pToRemove.forEach(this::auxiliaryRemove);
  }

  private void auxiliaryAdd(AbstractState pState) {
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

  private void auxiliaryRemove(AbstractState pState) {
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
    Preconditions.checkNotNull(pState);
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
    return ImmutableSet.of();
  }

  @Override
  public Precision getPrecision(AbstractState state) {

    ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
    if (argState != null && argState.getAppliedFrom() != null && !this.contains(state)) {
      ARGState baseParent = argState.getAppliedFrom().getFirst();
      return super.getPrecision(baseParent);
    }
    return super.getPrecision(state);
  }

  public int getProjectioinsNum() {
    return projections.size();
  }

  public int getThreadTransitionsNum() {
    return threadTransitions.size();
  }

  @Override
  public void clear() {
    super.clear();
    threadTransitions.clear();
    projections.clear();
  }

}
