// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.assertAt;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.createFormulaFor;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterIteration;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterIterationsUpTo;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.unroll;

import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.FormulaInContext;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InvariantStrengthening.NextCti;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariantCombination;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.input.InputState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

/**
 * Instances of this class are used to prove the safety of a program by applying an inductive
 * approach based on k-induction.
 */
class KInductionProver implements AutoCloseable {

  private final CFA cfa;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  private final Algorithm algorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final UnrolledReachedSet reachedSet;

  private final FormulaManagerView fmgr;

  private final BooleanFormulaManagerView bfmgr;

  private final PathFormulaManager pfmgr;

  private final PredicateAbstractionManager pam;

  private final BMCStatistics stats;

  private final ReachedSetFactory reachedSetFactory;

  private final InvariantGenerator invariantGenerator;

  private final ProverEnvironmentWithFallback prover;

  private ExpressionTreeSupplier expressionTreeSupplier;

  private BooleanFormula loopHeadInvariants;

  private final Map<CandidateInvariant, BooleanFormula> violationFormulas = new HashMap<>();

  private int previousK = -1;

  private final ImmutableSet<CFANode> loopHeads;

  private boolean invariantGenerationRunning = true;

  /** Creates an instance of the KInductionProver. */
  public KInductionProver(
      CFA pCFA,
      LogManager pLogger,
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      InvariantGenerator pInvariantGenerator,
      BMCStatistics pStats,
      ReachedSetFactory pReachedSetFactory,
      ShutdownNotifier pShutdownNotifier,
      Set<CFANode> pLoopHeads,
      boolean pUnsatCoreGeneration) {
    cfa = checkNotNull(pCFA);
    logger = checkNotNull(pLogger);
    algorithm = checkNotNull(pAlgorithm);
    cpa = checkNotNull(pCPA);
    invariantGenerator = checkNotNull(pInvariantGenerator);
    stats = checkNotNull(pStats);
    reachedSetFactory = checkNotNull(pReachedSetFactory);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    reachedSet =
        new UnrolledReachedSet(
            algorithm, cpa, pLoopHeads, reachedSetFactory.create(cpa), this::ensureK);

    @SuppressWarnings("resource")
    PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    if (pUnsatCoreGeneration) {
      prover =
          new ProverEnvironmentWithFallback(
              stepCasePredicateCPA.getSolver(),
              ProverOptions.GENERATE_UNSAT_CORE,
              ProverOptions.GENERATE_MODELS);
    } else {
      prover =
          new ProverEnvironmentWithFallback(
              stepCasePredicateCPA.getSolver(), ProverOptions.GENERATE_MODELS);
    }
    fmgr = stepCasePredicateCPA.getSolver().getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = stepCasePredicateCPA.getPathFormulaManager();
    pam = stepCasePredicateCPA.getPredicateManager();
    loopHeadInvariants = bfmgr.makeTrue();

    expressionTreeSupplier = ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;

    loopHeads = ImmutableSet.copyOf(pLoopHeads);
  }

