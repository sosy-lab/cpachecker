/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.lock;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.cpa.lock.LockState.LockStateBuilder;

@Options(prefix="cpa.lockstatistics")
public class LockReducer implements Reducer {

  @Option(description="reduce recursive locks to a single access")
  private boolean aggressiveReduction = false;

  @Option(description="reduce unused locks")
  private boolean reduceUselessLocks = false;

  public LockReducer(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedElement, Block pContext, CFANode pCallNode) {
    LockState lockState = (LockState) pExpandedElement;
    LockStateBuilder builder = lockState.builder();
    builder.reduce();
    if (reduceUselessLocks) {
      builder.reduceLocks(pContext.getCapturedLocks());
    } else if (aggressiveReduction) {
      builder.reduceLockCounters(pContext.getCapturedLocks());
    }
    return builder.build();
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootElement, Block pReducedContext,
      AbstractState pReducedElement) {

    LockState reducedState = (LockState)pReducedElement;
    LockState rootState = (LockState) pRootElement;
    LockStateBuilder builder = reducedState.builder();
    builder.expand(rootState);
    if (reduceUselessLocks) {
      builder.expandLocks(rootState, pReducedContext.getCapturedLocks());
    } else if (aggressiveReduction) {
      builder.expandLockCounters(rootState, pReducedContext.getCapturedLocks());
    }
    return builder.build();
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    return pReducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    return getHashCodeForState(pElementKey);
  }

  public Object getHashCodeForState(AbstractState pElementKey) {
    LockState elementKey = (LockState)pElementKey;

    return elementKey.getHashCodeForState();
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(AbstractState pRootState, AbstractState pEntryState,
      AbstractState pExpandedState, FunctionExitNode pExitLocation) {
    return pExpandedState;
  }
}
