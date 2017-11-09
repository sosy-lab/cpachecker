/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.loopbound;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

enum LoopBoundDomain implements AbstractDomain {

  INSTANCE;

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) {
    LoopBoundState state1 = (LoopBoundState) pState1;
    LoopBoundState state2 = (LoopBoundState) pState2;

    if (state1.deepEquals(state2)) {
      return state1;
    }

    throw new UnsupportedOperationException("The join operation is not supported for LoopBoundStates.");
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) {
    LoopBoundState state1 = (LoopBoundState) pState1;
    LoopBoundState state2 = (LoopBoundState) pState2;

    return state1.deepEquals(state2);
  }

}
