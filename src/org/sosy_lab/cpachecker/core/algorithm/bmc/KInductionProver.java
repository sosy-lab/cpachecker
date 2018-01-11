/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * Instances of this class are used to prove the safety of a program by
 * applying an inductive approach based on k-induction.
 */
class KInductionProver implements AutoCloseable {

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final Algorithm algorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final ReachedSet reached;

  private final Solver solver;

  private final FormulaManagerView fmgr;

  private final BooleanFormulaManagerView bfmgr;

  private final PathFormulaManager pfmgr;

  private final BMCStatistics stats;

  private final ReachedSetFactory reachedSetFactory;

  private final ReachedSetInitializer reachedSetInitializer =
      pReachedSet -> ensureReachedSetInitialized(pReachedSet);

  private final InvariantGenerator invariantGenerator;

  private @Nullable ProverEnvironment prover = null;

  private ExpressionTreeSupplier expressionTreeSupplier;

  private BooleanFormula loopHeadInvariants;

  private final Map<CandidateInvariant, BooleanFormula> violationFormulas = Maps.newHashMap();

  private int previousK = -1;

  private final ImmutableSet<CFANode> loopHeads;

  private boolean invariantGenerationRunning = true;

  /**
   * Creates an instance of the KInductionProver.
   */
  public KInductionProver(
      CFA pCFA,
      LogManager pLogger,
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      InvariantGenerator pInvariantGenerator,
      BMCStatistics pStats,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier,
      Set<CFANode> pLoopHeads) {
    cfa = checkNotNull(pCFA);
    logger = checkNotNull(pLogger);
    algorithm = checkNotNull(pAlgorithm);
    cpa = checkNotNull(pCPA);
    invariantGenerator  = checkNotNull(pInvariantGenerator);
    stats = checkNotNull(pStats);
    reachedSetFactory = checkNotNull(pReachedSetFactory);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    reached = reachedSetFactory.create();

    PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    solver = stepCasePredicateCPA.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = stepCasePredicateCPA.getPathFormulaManager();
    loopHeadInvariants = bfmgr.makeTrue();

    expressionTreeSupplier = ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;

    loopHeads = ImmutableSet.copyOf(pLoopHeads);
  }

  /**
   * Checks if the prover is already initialized.
   *
   * @return {@code true} if the prover is initialized, {@code false}
   * otherwise.
   */
  private boolean isProverInitialized() {
    return prover != null;
  }

  /**
   * Checks if the prover instance has been initialized, and, if not, initializes it. Afterwards,
   * the prover is guaranteed to be initialized.
   */
  @SuppressWarnings("unchecked")
  private void ensureProverInitialized() {
    if (!isProverInitialized()) {
      prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    }
    assert isProverInitialized();
  }

  private InvariantSupplier getCurrentInvariantSupplier() throws InterruptedException {
    if (invariantGenerationRunning) {
      try {
        if (invariantGenerator instanceof KInductionInvariantGenerator) {
          return ((KInductionInvariantGenerator) invariantGenerator).getSupplier();
        } else {
          // in the general case we have to retrieve the invariants from a reachedset
          return new FormulaInvariantsSupplier(invariantGenerator.get());
        }
      } catch (CPAException e) {
        logger.logUserException(Level.FINE, e, "Invariant generation failed.");
        invariantGenerationRunning = false;
      } catch (InterruptedException e) {
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Invariant generation was cancelled.");
        logger.logDebugException(e);
        invariantGenerationRunning = false;
      }
    }
    return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  private ExpressionTreeSupplier getCurrentExpressionTreeInvariantSupplier() throws InterruptedException {
    if (!invariantGenerationRunning) {
      return expressionTreeSupplier;
    }
    try {
      return new ExpressionTreeInvariantSupplier(invariantGenerator.get(), cfa);
    } catch (CPAException e) {
      logger.logUserException(Level.FINE, e, "Invariant generation failed.");
      invariantGenerationRunning = false;
      return expressionTreeSupplier;
    } catch (InterruptedException e) {
      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINE, "Invariant generation was cancelled.");
      logger.logDebugException(e);
      invariantGenerationRunning = false;
      return expressionTreeSupplier;
    }
  }

