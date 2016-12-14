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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdateListener;
import org.sosy_lab.cpachecker.core.algorithm.ParallelAlgorithm.ReachedSetUpdater;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.value.refiner.UnsoundRefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;

@Options(prefix = "cegar")
public class CEGARAlgorithm implements Algorithm, StatisticsProvider, ReachedSetUpdater {

  private static class CEGARStatistics implements Statistics {

    private final Timer totalTimer = new Timer();
    private final Timer refinementTimer = new Timer();

    @SuppressFBWarnings(value = "VO_VOLATILE_INCREMENT",
        justification = "only one thread writes, others read")
    private volatile int countRefinements = 0;
    private int countSuccessfulRefinements = 0;
    private int countFailedRefinements = 0;

    private int maxReachedSizeBeforeRefinement = 0;
    private int maxReachedSizeAfterRefinement = 0;
    private long totalReachedSizeBeforeRefinement = 0;
    private long totalReachedSizeAfterRefinement = 0;

    @Override
    public String getName() {
      return "CEGAR algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

      out.println("Number of refinements:                " + countRefinements);

      if (countRefinements > 0) {
        out.println("Number of successful refinements:     " + countSuccessfulRefinements);
        out.println("Number of failed refinements:         " + countFailedRefinements);
        out.println("Max. size of reached set before ref.: " + maxReachedSizeBeforeRefinement);
        out.println("Max. size of reached set after ref.:  " + maxReachedSizeAfterRefinement);
        out.println("Avg. size of reached set before ref.: " + div(totalReachedSizeBeforeRefinement, countRefinements));
        out.println("Avg. size of reached set after ref.:  " + div(totalReachedSizeAfterRefinement, countSuccessfulRefinements));
        out.println("");
        out.println("Total time for CEGAR algorithm:   " + totalTimer);
        out.println("Time for refinements:             " + refinementTimer);
        out.println("Average time for refinement:      " + refinementTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
        out.println("Max time for refinement:          " + refinementTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
      }
    }
  }

  private final CEGARStatistics stats = new CEGARStatistics();

  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners =
      new CopyOnWriteArrayList<>();

  public static interface CEGARMXBean {
    int getNumberOfRefinements();
    int getSizeOfReachedSetBeforeLastRefinement();
    boolean isRefinementActive();
  }

  private class CEGARMBean extends AbstractMBean implements CEGARMXBean {
    public CEGARMBean() {
      super("org.sosy_lab.cpachecker:type=CEGAR", logger);
      register();
    }

    @Override
    public int getNumberOfRefinements() {
      return stats.countRefinements;
    }

    @Override
    public int getSizeOfReachedSetBeforeLastRefinement() {
      return sizeOfReachedSetBeforeRefinement;
    }

    @Override
    public boolean isRefinementActive() {
      return stats.refinementTimer.isRunning();
    }
  }

  private volatile int sizeOfReachedSetBeforeRefinement = 0;

  @Option(
    secure = true,
    name = "refiner",
    required = true,
    description =
        "Which refinement algorithm to use? "
            + "(give class name, required for CEGAR) If the package name starts with "
            + "'org.sosy_lab.cpachecker.', this prefix can be omitted."
  )
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
  private Refiner.Factory refinerFactory;

  @Option(secure=true, name="globalRefinement", description="Whether to do refinement immediately after finding an error state, or globally after the ARG has been unrolled completely.")
  private boolean globalRefinement = false;

  private final LogManager logger;
  private final Algorithm algorithm;
  private final Refiner mRefiner;

  public CEGARAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    verifyNotNull(refinerFactory);
    this.algorithm = algorithm;
    this.logger = logger;

    mRefiner = refinerFactory.create(pCpa);
    new CEGARMBean(); // don't store it because we wouldn't know when to unregister anyway
  }

