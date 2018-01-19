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
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.assertAt;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.createFormulaFor;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterEndStates;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterIteration;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.filterIterationsUpTo;
import static org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.unroll;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.FormulaInContext;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.KInductionInvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
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
import org.sosy_lab.cpachecker.util.predicates.invariants.ExpressionTreeInvariantSupplier;
import org.sosy_lab.cpachecker.util.predicates.invariants.FormulaInvariantsSupplier;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
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

  private final Map<CandidateInvariant, BooleanFormula> violationFormulas = Maps.newHashMap();

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
    invariantGenerator  = checkNotNull(pInvariantGenerator);
    stats = checkNotNull(pStats);
    reachedSetFactory = checkNotNull(pReachedSetFactory);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    reachedSet =
        new UnrolledReachedSet(
            algorithm, cpa, pLoopHeads, reachedSetFactory.create(), this::ensureK);

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
  private FormulaInContext getCurrentLoopHeadInvariants(Iterable<AbstractState> pAssertionStates) {
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
        InvariantAbstractions.noAbstraction(),
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
  public final <
          S extends CandidateInvariant, T extends CandidateInvariant, D extends SuccessorViolation>
      InductionResult<T> check(
          Iterable<CandidateInvariant> pPredecessorAssumptions,
          int pK,
          S pCandidateInvariant,
          Set<Object> pCheckedKeys,
          InvariantAbstraction<S, T, D> pInvariantAbstraction,
          Lifting pLifting)
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

    // Ensure the reached set is prepared
    reachedSet.setDesiredK(pK + 1);
    reachedSet.ensureK();
    ReachedSet reached = reachedSet.getReachedSet();

    /*
     * For every induction problem we want so solve, create a formula asserting
     * it for k iterations.
     */
    Map<CandidateInvariant, BooleanFormula> assertions = new HashMap<>();

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
          predecessorAssertion =
              candidateInvariant.getAssertion(
                  filterBmcChecked(filterIterationsUpTo(reached, pK, loopHeads), pCheckedKeys),
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

    FluentIterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reached, loopHeads);

    BooleanFormula loopHeadInv = inductiveLoopHeadInvariantAssertion(loopHeadStates);
    this.previousK = pK + 1;
    stats.inductionPreparation.stop();

    // Attempt the induction proofs
    shutdownNotifier.shutdownIfNecessary();

    // Assert that *some* successor is reached
    BooleanFormula successorExistsAssertion = createFormulaFor(filterEndStates(reached), bfmgr);

    // Obtain the predecessor assertion created earlier
    final BooleanFormula predecessorAssertion =
        bfmgr.and(
            from(CandidateInvariantConjunction.getConjunctiveParts(
                    CandidateInvariantConjunction.of(pPredecessorAssumptions)))
                .transform(conjunctivePart -> assertions.get(conjunctivePart))
                .toList());
    // Create the successor violation formula
    D successorViolation =
        getSuccessorViolation(pInvariantAbstraction, pCandidateInvariant, reached, pK);
    // Record the successor violation formula to reuse its negation as an
    // assertion in a future induction attempt
    violationFormulas.put(
        pCandidateInvariant, bfmgr.and(successorViolation.getViolationAssertion()));

    logger.log(Level.INFO, "Starting induction check...");

    stats.inductionCheck.start();

    // Try to prove the invariance of the assertion
    prover.push(successorExistsAssertion);
    prover.push(predecessorAssertion); // Assert the formula we want to prove at the predecessors
    // Assert that the formula is violated at a successor
    Map<BooleanFormula, Object> successorViolationAssertionIds = new HashMap<>();
    for (BooleanFormula successorViolationComponent : successorViolation.getViolationAssertion()) {
      successorViolationAssertionIds.put(
          successorViolationComponent, prover.push(successorViolationComponent));
    }

    InductionResult<T> result = null;
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
          ImmutableList<ValueAssignment> modelAssignments = prover.getModelAssignments();
          if (logger.wouldBeLogged(Level.ALL)) {
            logger.log(Level.ALL, "Model returned for induction check:", modelAssignments);
          }

          if (!loopHeadInvChanged) {
            // We are in the last iteration and failed to prove the candidate invariant

            Set<? extends SingleLocationFormulaInvariant> model = Collections.emptySet();
            BooleanFormula inputAssignments = extractInputAssignments(reached, modelAssignments);
            Set<CounterexampleToInductivity> detectedCTIs = extractCTIs(reached, modelAssignments);
            if (pLifting.canLift()) {
              prover.pop(); // Pop the loop-head invariants
              // Pop the successor violation
              successorViolationAssertionIds
                  .values()
                  .forEach(
                      id -> {
                        prover.pop();
                      });
              // Push the successor assertion
              BooleanFormula candidateAssertion =
                  assertCandidate(reached, pCandidateInvariant, pK + 1);
              successorViolationAssertionIds =
                  Collections.singletonMap(candidateAssertion, prover.push(candidateAssertion));
              prover.push(loopHeadInv); // Push the known loop-head invariants back on
              prover.push(inputAssignments);
              ImmutableSet.Builder<SingleLocationFormulaInvariant> reducedCTIsBuilder =
                  ImmutableSet.builder();
              for (CounterexampleToInductivity cti : detectedCTIs) {
                final SingleLocationFormulaInvariant reducedCTI =
                    pLifting.lift(
                        fmgr,
                        prover,
                        cti,
                        (p ->
                            assertAt(
                                filterInductiveAssertionIteration(loopHeadStates),
                                p,
                                fmgr,
                                pfmgr,
                                true)));
                reducedCTIsBuilder.add(reducedCTI);
              }
              model = reducedCTIsBuilder.build();
              prover.pop(); // Pop input assignments
            } else {
              model = detectedCTIs;
            }
            result = InductionResult.getFailed(model, inputAssignments, pK);
          }
        }
      } else {
        T abstractedInvariant =
            pInvariantAbstraction.performAbstraction(
                prover,
                pam,
                successorViolation,
                successorViolationAssertionIds,
                Optional.of(loopHeadInv));
        result = InductionResult.getSuccessful(abstractedInvariant);
      }

      prover.pop(); // Pop the loop-head invariants
    }

    // If the proof is successful, remove its violation formula from the cache
    if (result.isSuccessful()) {
      violationFormulas.remove(pCandidateInvariant);
    }

    // Pop invariant successor violation (or, if we lifted a CTI, its assertion)
    successorViolationAssertionIds
        .values()
        .forEach(
            id -> {
              prover.pop();
            });

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
    if (pCandidateInvariant instanceof TargetLocationCandidateInvariant) {
      states =
          states.filter(
              s -> {
                ARGState argState = AbstractStates.extractStateByType(s, ARGState.class);
                return !argState.isTarget()
                    && (argState.getChildren().isEmpty()
                        || from(AbstractStates.extractLocations(s)).anyMatch(loopHeads::contains));
              });
      return createFormulaFor(states, bfmgr);
    }
    states =
        states.filter(
            s -> from(AbstractStates.extractLocations(s)).anyMatch(pCandidateInvariant::appliesTo));
    return bfmgr.and(
        createFormulaFor(states, bfmgr), pCandidateInvariant.getAssertion(states, fmgr, pfmgr));
  }

  private BooleanFormula inductiveLoopHeadInvariantAssertion(
      Iterable<AbstractState> pLoopHeadStates) throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates = filterInductiveAssertionIteration(pLoopHeadStates);
    return assertAt(loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr);
  }

  private <S extends CandidateInvariant, T extends CandidateInvariant, D extends SuccessorViolation>
      D getSuccessorViolation(
          InvariantAbstraction<S, T, D> pInvariantAbstraction,
          S pCandidateInvariant,
          ReachedSet pReached,
          int pK)
          throws CPATransferException, InterruptedException {
    Iterable<AbstractState> assertionStates =
        filterIteration(pCandidateInvariant.filterApplicable(pReached), pK + 1, loopHeads);
    return pInvariantAbstraction.getSuccessorViolation(
        fmgr, pfmgr, pCandidateInvariant, assertionStates);
  }

  private FluentIterable<AbstractState> filterInductiveAssertionIteration(
      Iterable<AbstractState> pStates) {
    return filterIteration(pStates, 1, loopHeads);
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

  private AlgorithmStatus ensureK(
      Algorithm pAlg, ConfigurableProgramAnalysis pCPA, ReachedSet pReached)
      throws InterruptedException, CPAException {
    if (pReached.size() <= 1 && cfa.getLoopStructure().isPresent()) {
      Stream<CFANode> relevantLoopHeads =
          cfa.getLoopStructure()
              .get()
              .getAllLoops()
              .stream()
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

  private BooleanFormula extractInputAssignments(
      ReachedSet pReached, Iterable<ValueAssignment> pModelAssignments) {
    BooleanFormula inputAssignments = bfmgr.makeTrue();
    if (pReached.isEmpty()
        || AbstractStates.extractStateByType(pReached.getFirstState(), InputState.class) == null) {
      return inputAssignments;
    }
    Map<String, CType> types = Maps.newHashMap();
    Multimap<String, Integer> inputs = extractInputs(pReached, types);
    if (inputs.isEmpty()) {
      return inputAssignments;
    }
    for (ValueAssignment valueAssignment : pModelAssignments) {
      if (!valueAssignment.isFunction()) {
        String fullName = valueAssignment.getName();
        Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
        String actualName = pair.getFirst();
        OptionalInt index = pair.getSecond();
        Object value = valueAssignment.getValue();
        if (index.isPresent() && value instanceof Number && inputs.containsKey(actualName)) {
          Formula formula = valueAssignment.getKey();
          FormulaType<?> formulaType = fmgr.getFormulaType(formula);
          ModelValue modelValue =
              new ModelValue(actualName, formulaType, (Number) valueAssignment.getValue());
          SSAMap ssaForInstantiation =
              SSAMap.emptySSAMap()
                  .builder()
                  .setIndex(actualName, types.get(actualName), index.getAsInt())
                  .build();
          inputAssignments =
              bfmgr.and(
                  inputAssignments,
                  fmgr.instantiate(modelValue.toAssignment(fmgr), ssaForInstantiation));
        }
      }
    }
    return inputAssignments;
  }

  private Multimap<String, Integer> extractInputs(ReachedSet pReached, Map<String, CType> types) {
    Multimap<String, Integer> inputs = LinkedHashMultimap.create();
    for (AbstractState s : pReached) {
      InputState is = AbstractStates.extractStateByType(s, InputState.class);
      if (is != null) {
        PredicateAbstractState pas =
            AbstractStates.extractStateByType(s, PredicateAbstractState.class);
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
    }
    return inputs;
  }

  private Set<CounterexampleToInductivity> extractCTIs(
      ReachedSet pReached, Iterable<ValueAssignment> pModelAssignments) {

    Map<String, CType> types = Maps.newHashMap();
    Multimap<String, Integer> inputs = extractInputs(pReached, types);

    ImmutableSet.Builder<CounterexampleToInductivity> ctis = ImmutableSet.builder();
    for (CFANode loopHead : loopHeads) {
      // We compute the CTI state "at the start of the second loop iteration",
      // because that is where we will later apply it (or its negation) as a candidate invariant.
      // Logically, there is no different to computing the CTI state
      // "at the start of the first iteration" and using it as a candidate invariant there,
      // but technically, this is easier:
      FluentIterable<AbstractState> loopHeadStates =
          filterIteration(
              AbstractStates.filterLocations(pReached, ImmutableSet.of(loopHead)), 1, loopHeads);

      for (AbstractState loopHeadState : loopHeadStates) {
        PredicateAbstractState pas =
            AbstractStates.extractStateByType(loopHeadState, PredicateAbstractState.class);
        SSAMap ssaMap = pas.getPathFormula().getSsa();

        ImmutableMap.Builder<String, ModelValue> modelBuilder = ImmutableMap.builder();

        for (ValueAssignment valueAssignment : pModelAssignments) {
          if (!valueAssignment.isFunction()) {
            String fullName = valueAssignment.getName();
            Pair<String, OptionalInt> pair = FormulaManagerView.parseName(fullName);
            String actualName = pair.getFirst();
            OptionalInt index = pair.getSecond();
            Object value = valueAssignment.getValue();
            if (index.isPresent()
                && ssaMap.containsVariable(actualName)
                && ssaMap.getIndex(actualName) == index.getAsInt()
                && value instanceof Number
                && !inputs.containsKey(actualName)) {
              Formula formula = valueAssignment.getKey();
              FormulaType<?> formulaType = fmgr.getFormulaType(formula);
              modelBuilder.put(
                  actualName,
                  new ModelValue(actualName, formulaType, (Number) valueAssignment.getValue()));
            }
          }
        }
        Map<String, ModelValue> model = modelBuilder.build();
        if (!model.isEmpty()) {
          CounterexampleToInductivity cti = new CounterexampleToInductivity(loopHead, model);
          ctis.add(cti);
        }
      }
    }
    return ctis.build();
  }

}
