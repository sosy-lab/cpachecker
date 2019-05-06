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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ThreadModularTransferRelation implements TransferRelation {

  private final TransferRelation wrappedTransfer;
  private final ThreadModularStatistics stats;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ApplyOperator applyOperator;

  public ThreadModularTransferRelation(
      TransferRelation pTransferRelation,
      ThreadModularStatistics pStats,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ApplyOperator pApplyOperator) {

    wrappedTransfer = pTransferRelation;
    stats = pStats;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    applyOperator = pApplyOperator;
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessors(
          AbstractState pState,
          UnmodifiableReachedSet pReached,
          Precision pPrecision)
          throws CPATransferException, InterruptedException {

    stats.totalTransfer.start();
    List<AbstractState> transitions = new ArrayList<>();

    stats.allApplyActions.start();
    shutdownNotifier.shutdownIfNecessary();
    for (AbstractState state : pReached) {
      stats.applyOperator.start();
      AbstractState newState = applyOperator.apply(state, pState);
      stats.applyOperator.stop();

      if (newState != null) {
        transitions.add(newState);
      }
      stats.applyOperator.start();
      newState = applyOperator.apply(pState, state);
      stats.applyOperator.stop();
      if (newState != null) {
        transitions.add(newState);
      }
    }
    stats.allApplyActions.stop();

    stats.wrappedTransfer.start();
    Collection<? extends AbstractState> successors =
        wrappedTransfer.getAbstractSuccessors(pState, pReached, pPrecision);
    stats.wrappedTransfer.stop();

    List<AbstractState> result = new ArrayList<>();
    result.addAll(successors);

    stats.projectOperator.start();
    for (AbstractState child : successors) {
      AbstractState projection = applyOperator.project(pState, child);
      if (projection != null) {
        result.add(projection);
      }
    }
    stats.projectOperator.stop();
    stats.totalTransfer.stop();
    return result;
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessorsForEdge(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
          throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException(
        "Thread Modular CPA does not support direct transitions with CFA edges");
  }

  @Override
  public Collection<? extends AbstractState>
      getAbstractSuccessors(AbstractState pState, Precision pPrecision)
          throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException(
        "Thread Modular CPA does not support transitions without reached set");
  }

}
