/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.callstack;

import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class CallstackPCCAbstractDomain implements AbstractDomain {

  @Override
  public AbstractState join(AbstractState pState1, AbstractState pState2) throws CPAException, InterruptedException {
    return pState2;
  }

  @Override
  public boolean isLessOrEqual(AbstractState pState1, AbstractState pState2) throws CPAException, InterruptedException {
    if (pState1 instanceof CallstackState && pState2 instanceof CallstackState) {
      return isLessOrEqual((CallstackState) pState1, (CallstackState) pState2);
    }
    return false;
  }

  private boolean isLessOrEqual(CallstackState state1, CallstackState state2){
    if (state1 == state2) {
      return true;
    }
    if (state1 == null || state2 == null) {
      return false;
    }
    if (state1.sameStateInProofChecking(state2)) {
      return true;
    }
    return false;
  }
}