  private InvariantSupplier getCurrentInvariantSupplier() throws InterruptedException {
    if (invariantGenerationRunning) {
      try {
        return invariantGenerator.getSupplier();
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

  private ExpressionTreeSupplier getCurrentExpressionTreeInvariantSupplier()
      throws InterruptedException {
    if (!invariantGenerationRunning) {
      return expressionTreeSupplier;
    }
    try {
      return invariantGenerator.getExpressionTreeSupplier();
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
  private FormulaInContext getCurrentLoopHeadInvariants(Iterable<AbstractState> pAssertionStates) {
    Set<CFANode> stopLoopHeads =
        AbstractStates.extractLocations(AbstractStates.filterLocations(pAssertionStates, loopHeads))
            .toSet();
    return pContext -> {
      shutdownNotifier.shutdownIfNecessary();
      if (!bfmgr.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
        BooleanFormula lhi = bfmgr.makeFalse();
        for (CFANode loopHead : stopLoopHeads) {
          lhi = bfmgr.or(lhi, getCurrentLocationInvariants(loopHead, fmgr, pfmgr, pContext));
          shutdownNotifier.shutdownIfNecessary();
        }
        loopHeadInvariants = lhi;
      }
      return loopHeadInvariants;
    };
  }

  public BooleanFormula getCurrentLocationInvariants(
      CFANode pLocation,
      FormulaManagerView pFormulaManager,
      PathFormulaManager pPathFormulaManager,
      PathFormula pContext)
      throws InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    InvariantSupplier currentInvariantsSupplier = getCurrentInvariantSupplier();

    return currentInvariantsSupplier.getInvariantFor(
        pLocation, Optional.empty(), pFormulaManager, pPathFormulaManager, pContext);
  }

  public ExpressionTree<Object> getCurrentLocationInvariants(CFANode pLocation)
      throws InterruptedException {
    ExpressionTreeSupplier currentInvariantsSupplier = getCurrentExpressionTreeInvariantSupplier();

    return currentInvariantsSupplier.getInvariantFor(pLocation);
  }

  @Override
  public void close() {
    prover.close();
  }

  /**
   * Attempts to perform the inductive check over the candidate invariant.
   *
   * @param pPredecessorAssumptions the set of assumptions that should be assumed at the predecessor
   *     states up to k.
   * @param pK The k value to use in the check.
   * @param pCandidateInvariant What should be checked at k + 1.
   * @param pCheckedKeys the keys of loop-iteration reporting states that were checked by BMC: only
   *     for those can we assert predecessor safety of an unproven candidate invariant.
   * @return <code>true</code> if k-induction successfully proved the correctness of the candidate
   *     invariant, <code>false</code> and a model otherwise.
   * @throws CPAException if the bounded analysis constructing the step case encountered an
   *     exception.
   * @throws InterruptedException if the bounded analysis constructing the step case was
   *     interrupted.
   */
  public final InductionResult<CandidateInvariant> check(
      Iterable<CandidateInvariant> pPredecessorAssumptions,
      int pK,
      CandidateInvariant pCandidateInvariant,
      Set<Object> pCheckedKeys)
      throws CPAException, InterruptedException, SolverException {
    return check(
        pPredecessorAssumptions,
        pK,
        pCandidateInvariant,
        pCheckedKeys,
        InvariantStrengthenings.noStrengthening(),
        StandardLiftings.NO_LIFTING);
  }

  /**
   * Attempts to perform the inductive check over the candidate invariant.
   *
   * @param pPredecessorAssumptions the set of assumptions that should be assumed at the predecessor
   *     states up to k.
   * @param pK The k value to use in the check.
   * @param pCandidateInvariant What should be checked at k + 1.
   * @param pCheckedKeys the keys of loop-iteration reporting states that were checked by BMC: only
   *     for those can we assert predecessor safety of an unproven candidate invariant.
   * @param pInvariantAbstraction The strategy to use to try to abstract the invariant if the check
   *     succeeds.
   * @param pLifting The strategy to use to try to reduce the model assignments if the check fails.
   * @return <code>true</code> if k-induction successfully proved the correctness of the candidate
   *     invariant, <code>false</code> and a model otherwise.
   * @throws CPAException if the bounded analysis constructing the step case encountered an
   *     exception.
   * @throws InterruptedException if the bounded analysis constructing the step case was
   *     interrupted.
   */
  public final <S extends CandidateInvariant, T extends CandidateInvariant>
      InductionResult<T> check(
          Iterable<CandidateInvariant> pPredecessorAssumptions,
          int pK,
          S pCandidateInvariant,
          Set<Object> pCheckedKeys,
          InvariantStrengthening<S, T> pInvariantAbstraction,
          Lifting pLifting)
          throws CPAException, InterruptedException, SolverException {

    stats.inductionPreparation.start();

    // Proving program safety with induction consists of two parts:
    // 1) Prove all paths safe that go only one iteration through the loop.
    //    This is part of the classic bounded model checking done in BMCAlgorithm,
    //    so we don't care about this here.
    // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.
    // For k-induction, assume that k loop iterations are safe and prove that the next one is safe,
    // too.

    // Create initial reached set:
    // Run algorithm in order to create formula (A & B)
    logger.log(Level.INFO, "Running algorithm to create induction hypothesis");

    // Ensure the reached set is prepared
    reachedSet.setDesiredK(pK + 1);
    reachedSet.ensureK();
    ReachedSet reached = reachedSet.getReachedSet();

    /*
     * For every induction problem we want so solve, create a formula asserting
     * it for k iterations.
     */
    Map<CandidateInvariant, BooleanFormula> assertions = new HashMap<>();
    ImmutableSet.Builder<AbstractState> inductionHypothesisBuilder = ImmutableSet.builder();

    for (CandidateInvariant candidateInvariant :
        CandidateInvariantCombination.getConjunctiveParts(pPredecessorAssumptions)) {
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
          // If we are not running KI-PDR and the candidate invariant is specified at a certain
          // location, the predecessor states are those within the BMC-checked range
          if (!pLifting.canLift() && candidateInvariant instanceof SingleLocationFormulaInvariant) {
            predecessorAssertion =
                candidateInvariant.getAssertion(
                    BMCHelper.filterBmcCheckedWithin(
                        reached, pCheckedKeys, cfa.getLoopStructure().orElseThrow().getAllLoops()),
                    fmgr,
                    pfmgr);
            // Record the states used in the hypothesis
            inductionHypothesisBuilder.addAll(
                ImmutableSet.copyOf(
                    candidateInvariant.filterApplicable(
                        BMCHelper.filterBmcCheckedWithin(
                            reached,
                            pCheckedKeys,
                            cfa.getLoopStructure().orElseThrow().getAllLoops()))));
          } else {
            // Build the formula
            predecessorAssertion =
                candidateInvariant.getAssertion(
                    BMCHelper.filterBmcChecked(
                        filterIterationsUpTo(reached, pK, loopHeads), pCheckedKeys),
                    fmgr,
                    pfmgr);
          }
        }
      }
      BooleanFormula storedAssertion = assertions.get(candidateInvariant);
      if (storedAssertion == null) {
        storedAssertion = bfmgr.makeBoolean(true);
      }
      assertions.put(candidateInvariant, bfmgr.and(storedAssertion, predecessorAssertion));
    }

    // Build the set of states used as induction hypothesis
    ImmutableSet<AbstractState> inductionHypothesis = inductionHypothesisBuilder.build();

    // Assert the known invariants at the loop head at end of the first iteration.

    FluentIterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reached, loopHeads);

    BooleanFormula loopHeadInv = inductiveLoopHeadInvariantAssertion(loopHeadStates);
    previousK = pK + 1;
    stats.inductionPreparation.stop();

    // Attempt the induction proofs
    shutdownNotifier.shutdownIfNecessary();

    // Assert that *some* successor is reached
    Iterable<AbstractState> endStates = FluentIterable.from(reached).filter(BMCHelper::isEndState);
    BooleanFormula successorExistsAssertion =
        createFormulaFor(endStates, bfmgr, Optional.of(shutdownNotifier));

    // Obtain the predecessor assertion created earlier
    final BooleanFormula predecessorAssertion =
        bfmgr.and(
            from(CandidateInvariantCombination.getConjunctiveParts(
                    CandidateInvariantCombination.conjunction(pPredecessorAssumptions)))
                .transform(conjunctivePart -> assertions.get(conjunctivePart))
                .toList());
    // Create the successor violation formula
    Multimap<BooleanFormula, BooleanFormula> successorViolationAssertions =
        getSuccessorViolationAssertions(
            pCandidateInvariant, pK + 1, inductionHypothesis, pLifting.canLift());
    // Record the successor violation formula to reuse its negation as an
    // assertion in a future induction attempt
    BooleanFormula successorViolation =
        BMCHelper.disjoinStateViolationAssertions(bfmgr, successorViolationAssertions);
    violationFormulas.put(pCandidateInvariant, successorViolation);

    logger.log(Level.INFO, "Starting induction check...");

    stats.inductionCheck.start();

    // Try to prove the invariance of the assertion
    Object successorExistsAssertionId = prover.push(successorExistsAssertion);
    Object predecessorAssertionId =
        prover.push(
            predecessorAssertion); // Assert the formula we want to prove at the predecessors
    // Assert that the formula is violated at a successor
    prover.push(successorViolation);

    InductionResult<T> result = null;
    AssertCandidate assertPredecessor =
        p -> assertAt(filterInductiveAssertionIteration(loopHeadStates), p, fmgr, pfmgr, true);
    while (result == null) {
      shutdownNotifier.shutdownIfNecessary();

      prover.push(loopHeadInv); // Assert the known loop-head invariants

      // The formula is invariant if the assertions are contradicting
      boolean isInvariant = prover.isUnsat();

      if (!isInvariant) {

        // Re-attempt the proof immediately before returning to the caller
        // if new invariants are available
        BooleanFormula oldLoopHeadInv = loopHeadInv;
        loopHeadInv = inductiveLoopHeadInvariantAssertion(loopHeadStates);
        boolean loopHeadInvChanged = !loopHeadInv.equals(oldLoopHeadInv);

        // We need to produce the model if we are in the last iteration
        // or want to log the model
        if (!loopHeadInvChanged || logger.wouldBeLogged(Level.ALL)) {
          List<ValueAssignment> modelAssignments = prover.getModelAssignments();
          if (logger.wouldBeLogged(Level.ALL)) {
            logger.log(Level.ALL, "Model returned for induction check:", modelAssignments);
          }

          if (!loopHeadInvChanged) {
            // We are in the last iteration and failed to prove the candidate invariant

            Iterable<? extends SymbolicCandiateInvariant> badStateBlockingClauses =
                ImmutableSet.of();
            Map<CounterexampleToInductivity, BooleanFormula> detectedCtis =
                extractCTIs(
                    reached,
                    modelAssignments,
                    pCheckedKeys,
                    pCandidateInvariant,
                    pK + 1,
                    pLifting.canLift());
            if (pLifting.canLift()) {
              prover.pop(); // Pop the loop-head invariants
              // Pop the successor violation
              prover.pop();
              // Push the successor assertion
              BooleanFormula candidateAssertion =
                  assertCandidate(reached, pCandidateInvariant, pK + 1);
              Object candidateSuccessorAssertionId = prover.push(candidateAssertion);
              Object invariantsAssertionId =
                  prover.push(loopHeadInv); // Push the known loop-head invariants back on

              ImmutableSet.Builder<SymbolicCandiateInvariant> badStateBlockingClauseBuilder =
                  ImmutableSet.builder();
              for (Map.Entry<CounterexampleToInductivity, BooleanFormula> ctiWithInput :
                  detectedCtis.entrySet()) {
                // Push the input assignments
                Object inputAssertionId = prover.push(ctiWithInput.getValue());
                final SymbolicCandiateInvariant blockedReducedCti =
                    pLifting.lift(
                        fmgr,
                        pam,
                        prover,
                        SymbolicCandiateInvariant.blockCti(loopHeads, ctiWithInput.getKey(), fmgr),
                        assertPredecessor,
                        Arrays.asList(
                            successorExistsAssertionId,
                            predecessorAssertionId,
                            candidateSuccessorAssertionId,
                            invariantsAssertionId,
                            inputAssertionId));
                badStateBlockingClauseBuilder.add(blockedReducedCti);
                prover.pop(); // Pop input assignments
              }
              badStateBlockingClauses = badStateBlockingClauseBuilder.build();
            } else {
              badStateBlockingClauses =
                  Iterables.transform(
                      detectedCtis.keySet(),
                      cti -> SymbolicCandiateInvariant.blockCti(loopHeads, cti, fmgr));
            }
            result = InductionResult.getFailed(badStateBlockingClauses, pK);
          }
        }
      } else {
        AssertCandidate assertSuccessorViolation =
            (candidate) -> {
              Multimap<BooleanFormula, BooleanFormula> succViolationAssertions =
                  getSuccessorViolationAssertions(
                      pCandidateInvariant, pK + 1, inductionHypothesis, pLifting.canLift());
              // Record the successor violation formula to reuse its negation as an
              // assertion in a future induction attempt
              return BMCHelper.disjoinStateViolationAssertions(bfmgr, succViolationAssertions);
            };
        NextCti nextCti =
            () -> {
              List<ValueAssignment> modelAssignments = prover.getModelAssignments();
              Iterable<CounterexampleToInductivity> detectedCtis =
                  extractCTIs(
                          reached,
                          modelAssignments,
                          pCheckedKeys,
                          pCandidateInvariant,
                          pK + 1,
                          pLifting.canLift())
                      .keySet();
              if (Iterables.isEmpty(detectedCtis)) {
                return Optional.empty();
              }
              return Optional.of(detectedCtis.iterator().next());
            };
        T abstractedInvariant =
            pInvariantAbstraction.strengthenInvariant(
                prover,
                fmgr,
                pam,
                pCandidateInvariant,
                assertPredecessor,
                assertSuccessorViolation,
                assertPredecessor,
                successorViolationAssertions,
                Optional.of(loopHeadInv),
                nextCti);
        result = InductionResult.getSuccessful(abstractedInvariant);
      }

      prover.pop(); // Pop the loop-head invariants
    }

    // If the proof is successful, remove its violation formula from the cache
    if (result.isSuccessful()) {
      violationFormulas.remove(pCandidateInvariant);
    }

    // Pop invariant successor violation (or, if we lifted a CTI, its assertion)
    prover.pop();

    prover.pop(); // Pop invariant predecessor assertion
    prover.pop(); // Pop end states

    stats.inductionCheck.stop();

    logger.log(Level.FINER, "Soundness after induction check:", result.isSuccessful());

    return result;
  }

  private BooleanFormula assertCandidate(
      Iterable<AbstractState> pReached, CandidateInvariant pCandidateInvariant, int pK)
      throws CPATransferException, InterruptedException {
    FluentIterable<AbstractState> states = filterIteration(pReached, pK, loopHeads);
    final List<BooleanFormula> assertions = new ArrayList<>();
    for (CandidateInvariant component :
        CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant)) {
      if (component instanceof TargetLocationCandidateInvariant) {
        Iterable<AbstractState> candidateAssertionStates =
            states.filter(
                s -> {
                  ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
                  return !argState.isTarget()
                      && (argState.getChildren().isEmpty()
                          || from(AbstractStates.extractLocations(s))
                              .anyMatch(loopHeads::contains));
                });
        assertions.add(createFormulaFor(candidateAssertionStates, bfmgr));
      } else {
        Iterable<AbstractState> candidateAssertionStates =
            states.filter(
                s -> from(AbstractStates.extractLocations(s)).anyMatch(component::appliesTo));
        assertions.add(createFormulaFor(candidateAssertionStates, bfmgr));
        assertions.add(component.getAssertion(candidateAssertionStates, fmgr, pfmgr));
      }
    }
    return bfmgr.and(assertions);
  }

