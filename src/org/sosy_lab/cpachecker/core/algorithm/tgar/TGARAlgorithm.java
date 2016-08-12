/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tgar;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.div;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult.CounterexampleInfoResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.tgar.comparator.DeeperLevelFirstComparator;
import org.sosy_lab.cpachecker.core.algorithm.tgar.interfaces.TestificationOperator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Refiner;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.TargetedRefiner;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonPrecision;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidComponentException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.presence.PresenceConditions;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

// GuidedRefinementAlgorithm
@Options(prefix="tgar")
public class TGARAlgorithm implements Algorithm, AlgorithmWithResult, StatisticsProvider {

  private static class TGARStatistics implements Statistics {

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
      return "TGAR algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result pResult,
        ReachedSet pReached) {

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

  private final TGARStatistics stats = new TGARStatistics();

  public static interface CEGARMXBean {
    int getNumberOfRefinements();
    int getSizeOfReachedSetBeforeLastRefinement();
    boolean isRefinementActive();
  }

  private class TGARMBean extends AbstractMBean implements CEGARMXBean {
    public TGARMBean() {
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

  @Option(secure=true, name="refiner", required = true,
      description = "Which refinement algorithm to use? "
      + "(give class name, required for TGAR) If the package name starts with "
      + "'org.sosy_lab.cpachecker.', this prefix can be omitted.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
  private Class<? extends TargetedRefiner> refiner = null;

  @Option(secure=true, name="comparator", required = true,
      description = "Which target state comparator to use? "
          + "(give class name, required for TGAR) If the package name starts with "
          + "'org.sosy_lab.cpachecker.core.algorithm.tgar.comparator.', this prefix can be omitted.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.tgar.comparator")
  private Class<? extends Comparator<ARGState>> comparatorClass = DeeperLevelFirstComparator.class;
  private final Comparator<ARGState> comparator;

  private final LogManager logger;
  private final Algorithm algorithm;
  private final TargetedRefiner mRefiner;
  private TestificationOperator testificationOp = null;

  private final Set<Integer> feasibleStateIds = Sets.newTreeSet();
  private Optional<ARGState> lastTargetState = Optional.absent();

  public TGARAlgorithm(Algorithm algorithm, ConfigurableProgramAnalysis pCpa, Configuration config, LogManager logger) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;

    comparator = createComparator();
    mRefiner = createInstance(pCpa);
    new TGARMBean(); // don't store it because we wouldn't know when to unregister anyway
  }

  public TGARAlgorithm(Algorithm algorithm, TargetedRefiner pRefiner, Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    this.algorithm = algorithm;
    this.logger = logger;
    mRefiner = Preconditions.checkNotNull(pRefiner);
    comparator = createComparator();
  }

  private Comparator<ARGState> createComparator() throws InvalidConfigurationException {
    return Classes.createInstance(Comparator.class, comparatorClass, new Class[] { }, new Object[] { });
  }

  // TODO Copied from CPABuilder, should be refactored into a generic implementation
  private TargetedRefiner createInstance(ConfigurableProgramAnalysis pCpa) throws CPAException,
                                                                                  InvalidConfigurationException {

    // get factory method
    Method factoryMethod;
    try {
      factoryMethod = refiner.getMethod("create", ConfigurableProgramAnalysis.class);
    } catch (NoSuchMethodException e) {
      throw new InvalidComponentException(refiner, "Refiner", "No public static method \"create\" with exactly one parameter of type ConfigurableProgramAnalysis.");
    }

    // verify signature
    if (!Modifier.isStatic(factoryMethod.getModifiers())) {
      throw new InvalidComponentException(refiner, "Refiner", "Factory method is not static");
    }

    String exception = Classes.verifyDeclaredExceptions(factoryMethod, CPAException.class, InvalidConfigurationException.class);
    if (exception != null) {
      throw new InvalidComponentException(refiner, "Refiner", "Factory method declares the unsupported checked exception " + exception + ".");
    }

    // invoke factory method
    Object refinerObj;
    try {
      refinerObj = factoryMethod.invoke(null, pCpa);

    } catch (IllegalAccessException e) {
      throw new InvalidComponentException(refiner, "Refiner", "Factory method is not public.");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, CPAException.class, InvalidConfigurationException.class);

      throw new UnexpectedCheckedException("instantiation of refiner " + refiner.getSimpleName(), cause);
    }

    if ((refinerObj == null) || !(refinerObj instanceof Refiner)) {
      throw new InvalidComponentException(refiner, "Refiner", "Factory method did not return a Refiner instance.");
    }

    return (TargetedRefiner)refinerObj;
  }

  public void setTestificationOp(TestificationOperator pTestificationOp) {
    testificationOp = pTestificationOp;
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {
    Preconditions.checkState(testificationOp != null, "A testification operator must be provided!");

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    int initialReachedSetSize = reached.size();
    boolean refinedInPreviousIteration = false;
    stats.totalTimer.start();
    try {
      // There might be unhandled target states left for refinement.
      //    This might be the case if there was a feasible path
      //    for that the algorithm returned.
      lastTargetState = chooseTarget(reached);

      do {
        if (lastTargetState.isPresent()) {
          Preconditions.checkState(AbstractStates.isTargetState(lastTargetState.get()));
          boolean counterexampleEliminated = refine(reached, lastTargetState.get());
          Set<SafetyProperty> targetProperties =
              AbstractStates.extractViolatedProperties(lastTargetState.get(), SafetyProperty.class);

          if (counterexampleEliminated) {
            logger.logf(Level.INFO, "Spurious CEX for: " + targetProperties);
          } else {
            logger.logf(Level.INFO, "Feasible CEX for: " + targetProperties);
            testificationOp.feasibleCounterexample(lastTargetState.get()
                .getCounterexampleInformation().get(), targetProperties);
          }
        }

        status = status.update(algorithm.run(reached));

        lastTargetState = chooseTarget(reached);

      } while (lastTargetState.isPresent() || reached.hasWaitingState());

    } finally {
      stats.totalTimer.stop();
    }
    return status;
  }

  @Override
  public AlgorithmResult getResult() {
    final Optional<CounterexampleInfo> info;
    if (lastTargetState.isPresent()) {
      ARGState e = (ARGState) lastTargetState.get();
      info = e.getCounterexampleInformation();
    } else {
      info = Optional.absent();
    }

    return new CounterexampleInfoResult(info);
  }

  private final Predicate<ARGState> STATE_NOT_YET_HANDLED = new Predicate<ARGState>() {
    @Override
    public boolean apply(@Nullable ARGState pAbstractState) {
      Preconditions.checkNotNull(pAbstractState);
      return !feasibleStateIds.contains(pAbstractState.getStateId());
    }
  };

  private final Predicate<ARGState> PROPERTY_NOT_BLACKLISTED = new Predicate<ARGState>() {
    @Override
    public boolean apply(@Nullable ARGState pAbstractState) {
      Preconditions.checkNotNull(pAbstractState);
      Preconditions.checkArgument(AbstractStates.isTargetState(pAbstractState));

      Set<SafetyProperty> violated =
          AbstractStates.extractViolatedProperties(pAbstractState, SafetyProperty.class);

      try {
        return !AutomatonPrecision.getGlobalPrecision()
            .areBlackListed(violated, PresenceConditions.manager().makeTrue());
      } catch (InterruptedException|CPAException pE) {
        throw new RuntimeException(pE);
      }
    }
  };

  private Optional<ARGState> chooseTarget(ReachedSet pReached) {
    ImmutableList<ARGState> rankedCandidates =
        from(pReached)
            .filter(IS_TARGET_STATE)
            .filter(ARGState.class)
            .filter(STATE_NOT_YET_HANDLED)
            .filter(PROPERTY_NOT_BLACKLISTED)
            .toSortedList(comparator);



    if (rankedCandidates.isEmpty()) {
      return Optional.absent();
    }

    return Optional.of(rankedCandidates.get(0));
  }


  @SuppressWarnings("NonAtomicVolatileUpdate") // statistics written only by one thread
  private boolean refine(ReachedSet reached, AbstractState target)
      throws CPAException, InterruptedException {

    logger.log(Level.FINE, "Counterexample found, performing TGAR");
    stats.countRefinements++;
    stats.totalReachedSizeBeforeRefinement += reached.size();
    stats.maxReachedSizeBeforeRefinement = Math.max(stats.maxReachedSizeBeforeRefinement, reached.size());
    sizeOfReachedSetBeforeRefinement = reached.size();

    stats.refinementTimer.start();
    final boolean counterexampleEliminated;
    try {
      counterexampleEliminated = mRefiner.performRefinement(reached, target);
      //  false: a FEASIBLE counterexample was found!
      //  true: a SPURIOUS counterexample was found; a refinement has been performed!

      if (counterexampleEliminated) {
        // An infeasible counterexample was found and eliminated.
      } else {
        Preconditions.checkArgument(target instanceof ARGState);
        ARGState e = (ARGState) target;
        feasibleStateIds.add(e.getStateId());
      }

    } catch (RefinementFailedException e) {
      stats.countFailedRefinements++;
      throw e;
    } finally {
      stats.refinementTimer.stop();
    }

    logger.log(Level.FINE, "Refinement successful:", counterexampleEliminated);

    if (counterexampleEliminated) {
      stats.countSuccessfulRefinements++;
      stats.totalReachedSizeAfterRefinement += reached.size();
      stats.maxReachedSizeAfterRefinement = Math.max(stats.maxReachedSizeAfterRefinement, reached.size());
    }

    return counterexampleEliminated;
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

  public TargetedRefiner getRefiner() {
    return mRefiner;
  }

}
