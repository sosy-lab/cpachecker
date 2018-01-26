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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.targetreachability.ReachabilityState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.automaton.CachingTargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix="bmc")
abstract class AbstractBMCAlgorithm implements StatisticsProvider {

  static final Predicate<AbstractState> IS_STOP_STATE =
    Predicates.compose(new Predicate<AssumptionStorageState>() {
                             @Override
                             public boolean apply(AssumptionStorageState pArg0) {
                               return (pArg0 != null) && pArg0.isStop();
                             }
                           },
                       AbstractStates.toState(AssumptionStorageState.class));

  static final Predicate<AbstractState> IS_SLICED_STATE = (state) ->
    AbstractStates.extractStateByType(state, ReachabilityState.class) == ReachabilityState.IRRELEVANT_TO_TARGET;

  @Option(secure=true, description = "If BMC did not find a bug, check whether "
      + "the bounding did actually remove parts of the state space "
      + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  @Option(secure=true, description="try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(secure=true, description="Strategy for generating auxiliary invariants")
  private InvariantGeneratorFactory invariantGenerationStrategy = InvariantGeneratorFactory.REACHED_SET;

  @Option(secure=true, description="Propagates the interrupts of the invariant generator.")
  private boolean propagateInvGenInterrupts = false;

  protected final BMCStatistics stats;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final @Nullable ConfigurableProgramAnalysis stepCaseCPA;
  private final @Nullable Algorithm stepCaseAlgorithm;

  protected final InvariantGenerator invariantGenerator;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  protected final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;
  private final Specification specification;

  protected final ShutdownNotifier shutdownNotifier;

  private final TargetLocationProvider targetLocationProvider;

  private final @Nullable ShutdownRequestListener propagateSafetyInterrupt;

  /** The candidate invariants that have been proven to hold at the loop heads. */
  private final Set<CandidateInvariant> confirmedCandidates = new CopyOnWriteArraySet<>();

  protected AbstractBMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      final ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification pSpecification,
      BMCStatistics pBMCStatistics,
      boolean pIsInvariantGenerator,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException {

    pConfig.inject(this, AbstractBMCAlgorithm.class);

    stats = pBMCStatistics;
    algorithm = pAlgorithm;
    cpa = pCPA;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;
    specification = checkNotNull(pSpecification);

    shutdownNotifier = pShutdownManager.getNotifier();
    targetLocationProvider = new CachingTargetLocationProvider(shutdownNotifier, logger, cfa);

    if (induction) {
      induction = checkIfInductionIsPossible(pCFA, pLogger);
      // if there is no loop we do not need induction, although loop information is available
      induction = induction && cfa.getLoopStructure().get().getCount() > 0 && !getLoopHeads().isEmpty();
    }

    if (induction) {
      LogManager stepCaseLogger = logger.withComponentName("InductionStepCase");
      CPABuilder builder =
          new CPABuilder(
              pConfig, stepCaseLogger, pShutdownManager.getNotifier(), pReachedSetFactory);
      stepCaseCPA = builder.buildCPAs(cfa, pSpecification, new AggregatedReachedSets());
      stepCaseAlgorithm =
          CPAAlgorithm.create(stepCaseCPA, stepCaseLogger, pConfig, pShutdownManager.getNotifier());
    } else {
      stepCaseCPA = null;
      stepCaseAlgorithm = null;
      invariantGenerationStrategy = InvariantGeneratorFactory.DO_NOTHING;
    }

    ShutdownManager invariantGeneratorShutdownManager = pShutdownManager;
    boolean addInvariantsByInduction = invariantGenerationStrategy == InvariantGeneratorFactory.INDUCTION;
    if (addInvariantsByInduction) {
      if (propagateInvGenInterrupts) {
        invariantGeneratorShutdownManager = pShutdownManager;
      } else {
        invariantGeneratorShutdownManager = ShutdownManager.createWithParent(pShutdownManager.getNotifier());
      }
      propagateSafetyInterrupt = new ShutdownRequestListener() {

        @Override
        public void shutdownRequested(String pReason) {
          InvariantGenerator invariantGenerator = AbstractBMCAlgorithm.this.invariantGenerator;
          if (invariantGenerator != null && invariantGenerator.isProgramSafe()) {
            pShutdownManager.requestShutdown(pReason);
          }
        }
      };
      invariantGeneratorShutdownManager.getNotifier().register(propagateSafetyInterrupt);
    } else {
      propagateSafetyInterrupt = null;
    }

    if (pIsInvariantGenerator && addInvariantsByInduction) {
      invariantGenerationStrategy = InvariantGeneratorFactory.REACHED_SET;
    }
    invariantGenerator = invariantGenerationStrategy.createInvariantGenerator(
            pConfig,
            pLogger,
            pReachedSetFactory,
            invariantGeneratorShutdownManager,
            pCFA,
            pSpecification,
            pAggregatedReachedSets,
            targetLocationProvider);

    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, BMCAlgorithm.class);
    solver = predCpa.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pmgr = predCpa.getPathFormulaManager();

  }

  static boolean checkIfInductionIsPossible(CFA cfa, LogManager logger) {
    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
      return false;
    }

    return true;
  }

