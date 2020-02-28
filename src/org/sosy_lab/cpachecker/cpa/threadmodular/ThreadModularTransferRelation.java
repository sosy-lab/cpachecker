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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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
import org.sosy_lab.cpachecker.core.reachedset.ThreadModularReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "cpa.threadmodular")
public class ThreadModularTransferRelation implements TransferRelation {

  @Option(secure = true, description = "apply projections only if the state is relevant")
  private boolean relevanceOptimization = true;

  private final TransferRelation wrappedTransfer;
  private final ThreadModularStatistics stats;
  private final ShutdownNotifier shutdownNotifier;
  private final ApplyOperator applyOperator;

  public ThreadModularTransferRelation(
      TransferRelation pTransferRelation,
      ThreadModularStatistics pStats,
      ShutdownNotifier pShutdownNotifier,
      ApplyOperator pApplyOperator,
      Configuration pConfig)
      throws InvalidConfigurationException {

    pConfig.inject(this);
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

    Map<AbstractState, Precision> toAdd = new TreeMap<>();

    boolean isProjection = ((AbstractStateWithEdge) pState).isProjection();
    // do not need stop and merge as they has been already performed on projections

    if (isProjection || !relevanceOptimization || isRelevant(pState)) {

      stats.allApplyActions.start();
      Collection<AbstractState> toApply =
          ((ThreadModularReachedSet) pReached).getStatesForApply(pState);

      for (AbstractState oldState : toApply) {
        AbstractState appliedState = null;
        Precision appliedPrecision = null;
        if (isProjection) {
          if (!relevanceOptimization || isRelevant(oldState)) {
            stats.innerApply.start();
            appliedState = applyOperator.apply(oldState, pState);
            stats.applyCounter.inc();
            stats.innerApply.stop();
            appliedPrecision = pReached.getPrecision(oldState);
          }
        } else {
          stats.innerApply.start();
          appliedState = applyOperator.apply(pState, oldState);
          stats.applyCounter.inc();
          stats.innerApply.stop();
          appliedPrecision = pPrecision;
        }
        if (appliedState != null) {
          stats.relevantApplyCounter.inc();
          toAdd.put(appliedState, appliedPrecision);
        }
      }
      stats.allApplyActions.stop();
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

    List<AbstractState> result = new ArrayList<>();
    Collection<? extends AbstractState> successors;

    if (!isProjection) {
      stats.wrappedTransfer.start();
      successors = wrappedTransfer.getAbstractSuccessors(pState, pReached, pPrecision);
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
    }

    stats.envTransfer.start();
    for (Entry<AbstractState, Precision> applied : toAdd.entrySet()) {
      successors =
          wrappedTransfer.getAbstractSuccessors(applied.getKey(), pReached, applied.getValue());
      result.addAll(successors);
    }
    stats.envTransfer.stop();

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

  private boolean isRelevant(AbstractState pState) {
    return !applyOperator.isInvariantToEffects(pState)
        && applyOperator.canBeAnythingApplied(pState);
  }
}