  private BooleanFormula inductiveLoopHeadInvariantAssertion(
      Iterable<AbstractState> pLoopHeadStates) throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates = filterInductiveAssertionIteration(pLoopHeadStates);
    return assertAt(loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr);
  }

  private FluentIterable<AbstractState> filterInductiveAssertionIteration(
      Iterable<AbstractState> pStates) {
    return filterIteration(pStates, 1, loopHeads);
  }

  private AlgorithmStatus ensureK(
      Algorithm pAlg, ConfigurableProgramAnalysis pCPA, ReachedSet pReached)
      throws InterruptedException, CPAException {
    if (pReached.size() <= 1 && cfa.getLoopStructure().isPresent()) {
      Stream<CFANode> relevantLoopHeads =
          cfa.getLoopStructure().orElseThrow().getAllLoops().stream()
              .filter(loop -> !BMCHelper.isTrivialSelfLoop(loop))
              .map(Loop::getLoopHeads)
              .flatMap(Collection::stream)
              .filter(loopHeads::contains)
              .distinct();
      Iterator<CFANode> relevantLoopHeadIterator = relevantLoopHeads.iterator();
      while (relevantLoopHeadIterator.hasNext()) {
        CFANode relevantLoopHead = relevantLoopHeadIterator.next();
        Precision precision =
            pCPA.getInitialPrecision(relevantLoopHead, StateSpacePartition.getDefaultPartition());
        AbstractState initialState =
            pCPA.getInitialState(relevantLoopHead, StateSpacePartition.getDefaultPartition());
        pReached.add(initialState, precision);
      }
      if (pReached.isEmpty()) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }
    return unroll(logger, pReached, pAlg, pCPA);
  }

  private Multimap<String, Integer> extractInputs(
      Iterable<AbstractState> pReached, Map<String, CType> types) {
    Multimap<String, Integer> inputs = LinkedHashMultimap.create();
    Set<AbstractState> visited = new HashSet<>();
    Deque<AbstractState> waitlist = new ArrayDeque<>();
    Iterables.addAll(waitlist, pReached);
    visited.addAll(waitlist);
    while (!waitlist.isEmpty()) {
      AbstractState current = waitlist.poll();
      InputState is = AbstractStates.extractStateByType(current, InputState.class);
      PredicateAbstractState pas =
          AbstractStates.extractStateByType(current, PredicateAbstractState.class);
      if (is != null && pas != null) {
        SSAMap ssaMap = pas.getPathFormula().getSsa();
        for (String input : is.getInputs()) {
          if (ssaMap.containsVariable(input)) {
            inputs.put(input, ssaMap.getIndex(input) - 1);
            inputs.put(input, ssaMap.getIndex(input));
            types.put(input, ssaMap.getType(input));
          }
        }
        for (String varName : ssaMap.allVariables()) {
          types.put(varName, ssaMap.getType(varName));
        }
      }
      ARGState argState = AbstractStates.extractStateByType(current, ARGState.class);
      if (argState != null) {
        for (ARGState parent : argState.getParents()) {
          if (visited.add(parent)) {
            waitlist.offer(parent);
          }
        }
      }
    }
    return inputs;
  }

  private Map<CounterexampleToInductivity, BooleanFormula> extractCTIs(
      Iterable<AbstractState> pReached,
      Iterable<ValueAssignment> pModelAssignments,
      Set<Object> pCheckedKeys,
      CandidateInvariant pCandidateInvariant,
      int pK,
      boolean pCanLift) {

    Map<String, CType> types = new HashMap<>();

    FluentIterable<AbstractState> inputStates =
        filterIteration(pCandidateInvariant.filterApplicable(pReached), pK, loopHeads);
    if (pCandidateInvariant == TargetLocationCandidateInvariant.INSTANCE) {
      inputStates = inputStates.filter(AbstractStates::isTargetState);
    }
    Multimap<String, Integer> inputs = extractInputs(inputStates, types);

    Map<CounterexampleToInductivity, BooleanFormula> ctis = new LinkedHashMap<>();
    for (CFANode loopHead : loopHeads) {
      // We compute the CTI state "at the start of the second loop iteration",
      // because that is where we will later apply it (or its negation) as a candidate invariant.
      // Logically, there is no different to computing the CTI state
      // "at the start of the first iteration" and using it as a candidate invariant there,
      // but technically, this is easier:

      // If we are not running KI-PDR and the candidate invariant is specified at a certain
      // location, we compute the CTI state "at the start of the first loop iteration"
      int loopIterationForCTI =
          !pCanLift && (pCandidateInvariant instanceof SingleLocationFormulaInvariant) ? 0 : 1;
      FluentIterable<AbstractState> loopHeadStates =
          filterIteration(
              AbstractStates.filterLocations(
                  BMCHelper.filterBmcChecked(pReached, pCheckedKeys), ImmutableSet.of(loopHead)),
              loopIterationForCTI,
              loopHeads);

      for (AbstractState loopHeadState : loopHeadStates) {
        PredicateAbstractState pas =
            AbstractStates.extractStateByType(loopHeadState, PredicateAbstractState.class);
        SSAMap ssaMap = pas.getPathFormula().getSsa();
        Supplier<Map<String, Formula>> variableFormulas =
            Suppliers.memoize(
                () -> {
                  VariableMapper variableMapper = new VariableMapper();
                  fmgr.visitRecursively(pas.getPathFormula().getFormula(), variableMapper);
                  return variableMapper.variableFormulas;
                });

        PersistentMap<String, ModelValue> model = PathCopyingPersistentTreeMap.of();
        final List<BooleanFormula> input = new ArrayList<>();

        for (ValueAssignment valueAssignment : pModelAssignments) {
          if (!valueAssignment.isFunction()) {
            String fullName = valueAssignment.getName();
            Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
            String actualName = pair.getFirst();
            OptionalInt index = pair.getSecond();
            Object value = valueAssignment.getValue();
            if (index.isPresent()
                && (ssaMap.containsVariable(actualName)
                    ? ssaMap.getIndex(actualName) == index.orElseThrow()
                    : index.orElseThrow() == 1)
                && value instanceof Number
                && !inputs.containsKey(actualName)) {
              BooleanFormula assignment =
                  fmgr.uninstantiate(valueAssignment.getAssignmentAsFormula());
              model = model.putAndCopy(actualName, new ModelValue(actualName, assignment, fmgr));
            }
          }
        }

        if (!model.isEmpty()) {
          CounterexampleToInductivity cti = new CounterexampleToInductivity(loopHead, model);

          if (ctis.containsKey(cti)) {
            continue;
          }

          for (ValueAssignment valueAssignment : pModelAssignments) {
            String fullName = valueAssignment.getName();
            Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
            String actualName = pair.getSecond().isPresent() ? pair.getFirst() : fullName;
            OptionalInt index = pair.getSecond();
            boolean isUnconnected = false;
            if (index.isPresent()
                && ssaMap.containsVariable(actualName)
                && index.orElseThrow() < ssaMap.getIndex(actualName)) {
              isUnconnected = !variableFormulas.get().containsKey(fullName);
            }
            if ((!index.isPresent()
                || (index.isPresent()
                    && (isUnconnected || inputs.get(actualName).contains(index.orElseThrow()))))) {
              input.add(valueAssignment.getAssignmentAsFormula());
            }
          }

          ctis.put(cti, bfmgr.and(input));
        }
      }
    }
    return ctis;
  }

  private Multimap<BooleanFormula, BooleanFormula> getSuccessorViolationAssertions(
      CandidateInvariant pCandidateInvariant,
      int pK,
      Set<AbstractState> pHypothesis,
      boolean pCanLift)
      throws CPATransferException, InterruptedException {
    ReachedSet reached = reachedSet.getReachedSet();

    ImmutableListMultimap.Builder<BooleanFormula, BooleanFormula> stateViolationAssertionsBuilder =
        ImmutableListMultimap.builder();
    Iterable<AbstractState> assertionStates;
    // If we are not running KI-PDR and the candidate invariant is specified at a certain location,
    // assertion states are those not in the induction hypothesis
    if (!pCanLift && pCandidateInvariant instanceof SingleLocationFormulaInvariant) {
      assertionStates =
          from(pCandidateInvariant.filterApplicable(reached))
              .filter(state -> !pHypothesis.contains(state));
    } else {
      assertionStates =
          filterIteration(pCandidateInvariant.filterApplicable(reached), pK, loopHeads);
    }

    for (AbstractState state : assertionStates) {
      Set<AbstractState> stateAsSet = Collections.singleton(state);
      BooleanFormula stateFormula =
          BMCHelper.createFormulaFor(stateAsSet, bfmgr, Optional.of(shutdownNotifier));
      BooleanFormula invariantFormula = bfmgr.makeTrue();
      for (CandidateInvariant component :
          CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant)) {
        if (!Iterables.isEmpty(component.filterApplicable(stateAsSet))) {
          shutdownNotifier.shutdownIfNecessary();
          invariantFormula =
              bfmgr.and(
                  invariantFormula, BMCHelper.assertAt(stateAsSet, component, fmgr, pfmgr, true));
        }
      }
      stateViolationAssertionsBuilder.put(stateFormula, bfmgr.not(invariantFormula));
    }

    return stateViolationAssertionsBuilder.build();
  }

  private static class VariableMapper implements FormulaVisitor<TraversalProcess> {

    private final Map<String, Formula> variableFormulas = new HashMap<>();

    @Override
    public TraversalProcess visitQuantifier(
        BooleanFormula pArg0, Quantifier pArg1, List<Formula> pArg2, BooleanFormula pArg3) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFunction(
        Formula pArg0, List<Formula> pArg1, FunctionDeclaration<?> pArg2) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFreeVariable(Formula pArg0, String pArg1) {
      variableFormulas.put(pArg1, pArg0);
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitConstant(Formula pArg0, Object pArg1) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitBoundVariable(Formula pArg0, int pArg1) {
      return TraversalProcess.CONTINUE;
    }
  }
}
