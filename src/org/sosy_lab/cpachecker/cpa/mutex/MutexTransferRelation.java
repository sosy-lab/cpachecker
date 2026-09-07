// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.mutex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Transfer relation for the MutexCPA. The {@link #getAbstractSuccessorsForEdge} method passes the
 * state through unchanged for all edges. The actual mutex state updates happen in {@link
 * #strengthen}, where the PORState is available to determine which thread (PID) is executing.
 */
class MutexTransferRelation extends SingleEdgeTransferRelation {

  private final ImmutableMap<String, String> mutexHandleCandidates;

  MutexTransferRelation(ImmutableMap<String, String> pMutexHandleCandidates) {
    mutexHandleCandidates = pMutexHandleCandidates;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    MutexState state = (MutexState) pState;
    Integer pid = state.getEdgePid(pCfaEdge);
    if (pid == null) {
      throw new CPATransferException("PID for edge not found in MutexState.");
    }

    Optional<MutexState> updated = state.update(pCfaEdge, pid, mutexHandleCandidates);
    if (updated.isEmpty()) {
      return ImmutableList.of();
    }
    return ImmutableList.of(updated.get());
  }
}
