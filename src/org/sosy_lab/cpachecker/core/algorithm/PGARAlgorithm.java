// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Verify.verifyNotNull;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.CPAs;

public class PGARAlgorithm
    implements Algorithm, StatisticsProvider, ReachedSetUpdater, AutoCloseable {

  @Options(prefix = "pgar")
  public static class PGARAlgorithmFactory implements AlgorithmFactory {

    @Option(
        secure = true,
        name = "refiner",
        required = true,
        description =
            "Which refinement algorithm to use? "
                + "(give class name, required for PGAR) If the package name starts with "
                + "'org.sosy_lab.cpachecker.', this prefix can be omitted.")
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    private Refiner.Factory refinerFactory;

    /*
     * Widely used in CPALockator, as there are many error paths, and refinement all of them takes
     * too much time, so, limit refinement iterations and remove at least some infeasible paths
     */
    @Option(
        name = "maxIterations",
        description = "Max number of refinement iterations, -1 for no limit")
    private int maxRefinementNum = -1;

    private final AlgorithmFactory algorithmFactory;
    private final LogManager logger;
    private final Refiner refiner;

    public PGARAlgorithmFactory(
        Algorithm pAlgorithm,
        ConfigurableProgramAnalysis pCpa,
        LogManager pLogger,
        Configuration pConfig,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException {
      this(() -> pAlgorithm, pCpa, pLogger, pConfig, pShutdownNotifier);
    }

    public PGARAlgorithmFactory(
        AlgorithmFactory pAlgorithmFactory,
        ConfigurableProgramAnalysis pCpa,
        LogManager pLogger,
        Configuration pConfig,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException {
      pConfig.inject(this);
      algorithmFactory = pAlgorithmFactory;
      logger = pLogger;
      verifyNotNull(refinerFactory);
      refiner = refinerFactory.create(pCpa, pLogger, pShutdownNotifier);
    }

    @Override
    public PGARAlgorithm newInstance() {
      return new PGARAlgorithm(algorithmFactory.newInstance(), refiner, logger, maxRefinementNum);
    }
  }

  private static class PGARStatistics implements Statistics {

    private final Timer totalTimer = new Timer();
    private final Timer refinementTimer = new Timer();

    @SuppressFBWarnings(
        value = "VO_VOLATILE_INCREMENT",
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
      return "PGAR algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

      out.println("Number of PGAR refinements:          " + countRefinements);

      if (countRefinements > 0) {
        out.println("Number of successful refinements:     " + countSuccessfulRefinements);
        out.println("Number of failed refinements:         " + countFailedRefinements);
        out.println("Max. size of reached set before ref.: " + maxReachedSizeBeforeRefinement);
        out.println("Max. size of reached set after ref.:  " + maxReachedSizeAfterRefinement);
        out.println(
            "Avg. size of reached set before ref.: "
                + div(totalReachedSizeBeforeRefinement, countRefinements));
        out.println(
            "Avg. size of reached set after ref.:  "
                + div(totalReachedSizeAfterRefinement, countSuccessfulRefinements));
        out.println("");
        out.println("Total time for CEGAR algorithm:   " + totalTimer);
        out.println("Time for refinements:             " + refinementTimer);
        out.println(
            "Average time for refinement:      "
                + refinementTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
        out.println(
            "Max time for refinement:          "
                + refinementTimer.getMaxTime().formatAs(TimeUnit.SECONDS));
      }
    }
  }

  private final PGARStatistics stats = new PGARStatistics();

  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners =
      new CopyOnWriteArrayList<>();

  private int maxRefinementNum = -1;

  private final LogManager logger;
  private final Algorithm algorithm;
  private final Refiner mRefiner;

  private PGARAlgorithm(
      Algorithm pAlgorithm,
      Refiner pRefiner,
      LogManager pLogger,
      int pMaxRefinementNum) {
    algorithm = pAlgorithm;
    mRefiner = Preconditions.checkNotNull(pRefiner);
    logger = pLogger;
    maxRefinementNum = pMaxRefinementNum;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    stats.totalTimer.start();
    try {
      boolean refinementSuccessful;
      do {
        refinementSuccessful = false;

        // run algorithm
        status = status.update(algorithm.run(reached));
        notifyReachedSetUpdateListeners(reached);

        if (stats.countRefinements == maxRefinementNum) {
          logger.log(
              Level.WARNING,
              "Aborting analysis because maximum number of refinements "
                  + maxRefinementNum
                  + " used");
          status = status.withPrecise(false);
          break;
        }

        // if there is a proof do a refinement
        if (refinementNecessary(reached)) {
          refinementSuccessful = refine(reached);
        }

        // restart exploration for unprecise refiners, since currently no such refiners exist ignore
        // this
      } while (refinementSuccessful);

    } finally {
      stats.totalTimer.stop();
    }
    return status;
  }

  private boolean refinementNecessary(ReachedSet reached) {
    // If the waitlist is empty we have achieved a fixpoint
    // This means that if there is no error state, then we have a proof
    return reached.getWaitlist().size() == 0 && !reached.wasTargetReached();
  }

  @SuppressWarnings("NonAtomicVolatileUpdate") // statistics written only by one thread
  @SuppressFBWarnings(
      value = "VO_VOLATILE_INCREMENT",
      justification = "only one thread writes countRefinements, others read")
  private boolean refine(ReachedSet reached) throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Proof found, performing PGAR");
    stats.countRefinements++;
    stats.totalReachedSizeBeforeRefinement += reached.size();
    stats.maxReachedSizeBeforeRefinement =
        Math.max(stats.maxReachedSizeBeforeRefinement, reached.size());

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
      stats.maxReachedSizeAfterRefinement =
          Math.max(stats.maxReachedSizeAfterRefinement, reached.size());
    }

    return refinementResult;
  }

  @SuppressWarnings("unused")
  private void notifyReachedSetUpdateListeners(ReachedSet pReachedSet) {
    for (ReachedSetUpdateListener rsul : reachedSetUpdateListeners) {
      rsul.updated(pReachedSet);
    }
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

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(pStatsCollection);
    }
    if (mRefiner instanceof StatisticsProvider) {
      ((StatisticsProvider) mRefiner).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  @Override
  public void close() {
    CPAs.closeIfPossible(mRefiner, logger);
  }
}