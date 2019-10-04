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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CompositeApplyOperator implements ApplyOperator {

  private final ImmutableList<ApplyOperator> applyOperators;

  CompositeApplyOperator(ImmutableList<ApplyOperator> pApplyOperators) {
    this.applyOperators = pApplyOperators;
  }

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    CompositeState state1 = (CompositeState) pState1;
    CompositeState state2 = (CompositeState) pState2;

    assert (state1.getNumberOfStates() == state2.getNumberOfStates());

    ImmutableList.Builder<AbstractState> appliedStates = ImmutableList.builder();
    Iterator<AbstractState> iter1 = state1.getWrappedStates().iterator();
    Iterator<AbstractState> iter2 = state2.getWrappedStates().iterator();

    boolean identicalStates = true;
    boolean emptyEffect = true;

    for (ApplyOperator applyOp : applyOperators) {
      AbstractState absState1 = iter1.next();
      AbstractState absState2 = iter2.next();
      AbstractState appliedState;

      appliedState = applyOp.apply(absState1, absState2);

      if (appliedState == null) {
        // Not compatible
        return null;
      }

      if (appliedState != absState1) {
        identicalStates = false;
      }
      if (appliedState instanceof AbstractStateWithEdge) {
        if (!((AbstractStateWithEdge) appliedState).hasEmptyEffect()) {
          // We do not store empty projected states, but applied state may become empty due to
          // optimizations
          emptyEffect = false;
        }
      }
      appliedStates.add(appliedState);
    }

    if (emptyEffect) {
      return null;
    }

    if (identicalStates) {
      return pState1;
    } else {
      return new CompositeState(appliedStates.build());
    }
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    AbstractStateWithLocations loc =
        AbstractStates.extractStateByType(pParent, AbstractStateWithLocations.class);
    if (loc instanceof AbstractStateWithEdge) {
      AbstractEdge edge = ((AbstractStateWithEdge) loc).getAbstractEdge();
      if (edge instanceof WrapperCFAEdge) {
        return project(pParent, pChild, edge);
      }
    }
    // Means its already a projection
    return null;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    CompositeState parent = (CompositeState) pParent;
    CompositeState child = (CompositeState) pChild;

    assert (parent.getNumberOfStates() == child.getNumberOfStates());

    ImmutableList.Builder<AbstractState> appliedStates = ImmutableList.builder();
    Iterator<AbstractState> iter1 = parent.getWrappedStates().iterator();
    Iterator<AbstractState> iter2 = child.getWrappedStates().iterator();

    boolean notEmptyEdge = false;
    for (ApplyOperator applyOp : applyOperators) {
      AbstractState absState1 = iter1.next();
      AbstractState absState2 = iter2.next();
      AbstractState appliedState;

      if (pEdge == null) {
        appliedState = applyOp.project(absState1, absState2);
      } else {
        appliedState = applyOp.project(absState1, absState2, pEdge);
      }

      if (appliedState instanceof AbstractStateWithEdge) {
        if (!((AbstractStateWithEdge) appliedState).hasEmptyEffect()) {
          notEmptyEdge = true;
        }
      }
      appliedStates.add(appliedState);
    }

    if (notEmptyEdge) {
      return new CompositeState(appliedStates.build());
    } else {
      return null;
    }
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    CompositeState state = (CompositeState) pState;
    Iterator<AbstractState> iter = state.getWrappedStates().iterator();

    for (ApplyOperator applyOp : applyOperators) {
      AbstractState absState = iter.next();

      boolean res = applyOp.isInvariantToEffects(absState);
      if (!res) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    CompositeState state = (CompositeState) pState;
    Iterator<AbstractState> iter = state.getWrappedStates().iterator();

    for (ApplyOperator applyOp : applyOperators) {
      AbstractState absState = iter.next();

      boolean res = applyOp.canBeAnythingApplied(absState);
      if (!res) {
        return false;
      }
    }
    return true;
  }
}
