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
package org.sosy_lab.cpachecker.cpa.lock;

import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;

public class LockApplyOperator implements ApplyOperator {

  public LockApplyOperator() {}

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    LockState state1 = (LockState) pState1;
    LockState state2 = (LockState) pState2;
    if (state1.isCompatibleWith(state2)) {
      return state1;
    }
    return null;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    return pParent;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    return pParent;
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    return true;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    return true;
  }

}
