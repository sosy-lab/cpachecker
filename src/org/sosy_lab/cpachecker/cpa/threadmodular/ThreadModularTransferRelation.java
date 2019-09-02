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

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class ThreadModularTransferRelation implements TransferRelation {

  private final TransferRelation wrappedTransfer;
  private final ThreadModularStatistics stats;
  private final ShutdownNotifier shutdownNotifier;
  private final ApplyOperator applyOperator;

  public ThreadModularTransferRelation(
      TransferRelation pTransferRelation,
      ThreadModularStatistics pStats,
      ShutdownNotifier pShutdownNotifier,
      ApplyOperator pApplyOperator) {

    wrappedTransfer = pTransferRelation;
    stats = pStats;
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

    if (((AbstractStateWithEdge) pState).isProjection()) {
      // Projection, but not applied transition
      stats.numberOfProjectionsConsidered.inc();
      stats.totalTransfer.stop();
      return Collections.emptySet();
    }
    // Just to statistics
    AbstractStateWithLocations loc =
        AbstractStates.extractStateByType(pState, AbstractStateWithLocations.class);
    if (loc instanceof AbstractStateWithEdge) {
      AbstractEdge edge = ((AbstractStateWithEdge) loc).getAbstractEdge();
      if (edge instanceof WrapperCFAEdge) {
        stats.numberOfTransitionsInThreadConsidered.inc();
      } else if (edge == EmptyEdge.getInstance()) {
        stats.numberOfTransitionsInEnvironmentConsidered.inc();
      }
    }
    ARGState argState = AbstractStates.extractStateByType(pState, ARGState.class);
    if (argState != null) {
      for (ARGState parent : argState.getParents()) {
        if (parent.getAppliedFrom() != null) {
          stats.numberOfValuableTransitionsInEnvironement.inc();
          break;
        }
      }
    }

    // TODO Get precision from ReachedSet!!
    List<AbstractState> result = new ArrayList<>();

    stats.wrappedTransfer.start();
    Collection<? extends AbstractState> successors =
        wrappedTransfer.getAbstractSuccessors(pState, pReached, pPrecision);
    stats.wrappedTransfer.stop();

    shutdownNotifier.shutdownIfNecessary();

    if (!successors.isEmpty()) {
      for (int i = 0; i < successors.size(); i++) {
        stats.numberOfTransitionsInThreadProduced.inc();
      }
      result.addAll(successors);

      stats.projectOperator.start();
      // Projection must be independent from child edge, so we may get only one
      AbstractState projection =
          applyOperator.project(pState, Iterables.getFirst(successors, null));
      if (projection != null) {
        result.add(projection);
        stats.numberOfProjectionsProduced.inc();
      }
      stats.projectOperator.stop();
    }
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
