// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.por.PORState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Transfer relation for the MutexCPA. The {@link #getAbstractSuccessorsForEdge} method passes the
 * state through unchanged for all edges. The actual mutex state updates happen in {@link
 * #strengthen}, where the PORState is available to determine which thread (PID) is executing.
 */
class MutexTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    // Pass through — actual work is done in strengthen where we can read the PID from PORState.
    return Collections.singleton(pState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (cfaEdge == null) {
      return Collections.singleton(pState);
    }

    MutexState state = (MutexState) pState;

    // Get the PID of the active thread from the PORState
    PORState porState =
        AbstractStates.projectToType(otherStates, PORState.class).iterator().next();
    Integer pid = porState.getEdgePid(cfaEdge);
    if (pid == null) {
      // Not a POR-managed edge, pass through
      return Collections.singleton(state);
    }

    if (cfaEdge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr =
          funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        String functionName = funcName.getName();
        var params = funcCall.getFunctionCallExpression().getParameterExpressions();

        if (!params.isEmpty()) {
          String mutexName = MutexFunctions.extractMutexName(params.get(0));
          if (mutexName != null) {
            if (MutexFunctions.isInitFunction(functionName)) {
              return Collections.singleton(state.withInit(mutexName));
            } else if (MutexFunctions.isLockFunction(functionName)) {
              if (state.isLockedByOther(mutexName, pid)) {
                // Blocked: another thread holds the mutex — bottom (no successor)
                return ImmutableSet.of();
              }
              return Collections.singleton(state.withLock(mutexName, pid));
            } else if (MutexFunctions.isUnlockFunction(functionName)) {
              return Collections.singleton(state.withUnlock(mutexName));
            } else if (MutexFunctions.isDestroyFunction(functionName)) {
              return Collections.singleton(state.withDestroy(mutexName));
            }
          }
        }
      }
    }

    return Collections.singleton(state);
  }
}