  public AlgorithmStatus run(final ReachedSet reachedSet) throws CPAException,
      SolverException,
      InterruptedException {
    CFANode initialLocation = extractLocation(reachedSet.getFirstState());
    invariantGenerator.start(initialLocation);

    // The set of candidate invariants that still need to be checked.
    // Successfully proven invariants are removed from the set.
    final CandidateGenerator candidateGenerator = getCandidateInvariants();

    if (!candidateGenerator.produceMoreCandidates()) {
      for (AbstractState state : from(reachedSet.getWaitlist()).toList()) {
        reachedSet.removeOnlyFromWaitlist(state);
      }
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    AlgorithmStatus status;

    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
         @SuppressWarnings("resource")
        KInductionProver kInductionProver = createInductionProver()) {

      do {
        shutdownNotifier.shutdownIfNecessary();

        logger.log(Level.INFO, "Creating formula for program");
        stats.bmcPreparation.start();
        status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
        stats.bmcPreparation.stop();
        if (from(reachedSet)
            .skip(1) // first state of reached is always an abstraction state, so skip it
            .filter(not(IS_TARGET_STATE)) // target states may be abstraction states
            .anyMatch(PredicateAbstractState.CONTAINS_ABSTRACTION_STATE)) {

          logger.log(Level.WARNING, "BMC algorithm does not work with abstractions. Could not check for satisfiability!");
          return status;
        }
        shutdownNotifier.shutdownIfNecessary();

        if (invariantGenerator.isProgramSafe()) {
          TargetLocationCandidateInvariant.INSTANCE.assumeTruth(reachedSet);
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }

        // Perform a bounded model check on each candidate invariant
        Iterator<CandidateInvariant> candidateInvariantIterator = candidateGenerator.iterator();
        while (candidateInvariantIterator.hasNext()) {
          shutdownNotifier.shutdownIfNecessary();
          CandidateInvariant candidateInvariant = candidateInvariantIterator.next();
          // first check safety in k iterations

          boolean safe = boundedModelCheck(reachedSet, prover, candidateInvariant);
          if (!safe) {
            if (candidateInvariant == TargetLocationCandidateInvariant.INSTANCE) {
              return AlgorithmStatus.UNSOUND_AND_PRECISE;
            }
            candidateInvariantIterator.remove();
          }

          if (invariantGenerator.isProgramSafe()) {
            TargetLocationCandidateInvariant.INSTANCE.assumeTruth(reachedSet);
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        // second check soundness
        boolean sound;

        // verify soundness, but don't bother if we are unsound anyway or we have found a bug
        if (status.isSound()) {

          // check bounding assertions
          sound = candidateGenerator.hasCandidatesAvailable() ? checkBoundingAssertions(reachedSet, prover) : true;

          if (invariantGenerator.isProgramSafe()) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }

          // try to prove program safety via induction
          if (induction && !sound) {
            sound = checkStepCase(reachedSet, candidateGenerator, kInductionProver);
          }
          if (invariantGenerator.isProgramSafe()
              || (sound && !candidateGenerator.produceMoreCandidates())) {
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }
        }

        if (!candidateGenerator.hasCandidatesAvailable()) {
          // no remaining invariants to be proven
          return status;
        }
      }
      while (status.isSound() && adjustConditions());
    }

    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  private boolean checkStepCase(
      final ReachedSet reachedSet,
      final CandidateGenerator candidateGenerator,
      KInductionProver kInductionProver)
      throws InterruptedException, CPAException, SolverException {

    final int k = CPAs.retrieveCPA(cpa, LoopIterationBounding.class).getMaxLoopIterations();

    Set<CandidateInvariant> candidates = from(candidateGenerator).toSet();
    shutdownNotifier.shutdownIfNecessary();

    Set<Object> checkedKeys = getCheckedKeys(reachedSet);
    boolean sound = true;
    Iterable<CandidateInvariant> artificialConjunctions =
        buildArtificialConjunctions(candidates);
    Iterable<CandidateInvariant> candidatesToCheck =
        Iterables.concat(candidates, artificialConjunctions);
    for (CandidateInvariant candidate : candidatesToCheck) {
      InductionResult<CandidateInvariant> inductionResult =
          kInductionProver.check(
              Iterables.concat(confirmedCandidates, Collections.singleton(candidate)),
              k,
              candidate,
              checkedKeys);
      if (inductionResult.isSuccessful()) {
        Iterables.addAll(
            confirmedCandidates,
            CandidateInvariantCombination.getConjunctiveParts(candidate));
        candidateGenerator.confirmCandidates(
            CandidateInvariantCombination.getConjunctiveParts(candidate));
        if (candidate == TargetLocationCandidateInvariant.INSTANCE) {
          sound = true;
          break;
        }
      } else {
        sound = false;
      }
    }
    return sound;
  }

  /**
   * Gets all keys of loop-iteration reporting states that were reached by unrolling.
   *
   * @param pReachedSet the reached set.
   * @return all keys of loop-iteration reporting states that were reached by unrolling.
   */
  private Set<Object> getCheckedKeys(ReachedSet pReachedSet) {
    return AbstractStates.filterLocations(pReachedSet, getLoopHeads())
        .transform(s -> AbstractStates.extractStateByType(s, LoopIterationReportingState.class))
        .transform(LoopIterationReportingState::getPartitionKey)
        .toSet();
  }

  /**
   * Gets the candidate invariants to be checked.
   *
   * @return the candidate invariants to be checked.
   */
  protected abstract CandidateGenerator getCandidateInvariants();

  /**
   * Adjusts the conditions of those CPAs that support the adjustment of conditions.
   *
   * @return {@code true} if the conditions were adjusted, {@code false} if no further adjustment is
   *     possible.
   */
  private boolean adjustConditions() {
    FluentIterable<AdjustableConditionCPA> conditionCPAs =
        CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    boolean adjusted = false;
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (condCpa.adjustPrecision()) {
        adjusted = true;
      }
    }
    if (!adjusted) {
      // these cpas said "do not continue"
      logger.log(
          Level.INFO,
          "Terminating because none of the following CPAs' precision can be adjusted any further ",
          Joiner.on(", ").join(conditionCPAs.transform(cpa -> cpa.getClass().getSimpleName())));
      return false;
    }
    return !Iterables.isEmpty(conditionCPAs);
  }


  protected boolean boundedModelCheck(final ReachedSet pReachedSet, final ProverEnvironment pProver, CandidateInvariant pInductionProblem) throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula program = bfmgr.not(pInductionProblem.getAssertion(pReachedSet, fmgr, pmgr));
    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    pProver.push(program);
    boolean safe = pProver.isUnsat();
    // Leave program formula on solver stack until error path is created
    stats.satCheck.stop();

    if (safe) {
      pInductionProblem.assumeTruth(pReachedSet);
    } else if (pInductionProblem == TargetLocationCandidateInvariant.INSTANCE) {
      analyzeCounterexample(program, pReachedSet, pProver);
    }

    // Now pop the program formula off of the stack
    pProver.pop();

    return safe;
  }

