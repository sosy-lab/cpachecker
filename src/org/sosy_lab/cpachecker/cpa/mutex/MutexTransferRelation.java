// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Transfer relation for the {@link MutexCPA}. Intercepts calls to common mutex functions (both
 * POSIX pthread and C11 threading) and updates the {@link MutexState} accordingly.
 */
class MutexTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof MutexState mutexState)) {
      return ImmutableList.of(state);
    }

    if (cfaEdge instanceof AStatementEdge sEdge
        && sEdge.getStatement() instanceof AFunctionCall funcCall) {
      AExpression funcNameExpr =
          funcCall.getFunctionCallExpression().getFunctionNameExpression();
      if (funcNameExpr instanceof AIdExpression funcName) {
        String name = funcName.getName();
        var params = funcCall.getFunctionCallExpression().getParameterExpressions();
        if (!params.isEmpty()) {
          String mutexName = MutexFunctions.extractMutexName(params.get(0));
          if (mutexName != null) {
            if (MutexFunctions.isInitFunction(name)) {
              return ImmutableList.of(mutexState.withInit(mutexName));
            } else if (MutexFunctions.isLockFunction(name)) {
              if (mutexState.isLocked(mutexName)) {
                return ImmutableList.of();
              }
              return ImmutableList.of(mutexState.withLock(mutexName));
            } else if (MutexFunctions.isUnlockFunction(name)) {
              return ImmutableList.of(mutexState.withUnlock(mutexName));
            } else if (MutexFunctions.isDestroyFunction(name)) {
              return ImmutableList.of(mutexState.withDestroy(mutexName));
            }
          }
        }
      }
    }

    return ImmutableList.of(mutexState);
  }
}