  /**
   * Gets the most current invariants generated by the invariant generator.
   *
   * @return the most current invariants generated by the invariant generator.
   */
  private BMCHelper.FormulaInContext getCurrentLoopHeadInvariants(
      Iterable<AbstractState> pAssertionStates) {
    Set<CFANode> stopLoopHeads =
        AbstractStates.extractLocations(AbstractStates.filterLocations(pAssertionStates, loopHeads))
            .toSet();
    return pContext -> {
      if (!bfmgr.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
        BooleanFormula lhi = bfmgr.makeFalse();
        for (CFANode loopHead : stopLoopHeads) {
          lhi = bfmgr.or(lhi, getCurrentLocationInvariants(loopHead, fmgr, pfmgr, pContext));
        }
        loopHeadInvariants = lhi;
      }
      return loopHeadInvariants;
    };
  }

  public BooleanFormula getCurrentLocationInvariants(
      CFANode pLocation, FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    InvariantSupplier currentInvariantsSupplier = getCurrentInvariantSupplier();

    return currentInvariantsSupplier.getInvariantFor(
        pLocation, Optional.empty(), pFMGR, pPFMGR, pContext);
  }

  public ExpressionTree<Object> getCurrentLocationInvariants(CFANode pLocation)
      throws InterruptedException {
    ExpressionTreeSupplier currentInvariantsSupplier = getCurrentExpressionTreeInvariantSupplier();

    return currentInvariantsSupplier.getInvariantFor(pLocation);
  }

  @Override
  public void close() {
    if (isProverInitialized()) {
      prover.close();
    }
  }

  /**
   * Attempts to perform the inductive check over all candidate invariants.
   *
   * @param pPredecessorAssumptions the set of assumptions that should be assumed at the predecessor
   *     states up to k.
   * @param pK The k value to use in the check.
   * @param pCandidateInvariant What should be checked at k + 1.
   * @param pCheckedKeys the keys of loop-iteration reporting states that were checked by BMC: only
   *     for those can we assert predecessor safety of an unproven candidate invariant.
   * @return <code>true</code> if k-induction successfully proved the correctness of all candidate
   *     invariants.
   * @throws CPAException if the bounded analysis constructing the step case encountered an
   *     exception.
   * @throws InterruptedException if the bounded analysis constructing the step case was
   *     interrupted.
   */
  public final boolean check(
      Iterable<CandidateInvariant> pPredecessorAssumptions,
      int pK,
      CandidateInvariant pCandidateInvariant,
      Set<Object> pCheckedKeys)
      throws CPAException, InterruptedException, SolverException {
    stats.inductionPreparation.start();

    // Proving program safety with induction consists of two parts:
    // 1) Prove all paths safe that go only one iteration through the loop.
    //    This is part of the classic bounded model checking done in BMCAlgorithm,
    //    so we don't care about this here.
    // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.
    // For k-induction, assume that k loop iterations are safe and prove that the next one is safe, too.

    // Create initial reached set:
    // Run algorithm in order to create formula (A & B)
    logger.log(Level.INFO, "Running algorithm to create induction hypothesis");
    LoopIterationBounding stepCaseBoundsCPA = CPAs.retrieveCPA(cpa, LoopIterationBounding.class);
    // Initialize the reached set if necessary
    ensureReachedSetInitialized(reached);

    /*
     * For every induction problem we want so solve, create a formula asserting
     * it for k iterations.
     */
    Map<CandidateInvariant, BooleanFormula> assertions = new HashMap<>();
    ReachedSet predecessorReachedSet = null;

    for (CandidateInvariant candidateInvariant : pPredecessorAssumptions) {
      shutdownNotifier.shutdownIfNecessary();

      final BooleanFormula predecessorAssertion;
      if (candidateInvariant == TargetLocationCandidateInvariant.INSTANCE
          && pCandidateInvariant == TargetLocationCandidateInvariant.INSTANCE) {
        // For the actual safety property, the predecessor safety assertion
        // is already implied by the successor violation:
        // Because we never continue after an error state,
        // a path that violates the property in iteration k+1
        // cannot have "passed through" any violation
        // in the previous iterations anyway.
        predecessorAssertion = bfmgr.makeBoolean(true);
      } else {
        // If we already built a formula for the violation of the invariant for
        // k (previous attempt), we can negate and reuse it here as an assertion
        BooleanFormula previousViolation = violationFormulas.get(candidateInvariant);
        if (previousViolation != null && previousK == pK) {
          predecessorAssertion = bfmgr.not(previousViolation);
        } else {
          // Build the formula
          if (predecessorReachedSet == null) {
            if (pK < 1) {
              predecessorReachedSet = reachedSetFactory.create();
            } else {
              predecessorReachedSet = reached;
              stepCaseBoundsCPA.setMaxLoopIterations(pK);
              BMCHelper.unroll(
                  logger, predecessorReachedSet, reachedSetInitializer, algorithm, cpa);
            }
          }
          predecessorAssertion =
              candidateInvariant.getAssertion(
                  filterBmcChecked(filterIterationsUpTo(predecessorReachedSet, pK), pCheckedKeys),
                  fmgr,
                  pfmgr);
        }
      }
      BooleanFormula storedAssertion = assertions.get(candidateInvariant);
      if (storedAssertion == null) {
        storedAssertion = bfmgr.makeBoolean(true);
      }
      assertions.put(candidateInvariant, bfmgr.and(storedAssertion, predecessorAssertion));
    }

    // Assert the known invariants at the loop head at end of the first iteration.
    stepCaseBoundsCPA.setMaxLoopIterations(pK + 1);
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);

    FluentIterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reached, loopHeads);

    BooleanFormula loopHeadInv = inductiveLoopHeadInvariantAssertion(loopHeadStates);
    this.previousK = pK + 1;
    ensureProverInitialized();
    stats.inductionPreparation.stop();

    // Attempt the induction proofs
    shutdownNotifier.shutdownIfNecessary();

    // Assert that *some* successor is reached
    BooleanFormula successorExistsAssertion =
        BMCHelper.createFormulaFor(filterEndStates(reached), bfmgr);

    // Obtain the predecessor assertion created earlier
    final BooleanFormula predecessorAssertion =
        bfmgr.and(
            from(CandidateInvariantConjunction.getConjunctiveParts(
                    CandidateInvariantConjunction.of(pPredecessorAssumptions)))
                .transform(conjunctivePart -> assertions.get(conjunctivePart))
                .toList());
    // Create the successor violation formula
    BooleanFormula successorViolation = getSuccessorViolation(pCandidateInvariant, reached, pK);
    // Record the successor violation formula to reuse its negation as an
    // assertion in a future induction attempt
    violationFormulas.put(pCandidateInvariant, successorViolation);

    logger.log(Level.INFO, "Starting induction check...");

    stats.inductionCheck.start();

    // Try to prove the invariance of the assertion
    prover.push(successorExistsAssertion);
    prover.push(loopHeadInv); // Assert the known invariants
    prover.push(predecessorAssertion); // Assert the formula we want to prove at the predecessors
    prover.push(successorViolation); // Assert that the formula is violated at a successor

    boolean isInvariant = false;
    boolean loopHeadInvChanged = true;
    boolean newInvariants = false;
    while (!isInvariant && loopHeadInvChanged) {
      shutdownNotifier.shutdownIfNecessary();

      // If we have new loop-head invariants, push them
      if (newInvariants) {
        prover.push(loopHeadInv);
      }

      // The formula is invariant if the assertions are contradicting
      isInvariant = prover.isUnsat();

      if (!isInvariant && logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Model returned for induction check:", prover.getModelAssignments());
      }

      // If we had new loop-head invariants, we also pushed them and need to pop them now
      if (newInvariants) {
        prover.pop();
      }

      // Re-attempt the proof immediately, if new invariants are available
      BooleanFormula oldLoopHeadInv = loopHeadInv;
      loopHeadInv = inductiveLoopHeadInvariantAssertion(loopHeadStates);
      loopHeadInvChanged = !loopHeadInv.equals(oldLoopHeadInv);
      if (loopHeadInvChanged) {
        newInvariants = true;
      }
    }

    // If the proof is successful, remove its violation formula from the cache
    if (isInvariant) {
      violationFormulas.remove(pCandidateInvariant);
    }

    prover.pop(); // Pop invariant successor violation
    prover.pop(); // Pop invariant predecessor assertion
    prover.pop(); // Pop loop head invariants
    prover.pop(); // Pop end states

    stats.inductionCheck.stop();

    logger.log(Level.FINER, "Soundness after induction check:", isInvariant);

    return isInvariant;
  }

  private BooleanFormula inductiveLoopHeadInvariantAssertion(
      Iterable<AbstractState> pLoopHeadStates) throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates = filterInductiveAssertionIteration(pLoopHeadStates);
    return BMCHelper.assertAt(loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr);
  }

  private BooleanFormula getSuccessorViolation(CandidateInvariant pCandidateInvariant,
      ReachedSet pReached, int pK) throws CPATransferException, InterruptedException {
    BooleanFormula assertion =
        pCandidateInvariant.getAssertion(filterIteration(pReached, pK + 1), fmgr, pfmgr);
    return bfmgr.not(assertion);
  }

  private FluentIterable<AbstractState> filterInductiveAssertionIteration(
      Iterable<AbstractState> pStates) {
    return filterIteration(pStates, 1);
  }

  private FluentIterable<AbstractState> filterIterationsBetween(
      Iterable<AbstractState> pStates, int pMinIt, int pMaxIt) {
    return FluentIterable.from(pStates)
        .filter(
            state -> {
              if (state == null) {
                return false;
              }
              LoopIterationReportingState ls =
                  AbstractStates.extractStateByType(state, LoopIterationReportingState.class);
              if (ls == null) {
                return false;
              }
              int minIt = convertIteration(pMinIt, state);
              int maxIt = convertIteration(pMaxIt, state);
              int actualIt = ls.getDeepestIteration();
              return minIt <= actualIt && actualIt <= maxIt;
            });
  }

  private FluentIterable<AbstractState> filterIterationsUpTo(
      Iterable<AbstractState> pStates, int pIteration) {
    return filterIterationsBetween(pStates, 1, pIteration);
  }

  private FluentIterable<AbstractState> filterIteration(
      Iterable<AbstractState> pStates, int pIteration) {
    return filterIterationsBetween(pStates, pIteration, pIteration);
  }

  private int convertIteration(int pIteration, AbstractState state) {
    /*
     * We want to consider as an "iteration" i
     * all states with loop-iteration counter i that are
     * - either target states or
     * - not at a loop head
     * and all states with loop-iteration counter i+1
     * that are at a loop head.
     *
     * Reason:
     * 1) A target state that is also a loop head
     * does not count as a loop-head for our purposes,
     * because the error "exits" the loop.
     * 2) It is more convenient to make a loop-head state "belong"
     * to the previous iteration instead of the one it starts.
     */
    return !AbstractStates.IS_TARGET_STATE.apply(state)
            && from(AbstractStates.extractLocations(state)).allMatch(loopHeads::contains)
        ? pIteration + 1
        : pIteration;
  }

  private static FluentIterable<AbstractState> filterBmcChecked(
      Iterable<AbstractState> pStates, Set<Object> pCheckedKeys) {
    return FluentIterable.from(pStates)
        .filter(
            pArg0 -> {
              if (pArg0 == null) {
                return false;
              }
              LoopIterationReportingState ls =
                  AbstractStates.extractStateByType(pArg0, LoopIterationReportingState.class);
              return ls != null && pCheckedKeys.contains(ls.getPartitionKey());
            });
  }

  private static FluentIterable<AbstractState> filterEndStates(Iterable<AbstractState> pStates) {
    return FluentIterable.from(pStates)
        .filter(
            s -> {
              ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
              return argState != null && argState.getChildren().isEmpty();
            });
  }

  private void ensureReachedSetInitialized(ReachedSet pReachedSet) throws InterruptedException {
    if (pReachedSet.size() > 1 || !cfa.getLoopStructure().isPresent()) {
      return;
    }
    for (Loop loop : cfa.getLoopStructure().get().getAllLoops()) {
      for (CFANode loopHead : from(loop.getLoopHeads()).filter(Predicates.in(loopHeads))) {
        Precision precision =
            cpa.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition());
        AbstractState initialState =
            cpa.getInitialState(loopHead, StateSpacePartition.getDefaultPartition());
        pReachedSet.add(initialState, precision);
      }
    }
  }

}
