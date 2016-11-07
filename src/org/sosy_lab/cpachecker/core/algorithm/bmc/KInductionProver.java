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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
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
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
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

  private InvariantSupplier invariantsSupplier;

  private ExpressionTreeSupplier expressionTreeSupplier;

  private BooleanFormula loopHeadInvariants;

  private int stackDepth = 0;

  private final Map<CandidateInvariant, BooleanFormula> violationFormulas = Maps.newHashMap();

  private int previousK = -1;

  // The CandidateInvariants that have been proven to hold at the loop heads of {@link loop}.
  private final Set<CandidateInvariant> confirmedCandidates = new CopyOnWriteArraySet<>();

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

    invariantsSupplier = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
    expressionTreeSupplier = ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;

    loopHeads = ImmutableSet.copyOf(pLoopHeads);
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
      if (invariantGenerator instanceof KInductionInvariantGenerator) {
        return ((KInductionInvariantGenerator) invariantGenerator).getSupplier();
      } else {
        // in the general case we have to retrieve the invariants from a reachedset
        return new FormulaInvariantsSupplier(invariantGenerator.get(), logger);
      }
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
      final FluentIterable<AbstractState> stopStates) {
    return pContext -> {
      if (!bfmgr.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
        BooleanFormula lhi = bfmgr.makeFalse();
        for (AbstractState state : stopStates) {
          CFANode loopHead = AbstractStates.extractLocation(state);
          lhi = bfmgr.or(
              lhi,
              getCurrentLocationInvariants(loopHead, fmgr, pfmgr, pContext));
        }
        loopHeadInvariants = lhi;
      }
      return loopHeadInvariants;
    };
  }

  public BooleanFormula getCurrentLocationInvariants(
      CFANode pLocation,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      PathFormula pContext)
      throws CPATransferException, InterruptedException {
    InvariantSupplier currentInvariantsSupplier = getCurrentInvariantSupplier();
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();

    BooleanFormula invariant = bfmgr.makeTrue();

    for (CandidateInvariant candidateInvariant :
          getConfirmedCandidates(pLocation).filter(CandidateInvariant.class)) {
      invariant = bfmgr.and(invariant, candidateInvariant.getFormula(pFMGR, pPFMGR, pContext));
    }

    invariant =
        bfmgr.and(
            invariant,
            currentInvariantsSupplier.getInvariantFor(
                pLocation, Optional.empty(), pFMGR, pPFMGR, pContext));

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

  private FluentIterable<LocationFormulaInvariant> getConfirmedCandidates(
      final CFANode pLocation) {
    return from(confirmedCandidates)
        .filter(LocationFormulaInvariant.class)
        .filter(
            pConfirmedCandidate -> pConfirmedCandidate.getLocations().contains(pLocation));
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
   * @param pK The k value to use in the check.
   * @param pCandidateInvariants What should be checked.
   * @return <code>true</code> if k-induction successfully proved the
   * correctness of all candidate invariants.
   *
   * @throws CPAException if the bounded analysis constructing the step case
   * encountered an exception.
   * @throws InterruptedException if the bounded analysis constructing the
   * step case was interrupted.
   */
  public final boolean check(
      final int pK,
      final Set<CandidateInvariant> pCandidateInvariants,
      final Set<CFANode> pImmediateLoopHeads)
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

    Set<CandidateInvariantConjunction> artificialConjunctions =
        buildArtificialConjunctions(pCandidateInvariants);
    Iterable<CandidateInvariant> candidatesToCheck = Iterables.concat(pCandidateInvariants, artificialConjunctions);
    for (CandidateInvariant candidateInvariant : candidatesToCheck) {

      if (!canBeAsserted(candidateInvariant, pImmediateLoopHeads)) {
        assertions.put(candidateInvariant, bfmgr.makeTrue());
        continue;
      }

      final BooleanFormula predecessorAssertion;

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
            BMCHelper.unroll(logger, predecessorReachedSet, reachedSetInitializer, algorithm, cpa);
          }
        }
        predecessorAssertion =
            candidateInvariant.getAssertion(predecessorReachedSet, fmgr, pfmgr, 1);
      }
      assertions.put(candidateInvariant, predecessorAssertion);
    }

    // Assert the known invariants at the loop head at the first iteration.

    FluentIterable<AbstractState> loopHeadStates = filterLoopHeadStates(reached, loopHeads);
    BooleanFormula loopHeadInv =
        bfmgr.and(
            BMCHelper.assertAt(
                loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr, 1));
    BooleanFormula invariants = loopHeadInv;
    for (CandidateInvariant candidateInvariant : confirmedCandidates) {
      invariants = bfmgr.and(invariants, candidateInvariant.getAssertion(reached, fmgr, pfmgr, 1));
    }

    // Create the formula asserting the faultiness of the successor
    stepCaseBoundsCPA.setMaxLoopIterations(pK + 1);
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);
    loopHeadStates = filterLoopHeadStates(reached, loopHeads);

    this.previousK = pK + 1;

    // Attempt the induction proofs
    ProverEnvironment prover = getProver();
    int numberOfSuccessfulProofs = 0;
    stats.inductionPreparation.stop();
    push(invariants); // Assert the known invariants

    for (CandidateInvariant candidateInvariant : candidatesToCheck) {
      if (artificialConjunctions.contains(candidateInvariant)
          && isSizeLessThanOrEqualTo(((CandidateInvariantConjunction) candidateInvariant).getElements(), 1)) {
        continue;
      }

      // Obtain the predecessor assertion created earlier
      BooleanFormula predecessorAssertion = assertions.get(candidateInvariant);
      // Create the successor violation formula
      BooleanFormula successorViolation =
          bfmgr.not(candidateInvariant.getAssertion(reached, fmgr, pfmgr, 1));
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
        logger.log(Level.ALL, "Model returned for induction check:", prover.getModelAssignments());
      }

      // Re-attempt the proof immediately, if new invariants are available
      boolean newInvariants = false;
      BooleanFormula oldLoopHeadInv = loopHeadInv;
      loopHeadInv =
          bfmgr.and(
              BMCHelper.assertAt(
                  loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr, 1));
      while (!isInvariant && !loopHeadInv.equals(oldLoopHeadInv)) {
        invariants = loopHeadInv;
        for (CandidateInvariant ci : confirmedCandidates) {
          invariants = bfmgr.and(invariants, ci.getAssertion(reached, fmgr, pfmgr, 1));
        }
        newInvariants = true;
        push(invariants);
        isInvariant = prover.isUnsat();

        if (!isInvariant && logger.wouldBeLogged(Level.ALL)) {
          logger.log(Level.ALL, "Model returned for induction check:", prover.getModelAssignments());
        }

        pop();
        oldLoopHeadInv = loopHeadInv;
        loopHeadInv =
            bfmgr.and(
                BMCHelper.assertAt(
                    loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr, 1));
      }

      // If the proof is successful, move the problem from the set of open
      // problems to the set of solved problems
      if (isInvariant) {
        if (artificialConjunctions.contains(candidateInvariant)) {
          for (CandidateInvariant element : ((CandidateInvariantConjunction) candidateInvariant).getElements()) {
            ++numberOfSuccessfulProofs;
            confirmedCandidates.add(element);
          }
        } else {
          ++numberOfSuccessfulProofs;
          confirmedCandidates.add(candidateInvariant);
        }
        violationFormulas.remove(candidateInvariant);
      }
      pop(); // Pop invariant successor violation
      pop(); // Pop invariant predecessor assertion
      if (isInvariant) {
        // Add confirmed candidate
        loopHeadInv =
            bfmgr.and(loopHeadInv, candidateInvariant.getAssertion(reached, fmgr, pfmgr, 1));
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

    return numberOfSuccessfulProofs == pCandidateInvariants.size();
  }

  private FluentIterable<AbstractState> filterLoopHeadStates(ReachedSet pReached,
      ImmutableSet<CFANode> pLoopHeads) {
    return AbstractStates.filterLocations(pReached, pLoopHeads)
        .filter(
            pArg0 -> {
              if (pArg0 == null) {
                return false;
              }
              BoundsState ls = AbstractStates.extractStateByType(pArg0, BoundsState.class);
              return ls != null && (ls.getDeepestIteration() == 1 || ls.getDeepestIteration() == 2);
            });
  }

  private static boolean isSizeLessThanOrEqualTo(Iterable<?> pElements, int pLimit) {
    return Iterables.isEmpty(Iterables.skip(pElements, pLimit));
  }

  private Set<CandidateInvariantConjunction> buildArtificialConjunctions(
      final Set<CandidateInvariant> pCandidateInvariants) {
    FluentIterable<? extends LocationFormulaInvariant> remainingLoopHeadCandidateInvariants = from(pCandidateInvariants)
        .filter(LocationFormulaInvariant.class)
        .filter(pLocationFormulaInvariant -> {
          for (CFANode location : pLocationFormulaInvariant.getLocations()) {
            if (!location.isLoopStart()) {
              return cfa.getLoopStructure().isPresent() && cfa.getLoopStructure().get().getAllLoopHeads().contains(location);
            }
          }
          return true;
        })
        .filter(Predicates.not(Predicates.in(confirmedCandidates)));
    if (remainingLoopHeadCandidateInvariants.isEmpty()) {
      return Collections.emptySet();
    }
    CandidateInvariantConjunction artificialConjunction = CandidateInvariantConjunction.of(remainingLoopHeadCandidateInvariants);
    Set<CandidateInvariantConjunction> artificialConjunctions = Sets.newHashSet();

    Multimap<String, LocationFormulaInvariant> functionInvariants = HashMultimap.create();
    for (LocationFormulaInvariant locationFormulaInvariant : remainingLoopHeadCandidateInvariants) {
      for (CFANode location : locationFormulaInvariant.getLocations()) {
        functionInvariants.put(location.getFunctionName(), locationFormulaInvariant);
      }
    }
    for (Map.Entry<String, Collection<LocationFormulaInvariant>> functionInvariantsEntry : functionInvariants.asMap().entrySet()) {
      if (functionInvariantsEntry.getValue().size() > 1
          && functionInvariantsEntry.getValue().size() < remainingLoopHeadCandidateInvariants.size()) {
        // Use filter instead of computed collection
        // so that it is updated dynamically if the underlying collection is updated
        artificialConjunctions.add(CandidateInvariantConjunction.of(
            remainingLoopHeadCandidateInvariants.filter(
                (Predicate<LocationFormulaInvariant>) pLocationFormulaInvariant -> {
                  for (CFANode location : pLocationFormulaInvariant.getLocations()) {
                    if (location.getFunctionName().equals(functionInvariantsEntry.getKey())) {
                      return true;
                    }
                  }
                  return false;
                })));
      }
    }

    artificialConjunctions.add(artificialConjunction);

    return artificialConjunctions;
  }

  /**
   * Check if the given invariant may be asserted at k predecessors,
   * given all loop heads reachable without unrolling any loops.
   *
   * @param pCandidateInvariant the candidate invariant.
   * @param pImmediateLoopHeads all loop heads reachable without unrolling any loops.
   * @return {@code true} if the invariant may be asserted for k predecessors,
   * {@code false} otherwise.
   */
  private boolean canBeAsserted(
      CandidateInvariant pCandidateInvariant, Set<CFANode> pImmediateLoopHeads) {
    if (pCandidateInvariant instanceof LocationFormulaInvariant) {
      for (CFANode immediateLoopHead : pImmediateLoopHeads) {
        for (Loop loop : cfa.getLoopStructure().get().getLoopsForLoopHead(immediateLoopHead)) {
          if (loop.getLoopNodes()
              .containsAll(((LocationFormulaInvariant) pCandidateInvariant).getLocations())) {
            return true;
          }
        }
      }
      return false;
    }
    return true;
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
