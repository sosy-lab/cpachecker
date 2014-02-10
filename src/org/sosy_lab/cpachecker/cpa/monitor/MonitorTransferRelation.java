/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.monitor.MonitorState.TimeoutState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.assumptions.PreventingHeuristic;

import com.google.common.base.Throwables;

@Options(prefix="cpa.monitor")
public class MonitorTransferRelation implements TransferRelation {

  long maxTotalTimeForPath = 0;
  final Timer totalTimeOfTransfer = new Timer();

  @Option(name="limit", description="time limit for a single post computation (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=0)
  private long timeLimit = 0; // given in milliseconds

  @Option(name="pathcomputationlimit", description="time limit for all computations on a path in milliseconds (use milliseconds or specify a unit; 0 for infinite)")
  @TimeSpanOption(codeUnit=TimeUnit.MILLISECONDS,
      defaultUserUnit=TimeUnit.MILLISECONDS,
      min=0)
  private long timeLimitForPath = 0;

  private final TransferRelation transferRelation;

  private final ExecutorService executor;

  public MonitorTransferRelation(ConfigurableProgramAnalysis pWrappedCPA,
      Configuration config) throws InvalidConfigurationException {
    config.inject(this);

    transferRelation = pWrappedCPA.getTransferRelation();

    if (timeLimit == 0) {
      executor = null;
    } else {
      // important to use daemon threads here, because we never have the chance to stop the executor
      executor = Executors.newSingleThreadExecutor(Threads.threadFactoryBuilder().setDaemon(true).build());
    }
  }

  @Override
  public Collection<MonitorState> getAbstractSuccessors(
      AbstractState pElement, final Precision pPrecision, final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    final MonitorState element = (MonitorState)pElement;

    if (element.getWrappedState() == TimeoutState.INSTANCE) {
      // cannot compute a successor
      return Collections.emptySet();
    }

    totalTimeOfTransfer.start();

    TransferCallable tc = new TransferCallable() {
      @Override
      public Collection<? extends AbstractState> call() throws CPATransferException, InterruptedException {
        assert !(element.getWrappedState() instanceof MonitorState) : element;
        return transferRelation.getAbstractSuccessors(element.getWrappedState(), pPrecision, pCfaEdge);
      }
    };

    Pair<PreventingHeuristic, Long> preventingCondition = null;

    Collection<? extends AbstractState> successors;
    if (timeLimit == 0) {
      successors = tc.call();
    } else {

      Future<Collection<? extends AbstractState>> future = executor.submit(tc);
      try {
        // here we get the result of the post computation but there is a time limit
        // given to complete the task specified by timeLimit
        successors = future.get(timeLimit, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        preventingCondition = Pair.of(PreventingHeuristic.SUCCESSORCOMPTIME, timeLimit);

        // add dummy successor
        successors = Collections.singleton(TimeoutState.INSTANCE);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // TODO handle InterruptedException better
        preventingCondition = Pair.of(PreventingHeuristic.SUCCESSORCOMPTIME, timeLimit);

        // add dummy successor
        successors = Collections.singleton(TimeoutState.INSTANCE);

      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPATransferException.class);
        // TransferRelation.getAbstractSuccessors() threw unexpected checked exception!
        throw new UnexpectedCheckedException("transfer relation", e.getCause());
      }
    }

    // update time information
    totalTimeOfTransfer.stop();
    long timeOfExecution = totalTimeOfTransfer.getLengthOfLastInterval().asMillis();
    long totalTimeOnPath = element.getTotalTimeOnPath() + timeOfExecution;

    if (totalTimeOnPath > maxTotalTimeForPath) {
      maxTotalTimeForPath = totalTimeOnPath;
    }

    //     return if there are no successors
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    // check for violation of limits
    if (preventingCondition == null && timeLimitForPath > 0 && totalTimeOnPath > timeLimitForPath) {
        preventingCondition = Pair.of(PreventingHeuristic.PATHCOMPTIME, timeLimitForPath);
    }

    // wrap elements
    List<MonitorState> wrappedSuccessors = new ArrayList<>(successors.size());
    for (AbstractState absElement : successors) {
      MonitorState successorElem = new MonitorState(absElement, totalTimeOnPath, preventingCondition);

      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement,
      final List<AbstractState> otherElements, final CFAEdge cfaEdge,
      final Precision precision) throws CPATransferException, InterruptedException {
    final MonitorState element = (MonitorState)pElement;

    if (element.getWrappedState() == TimeoutState.INSTANCE) {
      // ignore strengthen
      return null;
    }

    totalTimeOfTransfer.start();

    TransferCallable sc = new TransferCallable() {
      @Override
      public Collection<? extends AbstractState> call() throws CPATransferException, InterruptedException {
        return transferRelation.strengthen(element.getWrappedState(), otherElements, cfaEdge, precision);
      }
    };

    Pair<PreventingHeuristic, Long> preventingCondition = null;

    Collection<? extends AbstractState> successors;
    if (timeLimit == 0) {
      successors = sc.call();
    } else {
      Future<Collection<? extends AbstractState>> future = executor.submit(sc);
      try {
        // here we get the result of the post computation but there is a time limit
        // given to complete the task specified by timeLimit
        successors = future.get(timeLimit, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        preventingCondition = Pair.of(PreventingHeuristic.SUCCESSORCOMPTIME, timeLimit);

        // add dummy successor
        successors = Collections.singleton(TimeoutState.INSTANCE);

      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        // TODO handle InterruptedException better
        preventingCondition = Pair.of(PreventingHeuristic.SUCCESSORCOMPTIME, timeLimit);

        // add dummy successor
        successors = Collections.singleton(TimeoutState.INSTANCE);

      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPATransferException.class);
        // TransferRelation.strengthen() threw unexpected checked exception!
        throw new UnexpectedCheckedException("strengthen", e.getCause());
      }
    }

    // update time information
    totalTimeOfTransfer.stop();
    long timeOfExecution = totalTimeOfTransfer.getLengthOfLastInterval().asMillis();
    long totalTimeOnPath = element.getTotalTimeOnPath() + timeOfExecution;

    if (totalTimeOnPath > maxTotalTimeForPath) {
      maxTotalTimeForPath = totalTimeOnPath;
    }

    // if the returned list is null return null
    if (successors == null) {
      // wrapped strengthen didn't do anything, but we need to update totalTimeOnPath
      successors = Collections.singleton(element.getWrappedState());
    }

    // return if there are no successors
    if (successors.isEmpty()) {
      return Collections.emptySet();
    }

    // no need to update path length information here

    // check for violation of limits
    if (preventingCondition == null) {
      if (timeLimitForPath > 0 && totalTimeOnPath > timeLimitForPath) {
        preventingCondition = Pair.of(PreventingHeuristic.PATHCOMPTIME, timeLimitForPath);
      }
    }

    // wrap elements
    List<MonitorState> wrappedSuccessors = new ArrayList<>(successors.size());
    for (AbstractState absElement : successors) {
      MonitorState successorElem = new MonitorState(
          absElement, totalTimeOnPath, preventingCondition);

      wrappedSuccessors.add(successorElem);
    }
    return wrappedSuccessors;
  }

  private static interface TransferCallable extends Callable<Collection<? extends AbstractState>> {
    @Override
    public Collection<? extends AbstractState> call() throws CPATransferException, InterruptedException;
  }
}