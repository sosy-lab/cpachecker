// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.counterexamplecheck;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.toPercent;

import com.google.common.base.Predicates;
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
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.predicate.SlicingAbstractionsUtils;
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

  @Option(secure=true, name="ambigiousARG",
      description="True if the path to the error state can not always be uniquely determined from the ARG.\n"
                + "This is the case e.g. for Slicing Abstractions, where the abstraction states in the ARG\n"
                + "do not form a tree!")
  private boolean ambigiousARG = false;

  public CounterexampleCheckAlgorithm(
      Algorithm algorithm,
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      Specification pSpecification,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier,
      CFA cfa)
      throws InvalidConfigurationException {
    this.algorithm = algorithm;
    this.logger = logger;
    config.inject(this, CounterexampleCheckAlgorithm.class);

    if (!(pCpa instanceof ARGCPA || pCpa instanceof BAMCPA)) {
      throw new InvalidConfigurationException("ARG CPA needed for counterexample check");
    }

    switch (checkerType) {
    case CBMC:
      checker = new CBMCChecker(config, logger, cfa);
      break;
    case CPACHECKER:
      AssumptionToEdgeAllocator assumptionToEdgeAllocator =
          AssumptionToEdgeAllocator.create(config, logger, cfa.getMachineModel());
      checker =
          new CounterexampleCPAchecker(
              config,
              pSpecification,
              logger,
              pShutdownNotifier,
              cfa,
              s ->
                  ARGUtils.tryGetOrCreateCounterexampleInformation(
                      s, pCpa, assumptionToEdgeAllocator));
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

      final List<ARGState> errorStates =
          from(reached)
              .transform(AbstractStates.toState(ARGState.class))
              .filter(AbstractStates::isTargetState)
              .filter(Predicates.not(Predicates.in(checkedTargetStates)))
              .toList();

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
              transformedImmutableListCopy(infeasibleErrorPaths, ARGUtils::getOnePathTo));
        }
      } finally {
        checkTime.stop();
      }
    }
    return status;
  }

  private boolean checkCounterexample(ARGState errorState, ReachedSet reached)
      throws InterruptedException {

    logger.log(Level.INFO, "Error path found, starting counterexample check with " + checkerType + ".");
    final boolean feasibility;
    try {
      feasibility = checkErrorPaths(checker, errorState, reached);
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

  /**
   * check whether there is a feasible counterexample in the reachedset.
   *
   * @param pChecker executes a precise counterexample-check
   * @param errorState where the counterexample ends
   * @param reached all reached states of the analysis, some of the states are part of the CEX path
   */
  protected boolean checkErrorPaths(
      CounterexampleChecker pChecker, ARGState errorState, ReachedSet reached)
      throws CPAException, InterruptedException {

    ARGState rootState = (ARGState) reached.getFirstState();
    Set<ARGState> statesOnErrorPath;
    if (ambigiousARG) {
      statesOnErrorPath = SlicingAbstractionsUtils.getStatesOnErrorPath(errorState);
    } else {
      statesOnErrorPath = ARGUtils.getAllStatesOnPathsTo(errorState);
    }

    return pChecker.checkCounterexample(rootState, errorState, statesOnErrorPath);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(this);
    if (checker instanceof StatisticsProvider) {
      ((StatisticsProvider) checker).collectStatistics(pStatsCollection);
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

    out.println("Number of counterexample checks:    " + checkTime.getNumberOfIntervals());
    if (checkTime.getNumberOfIntervals() > 0) {
      out.println("Number of infeasible paths:         " + numberOfInfeasiblePaths + " (" + toPercent(numberOfInfeasiblePaths, checkTime.getNumberOfIntervals()) +")" );
      out.println("Time for counterexample checks:     " + checkTime);
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
