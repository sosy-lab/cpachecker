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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.bounds.BoundsCPA;
import org.sosy_lab.cpachecker.cpa.bounds.BoundsState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.api.ProverEnvironment;
import org.sosy_lab.solver.api.SolverContext.ProverOptions;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;

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

  private final ReachedSetInitializer reachedSetInitializer = new ReachedSetInitializer() {

    @Override
    public void initialize(ReachedSet pReachedSet) throws CPAException, InterruptedException {
      ensureReachedSetInitialized(pReachedSet);
    }
  };

  private final InvariantGenerator invariantGenerator;

  private ProverEnvironment prover = null;

  private InvariantSupplier invariantsSupplier;

  private ExpressionTreeSupplier expressionTreeSupplier;

  private BooleanFormula loopHeadInvariants;

  private int stackDepth = 0;

  private final Map<CandidateInvariant, BooleanFormula> violationFormulas = Maps.newHashMap();

  private int previousK = -1;

  // The CandidateInvariants that have been proven to hold at the loop heads of {@link loop}.
  private final Set<CandidateInvariant> confirmedCandidates = new CopyOnWriteArraySet<>();

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
      ShutdownNotifier pShutdownNotifier) {
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
    loopHeadInvariants = bfmgr.makeBoolean(true);

    invariantsSupplier = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
    expressionTreeSupplier = ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
  }

  public Collection<CandidateInvariant> getConfirmedCandidates() {
    return from(confirmedCandidates).toSet();
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
   * Gets the prover environment to be used within the KInductionProver.
   *
   * This prover may be preinitialized with additional supporting invariants.
   * The presence of these invariants, including pushing them onto and
   * popping them off of the prover stack, is taken care of automatically.
   *
   * @return the prover environment to be used within the KInductionProver.
   */
  private ProverEnvironment getProver() {
    if (!isProverInitialized()) {
      prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS);
    }
    assert isProverInitialized();
    return prover;
  }

  private InvariantSupplier getCurrentInvariantSupplier() throws InterruptedException {
    if (!invariantGenerationRunning) {
      return invariantsSupplier;
    }
    try {
      return invariantGenerator.get();
    } catch (CPAException e) {
      logger.logUserException(Level.FINE, e, "Invariant generation failed.");
      invariantGenerationRunning = false;
      return invariantsSupplier;
    } catch (InterruptedException e) {
      shutdownNotifier.shutdownIfNecessary();
      logger.log(Level.FINE, "Invariant generation was cancelled.");
      logger.logDebugException(e);
      invariantGenerationRunning = false;
      return invariantsSupplier;
    }
  }

  private ExpressionTreeSupplier getCurrentExpressionTreeInvariantSupplier() throws InterruptedException {
    if (!invariantGenerationRunning) {
      return expressionTreeSupplier;
    }
    try {
      return invariantGenerator.getAsExpressionTree();
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
  private BooleanFormula getCurrentLoopHeadInvariants(Set<CFANode> pStopLocations) throws CPATransferException, InterruptedException {
    if (!bfmgr.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
      BooleanFormula lhi = bfmgr.makeBoolean(false);
      for (CFANode loopHead : pStopLocations) {
        lhi = bfmgr.or(lhi, getCurrentLocationInvariants(loopHead, fmgr, pfmgr));
      }
      loopHeadInvariants = lhi;
    }
    return loopHeadInvariants;
  }

  public BooleanFormula getCurrentLocationInvariants(CFANode pLocation, FormulaManagerView pFMGR, PathFormulaManager pPFMGR) throws CPATransferException, InterruptedException {
    InvariantSupplier currentInvariantsSupplier = getCurrentInvariantSupplier();
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();

    BooleanFormula invariant = bfmgr.makeBoolean(true);

    for (CandidateInvariant candidateInvariant :
        getConfirmedCandidates(pLocation).filter(CandidateInvariant.class)) {
      invariant = bfmgr.and(invariant, candidateInvariant.getFormula(pFMGR, pPFMGR));
    }

    invariant =
        bfmgr.and(invariant, currentInvariantsSupplier.getInvariantFor(pLocation, pFMGR, pPFMGR));

    return invariant;
  }

  public ExpressionTree<Object> getCurrentLocationInvariants(CFANode pLocation)
      throws InterruptedException {
    ExpressionTreeSupplier currentInvariantsSupplier = getCurrentExpressionTreeInvariantSupplier();

    ExpressionTree<Object> invariant = ExpressionTrees.getTrue();

    for (ExpressionTreeCandidateInvariant expressionTreeCandidateInvariant :
        getConfirmedCandidates(pLocation).filter(ExpressionTreeCandidateInvariant.class)) {
      invariant = And.of(invariant, expressionTreeCandidateInvariant.asExpressionTree());
      if (ExpressionTrees.getFalse().equals(invariant)) {
        break;
      }
    }

    invariant = And.of(invariant, currentInvariantsSupplier.getInvariantFor(pLocation));

    return invariant;
  }

  private FluentIterable<LocationFormulaInvariant> getConfirmedCandidates(final CFANode pLocation) {
    return from(confirmedCandidates)
        .filter(LocationFormulaInvariant.class)
        .filter(
            new Predicate<LocationFormulaInvariant>() {

              @Override
              public boolean apply(LocationFormulaInvariant pConfirmedCandidate) {
                return pConfirmedCandidate.getLocations().contains(pLocation);
              }
            });
  }

  @Override
  public void close() {
    if (isProverInitialized()) {
      while (stackDepth-- > 0 && !shutdownNotifier.shouldShutdown()) {
        prover.pop();
      }
      prover.close();
    }
  }

  /**
   * Pops the last formula from the prover stack.
   */
  private void pop() {
    Preconditions.checkState(isProverInitialized());
    Preconditions.checkState(stackDepth > 0);
    prover.pop();
    --stackDepth;
  }

  /**
   * Pushes the given formula to the prover stack.
   *
   * @param pFormula the formula to be pushed.
   */
  private void push(BooleanFormula pFormula) {
    Preconditions.checkState(isProverInitialized());
    prover.push(pFormula);
    ++stackDepth;
  }

  /**
   * Attempts to perform the inductive check over all candidate invariants.
   *
   * @param k The k value to use in the check.
   * @param candidateInvariants What should be checked.
   * @return <code>true</code> if k-induction successfully proved the
   * correctness of all candidate invariants.
   *
   * @throws CPAException if the bounded analysis constructing the step case
   * encountered an exception.
   * @throws InterruptedException if the bounded analysis constructing the
   * step case was interrupted.
   */
  public final boolean check(final int k,
      final Set<CandidateInvariant> candidateInvariants)
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

    BoundsCPA stepCaseBoundsCPA = CPAs.retrieveCPA(cpa, BoundsCPA.class);

    // Initialize the reached set if necessary
    ensureReachedSetInitialized(reached);

    /*
     * For every induction problem we want so solve, create a formula asserting
     * it for k iterations.
     */
    Map<CandidateInvariant, BooleanFormula> assertions = new HashMap<>();
    ReachedSet predecessorReachedSet = null;
    for (CandidateInvariant candidateInvariant : candidateInvariants) {

      final BooleanFormula predecessorAssertion;

      // If we already built a formula for the violation of the invariant for
      // k (previous attempt), we can negate and reuse it here as an assertion
      BooleanFormula previousViolation = violationFormulas.get(candidateInvariant);
      if (previousViolation != null && previousK == k) {
        predecessorAssertion = bfmgr.not(previousViolation);
      } else {
        // Build the formula
        if (predecessorReachedSet == null) {
          if (k < 1) {
            predecessorReachedSet = reachedSetFactory.create();
          } else {
            predecessorReachedSet = reached;
            stepCaseBoundsCPA.setMaxLoopIterations(k);
            BMCHelper.unroll(logger, predecessorReachedSet, reachedSetInitializer, algorithm, cpa);
          }
        }
        predecessorAssertion = candidateInvariant.getAssertion(predecessorReachedSet, fmgr, pfmgr);
      }
      assertions.put(candidateInvariant, predecessorAssertion);
    }

    // Assert the known invariants at the loop head at the first iteration.
    Set<CFANode> stopLocations = getStopLocations(reached);
    Iterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reached, stopLocations).filter(new Predicate<AbstractState>() {

          @Override
          public boolean apply(AbstractState pArg0) {
            if (pArg0 == null) {
              return false;
            }
            BoundsState ls = AbstractStates.extractStateByType(pArg0, BoundsState.class);
            return ls != null && ls.getDeepestIteration() <= 1;
          }});
    BooleanFormula invariants = getCurrentLoopHeadInvariants(stopLocations);
    BooleanFormula loopHeadInv = bfmgr.and(from(BMCHelper.assertAt(loopHeadStates, invariants, fmgr)).toList());
    for (CandidateInvariant candidateInvariant : confirmedCandidates) {
      loopHeadInv = bfmgr.and(loopHeadInv, candidateInvariant.getAssertion(reached, fmgr, pfmgr));
    }

    // Create the formula asserting the faultiness of the successor
    stepCaseBoundsCPA.setMaxLoopIterations(k + 1);
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);
    stopLocations = getStopLocations(reached);

    this.previousK = k + 1;

    // Attempt the induction proofs
    ProverEnvironment prover = getProver();
    int numberOfSuccessfulProofs = 0;
    stats.inductionPreparation.stop();
    push(loopHeadInv); // Assert the known invariants
    for (CandidateInvariant candidateInvariant : candidateInvariants) {

      // Obtain the predecessor assertion created earlier
      BooleanFormula predecessorAssertion = assertions.get(candidateInvariant);
      // Create the successor violation formula
      BooleanFormula successorViolation = bfmgr.not(candidateInvariant.getAssertion(reached, fmgr, pfmgr));
      // Record the successor violation formula to reuse its negation as an
      // assertion in a future induction attempt
      violationFormulas.put(candidateInvariant, successorViolation);


      logger.log(Level.INFO, "Starting induction check...");

      stats.inductionCheck.start();

      // Try to prove the invariance of the assertion
      push(predecessorAssertion); // Assert the formula we want to prove at the predecessors
      push(successorViolation); // Assert that the formula is violated at a successor

      // The formula is invariant if the assertions are contradicting
      boolean isInvariant = prover.isUnsat();

      if (!isInvariant && logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
      }

      // Re-attempt the proof immediately, if new invariants are available
      BooleanFormula oldInvariants = invariants;
      BooleanFormula currentInvariants = getCurrentLoopHeadInvariants(stopLocations);
      boolean newInvariants = false;
      while (!isInvariant && !currentInvariants.equals(oldInvariants)) {
        invariants = getCurrentLoopHeadInvariants(stopLocations);
        loopHeadInv = bfmgr.and(from(BMCHelper.assertAt(loopHeadStates, invariants, fmgr)).toList());
        for (CandidateInvariant ci : confirmedCandidates) {
          loopHeadInv = bfmgr.and(loopHeadInv, ci.getAssertion(reached, fmgr, pfmgr));
        }
        newInvariants = true;
        push(loopHeadInv);
        isInvariant = prover.isUnsat();

        if (!isInvariant && logger.wouldBeLogged(Level.ALL)) {
          logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
        }

        pop();
        oldInvariants = currentInvariants;
        currentInvariants = getCurrentLoopHeadInvariants(stopLocations);
      }

      // If the proof is successful, move the problem from the set of open
      // problems to the set of solved problems
      if (isInvariant) {
        ++numberOfSuccessfulProofs;
        confirmedCandidates.add(candidateInvariant);
        violationFormulas.remove(candidateInvariant);

        // Try to inject the new invariant into the invariant generator
        candidateInvariant.attemptInjection(invariantGenerator);
      }
      pop(); // Pop invariant successor violation
      pop(); // Pop invariant predecessor assertion
      if (isInvariant) {
        // Add confirmed candidate
        loopHeadInv = bfmgr.and(loopHeadInv, candidateInvariant.getAssertion(reached, fmgr, pfmgr));
        newInvariants = true;
      }
      // Update invariants if required
      if (newInvariants) {
        pop();
        push(loopHeadInv);
      }
      stats.inductionCheck.stop();

      logger.log(Level.FINER, "Soundness after induction check:", isInvariant);
    }

    pop(); // Pop loop head invariants

    return numberOfSuccessfulProofs == candidateInvariants.size();
  }

  private void ensureReachedSetInitialized(ReachedSet pReachedSet) {
    if (pReachedSet.size() > 1 || !cfa.getLoopStructure().isPresent()) {
      return;
    }
    for (Loop loop : cfa.getLoopStructure().get().getAllLoops()) {
      for (CFANode loopHead : loop.getLoopHeads()) {
        Precision precision =
            cpa.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition());
        AbstractState initialState =
            cpa.getInitialState(loopHead, StateSpacePartition.getDefaultPartition());
        pReachedSet.add(initialState, precision);
      }
    }
  }

  private static Set<CFANode> getStopLocations(ReachedSet pReachedSet) {
    return from(pReachedSet).filter(BMCAlgorithm.IS_STOP_STATE).transform(AbstractStates.EXTRACT_LOCATION).toSet();
  }

}
