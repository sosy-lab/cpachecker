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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;

import org.eclipse.cdt.core.index.IPDOMASTProcessor.Abstract;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmResult.CounterexampleInfoResult;
import org.sosy_lab.cpachecker.core.algorithm.AlgorithmWithResult;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats;
import org.sosy_lab.cpachecker.core.algorithm.mpa.PropertyStats.StatHandle;
import org.sosy_lab.cpachecker.core.algorithm.mpa.TargetSummary;
import org.sosy_lab.cpachecker.core.algorithm.tgar.comparator.DeeperLevelFirstComparator;
import org.sosy_lab.cpachecker.core.algorithm.tgar.interfaces.TestificationOperator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Property;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

// GuidedRefinementAlgorithm
@Options(prefix="tgar")
public class TGARAlgorithm implements Algorithm, AlgorithmWithResult, StatisticsProvider {

  private TGARStatistics stats;

  @Option(secure=true, name="refiner", required = true,
      description = "Which refinement algorithm to use? "
      + "(give class name, required for TGAR) If the package name starts with "
      + "'org.sosy_lab.cpachecker.', this prefix can be omitted.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker")
  private Class<? extends TargetedRefiner> refinerClass = null;

  @Option(secure=true, name="comparator", required = true,
      description = "Which target state comparator to use? "
          + "(give class name, required for TGAR) If the package name starts with "
          + "'org.sosy_lab.cpachecker.core.algorithm.tgar.comparator.', this prefix can be omitted.")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.tgar.comparator")
  private Class<? extends Comparator<ARGState>> comparatorClass = DeeperLevelFirstComparator.class;
  private final Comparator<ARGState> comparator;

  private final LogManager logger;
  private final Algorithm algorithm;

  private final TargetedRefiner refineOp;
  private TestificationOperator testificationOp;

  private final Set<Integer> feasibleStateIds = Sets.newTreeSet();

  public static class TGARAlgorithmResult implements AlgorithmResult {
    private final Set<AbstractState> feasibleTargetStates = Sets.newHashSet();
    private TargetSummary targetSummary = TargetSummary.none();

    public void addFeasibleTarget(AbstractState pTargetState, TargetSummary pTargetSummary) {
      feasibleTargetStates.add(pTargetState);
      targetSummary = TargetSummary.union(targetSummary, pTargetSummary);
    }

    public TargetSummary getTargetSummary() {
      return targetSummary;
    }
  }

  TGARAlgorithmResult result = null;

