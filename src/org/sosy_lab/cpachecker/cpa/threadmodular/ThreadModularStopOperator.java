/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.threadmodular;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.EpsilonState;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.IOStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ThreadModularStopOperator implements StopOperator {

  private final StopOperator stateStop;
  private final IOStopOperator ioStop;
  private final ThreadModularStatistics tStats;

  public ThreadModularStopOperator(
      StopOperator pStateStop,
      IOStopOperator pIOStop,
      ThreadModularStatistics pStatistics) {
    stateStop = pStateStop;
    ioStop = pIOStop;
    tStats = pStatistics;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    ThreadModularState targetTMState = (ThreadModularState) pState;
    AbstractState targetState = targetTMState.getWrappedState();
    InferenceObject targetIO = targetTMState.getInferenceObject();

    if (targetIO == EmptyInferenceObject.getInstance()) {
      return true;
    }

    try {
      tStats.stopTimer.start();
      ARGState argState = null;

      if (targetState != EpsilonState.getInstance()) {

        tStats.stateExtractTimer.start();
        Collection<ARGState> innerStates =
            from(pReached).transform(s -> ((ThreadModularState) s).getWrappedState())
                .filter(s -> s != EpsilonState.getInstance())
                .transform(s -> (ARGState) s)
                .toSet();
        tStats.stateExtractTimer.stop();

        argState = (ARGState) targetState;
        tStats.stateCheckTimer.start();
        boolean result = checkTheState(argState, innerStates, pPrecision);
        tStats.stateCheckTimer.stop();

        if (targetIO == TauInferenceObject.getInstance() || !result) {
          return result;
        }
      }

      tStats.ioExtractTimer.start();
      Collection<InferenceObject> innerObjects =
          from(pReached)
              .transform(s -> ((ThreadModularState) s).getInferenceObject())
              .filter(
                  s -> s != TauInferenceObject.getInstance()
                      && s != EmptyInferenceObject.getInstance())
              .toSet();
      tStats.ioExtractTimer.stop();

      tStats.ioCheckTimer.start();
      boolean result = ioStop.stop(targetIO, innerObjects, pPrecision);
      tStats.ioCheckTimer.stop();
      // Do not remove covered states from arg
      /*
       * if (result && argState != null && !argState.isDestroyed()) { argState.removeFromARG(); }
       */
      return result;
    } finally {
      tStats.stopTimer.stop();
    }
  }

  private boolean stop(ARGState pElement, ARGState pReachedState, Precision pPrecision)
      throws CPAException, InterruptedException {

    if (!pReachedState.mayCover()) {
      return false;
    }
    if (pElement.isCovered()) {
      return true;
    }
    if (pElement.isOlderThan(pReachedState)) {
      // This is never the case in usual predicate abstraction,
      // but possibly with other algorithms
      // Checking this also implies that pElement gets not covered by
      // one of its children (because they are all newer than pElement).
      return false;
    }

    AbstractState wrappedState = pElement.getWrappedState();
    AbstractState wrappedReachedState = pReachedState.getWrappedState();

    boolean stop =
        stateStop.stop(wrappedState, Collections.singleton(wrappedReachedState), pPrecision);

    if (stop) {
      pElement.setCovered(pReachedState);
    }
    return stop;
  }

  private boolean
      checkTheState(ARGState argState, Collection<ARGState> pReached, Precision pPrecision)
          throws CPAException, InterruptedException {

    if (argState.isCovered()) {
      return true;
    }

    if (argState.getMergedWith() != null) {
      ARGState mergedWith = argState.getMergedWith();
      if (pReached.contains(mergedWith)) {
        // we do this single check first as it should return true in most of the cases

        if (stateStop.stop(
            argState.getWrappedState(),
            Collections.singleton(mergedWith.getWrappedState()),
            pPrecision)) {
          argState.setCovered(mergedWith);
        }
        return true;
      }
    }

    // Check if the argElement has only one parent and remember it for later:
    ARGState parent = null;
    if (argState.getParents().size() == 1) {
      parent = Iterables.get(argState.getParents(), 0);
    }

    for (ARGState argReachedState : pReached) {
      if (!argReachedState.isDestroyed()
          && argReachedState != argState
          && stop(argState, argReachedState, pPrecision)) {
        if (parent != null && argReachedState.getParents().contains(parent)) {
          // if the covering state has the same parent as the covered state
          // and if the covered state has no other parents,
          // it should always be safe to remove the covered state:
          return true;
        }
      }
    }
    return false;
  }
}
