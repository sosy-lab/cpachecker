// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Verify.verifyNotNull;
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
import org.sosy_lab.cpachecker.util.CPAs;

public class CEGARAlgorithm
    implements Algorithm, StatisticsProvider, ReachedSetUpdater, AutoCloseable {

  private static class CEGARStatistics implements Statistics {

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
      return "CEGAR algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult, UnmodifiableReachedSet pReached) {

      out.println("Number of CEGAR refinements:          " + countRefinements);

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

  private final CEGARStatistics stats = new CEGARStatistics();

  private final List<ReachedSetUpdateListener> reachedSetUpdateListeners =
      new CopyOnWriteArrayList<>();

  public interface CEGARMXBean {
    int getNumberOfRefinements();

    int getSizeOfReachedSetBeforeLastRefinement();

    boolean isRefinementActive();
  }

  private class CEGARMBean extends AbstractMBean implements CEGARMXBean {
    public CEGARMBean() {
      super("org.sosy_lab.cpachecker:type=CEGAR", logger);
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

  @Options(prefix = "cegar")
  public static class CEGARAlgorithmFactory implements AlgorithmFactory {

    @Option(
        secure = true,
        name = "refiner",
        required = true,
        description =
            "Which refinement algorithm to use? "
                + "(give class name, required for CEGAR) If the package name starts with "
                + "'org.sosy_lab.cpachecker.', this prefix can be omitted.")
    @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
    private Refiner.Factory refinerFactory;

    @Option(
        secure = true,
        name = "globalRefinement",
        description =
            "Whether to do refinement immediately after finding an error state, or globally after"
                + " the ARG has been unrolled completely.")
    private boolean globalRefinement = false;

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

    public CEGARAlgorithmFactory(
        Algorithm pAlgorithm,
        ConfigurableProgramAnalysis pCpa,
        LogManager pLogger,
        Configuration pConfig,
        ShutdownNotifier pShutdownNotifier)
        throws InvalidConfigurationException {
      this(() -> pAlgorithm, pCpa, pLogger, pConfig, pShutdownNotifier);
    }

    public CEGARAlgorithmFactory(
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
    public CEGARAlgorithm newInstance() {
      return new CEGARAlgorithm(
          algorithmFactory.newInstance(), refiner, logger, globalRefinement, maxRefinementNum);
    }
  }

  private volatile int sizeOfReachedSetBeforeRefinement = 0;
  private boolean globalRefinement = false;
  private int maxRefinementNum = -1;

  private final LogManager logger;
  private final Algorithm algorithm;
  private final Refiner mRefiner;

  /** This constructor gets a Refiner object instead of generating it from the refiner parameter. */
  private CEGARAlgorithm(
      Algorithm pAlgorithm,
      Refiner pRefiner,
      LogManager pLogger,
      boolean pGlobalRefinement,
      int pMaxRefinementNum) {
    algorithm = pAlgorithm;
    mRefiner = Preconditions.checkNotNull(pRefiner);
    logger = pLogger;
    globalRefinement = pGlobalRefinement;
    maxRefinementNum = pMaxRefinementNum;

    // don't store it because we wouldn't know when to unregister anyway
    new CEGARMBean().register();
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

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

        if (stats.countRefinements == maxRefinementNum) {
          logger.log(
              Level.WARNING,
              "Aborting analysis because maximum number of refinements "
                  + maxRefinementNum
                  + " used");
          status = status.withPrecise(false);
          break;
        }

        // if there is any target state do refinement
        if (refinementNecessary(reached, previousLastState)) {
          refinementSuccessful = refine(reached);
          refinedInPreviousIteration = true;
          // Note, with special options reached set still contains violated properties
          // i.e (stopAfterError = true) or race conditions analysis
        }

        // restart exploration for unsound refiners, as due to unsound refinement
        // a sound over-approximation has to be found for proving safety
        else if (mRefiner instanceof UnsoundRefiner) {
          if (!refinedInPreviousIteration) {
            break;
          }

          ((UnsoundRefiner) mRefiner).forceRestart(reached);
          refinementSuccessful = true;
          refinedInPreviousIteration = false;
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
      return reached.wasTargetReached();

    } else {
      // Check only last state, but only if it is different from the last iteration.
      // Otherwise we would attempt to refine the same state twice if CEGARAlgorithm.run
      // is called again but this time the inner algorithm does not find any successor states.
      return !Objects.equals(reached.getLastState(), previousLastState)
          && isTargetState(reached.getLastState());
    }
  }

  @SuppressWarnings("NonAtomicVolatileUpdate") // statistics written only by one thread
  @SuppressFBWarnings(
      value = "VO_VOLATILE_INCREMENT",
      justification = "only one thread writes countRefinements, others read")
  private boolean refine(ReachedSet reached) throws CPAException, InterruptedException {
    logger.log(Level.FINE, "Error found, performing CEGAR");
    stats.countRefinements++;
    stats.totalReachedSizeBeforeRefinement += reached.size();
    stats.maxReachedSizeBeforeRefinement =
        Math.max(stats.maxReachedSizeBeforeRefinement, reached.size());
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
      stats.maxReachedSizeAfterRefinement =
          Math.max(stats.maxReachedSizeAfterRefinement, reached.size());
    }

    return refinementResult;
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

  @Override
  public void close() {
    CPAs.closeIfPossible(mRefiner, logger);
  }
}