  public TGARAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCpa, Configuration pConfig,
       LogManager pLogManager) throws InvalidConfigurationException, CPAException {

    pConfig.inject(this);

    algorithm = pAlgorithm;
    logger = pLogManager;

    stats = new TGARStatistics(pLogManager);
    refineOp = createInstance(pCpa);
    comparator = createComparator();
  }

  public TGARAlgorithm(Algorithm pAlgorithm, TargetedRefiner pRefinerClass, Configuration pConfig, LogManager pLogManager)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    algorithm = pAlgorithm;
    logger = pLogManager;

    stats = new TGARStatistics(pLogManager);
    refineOp = Preconditions.checkNotNull(pRefinerClass);
    comparator = createComparator();
  }

  public void setStats(TGARStatistics pStats) {
    stats = pStats;
  }

  public TGARStatistics getStats() {
    return stats;
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
      factoryMethod = refinerClass.getMethod("create", ConfigurableProgramAnalysis.class);
    } catch (NoSuchMethodException e) {
      throw new InvalidComponentException(refinerClass, "Refiner", "No public static method \"create\" with exactly one parameter of type ConfigurableProgramAnalysis.");
    }

    // verify signature
    if (!Modifier.isStatic(factoryMethod.getModifiers())) {
      throw new InvalidComponentException(refinerClass, "Refiner", "Factory method is not static");
    }

    String exception = Classes.verifyDeclaredExceptions(factoryMethod, CPAException.class, InvalidConfigurationException.class);
    if (exception != null) {
      throw new InvalidComponentException(refinerClass, "Refiner", "Factory method declares the unsupported checked exception " + exception + ".");
    }

    // invoke factory method
    Object refinerObj;
    try {
      refinerObj = factoryMethod.invoke(null, pCpa);

    } catch (IllegalAccessException e) {
      throw new InvalidComponentException(refinerClass, "Refiner", "Factory method is not public.");

    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      Throwables.propagateIfPossible(cause, CPAException.class, InvalidConfigurationException.class);

      throw new UnexpectedCheckedException("instantiation of refinerClass " + refinerClass.getSimpleName(), cause);
    }

    if ((refinerObj == null) || !(refinerObj instanceof Refiner)) {
      throw new InvalidComponentException(refinerClass, "Refiner", "Factory method did not return a Refiner instance.");
    }

    return (TargetedRefiner)refinerObj;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {

    Preconditions.checkNotNull(testificationOp, "A testification operator must be configured!");
    Preconditions.checkNotNull(refineOp, "A refinement operator must be configured!");

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    result = new TGARAlgorithmResult();

    stats.totalTimer.start();
    try {
      // There might be unhandled target states left for refinement.
      //    This might be the case if there was a feasible path
      //    for that the algorithm returned.
      Optional<ARGState> lastTargetState = chooseTarget(pReached);

      do {
        while (lastTargetState.isPresent()) {
          final ARGState targetState = lastTargetState.get();
          Preconditions.checkState(AbstractStates.isTargetState(targetState));

          final Set<? extends Property> targetProperties = AbstractStates.extractViolatedProperties(targetState, Property.class);

          final boolean eliminated;
          try (StatHandle stat = PropertyStats.INSTANCE.startRefinement(targetProperties)) {
            eliminated = refine(pReached, targetState);
          }

          if (eliminated) {
            logger.logf(Level.INFO, "Spurious CEX for: " + targetProperties);
          } else {
            TargetSummary targetSummary = testificationOp.testify(pReached, targetState);
            logger.logf(Level.INFO, "Feasible CEX for %s. Testified for %s", targetProperties, targetSummary);
            if (targetSummary.getViolatedProperties().size() > 0) {
              result.addFeasibleTarget(targetState, targetSummary);
            }
          }

          lastTargetState = chooseTarget(pReached);
        }

        status = status.update(algorithm.run(pReached));
        lastTargetState = chooseTarget(pReached);

      } while (lastTargetState.isPresent() || pReached.hasWaitingState());

    } finally {
      stats.totalTimer.stop();
    }

    return status;
  }

  @Override
  public AlgorithmResult getResult() {
    return result;
  }

  public void setTestificationOp(TestificationOperator pTestificationOp) {
    testificationOp = pTestificationOp;
  }

  private final Predicate<ARGState> STATE_NOT_YET_HANDLED = new Predicate<ARGState>() {
    @Override
    public boolean apply(@Nullable ARGState pAbstractState) {
      Preconditions.checkNotNull(pAbstractState);
      return !feasibleStateIds.contains(pAbstractState.getStateId());
    }
  };

  private static final Predicate<ARGState> PROPERTY_NOT_BLACKLISTED = new Predicate<ARGState>() {
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

  private FluentIterable<ARGState> filterForTargetCandidates(ReachedSet pReached) {
    return from(pReached)
            .filter(IS_TARGET_STATE)
            .filter(ARGState.class)
            .filter(STATE_NOT_YET_HANDLED)
            .filter(PROPERTY_NOT_BLACKLISTED);
  }

  private static Multiset<Property> targetProperties(Iterable<ARGState> pStates) {
    Multiset result = HashMultiset.create();
    for (ARGState e : pStates) {
      result.addAll(AbstractStates.extractViolatedProperties(e, Property.class));
    }
    return result;
  }

  private Optional<ARGState> chooseTarget(ReachedSet pReached) {
    ImmutableList<ARGState> rankedCandidates = filterForTargetCandidates(pReached)
            .toSortedList(comparator);

    if (rankedCandidates.isEmpty()) {
      return Optional.absent();
    }

    logger.logf(Level.INFO, "Choosing a target from %d candidates.", rankedCandidates.size());
    logger.logf(Level.INFO, "Candidates and their frequency: %s", targetProperties(rankedCandidates));

    return Optional.of(rankedCandidates.get(0));
  }


  @SuppressWarnings("NonAtomicVolatileUpdate") // statistics written only by one thread
  private boolean refine(ReachedSet pReached, AbstractState pTarget)
      throws CPAException, InterruptedException {
    Preconditions.checkArgument(pTarget instanceof ARGState);
    final ARGState target = (ARGState) pTarget;

    logger.log(Level.FINE, "Counterexample found, performing TGAR");
    stats.beginRefinement(pReached, target);
    try {
      final int targetCandidatesBeforeRefinement = filterForTargetCandidates(pReached).size();

      final boolean counterexampleEliminated;
      try {
        counterexampleEliminated = refineOp.performRefinement(pReached, target);
        logger.log(Level.FINE, "Refinement successful: ", counterexampleEliminated);

        //  false: a FEASIBLE counterexample was found!
        //  true: a SPURIOUS counterexample was found; a refinement has been performed!

        if (counterexampleEliminated) {
          // An infeasible counterexample was found and eliminated.
          final int targetCandidatesAfterRefinement = filterForTargetCandidates(pReached).size();
          final int removedTargets = targetCandidatesBeforeRefinement -
              targetCandidatesAfterRefinement;
          stats.endWithInfeasible(pReached, target, removedTargets);
        } else {
          feasibleStateIds.add(target.getStateId());
          stats.endWithFeasible(pReached, target);
        }

      } catch (RefinementFailedException e) {
        stats.endWithError(pReached);
        throw e;
      }

      return counterexampleEliminated;

    } catch (InterruptedException e){
      stats.endWithError(pReached);
      throw e;
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    if (refineOp instanceof StatisticsProvider) {
      ((StatisticsProvider) refineOp).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
  }

  public TargetedRefiner getRefinerClass() {
    return refineOp;
  }

}