  /**
   * This method is called after a violation has been found
   * (i.e., the bounded-model-checking formula was satisfied).
   * The formula is still on the solver stack.
   * Subclasses can use this method to further analyze the counterexample
   * if necessary.
   *
   * @param pCounterexample the satisfiable formula that contains the specification violation
   * @param pReachedSet the reached used for analyzing
   * @param pProver the prover that was used (has pCounterexample formula pushed onto it)
   * @throws CPATransferException may be thrown in subclasses
   * @throws InterruptedException may be thrown in subclasses
   */
  protected void analyzeCounterexample(
      final BooleanFormula pCounterexample,
      final ReachedSet pReachedSet,
      final ProverEnvironment pProver)
      throws CPATransferException, InterruptedException {
    // by default, do nothing (just a hook for subclasses)
  }

    /**
   * Checks if the bounded unrolling completely unrolled all reachable loop
   * iterations by performing a satisfiablity check on the formulas encoding
   * the reachability of the states where the bounded model check stopped due
   * to reaching the bound.
   *
   * If this is is the case, then the bounded model check is guaranteed to be
   * sound.
   *
   * @param pReachedSet the reached set containing the frontier of the bounded
   * model check, i.e. where the bounded model check stopped.
   * @param prover the prover to be used to prove that the stop states are
   * unreachable.
   *
   * @return {@code true} if the bounded model check covered all reachable
   * states and was thus sound, {@code false} otherwise.
   *
   * @throws InterruptedException if the satisfiability check is interrupted.
   */
  private boolean checkBoundingAssertions(final ReachedSet pReachedSet, final ProverEnvironment prover)
      throws SolverException, InterruptedException {
    FluentIterable<AbstractState> stopStates = from(pReachedSet)
        .filter(IS_STOP_STATE)
        .filter(Predicates.not(IS_SLICED_STATE));

    if (boundingAssertions) {
      // create formula for unwinding assertions
      BooleanFormula assertions = BMCHelper.createFormulaFor(stopStates, bfmgr);

      logger.log(Level.INFO, "Starting assertions check...");

      stats.assertionsCheck.start();
      prover.push(assertions);
      boolean sound = prover.isUnsat();
      prover.pop();
      stats.assertionsCheck.stop();

      logger.log(Level.FINER, "Soundness after assertion checks:", sound);
      return sound;

    } else {
      // fast check for trivial cases
      return stopStates.isEmpty();
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider)algorithm).collectStatistics(pStatsCollection);
    }
    pStatsCollection.add(stats);
    if (invariantGenerator instanceof StatisticsProvider) {
      ((StatisticsProvider)invariantGenerator).collectStatistics(pStatsCollection);
    }
  }

  protected @Nullable KInductionProver createInductionProver() {
    return induction
        ? new KInductionProver(
            cfa,
            logger,
            stepCaseAlgorithm,
            stepCaseCPA,
            invariantGenerator,
            stats,
            reachedSetFactory,
            shutdownNotifier,
            getLoopHeads(),
            false)
        : null;
  }

  /**
   * Gets the potential target locations.
   *
   * @return the potential target locations.
   */
  protected Collection<CFANode> getTargetLocations() {
    return targetLocationProvider.tryGetAutomatonTargetLocations(
        cfa.getMainFunction(), specification);
  }

  /**
   * Gets the loop heads.
   *
   * @return the loop heads.
   */
  protected Set<CFANode> getLoopHeads() {
    return BMCHelper.getLoopHeads(cfa, targetLocationProvider);
  }

  public static enum InvariantGeneratorFactory {

    INDUCTION {

      @Override
      InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) throws InvalidConfigurationException, CPAException {
        return
            KInductionInvariantGenerator.create(
                pConfig,
                pLogger,
                pShutdownManager,
                pCFA,
                pSpecification,
                pReachedSetFactory,
                pTargetLocationProvider,
                pAggregatedReachedSets);
      }
    },

    REACHED_SET {
      @Override
      InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) {
        return new AbstractInvariantGenerator() {

          @Override
          protected void startImpl(CFANode pInitialLocation) {
            // do nothing
          }

          @Override
          public boolean isProgramSafe() {
            // just return false, program will be ended by parallel algorithm if the invariant
            // generator can prove safety before the current analysis
            return false;
          }

          @Override
          public void cancel() {
            // do nothing
          }

          @Override
          public AggregatedReachedSets get() throws CPAException, InterruptedException {
            return pAggregatedReachedSets;
          }
        };
      }
    },

    DO_NOTHING {

      @Override
      InvariantGenerator createInvariantGenerator(
          Configuration pConfig,
          LogManager pLogger,
          ReachedSetFactory pReachedSetFactory,
          ShutdownManager pShutdownManager,
          CFA pCFA,
          Specification pSpecification,
          AggregatedReachedSets pAggregatedReachedSets,
          TargetLocationProvider pTargetLocationProvider) {
        return new DoNothingInvariantGenerator();
      }

    };

    abstract InvariantGenerator createInvariantGenerator(
        Configuration pConfig,
        LogManager pLogger,
        ReachedSetFactory pReachedSetFactory,
        ShutdownManager pShutdownManager,
        CFA pCFA,
        Specification pSpecification,
        AggregatedReachedSets pAggregatedReachedSets,
        TargetLocationProvider pTargetLocationProvider) throws InvalidConfigurationException, CPAException;

  }

  protected FluentIterable<CandidateInvariant> getConfirmedCandidates(final CFANode pLocation) {
    return from(confirmedCandidates)
        .filter(pConfirmedCandidate -> pConfirmedCandidate.appliesTo(pLocation));
  }

  private Iterable<CandidateInvariant> buildArtificialConjunctions(
      final Set<CandidateInvariant> pCandidateInvariants) {
    FluentIterable<CandidateInvariant> remainingLoopHeadCandidateInvariants =
        from(pCandidateInvariants)
            .filter(
                pLocationFormulaInvariant -> {
                  return cfa.getLoopStructure().isPresent()
                      && cfa.getLoopStructure()
                          .get()
                          .getAllLoopHeads()
                          .stream()
                          .anyMatch(lh -> pLocationFormulaInvariant.appliesTo(lh));
                })
            .filter(Predicates.not(Predicates.in(confirmedCandidates)));
    if (remainingLoopHeadCandidateInvariants.size() <= 1) {
      return Collections.emptySet();
    }

    Multimap<String, CandidateInvariant> functionInvariants = HashMultimap.create();
    Set<CandidateInvariant> others = new HashSet<>();
    for (CandidateInvariant locationFormulaInvariant : remainingLoopHeadCandidateInvariants) {
      if (locationFormulaInvariant instanceof SingleLocationFormulaInvariant) {
        functionInvariants.put(
            ((SingleLocationFormulaInvariant) locationFormulaInvariant)
                .getLocation()
                .getFunctionName(),
            locationFormulaInvariant);
      } else {
        others.add(locationFormulaInvariant);
      }
    }
    for (String key : new ArrayList<>(functionInvariants.keys())) {
      functionInvariants.putAll(key, others);
    }

    Iterator<Map.Entry<String, Collection<CandidateInvariant>>> functionInvariantsEntryIterator =
        functionInvariants.asMap().entrySet().iterator();

    return () ->
        new Iterator<CandidateInvariant>() {

          private boolean allComputed = false;

          private @Nullable CandidateInvariant next = null;

          @Override
          public boolean hasNext() {
            if (next != null) {
              return true;
            }

            // Create the next conjunction over function candidates
            while (next == null && functionInvariantsEntryIterator.hasNext()) {
              assert !allComputed;
              Map.Entry<String, Collection<CandidateInvariant>> functionInvariantsEntry =
                  functionInvariantsEntryIterator.next();
              // We want at least two operands, but less than "all"; "all" comes separately later
              if (functionInvariantsEntry.getValue().size() > 1
                  && functionInvariantsEntry.getValue().size()
                      < remainingLoopHeadCandidateInvariants.size()) {
                // Only now, directly before it is used, compute the final set of operands for the
                // conjunction
                Set<CandidateInvariant> remainingFunctionCandidateInvariants =
                    remainingLoopHeadCandidateInvariants
                        .filter(
                            pCandidateInvariant -> {
                              if (pCandidateInvariant instanceof SingleLocationFormulaInvariant) {
                                return ((SingleLocationFormulaInvariant) pCandidateInvariant)
                                    .getLocation()
                                    .getFunctionName()
                                    .equals(functionInvariantsEntry.getKey());
                              }
                              return true;
                            })
                        .toSet();
                // Create the conjunction only if there are actually at least two operands
                if (remainingFunctionCandidateInvariants.size() > 1) {
                  next = CandidateInvariantCombination.conjunction(remainingFunctionCandidateInvariants);
                }
              }
            }

            // Create the conjunction over all operands, if we have not done so yet
            if (next == null && !allComputed && remainingLoopHeadCandidateInvariants.size() > 1) {
              allComputed = true;
              next = CandidateInvariantCombination.conjunction(remainingLoopHeadCandidateInvariants);
            }

            return next != null;
          }

          @Override
          public CandidateInvariant next() {
            if (!hasNext()) {
              throw new NoSuchElementException("There is no next element.");
            }
            CandidateInvariant result = next;
            next = null;
            return result;
          }
        };
  }
}