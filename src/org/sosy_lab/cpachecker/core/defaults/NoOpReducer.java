/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;

public class NoOpReducer implements Reducer {

  private static final NoOpReducer instance = new NoOpReducer();

  public static Reducer getInstance() {
    return instance;
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext, CFANode pCallNode) {
    return pExpandedState;
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext, AbstractState pReducedState) {
    return pReducedState;
  }

  @Override
  public Object getHashCodeForState(AbstractState pStateKey, Precision pPrecisionKey) {
    return pStateKey;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision) {
   return reducedPrecision;
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation) {
    return expandedState;
  }
}
