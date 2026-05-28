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

import com.google.common.base.Functions;
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
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper.FormulaInContext;
import org.sosy_lab.cpachecker.core.algorithm.bmc.InvariantStrengthening.NextCti;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariantCombination;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.StatewiseCandidateInvariantConjunction;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.LoopIterationReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
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

  private static final int MAX_NON_TERMINATION_REFINEMENT_LITERALS = 8;

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

  private final boolean requireSatisfiablePredecessor;

  private Optional<CandidateInvariant> lastNonTerminationRefinement = Optional.empty();

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
      boolean pUnsatCoreGeneration,
      boolean pRequireSatisfiablePredecessor) {
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
    requireSatisfiablePredecessor = pRequireSatisfiablePredecessor;
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

  public final boolean checkNonTerminationClosure(
      CandidateInvariant pCandidateInvariant,
      int pK,
      Set<Object> pCheckedKeys,
      Optional<NonTerminationLoopScope> pLoopScope,
      boolean pBuildRefinement)
      throws CPAException, InterruptedException, SolverException {

    lastNonTerminationRefinement = Optional.empty();
    stats.inductionPreparation.start();

    logger.log(Level.INFO, "Running algorithm to create non-termination closure check");
    Set<CFANode> relevantLoopHeads =
        pLoopScope.map(NonTerminationLoopScope::loopHeads).orElse(loopHeads);

    reachedSet.setDesiredK(pK + 2);
    reachedSet.ensureK();
    ReachedSet reached = reachedSet.getReachedSet();

    Optional<CandidateInvariant> predecessorCandidate =
        getNonTerminationPredecessorCandidate(pCandidateInvariant);
    if (predecessorCandidate.isEmpty()) {
      logger.log(
          Level.FINER,
          "The non-termination closure predecessor contains only successor-only components;"
              + " refusing vacuous proof.");
      stats.inductionPreparation.stop();
      return false;
    }

    FluentIterable<AbstractState> predecessorStates =
        filterNonTerminationPredecessorStates(reached, pK, pCheckedKeys, pLoopScope);
    ImmutableSet<AbstractState> inductionHypothesis =
        ImmutableSet.copyOf(predecessorCandidate.orElseThrow().filterApplicable(predecessorStates));
    if (inductionHypothesis.isEmpty()) {
      logger.log(
          Level.FINER,
          "The non-termination closure predecessor has no applicable states; refusing vacuous"
              + " proof.");
      stats.inductionPreparation.stop();
      return false;
    }

    BooleanFormula predecessorAssertion =
        predecessorCandidate.orElseThrow().getAssertion(predecessorStates, fmgr, pfmgr);
    FluentIterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reached, relevantLoopHeads);

    // Path formula (transition relation) for the universal closure check.
    Iterable<AbstractState> endStates = FluentIterable.from(reached).filter(BMCHelper::isEndState);
    BooleanFormula successorExistsAssertion =
        createFormulaFor(endStates, bfmgr, Optional.of(shutdownNotifier));

    stats.inductionPreparation.stop();

    // Universal closure check: no path from a C-predecessor reaches a not-C successor.
    BooleanFormula loopHeadInv =
        inductiveLoopHeadInvariantAssertion(loopHeadStates, relevantLoopHeads);
    Multimap<BooleanFormula, BooleanFormula> successorViolationAssertions =
        getNonTerminationClosureViolationAssertions(pCandidateInvariant, inductionHypothesis);
    if (successorViolationAssertions.isEmpty()) {
      logger.log(
          Level.FINER,
          "The non-termination closure check has no successor assertions; refusing vacuous proof.");
      return false;
    }
    BooleanFormula successorViolation =
        BMCHelper.disjoinStateViolationAssertions(bfmgr, successorViolationAssertions);

    logger.log(Level.INFO, "Starting universal non-termination closure check...");

    stats.inductionCheck.start();
    int pushes = 0;
    try {
      prover.push(successorExistsAssertion);
      pushes++;
      prover.push(predecessorAssertion);
      pushes++;
      if (requireSatisfiablePredecessor && prover.isUnsat()) {
        logger.log(
            Level.FINER,
            "The non-termination closure predecessor is unsatisfiable; refusing vacuous proof.");
        return false;
      }
      prover.push(loopHeadInv);
      pushes++;
      prover.push(successorViolation);
      pushes++;
      if (prover.isUnsat()) {
        return true;
      }
      if (pBuildRefinement) {
        List<ValueAssignment> model = prover.getModelAssignments();
        lastNonTerminationRefinement =
            buildValidatedNonTerminationRefinement(
                pCandidateInvariant, model, inductionHypothesis, successorViolation);
      }
      return false;
    } finally {
      while (pushes > 0) {
        prover.pop();
        pushes--;
      }
      stats.inductionCheck.stop();
    }
  }

  public Optional<CandidateInvariant> getLastNonTerminationRefinement() {
    return lastNonTerminationRefinement;
  }

  /**
   * Symbolic 1-step universal-closure check for non-termination.
   *
   * <p>For every candidate-source location, builds a fresh pre-state path formula via {@link
   * PathFormulaManager#makeEmptyPathFormulaWithContextFrom} (inherits the SSA/PTS context without
   * binding values to the main-init constraints), enumerates the body's CFA paths to all
   * candidate-target locations via {@link #propagateOneIterationSymbolically}, and asks the
   * solver whether
   *
   * <pre>{@code
   *   C(s) AND T(s, s') AND NOT C(s')
   * }</pre>
   *
   * is satisfiable for any (source, target) pair.
   *
   * <p>The return value is tri-state to let the caller distinguish "the check actually decided"
   * from "the check could not even run":
   *
   * <ul>
   *   <li>{@code Optional.of(true)} - UNSAT, real one-step closure proven ({@code for all s in C,
   *       for all s': T(s,s') => C(s')}).
   *   <li>{@code Optional.of(false)} - SAT, the candidate is not 1-step inductive and a concrete
   *       symbolic counterexample exists. The algorithm must not declare non-termination based on
   *       any heuristic (e.g., BMC-bounded closure check) that contradicts this.
   *   <li>{@code Optional.empty()} - the check bailed out before producing a verdict (nested
   *       loops, function calls inside the body, exhausted path-enumeration budget, etc.). The
   *       caller is free to fall back to a less-precise verdict source.
   * </ul>
   *
   * <p>This check is independent of the BMC unrolling depth k: the closure property holds (or
   * not) universally over the symbolic state space and does not need to "match" the base-case
   * depth. The auxiliary loop-head invariant from the parallel invariant generator is
   * deliberately NOT asserted here, to keep the check independent of side-channels.
   */
  public final Optional<Boolean> checkSymbolicNonTerminationClosure(
      CandidateInvariant pCandidateInvariant,
      Optional<NonTerminationLoopScope> pLoopScope,
      boolean pBuildRefinement)
      throws CPAException, InterruptedException, SolverException {

    lastNonTerminationRefinement = Optional.empty();
    stats.inductionPreparation.start();
    BooleanFormula totalBad;
    BooleanFormula totalPrecondition;
    List<SymbolicBadObligation> badObligations;
    try {
      Loop loop = findLoopForSymbolicCheck(pCandidateInvariant, pLoopScope);
      if (loop == null) {
        // DIAG: bail #1 - loop matching failure.
        logger.logf(
            Level.INFO,
            "Symbolic bail #1 (findLoop): could not determine target loop for candidate %s.",
            pCandidateInvariant);
        return Optional.empty();
      }

      Map<CFANode, List<CandidateInvariant>> componentsByLocation = new LinkedHashMap<>();
      for (CandidateInvariant component :
          CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant)) {
        if (!(component instanceof SingleLocationFormulaInvariant slfi)) {
          // DIAG: bail #2 - candidate carries a non-SLFI component.
          logger.logf(
              Level.INFO,
              "Symbolic bail #2 (unsupported component): component type %s in candidate %s.",
              component.getClass().getSimpleName(),
              pCandidateInvariant);
          return Optional.empty();
        }
        componentsByLocation
            .computeIfAbsent(slfi.getLocation(), k -> new ArrayList<>())
            .add(component);
      }

      ImmutableSet<CFANode> exitSuccessors =
          FluentIterable.from(loop.getOutgoingEdges()).transform(CFAEdge::getSuccessor).toSet();
      if (symbolicTransferPropagationUnsupported(loop)) {
        return Optional.empty();
      }

      // Unified closure semantics:
      //   The loop head is only the iteration anchor used for SSA/path-formula context.
      //   The actual induction source is every non-successor-only candidate location P.
      //
      //   For every loop head H and candidate source P, first build the current-iteration
      //   prefix H_k -> P_k. Then assert C(P_k) at P's own SSA context. This is the
      //   missing current-round assumption for candidates that are not located at the loop
      //   head (for example candidates at a while guard).
      //
      //   Bad clauses:
      //     T(H_k -> P_k) and C(P_k) and T(P_k -> exit)
      //     T(H_k -> P_k) and C(P_k) and T(P_k -> H_{k+1})
      //       and T(H_{k+1} -> exit)
      //     T(H_k -> P_k) and C(P_k) and T(P_k -> H_{k+1})
      //       and T(H_{k+1} -> P_{k+1}) and not C(P_{k+1})
      //
      //   The last clause is generated for the same candidate location P in the next
      //   iteration. This checks that each candidate component is preserved by one full
      //   loop iteration at its own CFA location instead of requiring a source P to imply
      //   unrelated candidate locations Q.
      //
      //   UNSAT(all bad clauses) means no candidate source can exit and no next visit to
      //   the same candidate location can violate its component. SAT is a real closure
      //   counterexample, so the caller must not fall back to the bounded closure check as a
      //   proof.
      ImmutableSet<CFANode> loopHeads = ImmutableSet.copyOf(loop.getLoopHeads());
      if (loopHeads.isEmpty()) {
        logger.logf(
            Level.INFO,
            "Symbolic bail #3 (no loop heads): loop=%s.",
            loop);
        return Optional.empty();
      }
      Map<CFANode, List<CandidateInvariant>> nonSuccOnlyComponentsByLocation =
          getNonSuccessorOnlyComponentsByLocation(componentsByLocation);
      nonSuccOnlyComponentsByLocation
          .entrySet()
          .removeIf(entry -> !loop.getLoopNodes().contains(entry.getKey()));
      Map<CFANode, List<CandidateInvariant>> sourceComponentsByLocation =
          getTerminalCandidateSources(
              nonSuccOnlyComponentsByLocation, loop, loopHeads, exitSuccessors);
      if (nonSuccOnlyComponentsByLocation.isEmpty()) {
        logger.logf(
            Level.INFO,
            "Symbolic bail #3 (no sources): every in-loop component is successor-only;"
                + " componentsByLocation=%s.",
            componentsByLocation.keySet());
        return Optional.empty();
      }
      if (sourceComponentsByLocation.isEmpty()) {
        logger.logf(
            Level.INFO,
            "Symbolic bail #3 (no terminal sources): sourceCandidates=%s.",
            nonSuccOnlyComponentsByLocation.keySet());
        return Optional.empty();
      }

      // Make sure KInductionProver's *internal* reached set has been unrolled at least one
      // iteration, so we can pull a PredicateAbstractState (and thus an SSA context) from
      // each loop head.
      reachedSet.setDesiredK(Math.max(2, reachedSet.getDesiredK()));
      reachedSet.ensureK();
      ReachedSet reached = reachedSet.getReachedSet();

      List<BooleanFormula> allBads = new ArrayList<>();
      badObligations = new ArrayList<>();
      List<BooleanFormula> allPreconditions = new ArrayList<>();
      int sourcesProcessed = 0;
      int loopHeadsSkippedNoState = 0;
      int sourcePrefixesSkipped = 0;

      for (CFANode loopHead : loopHeads) {
        shutdownNotifier.shutdownIfNecessary();
        Optional<AbstractState> anySourceState =
            AbstractStates.filterLocations(reached, ImmutableSet.of(loopHead)).stream().findFirst();
        if (anySourceState.isEmpty()) {
          loopHeadsSkippedNoState++;
          logger.logf(
              Level.INFO, "Symbolic per-loop-head skip (no reached state): source=%s.", loopHead);
          continue;
        }
        PredicateAbstractState pas =
            AbstractStates.extractStateByType(
                anySourceState.orElseThrow(), PredicateAbstractState.class);
        if (pas == null) {
          loopHeadsSkippedNoState++;
          logger.logf(
              Level.INFO,
              "Symbolic per-loop-head skip (no PredicateAbstractState): source=%s.",
              loopHead);
          continue;
        }
        PathFormula pfAtLoopHead = pfmgr.makeEmptyPathFormulaWithContextFrom(pas.getPathFormula());
        AbstractState loopHeadState = anySourceState.orElseThrow();
        Precision loopHeadPrecision = reached.getPrecision(loopHeadState);

        for (Map.Entry<CFANode, List<CandidateInvariant>> sourceEntry :
            sourceComponentsByLocation.entrySet()) {
          shutdownNotifier.shutdownIfNecessary();
          CFANode source = sourceEntry.getKey();

          List<SymbolicTransferArrival> sourceArrivals;
          if (source.equals(loopHead)) {
            Optional<AbstractState> seededLoopHeadState =
                replacePredicatePathFormula(loopHeadState, pfAtLoopHead);
            if (seededLoopHeadState.isEmpty()) {
              logger.logf(
                  Level.INFO,
                  "Symbolic bail #4 (loop-head seed failed): loopHead=%s.",
                  loopHead);
              return Optional.empty();
            }
            sourceArrivals =
                List.of(
                    new SymbolicTransferArrival(
                        loopHead,
                        pfAtLoopHead,
                        seededLoopHeadState.orElseThrow(),
                        loopHeadPrecision));
          } else {
            Map<CFANode, List<SymbolicTransferArrival>> sourcePrefixArrivals =
                propagateOneIterationWithTransferRelation(
                    loopHead,
                    pfAtLoopHead,
                    loopHeadState,
                    loopHeadPrecision,
                    ImmutableSet.of(source),
                    exitSuccessors,
                    nonSuccOnlyComponentsByLocation,
                    false);
            if (sourcePrefixArrivals == null) {
              logger.logf(
                  Level.INFO,
                  "Symbolic bail #4 (source-prefix propagation null): loopHead=%s, source=%s.",
                  loopHead,
                  source);
              return Optional.empty();
            }
            sourceArrivals = sourcePrefixArrivals.getOrDefault(source, List.of());
            if (sourceArrivals.isEmpty()) {
              sourcePrefixesSkipped++;
              logger.logf(
                  Level.INFO,
                  "Symbolic per-source skip (no prefix from loop head): loopHead=%s, source=%s.",
                  loopHead,
                  source);
              continue;
            }
          }

          for (SymbolicTransferArrival sourceArrival : sourceArrivals) {
            PathFormula pfAtSource = sourceArrival.pathFormula();
            BooleanFormula cAtSource = instantiateComponentsAt(sourceEntry.getValue(), pfAtSource);
            BooleanFormula sourcePrecondition = bfmgr.and(pfAtSource.getFormula(), cAtSource);
            List<BooleanFormula> sourceBads = new ArrayList<>();
            boolean hasPreservationPath = false;

            if (!exitSuccessors.isEmpty()) {
              Map<CFANode, List<SymbolicTransferArrival>> exitArrivals =
                  propagateOneIterationWithTransferRelation(
                      source,
                      pfAtSource,
                      sourceArrival.state(),
                      sourceArrival.precision(),
                      exitSuccessors,
                      exitSuccessors,
                      nonSuccOnlyComponentsByLocation,
                      false);
              if (exitArrivals == null) {
                logger.logf(
                    Level.INFO, "Symbolic bail #4 (exit propagation null): source=%s.", source);
                return Optional.empty();
              }
              if (!exitArrivals.isEmpty()) {
                List<BooleanFormula> exitTransitions = new ArrayList<>();
                for (List<SymbolicTransferArrival> arrivalsAtExit : exitArrivals.values()) {
                  for (SymbolicTransferArrival exitArrival : arrivalsAtExit) {
                    exitTransitions.add(exitArrival.pathFormula().getFormula());
                  }
                }
                BooleanFormula exitBad = bfmgr.and(cAtSource, bfmgr.or(exitTransitions));
                sourceBads.add(exitBad);
                badObligations.add(
                    new SymbolicBadObligation(
                        "exit",
                        source,
                        Optional.empty(),
                        pfAtSource,
                        sourceEntry.getValue(),
                        exitBad));
              }
            }

            Map<CFANode, List<SymbolicTransferArrival>> nextLoopHeadArrivals =
                propagateOneIterationWithTransferRelation(
                    source,
                    pfAtSource,
                    sourceArrival.state(),
                    sourceArrival.precision(),
                    loopHeads,
                    exitSuccessors,
                    nonSuccOnlyComponentsByLocation,
                    false);
            if (nextLoopHeadArrivals == null) {
              logger.logf(
                  Level.INFO,
                  "Symbolic bail #4 (next-loop-head propagation null): source=%s.",
                  source);
              return Optional.empty();
            }
            for (Map.Entry<CFANode, List<SymbolicTransferArrival>> nextLoopHeadEntry :
                nextLoopHeadArrivals.entrySet()) {
              shutdownNotifier.shutdownIfNecessary();
              CFANode nextLoopHead = nextLoopHeadEntry.getKey();

              for (SymbolicTransferArrival nextLoopHeadArrival : nextLoopHeadEntry.getValue()) {
                if (!exitSuccessors.isEmpty()) {
                  Map<CFANode, List<SymbolicTransferArrival>> postIterationExitArrivals =
                      propagateOneIterationWithTransferRelation(
                          nextLoopHead,
                          nextLoopHeadArrival.pathFormula(),
                          nextLoopHeadArrival.state(),
                          nextLoopHeadArrival.precision(),
                          exitSuccessors,
                          exitSuccessors,
                          Map.of(),
                          false);
                  if (postIterationExitArrivals == null) {
                    logger.logf(
                        Level.INFO,
                        "Symbolic bail #4 (post-iteration exit propagation null): source=%s,"
                            + " nextLoopHead=%s.",
                        source,
                        nextLoopHead);
                    return Optional.empty();
                  }
                  if (!postIterationExitArrivals.isEmpty()) {
                    List<BooleanFormula> postIterationExitTransitions = new ArrayList<>();
                    for (List<SymbolicTransferArrival> arrivalsAtExit :
                        postIterationExitArrivals.values()) {
                      for (SymbolicTransferArrival exitArrival : arrivalsAtExit) {
                        postIterationExitTransitions.add(exitArrival.pathFormula().getFormula());
                      }
                    }
                    BooleanFormula postIterationExitBad =
                        bfmgr.and(cAtSource, bfmgr.or(postIterationExitTransitions));
                    sourceBads.add(postIterationExitBad);
                    badObligations.add(
                        new SymbolicBadObligation(
                            "post-iteration-exit",
                            source,
                            Optional.empty(),
                            pfAtSource,
                            sourceEntry.getValue(),
                            postIterationExitBad));
                  }
                }

                List<SymbolicTransferArrival> nextSourceArrivals;
                if (source.equals(nextLoopHead)) {
                  nextSourceArrivals = List.of(nextLoopHeadArrival);
                } else {
                  Map<CFANode, List<SymbolicTransferArrival>> targetArrivals =
                      propagateOneIterationWithTransferRelation(
                          nextLoopHead,
                          nextLoopHeadArrival.pathFormula(),
                          nextLoopHeadArrival.state(),
                          nextLoopHeadArrival.precision(),
                          ImmutableSet.of(source),
                          exitSuccessors,
                          Map.of(),
                          false);
                  if (targetArrivals == null) {
                    logger.logf(
                        Level.INFO,
                        "Symbolic bail #4 (target propagation null): source=%s,"
                            + " nextLoopHead=%s, target=%s.",
                        source,
                        nextLoopHead,
                        source);
                    return Optional.empty();
                  }
                  nextSourceArrivals = targetArrivals.getOrDefault(source, List.of());
                  if (nextSourceArrivals.isEmpty()) {
                    continue;
                  }
                }

                for (SymbolicTransferArrival nextSourceArrival : nextSourceArrivals) {
                  hasPreservationPath = true;
                  PathFormula pfAtNextSource = nextSourceArrival.pathFormula();
                  BooleanFormula cAtNextSource =
                      instantiateComponentsAt(sourceEntry.getValue(), pfAtNextSource);
                  BooleanFormula preservationBad =
                      bfmgr.and(cAtSource, pfAtNextSource.getFormula(), bfmgr.not(cAtNextSource));
                  sourceBads.add(preservationBad);
                  badObligations.add(
                      new SymbolicBadObligation(
                          "preservation",
                          source,
                          Optional.of(source),
                          pfAtSource,
                          sourceEntry.getValue(),
                          preservationBad));
                }
              }
            }

            if (!hasPreservationPath && sourceBads.isEmpty()) {
              logger.logf(
                  Level.INFO,
                  "Symbolic bail #5 (source has no preservation path): loopHead=%s, source=%s.",
                  loopHead,
                  source);
              return Optional.empty();
            }
            if (!sourceBads.isEmpty()) {
              sourcesProcessed++;
              allBads.add(bfmgr.or(sourceBads));
              allPreconditions.add(sourcePrecondition);
            }
          }
        }
      }

      if (allBads.isEmpty()) {
        logger.logf(
            Level.INFO,
            "Symbolic bail #5 (allBads empty): loopHeads=%d, processedSources=%d,"
                + " skipped(no state)=%d, skipped(no source prefix)=%d.",
            loopHeads.size(),
            sourcesProcessed,
            loopHeadsSkippedNoState,
            sourcePrefixesSkipped);
        return Optional.empty();
      }
      totalBad = bfmgr.or(allBads);
      totalPrecondition =
          allPreconditions.isEmpty() ? bfmgr.makeFalse() : bfmgr.or(allPreconditions);
    } finally {
      stats.inductionPreparation.stop();
    }

    logger.log(Level.INFO, "Starting symbolic non-termination closure check...");
    stats.inductionCheck.start();
    try {
      prover.push(totalBad);
      boolean unsat;
      try {
        unsat = prover.isUnsat();
      } finally {
        prover.pop();
      }
      if (!unsat) {
        logger.log(
            Level.INFO,
            "Symbolic non-termination closure check: SAT (closure refuted).");
        if (pBuildRefinement) {
          lastNonTerminationRefinement =
              findSymbolicNonTerminationRefinement(pCandidateInvariant, badObligations);
        }
        return Optional.of(false);
      }
      // UNSAT on bad means: assuming a feasible source state with C(P), no exit or next
      // candidate violation exists. If every source precondition is infeasible, the closure is
      // vacuous, so verify that at least one source precondition is satisfiable.
      prover.push(totalPrecondition);
      boolean precondUnsat;
      try {
        precondUnsat = prover.isUnsat();
      } finally {
        prover.pop();
      }
      if (precondUnsat) {
        logger.log(
            Level.INFO,
            "Symbolic non-termination closure check: UNSAT but precondition is also UNSAT"
                + " -- vacuous closure; deferring to BMC fallback.");
        return Optional.empty();
      }
      logger.log(
          Level.INFO,
          "Symbolic non-termination closure check: UNSAT (closure proven).");
      return Optional.of(true);
    } finally {
      stats.inductionCheck.stop();
    }
  }

  private Loop findLoopForSymbolicCheck(
      CandidateInvariant pCandidateInvariant, Optional<NonTerminationLoopScope> pLoopScope) {
    if (cfa.getLoopStructure().isEmpty()) {
      return null;
    }
    if (pLoopScope.isPresent()) {
      Set<CFANode> scopeNodes = pLoopScope.orElseThrow().loopNodes();
      for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
        if (loop.getLoopNodes().equals(scopeNodes)) {
          return loop;
        }
      }
      return null;
    }
    Set<CFANode> candidateLocations = new HashSet<>();
    for (CandidateInvariant component :
        CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant)) {
      if (component instanceof SingleLocationFormulaInvariant slfi) {
        candidateLocations.add(slfi.getLocation());
      }
    }
    if (candidateLocations.isEmpty()) {
      return null;
    }
    for (Loop loop : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      Set<CFANode> reachable = new HashSet<>(loop.getLoopNodes());
      for (CFAEdge edge : loop.getOutgoingEdges()) {
        reachable.add(edge.getSuccessor());
      }
      if (reachable.containsAll(candidateLocations)) {
        return loop;
      }
    }
    return null;
  }

  private static final int MAX_SYMBOLIC_PATHS = 64;
  private static final int MAX_SYMBOLIC_PATH_LENGTH = 200;

  private Map<CFANode, PathFormula> propagateOneIterationSymbolically(
      Loop pLoop,
      CFANode pSource,
      PathFormula pPfPre,
      Set<CFANode> pTargets,
      Set<CFANode> pExitSuccessors)
      throws CPAException, InterruptedException {
    return propagateOneIterationSymbolically(
        pLoop, pSource, pPfPre, pTargets, pExitSuccessors, Map.of(), false);
  }

  private Map<CFANode, PathFormula> propagateOneIterationSymbolically(
      Loop pLoop,
      CFANode pSource,
      PathFormula pPfPre,
      Set<CFANode> pTargets,
      Set<CFANode> pExitSuccessors,
      Map<CFANode, List<CandidateInvariant>> pAssertionsByLocation,
      boolean pAssertAtTargets)
      throws CPAException, InterruptedException {

    if (symbolicTransferPropagationUnsupported(pLoop)) {
      return null;
    }

    StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
    AbstractState initialState = cpa.getInitialState(pSource, partition);
    Precision initialPrecision = cpa.getInitialPrecision(pSource, partition);
    return mergeTransferArrivals(
        propagateOneIterationWithTransferRelation(
            pSource,
            pPfPre,
            initialState,
            initialPrecision,
            pTargets,
            pExitSuccessors,
            pAssertionsByLocation,
            pAssertAtTargets));
  }

  private boolean symbolicTransferPropagationUnsupported(Loop pLoop) {
    if (loopRequiresNestedLoopSummary(pLoop)) {
      return true;
    }
    if (loopContainsUnsupportedAliasingWrite(pLoop)) {
      logger.logf(
          Level.INFO,
          "Symbolic propagation bail (aliasing write): loop=%s contains a write through a"
              + " non-id left-hand side.",
          pLoop.getLoopHeads());
      return true;
    }
    if (loopContainsUnsupportedFunctionCall(pLoop)) {
      logger.logf(
          Level.INFO,
          "Symbolic propagation bail (function call): loop=%s contains a function call that is"
              + " not pure scalar.",
          pLoop.getLoopHeads());
      return true;
    }
    return false;
  }

  private boolean loopRequiresNestedLoopSummary(Loop pLoop) {
    Set<CFANode> loopNodes = pLoop.getLoopNodes();
    for (Loop other : cfa.getLoopStructure().orElseThrow().getAllLoops()) {
      if (other.equals(pLoop)) {
        continue;
      }
      Set<CFANode> otherLoopNodes = other.getLoopNodes();
      if (Collections.disjoint(loopNodes, otherLoopNodes)) {
        continue;
      }
      if (loopNodes.containsAll(otherLoopNodes)) {
        logger.logf(
            Level.INFO,
            "Symbolic propagation bail (nested loop summary missing): loop=%s contains nested"
                + " loop=%s.",
            pLoop.getLoopHeads(),
            other.getLoopHeads());
        return true;
      }
      if (!otherLoopNodes.containsAll(loopNodes)) {
        logger.logf(
            Level.INFO,
            "Symbolic propagation bail (overlapping loop): loop=%s partially overlaps with"
                + " loop=%s.",
            pLoop.getLoopHeads(),
            other.getLoopHeads());
        return true;
      }
    }
    return false;
  }

  private boolean loopContainsUnsupportedAliasingWrite(Loop pLoop) {
    for (CFANode node : pLoop.getLoopNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (edge instanceof CStatementEdge statementEdge
            && statementEdge.getStatement() instanceof CAssignment assignment
            && !isSupportedSymbolicWriteLeftHandSide(assignment.getLeftHandSide())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isSupportedSymbolicWriteLeftHandSide(CExpression pLeftHandSide) {
    if (pLeftHandSide instanceof CIdExpression) {
      return true;
    }
    return isFixedDirectArrayElement(pLeftHandSide);
  }

  private boolean isFixedDirectArrayElement(CExpression pExpression) {
    if (!(pExpression instanceof CArraySubscriptExpression arraySubscriptExpression)
        || !(arraySubscriptExpression.getArrayExpression() instanceof CIdExpression arrayId)
        || !(arraySubscriptExpression.getSubscriptExpression()
            instanceof CIntegerLiteralExpression)) {
      return false;
    }
    return arrayId.getExpressionType().getCanonicalType() instanceof CArrayType;
  }

  private boolean loopContainsUnsupportedFunctionCall(Loop pLoop) {
    for (CFANode node : pLoop.getLoopNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (edge instanceof CFunctionCallEdge functionCallEdge
            && !isPureScalarFunctionCall(functionCallEdge, new HashSet<>())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isPureScalarFunctionCall(
      CFunctionCallEdge pFunctionCallEdge, Set<String> pVisitedFunctions) {
    CFunctionEntryNode functionEntry = pFunctionCallEdge.getSuccessor();
    CFunctionDeclaration declaration = functionEntry.getFunctionDefinition();
    CFunctionType functionType = declaration.getType();
    if (functionType.takesVarArgs()) {
      return false;
    }
    if (!isPureScalarReturnType(functionType.getReturnType())) {
      return false;
    }
    for (CParameterDeclaration parameter : functionEntry.getFunctionParameters()) {
      if (!isPureScalarValueType(parameter.getType())) {
        return false;
      }
    }
    for (CExpression argument : pFunctionCallEdge.getArguments()) {
      if (!isPureScalarValueType(argument.getExpressionType())) {
        return false;
      }
    }
    return isPureScalarFunctionBody(functionEntry, pVisitedFunctions);
  }

  private boolean isPureScalarFunctionBody(
      CFunctionEntryNode pFunctionEntry, Set<String> pVisitedFunctions) {
    String functionName = pFunctionEntry.getFunctionName();
    if (!pVisitedFunctions.add(functionName)) {
      return false;
    }
    try {
      for (CFANode node : cfa.nodes()) {
        if (!node.getFunctionName().equals(functionName)) {
          continue;
        }
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge edge = node.getLeavingEdge(i);
          if (edge instanceof CFunctionCallEdge nestedCallEdge) {
            if (!isPureScalarFunctionCall(nestedCallEdge, pVisitedFunctions)) {
              return false;
            }
            continue;
          }
          if (edge instanceof CStatementEdge statementEdge) {
            if (statementEdge.getStatement() instanceof CAssignment assignment) {
              if (!isPureScalarAssignment(assignment)) {
                return false;
              }
            } else if (statementEdge.getStatement() instanceof CFunctionCall) {
              return false;
            }
          }
          if (edge instanceof CDeclarationEdge declarationEdge
              && declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDecl) {
            if (variableDecl.isGlobal() || !isPureScalarValueType(variableDecl.getType())) {
              return false;
            }
          }
        }
      }
      return true;
    } finally {
      pVisitedFunctions.remove(functionName);
    }
  }

  private boolean isPureScalarAssignment(CAssignment pAssignment) {
    if (!(pAssignment.getLeftHandSide() instanceof CIdExpression idExpression)) {
      return false;
    }
    if (idExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration
        && variableDeclaration.isGlobal()) {
      return false;
    }
    return isPureScalarValueType(idExpression.getExpressionType())
        && isPureScalarValueType(pAssignment.getRightHandSide().getExpressionType());
  }

  private boolean isPureScalarReturnType(CType pType) {
    return pType.getCanonicalType() instanceof CVoidType || isPureScalarValueType(pType);
  }

  private boolean isPureScalarValueType(CType pType) {
    CType canonicalType = pType.getCanonicalType();
    return canonicalType instanceof CSimpleType
        || canonicalType instanceof CEnumType
        || canonicalType instanceof CBitFieldType;
  }

  private static final int MAX_SYMBOLIC_TRANSFER_STATES = 512;

  private Map<CFANode, List<SymbolicTransferArrival>> propagateOneIterationWithTransferRelation(
      CFANode pSource,
      PathFormula pPfPre,
      AbstractState pSeedState,
      Precision pSeedPrecision,
      Set<CFANode> pTargets,
      Set<CFANode> pExitSuccessors,
      Map<CFANode, List<CandidateInvariant>> pAssertionsByLocation,
      boolean pAssertAtTargets)
      throws CPAException, InterruptedException {

    PathFormula seedPathFormula = pPfPre;
    if (pAssertionsByLocation.containsKey(pSource)) {
      seedPathFormula =
          pfmgr.makeAnd(
              seedPathFormula,
              getUninstantiatedComponentsFormula(
                  pAssertionsByLocation.get(pSource), seedPathFormula));
    }

    Optional<AbstractState> seededInitialState =
        replacePredicatePathFormula(pSeedState, seedPathFormula);
    if (seededInitialState.isEmpty()) {
      logger.logf(
          Level.INFO,
          "Symbolic propagation bail (transfer relation): could not seed predicate path"
              + " formula at source %s.",
          pSource);
      return null;
    }

    ReachedSet localReached = reachedSetFactory.create(cpa);
    TransferRelation transfer = cpa.getTransferRelation();
    localReached.add(seededInitialState.orElseThrow(), pSeedPrecision);
    Map<AbstractState, Integer> depths = new HashMap<>();
    depths.put(seededInitialState.orElseThrow(), 0);

    Map<CFANode, List<SymbolicTransferArrival>> byTarget = new LinkedHashMap<>();
    int processedStates = 0;

    while (localReached.hasWaitingState()) {
      shutdownNotifier.shutdownIfNecessary();
      if (processedStates++ >= MAX_SYMBOLIC_TRANSFER_STATES) {
        logger.logf(
            Level.INFO,
            "Symbolic propagation bail (transfer state budget): source=%s, targets=%s,"
                + " processed=%d == MAX_SYMBOLIC_TRANSFER_STATES=%d.",
            pSource,
            pTargets,
            processedStates,
            MAX_SYMBOLIC_TRANSFER_STATES);
        return null;
      }

      AbstractState current = localReached.popFromWaitlist();
      Precision precision = localReached.getPrecision(current);
      int currentDepth = depths.getOrDefault(current, 0);
      if (currentDepth >= MAX_SYMBOLIC_PATH_LENGTH) {
        logger.logf(
            Level.INFO,
            "Symbolic propagation bail (transfer path-length budget): source=%s,"
                + " current=%s, depth=%d == MAX_SYMBOLIC_PATH_LENGTH=%d.",
            pSource,
            AbstractStates.extractLocation(current),
            currentDepth,
            MAX_SYMBOLIC_PATH_LENGTH);
        return null;
      }

      Collection<? extends AbstractState> successors =
          transfer.getAbstractSuccessors(current, precision);
      for (AbstractState rawSuccessor : successors) {
        shutdownNotifier.shutdownIfNecessary();
        Optional<PrecisionAdjustmentResult> adjustedOptional =
            cpa.getPrecisionAdjustment()
                .prec(rawSuccessor, precision, localReached, Functions.identity(), rawSuccessor);
        if (adjustedOptional.isEmpty()) {
          continue;
        }
        PrecisionAdjustmentResult adjustedResult = adjustedOptional.orElseThrow();
        AbstractState successor = adjustedResult.abstractState();
        Precision successorPrecision = adjustedResult.precision();
        CFANode successorLocation = AbstractStates.extractLocation(successor);
        if (successorLocation == null) {
          logger.logf(
              Level.INFO,
              "Symbolic propagation bail (transfer relation): successor without unique"
                  + " CFA location from source %s.",
              pSource);
          return null;
        }

        Optional<PathFormula> successorPathFormula = extractPredicatePathFormula(successor);
        if (successorPathFormula.isEmpty()) {
          logger.logf(
              Level.INFO,
              "Symbolic propagation bail (transfer relation): successor without predicate"
                  + " path formula at location %s from source %s.",
              successorLocation,
              pSource);
          return null;
        }
        PathFormula pathFormula = successorPathFormula.orElseThrow();
        if ((pAssertAtTargets || !pTargets.contains(successorLocation))
            && pAssertionsByLocation.containsKey(successorLocation)) {
          pathFormula =
              pfmgr.makeAnd(
                  pathFormula,
                  getUninstantiatedComponentsFormula(
                      pAssertionsByLocation.get(successorLocation), pathFormula));
          Optional<AbstractState> successorWithAssertion =
              replacePredicatePathFormula(successor, pathFormula);
          if (successorWithAssertion.isEmpty()) {
            logger.logf(
                Level.INFO,
                "Symbolic propagation bail (transfer relation): could not add assertion at"
                    + " location %s from source %s.",
                successorLocation,
                pSource);
            return null;
          }
          successor = successorWithAssertion.orElseThrow();
        }

        int successorDepth = currentDepth + 1;
        if (pTargets.contains(successorLocation)) {
          byTarget
              .computeIfAbsent(successorLocation, unused -> new ArrayList<>())
              .add(
                  new SymbolicTransferArrival(
                      successorLocation, pathFormula, successor, successorPrecision));
          if (countTransferArrivals(byTarget) >= MAX_SYMBOLIC_PATHS) {
            logger.logf(
                Level.INFO,
                "Symbolic propagation bail (transfer path-count budget): source=%s,"
                    + " targets=%s, collected=%d == MAX_SYMBOLIC_PATHS=%d.",
                pSource,
                pTargets,
                countTransferArrivals(byTarget),
                MAX_SYMBOLIC_PATHS);
            return null;
          }
          continue;
        }
        if (pExitSuccessors.contains(successorLocation)) {
          continue;
        }

        boolean stop =
            cpa.getStopOperator()
                .stop(successor, localReached.getReached(successor), successorPrecision);
        if (!stop) {
          localReached.add(successor, successorPrecision);
          depths.put(successor, successorDepth);
        }
      }
    }

    if (byTarget.isEmpty()) {
      logger.logf(
          Level.INFO,
          "Symbolic propagation result (no transfer paths found): source=%s, targets=%s,"
              + " exitSuccessors=%d.",
          pSource,
          pTargets,
          pExitSuccessors.size());
      return new HashMap<>();
    }
    return byTarget;
  }

  private Map<CFANode, PathFormula> mergeTransferArrivals(
      Map<CFANode, List<SymbolicTransferArrival>> pByTarget) throws InterruptedException {
    if (pByTarget == null) {
      return null;
    }
    Map<CFANode, PathFormula> arrivals = new HashMap<>();
    for (Map.Entry<CFANode, List<SymbolicTransferArrival>> entry : pByTarget.entrySet()) {
      List<SymbolicTransferArrival> targetArrivals = entry.getValue();
      PathFormula merged = targetArrivals.get(0).pathFormula();
      for (int i = 1; i < targetArrivals.size(); i++) {
        shutdownNotifier.shutdownIfNecessary();
        merged = pfmgr.makeOr(merged, targetArrivals.get(i).pathFormula());
      }
      arrivals.put(entry.getKey(), merged);
    }
    return arrivals;
  }

  private int countTransferArrivals(Map<CFANode, List<SymbolicTransferArrival>> pArrivals) {
    int result = 0;
    for (List<SymbolicTransferArrival> arrivals : pArrivals.values()) {
      result += arrivals.size();
    }
    return result;
  }

  private Optional<PathFormula> extractPredicatePathFormula(AbstractState pState) {
    PredicateAbstractState predicateState =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    if (predicateState == null) {
      return Optional.empty();
    }
    return Optional.of(predicateState.getPathFormula());
  }

  private Optional<AbstractState> replacePredicatePathFormula(
      AbstractState pState, PathFormula pPathFormula) {
    if (pState instanceof PredicateAbstractState predicateState) {
      return Optional.of(
          PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
              pPathFormula, predicateState));
    }
    if (pState instanceof ARGState argState) {
      return replacePredicatePathFormula(argState.getWrappedState(), pPathFormula)
          .map(wrappedState -> new ARGState(wrappedState, null));
    }
    if (pState instanceof CompositeState compositeState) {
      List<AbstractState> newWrappedStates = new ArrayList<>(compositeState.getWrappedStates());
      boolean changed = false;
      for (int i = 0; i < newWrappedStates.size(); i++) {
        Optional<AbstractState> replacement =
            replacePredicatePathFormula(newWrappedStates.get(i), pPathFormula);
        if (replacement.isPresent()) {
          newWrappedStates.set(i, replacement.orElseThrow());
          changed = true;
        }
      }
      return changed ? Optional.of(new CompositeState(newWrappedStates)) : Optional.empty();
    }
    return Optional.empty();
  }

  private Map<CFANode, List<CandidateInvariant>> getNonSuccessorOnlyComponentsByLocation(
      Map<CFANode, List<CandidateInvariant>> pComponentsByLocation)
      throws CPATransferException, InterruptedException {
    Map<CFANode, List<CandidateInvariant>> result = new LinkedHashMap<>();
    for (Map.Entry<CFANode, List<CandidateInvariant>> entry : pComponentsByLocation.entrySet()) {
      List<CandidateInvariant> nonSuccOnly = new ArrayList<>();
      for (CandidateInvariant comp : entry.getValue()) {
        if (!isSuccessorOnlyComponent(comp)) {
          nonSuccOnly.add(comp);
        }
      }
      if (!nonSuccOnly.isEmpty()) {
        result.put(entry.getKey(), nonSuccOnly);
      }
    }
    return result;
  }

  private Map<CFANode, List<CandidateInvariant>> getTerminalCandidateSources(
      Map<CFANode, List<CandidateInvariant>> pComponentsByLocation,
      Loop pLoop,
      Set<CFANode> pLoopHeads,
      Set<CFANode> pExitSuccessors)
      throws InterruptedException {
    Map<CFANode, List<CandidateInvariant>> result = new LinkedHashMap<>();
    Set<CFANode> candidateLocations = pComponentsByLocation.keySet();
    for (Map.Entry<CFANode, List<CandidateInvariant>> entry : pComponentsByLocation.entrySet()) {
      shutdownNotifier.shutdownIfNecessary();
      CFANode location = entry.getKey();
      if (!canReachAnotherCandidateBeforeNextLoopHead(
          location, candidateLocations, pLoop, pLoopHeads, pExitSuccessors)) {
        result.put(location, entry.getValue());
      }
    }
    return result;
  }

  private boolean canReachAnotherCandidateBeforeNextLoopHead(
      CFANode pSource,
      Set<CFANode> pCandidateLocations,
      Loop pLoop,
      Set<CFANode> pLoopHeads,
      Set<CFANode> pExitSuccessors)
      throws InterruptedException {
    Deque<CFANode> waitlist = new ArrayDeque<>();
    Set<CFANode> visited = new HashSet<>();
    visited.add(pSource);
    for (int i = 0; i < pSource.getNumLeavingEdges(); i++) {
      CFANode successor = pSource.getLeavingEdge(i).getSuccessor();
      if (!pLoop.getLoopNodes().contains(successor) || pExitSuccessors.contains(successor)) {
        continue;
      }
      if (pLoopHeads.contains(successor)) {
        continue;
      }
      waitlist.add(successor);
    }
    while (!waitlist.isEmpty()) {
      shutdownNotifier.shutdownIfNecessary();
      CFANode current = waitlist.removeFirst();
      if (!visited.add(current)) {
        continue;
      }
      if (pCandidateLocations.contains(current)) {
        return true;
      }
      for (int i = 0; i < current.getNumLeavingEdges(); i++) {
        CFANode successor = current.getLeavingEdge(i).getSuccessor();
        if (!pLoop.getLoopNodes().contains(successor)
            || pExitSuccessors.contains(successor)
            || pLoopHeads.contains(successor)) {
          continue;
        }
        waitlist.add(successor);
      }
    }
    return false;
  }

  private BooleanFormula instantiateComponentsAt(
      List<CandidateInvariant> pComponents, PathFormula pPfAt)
      throws CPATransferException, InterruptedException {
    if (pComponents == null || pComponents.isEmpty()) {
      return bfmgr.makeTrue();
    }
    SSAMap ssa = pPfAt.getSsa().withDefault(1);
    BooleanFormula result = bfmgr.makeTrue();
    for (CandidateInvariant component : pComponents) {
      shutdownNotifier.shutdownIfNecessary();
      BooleanFormula uninstantiated = component.getFormula(fmgr, pfmgr, pPfAt);
      BooleanFormula instantiated = fmgr.instantiate(uninstantiated, ssa);
      result = bfmgr.and(result, instantiated);
    }
    return result;
  }

  private BooleanFormula getUninstantiatedComponentsFormula(
      List<CandidateInvariant> pComponents, PathFormula pPfAt)
      throws CPATransferException, InterruptedException {
    if (pComponents == null || pComponents.isEmpty()) {
      return bfmgr.makeTrue();
    }
    BooleanFormula result = bfmgr.makeTrue();
    for (CandidateInvariant component : pComponents) {
      shutdownNotifier.shutdownIfNecessary();
      result = bfmgr.and(result, component.getFormula(fmgr, pfmgr, pPfAt));
    }
    return result;
  }

  private record SymbolicBadObligation(
      String kind,
      CFANode source,
      Optional<CFANode> target,
      PathFormula sourcePathFormula,
      List<CandidateInvariant> sourceComponents,
      BooleanFormula formula) {}

  private record SymbolicTransferArrival(
      CFANode location, PathFormula pathFormula, AbstractState state, Precision precision) {}

  private Optional<CandidateInvariant> findSymbolicNonTerminationRefinement(
      CandidateInvariant pOriginal, List<SymbolicBadObligation> pBadObligations)
      throws CPATransferException, InterruptedException, SolverException {
    for (SymbolicBadObligation obligation : pBadObligations) {
      shutdownNotifier.shutdownIfNecessary();
      prover.push(obligation.formula());
      try {
        if (!prover.isUnsat()) {
          List<ValueAssignment> modelAssignments;
          try {
            modelAssignments = prover.getModelAssignments();
          } catch (SolverException | RuntimeException e) {
            logger.logUserException(
                Level.INFO,
                e,
                "Symbolic non-termination refinement: could not extract model; continuing"
                    + " without refinement.");
            return Optional.empty();
          }
          Optional<CandidateInvariant> refinement =
              buildSymbolicNonTerminationRefinement(pOriginal, obligation, modelAssignments);
          if (refinement.isPresent()) {
            logger.logf(
                Level.INFO,
                "Symbolic non-termination refinement: blocking %s counterexample at %s%s.",
                obligation.kind(),
                obligation.source(),
                obligation.target().map(target -> " toward " + target).orElse(""));
          } else {
            logger.logf(
                Level.FINER,
                "Symbolic non-termination refinement: no usable model literals for %s"
                    + " counterexample at %s%s.",
                obligation.kind(),
                obligation.source(),
                obligation.target().map(target -> " toward " + target).orElse(""));
          }
          return refinement;
        }
      } finally {
        prover.pop();
      }
    }
    return Optional.empty();
  }

  private Optional<CandidateInvariant> buildSymbolicNonTerminationRefinement(
      CandidateInvariant pOriginal, SymbolicBadObligation pObligation, List<ValueAssignment> pModel)
      throws CPATransferException, InterruptedException {
    Set<String> relevantVariables =
        getRelevantSymbolicRefinementVariables(
            pObligation.sourceComponents(), pObligation.sourcePathFormula());
    SSAMap sourceSsa = pObligation.sourcePathFormula().getSsa().withDefault(1);
    List<BooleanFormula> equalities = new ArrayList<>();
    Set<String> seenVariables = new HashSet<>();

    for (ValueAssignment valueAssignment : pModel) {
      if (valueAssignment.isFunction()
          || !isSupportedSymbolicRefinementValue(valueAssignment)) {
        continue;
      }
      Pair<String, OptionalInt> parsedName =
          FormulaManagerView.parseName(valueAssignment.getName());
      String variableName = parsedName.getFirst();
      OptionalInt index = parsedName.getSecond();
      if (index.isEmpty()
          || !sourceSsa.containsVariable(variableName)
          || sourceSsa.getIndex(variableName) != index.orElseThrow()
          || (!relevantVariables.isEmpty() && !relevantVariables.contains(variableName))
          || !seenVariables.add(variableName)) {
        continue;
      }
      equalities.add(valueAssignment.getAssignmentAsFormula());
      if (equalities.size() >= MAX_NON_TERMINATION_REFINEMENT_LITERALS) {
        break;
      }
    }

    if (equalities.isEmpty()) {
      return Optional.empty();
    }

    BooleanFormula modelCube = fmgr.uninstantiate(bfmgr.and(equalities));
    BooleanFormula blockingClause = bfmgr.not(modelCube);
    if (bfmgr.isTrue(blockingClause) || bfmgr.isFalse(blockingClause)) {
      return Optional.empty();
    }

    CandidateInvariant blockingCandidate =
        SingleLocationFormulaInvariant.makeLocationInvariant(
            pObligation.source(), blockingClause, fmgr);
    List<CandidateInvariant> refinedParts = new ArrayList<>();
    Iterables.addAll(refinedParts, CandidateInvariantCombination.getConjunctiveParts(pOriginal));
    refinedParts.add(blockingCandidate);
    return Optional.of(CandidateInvariantCombination.conjunction(refinedParts));
  }

  private Set<String> getRelevantSymbolicRefinementVariables(
      List<CandidateInvariant> pComponents, PathFormula pPathFormula)
      throws CPATransferException, InterruptedException {
    Set<String> relevantVariables = new HashSet<>();
    for (CandidateInvariant component : pComponents) {
      shutdownNotifier.shutdownIfNecessary();
      BooleanFormula componentFormula = component.getFormula(fmgr, pfmgr, pPathFormula);
      for (String variableName : fmgr.extractVariableNames(componentFormula)) {
        relevantVariables.add(FormulaManagerView.parseName(variableName).getFirst());
      }
    }
    return relevantVariables;
  }

  private boolean isSupportedSymbolicRefinementValue(ValueAssignment pValueAssignment) {
    Object value = pValueAssignment.getValue();
    return value instanceof Number || value instanceof Boolean;
  }

  /**
   * Build a non-termination refinement from the step-case counterexample, applying two
   * soundness/utility checks before accepting it:
   *
   * <ul>
   *   <li><b>Fix 3 (cex classification):</b> Pin the predecessor to the cex values and ask whether
   *       any path from there has all successors satisfying the original candidate. If unsat, the
   *       cex represents real termination (every continuation violates C), so we must not refine.
   *   <li><b>Fix 1 (post-refinement non-vacuity):</b> Build the refined candidate's violation
   *       formula and check it is still satisfiable under the existing path/predecessor context. If
   *       unsat, the refinement only collapses the violation to false without removing any real
   *       successor, so its closure would be vacuous.
   * </ul>
   *
   * Prover stack on entry: {successorExists, predecessor, loopHeadInv, successorViolation}. This
   * method temporarily pops and re-pushes successorViolation so the caller's stack accounting
   * remains correct.
   */
  private Optional<CandidateInvariant> buildValidatedNonTerminationRefinement(
      CandidateInvariant pOriginal,
      List<ValueAssignment> pModel,
      Set<AbstractState> pInductionHypothesis,
      BooleanFormula pSuccessorViolation)
      throws CPATransferException, InterruptedException, SolverException {
    Map<CFANode, List<BooleanFormula>> equalitiesByLoopHead =
        extractBestRefinementEqualitiesByLoopHead(pOriginal, pModel, pInductionHypothesis);
    if (equalitiesByLoopHead.isEmpty()) {
      return Optional.empty();
    }

    // Free the stack slot held by the original successorViolation so we can run our SAT
    // checks against a clean violation/continuation formula.
    prover.pop();
    try {
      // Fix 3: classify the cex. If pinning predecessor to the model values still admits some
      // path where no successor violates C, the cex picks one of several branches and refining
      // is meaningful. Otherwise every continuation leaves C (real termination signal).
      List<BooleanFormula> cexEqualities = new ArrayList<>();
      for (List<BooleanFormula> eqs : equalitiesByLoopHead.values()) {
        cexEqualities.addAll(eqs);
      }
      if (!cexEqualities.isEmpty()) {
        BooleanFormula cexPredAssertion = bfmgr.and(cexEqualities);
        prover.push(cexPredAssertion);
        try {
          prover.push(bfmgr.not(pSuccessorViolation));
          try {
            if (prover.isUnsat()) {
              logger.log(
                  Level.FINER,
                  "Non-termination cex has no continuation in C; refusing termination-masking"
                      + " refinement.");
              return Optional.empty();
            }
          } finally {
            prover.pop();
          }
        } finally {
          prover.pop();
        }
      }

      // Build the tentative refined candidate.
      Optional<CandidateInvariant> tentative =
          buildRefinementFromEqualities(pOriginal, equalitiesByLoopHead);
      if (tentative.isEmpty()) {
        return Optional.empty();
      }

      // Fix 1: ensure the refined candidate's violation formula is not vacuously false.
      CandidateInvariant refined = tentative.orElseThrow();
      Multimap<BooleanFormula, BooleanFormula> refinedViolationAssertions =
          getNonTerminationClosureViolationAssertions(refined, pInductionHypothesis);
      if (refinedViolationAssertions.isEmpty()) {
        logger.log(
            Level.FINER,
            "Refined non-termination candidate has no violation assertions; refusing vacuous"
                + " refinement.");
        return Optional.empty();
      }
      BooleanFormula refinedViolation =
          BMCHelper.disjoinStateViolationAssertions(bfmgr, refinedViolationAssertions);
      prover.push(refinedViolation);
      try {
        if (prover.isUnsat()) {
          logger.log(
              Level.FINER,
              "Refined non-termination candidate's violation is unsatisfiable under the reached"
                  + " set; refusing vacuous refinement.");
          return Optional.empty();
        }
      } finally {
        prover.pop();
      }

      return tentative;
    } finally {
      // Restore the original successorViolation so the caller's stack accounting matches.
      prover.push(pSuccessorViolation);
    }
  }

  private Map<CFANode, List<BooleanFormula>> extractBestRefinementEqualitiesByLoopHead(
      CandidateInvariant pOriginal,
      List<ValueAssignment> pModel,
      Set<AbstractState> pInductionHypothesis)
      throws CPATransferException, InterruptedException {
    Map<CFANode, List<BooleanFormula>> bestEqualitiesByLoopHead = new LinkedHashMap<>();
    for (AbstractState state : pInductionHypothesis) {
      CFANode loc = AbstractStates.extractLocation(state);
      if (loc == null) {
        continue;
      }
      PredicateAbstractState pas =
          AbstractStates.extractStateByType(state, PredicateAbstractState.class);
      if (pas == null) {
        continue;
      }
      PathFormula pathFormula = pas.getPathFormula();
      Set<String> relevantVariables =
          getRelevantNonTerminationRefinementVariables(pOriginal, state);
      if (relevantVariables.isEmpty()) {
        continue;
      }
      SSAMap ssaMap = pathFormula.getSsa().withDefault(1);
      List<BooleanFormula> equalities = new ArrayList<>();
      Set<String> seenVariables = new HashSet<>();
      for (ValueAssignment va : pModel) {
        if (va.isFunction()) {
          continue;
        }
        Pair<String, OptionalInt> parsed = FormulaManagerView.parseName(va.getName());
        String varName = parsed.getFirst();
        OptionalInt idx = parsed.getSecond();
        if (idx.isPresent()
            && relevantVariables.contains(varName)
            && ssaMap.getIndex(varName) == idx.orElseThrow()
            && seenVariables.add(varName)) {
          equalities.add(va.getAssignmentAsFormula());
          if (equalities.size() >= MAX_NON_TERMINATION_REFINEMENT_LITERALS) {
            break;
          }
        }
      }
      if (!equalities.isEmpty()) {
        List<BooleanFormula> existing = bestEqualitiesByLoopHead.get(loc);
        if (existing == null || equalities.size() < existing.size()) {
          bestEqualitiesByLoopHead.put(loc, equalities);
        }
      }
    }
    return bestEqualitiesByLoopHead;
  }

  private Optional<CandidateInvariant> buildRefinementFromEqualities(
      CandidateInvariant pOriginal, Map<CFANode, List<BooleanFormula>> pBestEqualitiesByLoopHead) {
    if (pBestEqualitiesByLoopHead.isEmpty()) {
      return Optional.empty();
    }
    List<CandidateInvariant> blockingComponents = new ArrayList<>();
    for (Map.Entry<CFANode, List<BooleanFormula>> entry : pBestEqualitiesByLoopHead.entrySet()) {
      CFANode loopHead = entry.getKey();
      BooleanFormula blockingClause = bfmgr.not(fmgr.uninstantiate(bfmgr.and(entry.getValue())));
      blockingComponents.add(
          SingleLocationFormulaInvariant.makeLocationInvariant(loopHead, blockingClause, fmgr));
    }
    List<CandidateInvariant> allParts = new ArrayList<>();
    Iterables.addAll(allParts, CandidateInvariantCombination.getConjunctiveParts(pOriginal));
    allParts.addAll(blockingComponents);
    return Optional.of(CandidateInvariantCombination.conjunction(allParts));
  }

  private Set<String> getRelevantNonTerminationRefinementVariables(
      CandidateInvariant pCandidateInvariant, AbstractState pState)
      throws CPATransferException, InterruptedException {
    Set<AbstractState> stateAsSet = Collections.singleton(pState);
    PredicateAbstractState pas =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);
    if (pas == null) {
      return ImmutableSet.of();
    }
    Set<String> relevantVariables = new HashSet<>();
    for (CandidateInvariant component :
        CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant)) {
      if (Iterables.isEmpty(component.filterApplicable(stateAsSet))) {
        continue;
      }
      BooleanFormula componentFormula = component.getFormula(fmgr, pfmgr, pas.getPathFormula());
      for (String variableName : fmgr.extractVariableNames(componentFormula)) {
        relevantVariables.add(FormulaManagerView.parseName(variableName).getFirst());
      }
    }
    return relevantVariables;
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
    if (requireSatisfiablePredecessor && prover.isUnsat()) {
      logger.log(
          Level.FINER,
          "The induction predecessor is unsatisfiable; refusing vacuous non-termination proof.");
      prover.pop(); // Pop invariant predecessor assertion
      prover.pop(); // Pop end states
      stats.inductionCheck.stop();
      return InductionResult.getFailed(ImmutableSet.of(), pK);
    }
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
            candidate -> {
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
    return inductiveLoopHeadInvariantAssertion(pLoopHeadStates, loopHeads);
  }

  private BooleanFormula inductiveLoopHeadInvariantAssertion(
      Iterable<AbstractState> pLoopHeadStates, Set<CFANode> pLoopHeads)
      throws CPATransferException, InterruptedException {
    Iterable<AbstractState> loopHeadStates =
        filterInductiveAssertionIteration(pLoopHeadStates, pLoopHeads);
    return assertAt(loopHeadStates, getCurrentLoopHeadInvariants(loopHeadStates), fmgr);
  }

  private FluentIterable<AbstractState> filterInductiveAssertionIteration(
      Iterable<AbstractState> pStates) {
    return filterInductiveAssertionIteration(pStates, loopHeads);
  }

  private FluentIterable<AbstractState> filterInductiveAssertionIteration(
      Iterable<AbstractState> pStates, Set<CFANode> pLoopHeads) {
    return filterIteration(pStates, 1, pLoopHeads);
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
            types.put(input, (CType) ssaMap.getType(input));
          }
        }
        for (String varName : ssaMap.allVariables()) {
          types.put(varName, (CType) ssaMap.getType(varName));
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

    SequencedMap<CounterexampleToInductivity, BooleanFormula> ctis = new LinkedHashMap<>();
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

  private Multimap<BooleanFormula, BooleanFormula> getNonTerminationClosureViolationAssertions(
      CandidateInvariant pCandidateInvariant, Set<AbstractState> pHypothesis)
      throws CPATransferException, InterruptedException {
    ReachedSet reached = reachedSet.getReachedSet();

    ImmutableListMultimap.Builder<BooleanFormula, BooleanFormula> stateViolationAssertionsBuilder =
        ImmutableListMultimap.builder();
    Iterable<AbstractState> assertionStates =
        from(pCandidateInvariant.filterApplicable(reached))
            .filter(state -> !pHypothesis.contains(state));

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

  private FluentIterable<AbstractState> filterNonTerminationPredecessorStates(
      Iterable<AbstractState> pReached,
      int pK,
      Set<Object> pCheckedKeys,
      Optional<NonTerminationLoopScope> pLoopScope) {
    Set<CFANode> relevantLoopHeads =
        pLoopScope.map(NonTerminationLoopScope::loopHeads).orElse(loopHeads);
    Iterable<AbstractState> scopedReached =
        pLoopScope
            .<Iterable<AbstractState>>map(
                loopScope -> AbstractStates.filterLocations(pReached, loopScope.loopNodes()))
            .orElse(pReached);
    return BMCHelper.filterBmcChecked(
            filterIterationsUpTo(scopedReached, pK, relevantLoopHeads), pCheckedKeys)
        .filter(state -> !isTerminalLoopHeadSuccessor(state, pK, relevantLoopHeads));
  }

  private boolean isTerminalLoopHeadSuccessor(
      AbstractState pState, int pK, Set<CFANode> pLoopHeads) {
    if (!BMCHelper.hasMatchingLocation(pState, pLoopHeads)) {
      return false;
    }
    LoopIterationReportingState loopState =
        AbstractStates.extractStateByType(pState, LoopIterationReportingState.class);
    return loopState != null && loopState.getDeepestIteration() == pK + 1;
  }

  private Optional<CandidateInvariant> getNonTerminationPredecessorCandidate(
      CandidateInvariant pCandidateInvariant) throws CPATransferException, InterruptedException {
    List<CandidateInvariant> predecessorComponents = new ArrayList<>();
    for (CandidateInvariant component :
        CandidateInvariantCombination.getConjunctiveParts(pCandidateInvariant)) {
      if (!isSuccessorOnlyComponent(component)) {
        predecessorComponents.add(component);
      }
    }

    if (predecessorComponents.isEmpty()) {
      return Optional.empty();
    }
    if (predecessorComponents.size() == 1) {
      return Optional.of(predecessorComponents.getFirst());
    }
    return Optional.of(new StatewiseCandidateInvariantConjunction(predecessorComponents));
  }

  private boolean isSuccessorOnlyComponent(CandidateInvariant pCandidateInvariant)
      throws CPATransferException, InterruptedException {
    return pCandidateInvariant instanceof SingleLocationFormulaInvariant
        && bfmgr.isFalse(pCandidateInvariant.getFormula(fmgr, pfmgr, pfmgr.makeEmptyPathFormula()));
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
  }
}
