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

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.FILTER_ABSTRACTION_STATES;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.DoNothingInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AdjustableConditionCPA;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.bounds.BoundsCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

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

  @Option(secure=true, description = "If BMC did not find a bug, check whether "
      + "the bounding did actually remove parts of the state space "
      + "(this is similar to CBMC's unwinding assertions).")
  private boolean boundingAssertions = true;

  @Option(secure=true, description="try using induction to verify programs with loops")
  private boolean induction = false;

  @Option(secure=true, description="Generate invariants and add them to the induction hypothesis.")
  private boolean addInvariantsByAI = false;

  @Option(secure=true, description="Generate additional invariants by induction and add them to the induction hypothesis.")
  private boolean addInvariantsByInduction = true;

  @Option(secure=true, description="Adds pre-loop information to the induction hypothesis. "
      + "This is unsound and should generally not be used; however "
      + "it is provided as an implementation of the technique introduced in "
      + "the SV-COMP 2013 competition contribution of ESBMC 1.20.")
  private boolean havocLoopTerminationConditionVariablesOnly = false;

  protected final BMCStatistics stats;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;

  private final ConfigurableProgramAnalysis stepCaseCPA;
  private final Algorithm stepCaseAlgorithm;

  protected final InvariantGenerator invariantGenerator;

  private final FormulaManagerView fmgr;
  private final PathFormulaManager pmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  protected final LogManager logger;
  private final ReachedSetFactory reachedSetFactory;
  private final CFA cfa;

  protected final ShutdownNotifier shutdownNotifier;

  private final TargetLocationProvider targetLocationProvider;

  protected AbstractBMCAlgorithm(Algorithm pAlgorithm, ConfigurableProgramAnalysis pCPA,
                      Configuration pConfig, LogManager pLogger,
                      ReachedSetFactory pReachedSetFactory,
                      ShutdownNotifier pShutdownNotifier, CFA pCFA,
                      BMCStatistics pBMCStatistics,
                      boolean pIsInvariantGenerator)
                      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this, AbstractBMCAlgorithm.class);

    stats = pBMCStatistics;
    algorithm = pAlgorithm;
    cpa = pCPA;
    logger = pLogger;
    reachedSetFactory = pReachedSetFactory;
    cfa = pCFA;

    if (induction) {
      induction = checkIfInductionIsPossible(pCFA, pLogger);
    }

    if (induction) {
      LogManager stepCaseLogger = logger.withComponentName("InductionStepCase");
      CPABuilder builder = new CPABuilder(pConfig, stepCaseLogger, pShutdownNotifier, pReachedSetFactory);
      stepCaseCPA = builder.buildCPAWithSpecAutomatas(cfa);
      stepCaseAlgorithm = CPAAlgorithm.create(stepCaseCPA, stepCaseLogger, pConfig, pShutdownNotifier);
    } else {
      stepCaseCPA = null;
      stepCaseAlgorithm = null;
    }

    if (!pIsInvariantGenerator
        && induction
        && addInvariantsByInduction) {
      addInvariantsByInduction = false;
      invariantGenerator = KInductionInvariantGenerator.create(pConfig, pLogger,
          pShutdownNotifier, pCFA, pReachedSetFactory);

    } else if (induction && addInvariantsByAI) {
      invariantGenerator = CPAInvariantGenerator.create(pConfig, pLogger, pShutdownNotifier, cfa);
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
    shutdownNotifier = pShutdownNotifier;

    targetLocationProvider = new TargetLocationProvider(reachedSetFactory, shutdownNotifier, logger, pConfig, cfa);
  }

  static boolean checkIfInductionIsPossible(CFA cfa, LogManager logger) {
    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
      return false;
    }

    LoopStructure loops = cfa.getLoopStructure().get();

    if (loops.getCount() == 0) {
      // induction is unnecessary, program has no loops
      return false;
    }

    return true;
  }

  public AlgorithmStatus run(final ReachedSet reachedSet) throws CPAException, InterruptedException {
    CFANode initialLocation = extractLocation(reachedSet.getFirstState());
    invariantGenerator.start(initialLocation);

    // The set of candidate invariants that still need to be checked.
    // Successfully proven invariants are removed from the set.
    Collection<CFANode> targetLocations = targetLocationProvider.tryGetAutomatonTargetLocations(cfa.getMainFunction());
    if (targetLocations == null) {
      targetLocations = cfa.getAllNodes();
    }
    final CandidateGenerator candidateGenerator = getCandidateInvariants(cfa, targetLocations);

    try {
      if (!candidateGenerator.produceMoreCandidates()) {
        for (AbstractState state : from(reachedSet.getWaitlist()).toList()) {
          reachedSet.removeOnlyFromWaitlist(state);
        }
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

      AlgorithmStatus status;

      try (ProverEnvironment prover = solver.newProverEnvironmentWithModelGeneration();
          @SuppressWarnings("resource")
          KInductionProver kInductionProver = createInductionProver()) {

        do {
          shutdownNotifier.shutdownIfNecessary();

          logger.log(Level.INFO, "Creating formula for program");
          status = BMCHelper.unroll(logger, reachedSet, algorithm, cpa);
          if (from(reachedSet)
              .skip(1) // first state of reached is always an abstraction state, so skip it
              .transform(toState(PredicateAbstractState.class))
              .anyMatch(FILTER_ABSTRACTION_STATES)) {

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
              sound = sound || kInductionProver.check(k, from(candidateGenerator).toSet());
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
    } finally {
    }
  }

  /**
   * Gets the candidate invariants to be checked.
   *
   * @return the candidate invariants to be checked.
   */
  protected abstract CandidateGenerator getCandidateInvariants(CFA cfa,
      Collection<CFANode> targetLocations);

  /**
   * Adjusts the conditions of the CPAs which support the adjusting of
   * conditions.
   *
   * @return {@code true} if all CPAs supporting the feature agreed on
   * adjusting their conditions, {@code false} if one of the CPAs does not
   * support any further adjustment of conditions.
   */
  private boolean adjustConditions() {
    Iterable<AdjustableConditionCPA> conditionCPAs = CPAs.asIterable(cpa).filter(AdjustableConditionCPA.class);
    for (AdjustableConditionCPA condCpa : conditionCPAs) {
      if (!condCpa.adjustPrecision()) {
        // this cpa said "do not continue"
        logger.log(Level.INFO, "Terminating because of", condCpa.getClass().getSimpleName());
        return false;
      }
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
    } else {
      analyzeCounterexample(pReachedSet, pProver);
    }

    // Now pop the program formula off of the stack
    pProver.pop();

    return safe;
  }

  /**
   * This class is called after a violation has been found
   * (i.e., the bounded-model-checking formula was satisfied).
   * The formula is still on the solver stack.
   * Subclasses can use this method to further analyze the counterexample
   * if necessary.
   */
  protected void analyzeCounterexample(final ReachedSet pReachedSet, final ProverEnvironment pProver)
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
                                                    .filter(IS_STOP_STATE);

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

  protected KInductionProver createInductionProver() {
     return induction ? new KInductionProver(
        cfa,
        logger,
        stepCaseAlgorithm,
        stepCaseCPA,
        invariantGenerator,
        stats,
        reachedSetFactory,
        havocLoopTerminationConditionVariablesOnly,
        shutdownNotifier) : null;
  }
}
