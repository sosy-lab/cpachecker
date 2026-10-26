// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

class ConcurrentStopOperator implements StopOperator {

  private final ConfigurableProgramAnalysis wrappedCpa;
  private final StopOperator threadSpecificStop;
  private final Precision threadSpecificUnitPrecision;

  ConcurrentStopOperator(
      ConfigurableProgramAnalysis pWrappedCpa, ConfigurableProgramAnalysis pThreadSpecificCpa) {
    wrappedCpa = pWrappedCpa;
    threadSpecificStop = pThreadSpecificCpa.getStopOperator();
    try {
      threadSpecificUnitPrecision =
          pThreadSpecificCpa.getInitialPrecision(
              CFANode.newDummyCFANode(), StateSpacePartition.getDefaultPartition());
    } catch (InterruptedException e) {
      throw new IllegalArgumentException(
          "Could not get initial precision for thread-specific CPA", e);
    }
  }

  @Override
  public boolean stop(AbstractState state, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    if (state instanceof ConcurrentState concurrentState
        && precision instanceof ConcurrentPrecision concurrentPrecision) {
      ImmutableList.Builder<AbstractState> builder = ImmutableList.builder();
      for (AbstractState reachedState : reached) {
        if (reachedState instanceof ConcurrentState reachedConcurrentState
            && concurrentState.threads().size() == reachedConcurrentState.threads().size()
            && Objects.equals(
                concurrentState.liveThreadIds(), reachedConcurrentState.liveThreadIds())) {
          boolean allThreadsStop = true;
          for (var entry : concurrentState.threads.entrySet()) {
            int threadId = entry.getKey();
            ThreadState reachedThreadState = reachedConcurrentState.threads.get(threadId);
            if (reachedThreadState == null) {
              allThreadsStop = false;
              break;
            }
            ThreadState threadState = entry.getValue();
            if (!threadSpecificStop.stop(
                threadState.getWrappedState(),
                ImmutableList.of(reachedThreadState.getWrappedState()),
                threadSpecificUnitPrecision)) {
              allThreadsStop = false;
              break;
            }
          }
          if (allThreadsStop) {
            builder.add(reachedConcurrentState.getWrappedState());
          }
        }
      }
      return wrappedCpa
          .getStopOperator()
          .stop(
              concurrentState.getWrappedState(),
              builder.build(),
              concurrentPrecision.getWrappedPrecision());
    }
    return false;
  }
}
