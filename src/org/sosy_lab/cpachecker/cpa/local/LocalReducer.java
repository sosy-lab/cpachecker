/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.local;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.defaults.GenericReducer;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class LocalReducer extends GenericReducer<LocalState, SingletonPrecision> {

  @Override
  protected LocalState getVariableReducedState0(
      LocalState pExpandedState, Block pContext, CFANode pCallNode) throws InterruptedException {
    return pExpandedState;
  }

  @Override
  protected LocalState getVariableExpandedState0(
      LocalState pRootState, Block pReducedContext, LocalState pReducedState)
      throws InterruptedException {
    return pReducedState.expand(pRootState);
  }

  @Override
  protected Object getHashCodeForState0(LocalState pStateKey, SingletonPrecision pPrecisionKey) {
    return pStateKey.hashCode();
  }

  @Override
  protected Precision getVariableReducedPrecision0(SingletonPrecision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  protected SingletonPrecision getVariableExpandedPrecision0(
      SingletonPrecision pRootPrecision, Block pRootContext, SingletonPrecision pReducedPrecision) {
    return pRootPrecision;
  }

  @Override
  protected LocalState rebuildStateAfterFunctionCall0(
      LocalState pRootState,
      LocalState pEntryState,
      LocalState pExpandedState,
      FunctionExitNode pExitLocation) {
    return pExpandedState;
  }
}
