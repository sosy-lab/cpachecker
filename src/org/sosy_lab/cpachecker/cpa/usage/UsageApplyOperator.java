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
package org.sosy_lab.cpachecker.cpa.usage;

import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;

public class UsageApplyOperator implements ApplyOperator {

  private final ApplyOperator wrappedApply;

  UsageApplyOperator(ApplyOperator innerOperator) {
    wrappedApply = innerOperator;
  }

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    UsageState state1 = (UsageState) pState1;
    UsageState state2 = (UsageState) pState2;
    AbstractState wrappedState =
        wrappedApply.apply(state1.getWrappedState(), state2.getWrappedState());

    if (wrappedState == null) {
      return null;
    }

    return state1.copy(wrappedState);
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    UsageState state1 = (UsageState) pParent;
    UsageState state2 = (UsageState) pChild;
    AbstractState wrappedState =
        wrappedApply.project(state1.getWrappedState(), state2.getWrappedState());

    if (wrappedState == null) {
      return null;
    }

    return state1.copy(wrappedState);
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    UsageState state1 = (UsageState) pParent;
    UsageState state2 = (UsageState) pChild;
    AbstractState wrappedState =
        wrappedApply.project(state1.getWrappedState(), state2.getWrappedState(), pEdge);

    if (wrappedState == null) {
      return null;
    }

    return state1.copy(wrappedState);
  }

}
