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
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationBounding;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
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
      final FluentIterable<AbstractState> pAssertionStates) {
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
      CFANode pLocation,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      PathFormula pContext)
      throws CPATransferException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    InvariantSupplier currentInvariantsSupplier = getCurrentInvariantSupplier();
    BooleanFormulaManager booleanFormulaManager = pFMGR.getBooleanFormulaManager();

    BooleanFormula invariant = booleanFormulaManager.makeTrue();

    for (CandidateInvariant candidateInvariant : getConfirmedCandidates(pLocation)) {
      invariant = booleanFormulaManager.and(invariant, candidateInvariant.getFormula(pFMGR, pPFMGR, pContext));
    }

    invariant =
        booleanFormulaManager.and(
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

  private FluentIterable<CandidateInvariant> getConfirmedCandidates(final CFANode pLocation) {
    return from(confirmedCandidates)
        .filter(pConfirmedCandidate -> pConfirmedCandidate.appliesTo(pLocation));
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
   * @return <code>true</code> if k-induction successfully proved the correctness of all candidate invariants.
   * @throws CPAException if the bounded analysis constructing the step case encountered an exception.
   * @throws InterruptedException if the bounded analysis constructing the step case was interrupted.
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

    LoopIterationBounding stepCaseBoundsCPA = CPAs.retrieveCPA(cpa, LoopIterationBounding.class);

    // Initialize the reached set if necessary
    ensureReachedSetInitialized(reached);

    /*
     * For every induction problem we want so solve, create a formula asserting
     * it for k iterations.
     */
    Map<CandidateInvariant, BooleanFormula> assertions = new HashMap<>();
    ReachedSet predecessorReachedSet = null;

    for (CandidateInvariant candidateInvariant : pCandidateInvariants) {
      shutdownNotifier.shutdownIfNecessary();

      if (!canBeAsserted(candidateInvariant, pImmediateLoopHeads)) {
        assertions.put(candidateInvariant, bfmgr.makeTrue());
        continue;
      }

      final BooleanFormula predecessorAssertion;
      if (candidateInvariant instanceof TargetLocationCandidateInvariant) {
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
              BMCHelper.unroll(logger, predecessorReachedSet, reachedSetInitializer, algorithm, cpa);
            }
          }
          predecessorAssertion =
              candidateInvariant.getAssertion(predecessorReachedSet, fmgr, pfmgr);
        }
      }
      assertions.put(candidateInvariant, predecessorAssertion);
    }

    // Assert the known invariants at the loop head at end of the first iteration.

    stepCaseBoundsCPA.setMaxLoopIterations(pK + 1);
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);

    // We cannot use the initial loop-head state,
    // because its SSA map and pointer-target sets are empty.
    // We also do not want to always use the last state,
    // because it may lead to unnecessarily large formulas.
    FluentIterable<AbstractState> loopHeadStates =
        filterIteration(AbstractStates.filterLocations(reached, loopHeads), 2);

    BooleanFormula loopHeadInv =
        bfmgr.and(
            BMCHelper.assertAt(loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr));
    BooleanFormula confirmedCandidateInvariants = bfmgr.makeTrue();
    for (CandidateInvariant candidateInvariant : confirmedCandidates) {
      shutdownNotifier.shutdownIfNecessary();
      confirmedCandidateInvariants =
          bfmgr.and(
              confirmedCandidateInvariants,
              candidateInvariant.getAssertion(filterIteration(reached, 2), fmgr, pfmgr));
    }

    // Create the formula asserting the faultiness of the successor

    this.previousK = pK + 1;

    // Attempt the induction proofs
    ensureProverInitialized();
    int numberOfSuccessfulProofs = 0;
    stats.inductionPreparation.stop();
    push(bfmgr.and(loopHeadInv, confirmedCandidateInvariants)); // Assert the known invariants

    Iterable<CandidateInvariant> artificialConjunctions =
        buildArtificialConjunctions(pCandidateInvariants);
    Iterable<CandidateInvariant> candidatesToCheck =
        Iterables.concat(pCandidateInvariants, artificialConjunctions);
    for (CandidateInvariant candidateInvariant : candidatesToCheck) {
      shutdownNotifier.shutdownIfNecessary();

      // Obtain the predecessor assertion created earlier
      final BooleanFormula predecessorAssertion =
          bfmgr.and(
              from(CandidateInvariantConjunction.getConjunctiveParts(candidateInvariant))
                  .transform(conjunctivePart -> assertions.get(conjunctivePart))
                  .toList());
      // Create the successor violation formula
      BooleanFormula successorViolation = getSuccessorViolation(candidateInvariant, reached, pK);
      // Record the successor violation formula to reuse its negation as an
      // assertion in a future induction attempt
      violationFormulas.put(candidateInvariant, successorViolation);


      logger.log(Level.INFO, "Starting induction check...");

      stats.inductionCheck.start();

      // Try to prove the invariance of the assertion
      push(predecessorAssertion); // Assert the formula we want to prove at the predecessors
      push(successorViolation); // Assert that the formula is violated at a successor

      boolean newInvariants = false;
      boolean isInvariant = false;
      boolean loopHeadInvChanged = true;
      while (!isInvariant && loopHeadInvChanged) {
        shutdownNotifier.shutdownIfNecessary();

        // If we have new loop-head invariants, push them
        if (newInvariants) {
          push(loopHeadInv);
        }

        // The formula is invariant if the assertions are contradicting
        isInvariant = prover.isUnsat();

        if (!isInvariant && logger.wouldBeLogged(Level.ALL)) {
          logger.log(Level.ALL, "Model returned for induction check:", prover.getModelAssignments());
        }

        // If we had new loop-head invariants, we also pushed them and need to pop them now
        if (newInvariants) {
          pop();
        }

        // Re-attempt the proof immediately, if new invariants are available
        BooleanFormula oldLoopHeadInv = loopHeadInv;
        loopHeadInv =
            bfmgr.and(
                BMCHelper.assertAt(
                    loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr));
        loopHeadInvChanged = !loopHeadInv.equals(oldLoopHeadInv);
        if (loopHeadInvChanged) {
          newInvariants = true;
        }
      }

      // If the proof is successful, move the problem from the set of open
      // problems to the set of solved problems
      if (isInvariant) {
        for (CandidateInvariant conjunctivePart :
            CandidateInvariantConjunction.getConjunctiveParts(candidateInvariant)) {
          ++numberOfSuccessfulProofs;
          confirmedCandidates.add(conjunctivePart);
        }
        violationFormulas.remove(candidateInvariant);
      }
      pop(); // Pop invariant successor violation
      pop(); // Pop invariant predecessor assertion
      if (isInvariant) {
        // Add confirmed candidate
        confirmedCandidateInvariants =
            bfmgr.and(
                confirmedCandidateInvariants,
                candidateInvariant.getAssertion(filterIteration(reached, 2), fmgr, pfmgr));
        newInvariants = true;
      }
      // Update invariants if required
      if (newInvariants) {
        pop();
        push(bfmgr.and(loopHeadInv, confirmedCandidateInvariants));
      }
      stats.inductionCheck.stop();

      logger.log(Level.FINER, "Soundness after induction check:", isInvariant);
    }

    pop(); // Pop loop head invariants

    return numberOfSuccessfulProofs == pCandidateInvariants.size();
  }

  private BooleanFormula getSuccessorViolation(CandidateInvariant pCandidateInvariant,
      ReachedSet pReached, int pK) throws CPATransferException, InterruptedException {
    FluentIterable<AbstractState> states = FluentIterable.from(pReached);
    if (pCandidateInvariant instanceof TargetLocationCandidateInvariant) {
      states = filterIteration(states, pK + 1);
    }
    BooleanFormula assertion = pCandidateInvariant.getAssertion(states, fmgr, pfmgr);
    return bfmgr.not(assertion);
  }

  private FluentIterable<AbstractState> filterIteration(
      Iterable<AbstractState> pStates, int pIteration) {
    return FluentIterable.from(pStates).filter(
        pArg0 -> {
          if (pArg0 == null) {
            return false;
          }
          LoopIterationReportingState ls = AbstractStates.extractStateByType(pArg0, LoopIterationReportingState.class);
          return ls != null && ls.getDeepestIteration() == pIteration;
        });
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
                  next = CandidateInvariantConjunction.of(remainingFunctionCandidateInvariants);
                }
              }
            }

            // Create the conjunction over all operands, if we have not done so yet
            if (next == null && !allComputed && remainingLoopHeadCandidateInvariants.size() > 1) {
              allComputed = true;
              next = CandidateInvariantConjunction.of(remainingLoopHeadCandidateInvariants);
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
    for (CFANode immediateLoopHead : pImmediateLoopHeads) {
      for (Loop loop : cfa.getLoopStructure().get().getLoopsForLoopHead(immediateLoopHead)) {
        if (loop.getLoopNodes().stream().anyMatch(lh -> pCandidateInvariant.appliesTo(lh))) {
          return true;
        }
      }
    }
    return false;
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
