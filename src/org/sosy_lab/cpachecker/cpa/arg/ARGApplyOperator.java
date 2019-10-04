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
package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;

public class ARGApplyOperator implements ApplyOperator {

  private final ApplyOperator wrappedApply;

  ARGApplyOperator(ApplyOperator pApplyOperator) {
    wrappedApply = pApplyOperator;
  }

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    ARGState state1 = (ARGState) pState1;
    ARGState state2 = (ARGState) pState2;
    AbstractState wrappedState =
        wrappedApply.apply(state1.getWrappedState(), state2.getWrappedState());

    if (wrappedState == null) {
      return null;
    }

    ARGState result = new ARGState(wrappedState, null);
    result.setAsAppliedFrom(state1, state2);

    // Evil hack to restore path
    result.addParent(state1);
    return result;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    ARGState parent = (ARGState) pParent;
    ARGState child = (ARGState) pChild;
    AbstractState wrappedState =
        wrappedApply.project(parent.getWrappedState(), child.getWrappedState());

    if (wrappedState == null) {
      return null;
    }

    ARGState result = new ARGState(wrappedState, null);
    parent.addProjection(result);
    return result;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    ARGState parent = (ARGState) pParent;
    ARGState child = (ARGState) pChild;
    AbstractState wrappedState =
        wrappedApply.project(parent.getWrappedState(), child.getWrappedState(), pEdge);

    if (wrappedState == null) {
      return null;
    }

    ARGState result = new ARGState(wrappedState, null);
    parent.addProjection(result);
    return result;
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    ARGState state = (ARGState) pState;
    return wrappedApply.isInvariantToEffects(state.getWrappedState());
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    ARGState state = (ARGState) pState;
    return wrappedApply.canBeAnythingApplied(state.getWrappedState());
  }

}
