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
package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InfeasibleCounterexampleException;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "counterexample")
public class CounterexampleCheckAlgorithm
    implements Algorithm, StatisticsProvider, Statistics, ReachedSetUpdater {

  enum CounterexampleCheckerType {
    CBMC, CPACHECKER, CONCRETE_EXECUTION;
  }

  private final Algorithm algorithm;
  private final CounterexampleChecker checker;
  private final LogManager logger;

  private final Timer checkTime = new Timer();
  private int numberOfInfeasiblePaths = 0;

  private final Set<ARGState> checkedTargetStates = Collections.newSetFromMap(new WeakHashMap<>());

  @Option(secure=true, name="checker",
          description="Which model checker to use for verifying counterexamples as a second check.\n"
                    + "Currently CBMC or CPAchecker with a different config or the concrete execution \n"
                    + "checker can be used.")
  private CounterexampleCheckerType checkerType = CounterexampleCheckerType.CBMC;

  public CounterexampleCheckAlgorithm(Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger,
      ShutdownNotifier pShutdownNotifier, CFA cfa, String filename) throws InvalidConfigurationException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this);

    if (!(pCpa instanceof ARGCPA)) {
      throw new InvalidConfigurationException("ARG CPA needed for counterexample check");
    }

    switch (checkerType) {
    case CBMC:
      checker = new CBMCChecker(config, logger, cfa);
      break;
    case CPACHECKER:
      checker = new CounterexampleCPAChecker(config, logger, pShutdownNotifier, cfa, filename);
      break;
    case CONCRETE_EXECUTION:
      checker = new ConcretePathExecutionChecker(config, logger, cfa);
      break;
    default:
      throw new AssertionError("Unhandled case statement: " + checkerType);
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    while (reached.hasWaitingState()) {
      status = status.update(algorithm.run(reached));
      assert ARGUtils.checkARG(reached);

      ARGState lastState = (ARGState)reached.getLastState();

      final List<ARGState> errorStates;
      if (lastState != null && lastState.isTarget()) {
        errorStates =
            checkedTargetStates.contains(lastState)
                ? ImmutableList.of()
                : ImmutableList.of(lastState);
      } else {
        errorStates =
            from(reached)
                .transform(AbstractStates.toState(ARGState.class))
                .filter(AbstractStates.IS_TARGET_STATE)
                .filter(Predicates.not(Predicates.in(checkedTargetStates)))
                .toList();
      }

      if (errorStates.isEmpty()) {
        // no errors, so no analysis necessary
        break;
      }

      // check counterexample
      checkTime.start();
      try {
        List<ARGState> infeasibleErrorPaths = new ArrayList<>();
        boolean foundCounterexample = false;

        for (ARGState errorState : errorStates) {
          boolean counterexampleProvedFeasible = checkCounterexample(errorState, reached);
          if (counterexampleProvedFeasible) {
            checkedTargetStates.add(errorState);
            foundCounterexample = true;
            status = status.withPrecise(true);
          } else {
            infeasibleErrorPaths.add(errorState);
            status = status.withSound(false);
          }
        }

        if (foundCounterexample) {
          break;
        } else {
          assert !infeasibleErrorPaths.isEmpty();
          throw new InfeasibleCounterexampleException(
              "Error path found, but identified as infeasible by counterexample check with "
                  + checkerType
                  + ".",
              from(infeasibleErrorPaths).transform(ARGUtils::getOnePathTo).toList());
        }
      } finally {
        checkTime.stop();
      }
    }
    return status;
  }

  private boolean checkCounterexample(ARGState errorState, ReachedSet reached)
      throws InterruptedException {
    ARGState rootState = (ARGState)reached.getFirstState();

    Set<ARGState> statesOnErrorPath = ARGUtils.getAllStatesOnPathsTo(errorState);

    logger.log(Level.INFO, "Error path found, starting counterexample check with " + checkerType + ".");
    final boolean feasibility;
    try {
      feasibility = checker.checkCounterexample(rootState, errorState, statesOnErrorPath);
    } catch (CPAException e) {
      logger.logUserException(Level.WARNING, e, "Counterexample found, but feasibility could not be verified");
      return false;
    }

    if (feasibility) {
      logger.log(Level.INFO, "Error path found and confirmed by counterexample check with " + checkerType + ".");

    } else {
      numberOfInfeasiblePaths++;
      logger.log(Level.INFO, "Error path found but identified as infeasible.");
    }
    return feasibility;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(this);
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

    out.println("Number of counterexample checks:    " + checkTime.getNumberOfIntervals());
    if (checkTime.getNumberOfIntervals() > 0) {
      out.println("Number of infeasible paths:         " + numberOfInfeasiblePaths + " (" + toPercent(numberOfInfeasiblePaths, checkTime.getNumberOfIntervals()) +")" );
      out.println("Time for counterexample checks:     " + checkTime);
      if (checker instanceof Statistics) {
        ((Statistics)checker).printStatistics(out, pResult, pReached);
      }
    }
  }

  @Override
  public String getName() {
    return "Counterexample-Check Algorithm";
  }

  @Override
  public void register(ReachedSetUpdateListener pReachedSetUpdateListener) {
    if (algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).register(pReachedSetUpdateListener);
    }
  }

  @Override
  public void unregister(ReachedSetUpdateListener pReachedSetUpdateListener) {
    if (algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).unregister(pReachedSetUpdateListener);
    }
  }
}
