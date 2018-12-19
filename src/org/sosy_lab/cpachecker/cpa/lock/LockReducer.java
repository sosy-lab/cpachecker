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

import com.google.common.collect.Sets;
import java.util.Set;
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

@Options(prefix = "cpa.lock")
public class LockReducer implements Reducer {

  public enum ReduceStrategy {
    NONE,
    BLOCK,
    ALL
  }

  @Option(description = "reduce recursive locks to a single access", secure = true)
  private ReduceStrategy reduceLockCounters = ReduceStrategy.BLOCK;

  // Attention! Error trace may be restored incorrectly.
  // If two states with different locks are reduced to the one state,
  // the path will be always restored through the first one
  @Option(description = "reduce unused locks", secure = true)
  private boolean reduceUselessLocks = false;

  public LockReducer(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    if (reduceUselessLocks && reduceLockCounters == ReduceStrategy.BLOCK) {
      // reducing counters in this case is useless
      reduceLockCounters = ReduceStrategy.NONE;
    }
  }

  @Override
  public AbstractLockState
      getVariableReducedState(AbstractState pExpandedElement, Block pContext, CFANode pCallNode) {
    AbstractLockState expandedElement = (AbstractLockState) pExpandedElement;
    AbstractLockStateBuilder builder = expandedElement.builder();
    Set<LockIdentifier> locksToProcess = expandedElement.getLocks();

    builder.reduce();
    if (reduceUselessLocks) {
      builder.removeLocksExcept(pContext.getCapturedLocks());
      // All other locks are successfully removed
      locksToProcess = Sets.intersection(locksToProcess, pContext.getCapturedLocks());
    }
    switch (reduceLockCounters) {
      case BLOCK:
        locksToProcess = Sets.difference(locksToProcess, pContext.getCapturedLocks());
        //$FALL-THROUGH$
      case ALL:
        builder.reduceLockCounters(locksToProcess);
        break;
      case NONE:
        break;
    }
    AbstractLockState reducedState = builder.build();
    assert getVariableExpandedState(pExpandedElement, pContext, reducedState)
        .equals(pExpandedElement);
    return reducedState;
  }

  @Override
  public AbstractState getVariableExpandedState(
      AbstractState pRootElement,
      Block pReducedContext,
      AbstractState pReducedElement) {

    AbstractLockState rootElement = (AbstractLockState) pRootElement;
    AbstractLockState reducedElement = (AbstractLockState) pReducedElement;
    AbstractLockStateBuilder builder = reducedElement.builder();
    // Restore only what we reduced
    Set<LockIdentifier> locksToProcess = rootElement.getLocks();

    builder.expand(rootElement);
    if (reduceUselessLocks) {
      builder.returnLocksExcept((LockState) pRootElement, pReducedContext.getCapturedLocks());
      locksToProcess = Sets.intersection(locksToProcess, pReducedContext.getCapturedLocks());
    }
    switch (reduceLockCounters) {
      case BLOCK:
        locksToProcess = Sets.difference(locksToProcess, pReducedContext.getCapturedLocks());
        //$FALL-THROUGH$
      case ALL:
        builder.expandLockCounters(rootElement, locksToProcess);
        break;
      case NONE:
        break;
    }
    return builder.build();
  }

  @Override
  public Precision getVariableReducedPrecision(
      Precision pPrecision,
      Block pContext) {
    return pPrecision;
  }

  @Override
  public Precision getVariableExpandedPrecision(
      Precision pRootPrecision,
      Block pRootContext,
      Precision pReducedPrecision) {
    return pReducedPrecision;
  }

  @Override
  public Object getHashCodeForState(
      AbstractState pElementKey,
      Precision pPrecisionKey) {
    return ((AbstractLockState) pElementKey).getHashCodeForState();
  }

  @Override
  public AbstractState rebuildStateAfterFunctionCall(
      AbstractState pRootState,
      AbstractState pEntryState,
      AbstractState pExpandedState,
      FunctionExitNode pExitLocation) {
    return pExpandedState;
  }
}
