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
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
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
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.bounds.BoundsCPA;
import org.sosy_lab.cpachecker.cpa.bounds.BoundsState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.targetreachability.ReachabilityState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
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

  @Option(secure=true, description="Generate additional invariants by induction and add them to the induction hypothesis.")
  private boolean addInvariantsByInduction = true;

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
    }

    ShutdownManager invariantGeneratorShutdownManager = pShutdownManager;
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

    if (!pIsInvariantGenerator
        && induction
        && addInvariantsByInduction) {
      addInvariantsByInduction = false;
      invariantGenerator =
          KInductionInvariantGenerator.create(
              pConfig,
              pLogger,
              invariantGeneratorShutdownManager,
              pCFA,
              pSpecification,
              pReachedSetFactory,
              targetLocationProvider,
              pAggregatedReachedSets);
    } else if (induction) {
      invariantGenerator =
          new AbstractInvariantGenerator() {

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
    } else {
      invariantGenerator = new DoNothingInvariantGenerator();
    }

    PredicateCPA predCpa = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (predCpa == null) {
      throw new InvalidConfigurationException("PredicateCPA needed for BMCAlgorithm");
    }
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

    try {
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

        Set<CFANode> immediateLoopHeads = null;

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

          if (invariantGenerator.isProgramSafe()) {
            // The reachedSet might contain target states which would give a wrong
            // indication of safety to the caller. So remove them.
            for (CandidateInvariant candidateInvariant : candidateGenerator) {
              candidateInvariant.assumeTruth(reachedSet);
            }
            return AlgorithmStatus.SOUND_AND_PRECISE;
          }

          // Perform a bounded model check on each candidate invariant
          Iterator<CandidateInvariant> candidateInvariantIterator = candidateGenerator.iterator();
          while (candidateInvariantIterator.hasNext()) {
            CandidateInvariant candidateInvariant = candidateInvariantIterator.next();
            // first check safety in k iterations

            boolean safe = boundedModelCheck(reachedSet, prover, candidateInvariant);
            if (!safe) {
              candidateInvariantIterator.remove();
            }

            if (invariantGenerator.isProgramSafe()) {
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
            if (induction) {
              final int k = CPAs.retrieveCPA(cpa, BoundsCPA.class).getMaxLoopIterations();

              if (immediateLoopHeads == null) {
                immediateLoopHeads = getImmediateLoopHeads(reachedSet);
              }
              Set<CandidateInvariant> candidates = from(candidateGenerator).toSet();
              sound = sound || kInductionProver.check(k, candidates, immediateLoopHeads);
              candidateGenerator.confirmCandidates(kInductionProver.getConfirmedCandidates());
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
    } catch (InterruptedException e) {
      if (invariantGenerator.isProgramSafe()) {
        // The wait list may not be empty, which would wrongly indicate to the
        // caller that the analysis is incomplete
        for (AbstractState state : new ArrayList<>(reachedSet.getWaitlist())) {
          reachedSet.removeOnlyFromWaitlist(state);
        }
        // The reachedSet might contain target states, which would give a wrong
        // indication of safety to the caller, so remove them.
        for (CandidateInvariant candidateInvariant : candidateGenerator) {
          candidateInvariant.assumeTruth(reachedSet);
        }

        // The reached set may be in an inconsistent state where the ARG
        // contains states that are not covered and where the parents are not
        // in the wait list
        removeMissingStatesFromARG(reachedSet);

        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      throw e;
    } finally {
    }
  }

  /**
   * Gets all loop heads in the reached set
   * that were reached without unrolling any loops.
   *
   * @param pReachedSet the reached set.
   * @return all loop heads in the reached set
   * that were reached without unrolling any loops.
   */
  private Set<CFANode> getImmediateLoopHeads(ReachedSet pReachedSet) {
    return AbstractStates.filterLocations(pReachedSet, getLoopHeads())
        .filter(
            new Predicate<AbstractState>() {

              @Override
              public boolean apply(AbstractState pLoopHeadState) {
                BoundsState state =
                    AbstractStates.extractStateByType(pLoopHeadState, BoundsState.class);
                for (CFANode location : AbstractStates.extractLocations(pLoopHeadState)) {
                  Set<Loop> loops = cfa.getLoopStructure().get().getLoopsForLoopHead(location);
                  for (Loop loop : loops) {
                    if (state.getIteration(loop) <= 1
                        && state.getDeepestIterationLoops().equals(loops)) {
                      return true;
                    }
                  }
                }
                return false;
              }
            })
        .transformAndConcat(AbstractStates::extractLocations)
        .toSet();
  }

  private void removeMissingStatesFromARG(ReachedSet pReachedSet) {
    Collection<ARGState> missingChildren = new ArrayList<>();
    for (ARGState e : from(pReachedSet).transform(toState(ARGState.class))) {
      for (ARGState child : e.getChildren()) {
        if ((!pReachedSet.contains(child) && !(child.isCovered() && child.getChildren().isEmpty()))
            || pReachedSet.getWaitlist().containsAll(child.getParents())) {
          missingChildren.add(child);
        }
      }
    }
    for (ARGState missingChild : missingChildren) {
      missingChild.removeFromARG();
    }
  }

  /**
   * Gets the candidate invariants to be checked.
   *
   * @return the candidate invariants to be checked.
   */
  protected abstract CandidateGenerator getCandidateInvariants();

  /**
   * Adjusts the conditions of the CPAs which support the adjusting of conditions.
   *
   * @return {@code true} if the conditions were adjusted, {@code false} if the BoundsCPA used to
   *     unroll the loops does not support any further adjustment of conditions.
   */
  private boolean adjustConditions() {
    Iterable<AdjustableConditionCPA> conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (!condCpa.adjustPrecision() && condCpa instanceof BoundsCPA) {
        // this cpa said "do not continue"
        logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
        return false;
      }
    }
    return !Iterables.isEmpty(conditionCPAs);
  }


  protected boolean boundedModelCheck(final ReachedSet pReachedSet, final ProverEnvironment pProver, CandidateInvariant pInductionProblem) throws CPATransferException, InterruptedException, SolverException {
    BooleanFormula program = bfmgr.not(pInductionProblem.getAssertion(pReachedSet, fmgr, pmgr, 0));
    logger.log(Level.INFO, "Starting satisfiability check...");
    stats.satCheck.start();
    pProver.push(program);
    boolean safe = pProver.isUnsat();
    // Leave program formula on solver stack until error path is created
    stats.satCheck.stop();

    if (safe) {
      pInductionProblem.assumeTruth(pReachedSet);
    } else {
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
     return induction ? new KInductionProver(
        cfa,
        logger,
        stepCaseAlgorithm,
        stepCaseCPA,
        invariantGenerator,
        stats,
        reachedSetFactory,
        shutdownNotifier,
        getLoopHeads()) : null;
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
}
