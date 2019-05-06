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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;

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
    for (ApplyOperator applyOp : applyOperators) {
      AbstractState absState1 = iter1.next();
      AbstractState absState2 = iter2.next();
      AbstractState appliedState = applyOp.apply(absState1, absState2);

      if (appliedState != absState1) {
        identicalStates = false;
      }
      appliedStates.add(appliedState);
    }

    if (identicalStates) {
      return pState1;
    } else {
      return new CompositeState(appliedStates.build());
    }
  }

}
