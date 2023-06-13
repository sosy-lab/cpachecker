// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