  /**
   * This constructor gets a Refiner object instead of generating it
   * from the refiner parameter.
   */
  public CEGARAlgorithm(Algorithm algorithm, Refiner pRefiner, Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;
    mRefiner = Preconditions.checkNotNull(pRefiner);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    int initialReachedSetSize = reached.size();
    boolean refinedInPreviousIteration = false;
    stats.totalTimer.start();
    try {
      boolean refinementSuccessful;
      do {
        refinementSuccessful = false;
        final AbstractState previousLastState = reached.getLastState();

        // run algorithm
        status = status.update(algorithm.run(reached));
        notifyReachedSetUpdateListeners(reached);

        // if there is any target state do refinement
        if (refinementNecessary(reached, previousLastState)) {
          refinementSuccessful = refine(reached);
          refinedInPreviousIteration = true;
          // assert that reached set is free of target states,
          // if refinement was successful and initial reached set was empty (i.e. stopAfterError=true)
          if (refinementSuccessful && initialReachedSetSize == 1) {
            assert !from(reached).anyMatch(IS_TARGET_STATE) : "Target state should not be present"
                + " in the reached set after refinement.";
          }
        }

        // restart exploration for unsound refiners, as due to unsound refinement
        // a sound over-approximation has to be found for proving safety
        else if(mRefiner instanceof UnsoundRefiner) {
          if (!refinedInPreviousIteration) {
            break;
          }

          ((UnsoundRefiner)mRefiner).forceRestart(reached);
          refinementSuccessful        = true;
          refinedInPreviousIteration  = false;
        }

      } while (refinementSuccessful);

    } finally {
      stats.totalTimer.stop();
    }
    return status;
  }

  private boolean refinementNecessary(ReachedSet reached, AbstractState previousLastState) {
    if (globalRefinement) {
      // check other states
      return from(reached).anyMatch(IS_TARGET_STATE);

    } else {
      // Check only last state, but only if it is different from the last iteration.
      // Otherwise we would attempt to refine the same state twice if CEGARAlgorithm.run
      // is called again but this time the inner algorithm does not find any successor states.
      return !Objects.equals(reached.getLastState(), previousLastState)
          && isTargetState(reached.getLastState());
    }
  }

  @SuppressWarnings("NonAtomicVolatileUpdate") // statistics written only by one thread
  private boolean refine(ReachedSet reached) throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Error found, performing CEGAR");
    stats.countRefinements++;
    stats.totalReachedSizeBeforeRefinement += reached.size();
    stats.maxReachedSizeBeforeRefinement = Math.max(stats.maxReachedSizeBeforeRefinement, reached.size());
    sizeOfReachedSetBeforeRefinement = reached.size();

    stats.refinementTimer.start();
    boolean refinementResult;
    try {
      refinementResult = mRefiner.performRefinement(reached);

    } catch (RefinementFailedException e) {
      stats.countFailedRefinements++;
      throw e;
    } finally {
      stats.refinementTimer.stop();
    }

    logger.log(Level.FINE, "Refinement successful:", refinementResult);

    if (refinementResult) {
      stats.countSuccessfulRefinements++;
      stats.totalReachedSizeAfterRefinement += reached.size();
      stats.maxReachedSizeAfterRefinement = Math.max(stats.maxReachedSizeAfterRefinement, reached.size());
    }

    return refinementResult;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    if (mRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider)mRefiner).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  @Override
  public void register(ReachedSetUpdateListener pReachedSetUpdateListener) {
    if (algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).register(pReachedSetUpdateListener);
    }
    reachedSetUpdateListeners.add(pReachedSetUpdateListener);
  }

  @Override
  public void unregister(ReachedSetUpdateListener pReachedSetUpdateListener) {
    if (algorithm instanceof ReachedSetUpdater) {
      ((ReachedSetUpdater) algorithm).unregister(pReachedSetUpdateListener);
    }
    reachedSetUpdateListeners.remove(pReachedSetUpdateListener);
  }

  private void notifyReachedSetUpdateListeners(ReachedSet pReachedSet) {
    for (ReachedSetUpdateListener rsul : reachedSetUpdateListeners) {
      rsul.updated(pReachedSet);
    }
  }

}
