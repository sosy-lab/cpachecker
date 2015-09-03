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
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
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
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.edgeexclusion.EdgeExclusionPrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
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

  private final boolean havocLoopTerminationConditionVariablesOnly;

  private ProverEnvironment prover = null;

  private InvariantSupplier invariantsSupplier;

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
      boolean pHavocLoopTerminationConditionVariablesOnly,
      ShutdownNotifier pShutdownNotifier) {
    cfa = checkNotNull(pCFA);
    logger = checkNotNull(pLogger);
    algorithm = checkNotNull(pAlgorithm);
    cpa = checkNotNull(pCPA);
    invariantGenerator  = checkNotNull(pInvariantGenerator);
    stats = checkNotNull(pStats);
    reachedSetFactory = checkNotNull(pReachedSetFactory);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    havocLoopTerminationConditionVariablesOnly = pHavocLoopTerminationConditionVariablesOnly;
    reached = reachedSetFactory.create();

    PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    solver = stepCasePredicateCPA.getSolver();
    fmgr = solver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
    pfmgr = stepCasePredicateCPA.getPathFormulaManager();
    loopHeadInvariants = bfmgr.makeBoolean(true);

    invariantsSupplier = InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
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
   *
   * @throws CPAException if the supporting invariant generation encountered
   * an exception.
   * @throws InterruptedException if the supporting invariant generation is
   * interrupted.
   */
  private ProverEnvironment getProver() {
    if (!isProverInitialized()) {
      prover = solver.newProverEnvironmentWithModelGeneration();
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

  /**
   * Gets the most current invariants generated by the invariant generator.
   *
   * @return the most current invariants generated by the invariant generator.
   * @throws InterruptedException
   * @throws CPATransferException
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

    BooleanFormula invariant = currentInvariantsSupplier.getInvariantFor(pLocation, pFMGR, pPFMGR);

    for (CandidateInvariant confirmedCandidate : this.confirmedCandidates) {
      invariant = bfmgr.and(invariant, confirmedCandidate.getFormula(pFMGR, pPFMGR));
    }

    return invariant;
  }

  @Override
  public void close() {
    if (isProverInitialized()) {
      while (stackDepth-- > 0) {
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
      throws CPAException, InterruptedException {
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
    CallstackCPA stepCaseCallstackCPA = CPAs.retrieveCPA(cpa, CallstackCPA.class);

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
            stepCaseCallstackCPA.setMaxRecursionDepth(k);
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

    // Create the formula asserting the faultiness of the successor
    stepCaseBoundsCPA.setMaxLoopIterations(k + 1);
    stepCaseCallstackCPA.setMaxRecursionDepth(k + 1);
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);
    stopLocations = getStopLocations(reached);

    this.previousK = k + 1;

    // Attempt the induction proofs
    ProverEnvironment prover = getProver();
    int numberOfSuccessfulProofs = 0;
    stats.inductionPreparation.stop();
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
      push(loopHeadInv); // Assert the known invariants
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
      while (!isInvariant && !currentInvariants.equals(oldInvariants)) {
        push(fmgr.instantiate(currentInvariants, SSAMap.emptySSAMap().withDefault(1)));
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
      pop(); // Pop loop head invariants
      stats.inductionCheck.stop();

      logger.log(Level.FINER, "Soundness after induction check:", isInvariant);
    }

    return numberOfSuccessfulProofs == candidateInvariants.size();
  }

  private void ensureReachedSetInitialized(ReachedSet pReachedSet) throws InterruptedException, CPAException {
    if (pReachedSet.size() > 1 || !cfa.getLoopStructure().isPresent()) {
      return;
    }
    for (Loop loop : cfa.getLoopStructure().get().getAllLoops()) {
      for (CFANode loopHead : loop.getLoopHeads()) {
        if (havocLoopTerminationConditionVariablesOnly) {
          CFANode mainEntryNode = cfa.getMainFunction();
          Precision precision = cpa.getInitialPrecision(mainEntryNode, StateSpacePartition.getDefaultPartition());
          precision = excludeEdges(precision, CFAUtils.leavingEdges(loopHead));
          pReachedSet.add(cpa.getInitialState(mainEntryNode, StateSpacePartition.getDefaultPartition()), precision);
          algorithm.run(pReachedSet);
          Collection<AbstractState> loopHeadStates = new ArrayList<>();
          Iterables.addAll(loopHeadStates, filterLocation(pReachedSet, loopHead));
          pReachedSet.clear();
          Collection<String> loopTerminationConditionVariables = getTerminationConditionVariables(loop);
          for (AbstractState loopHeadState : loopHeadStates) {
            // Havoc the "loop termination condition" variables in predicate analysis state
            PredicateAbstractState pas = extractStateByType(loopHeadState, PredicateAbstractState.class);
            PathFormula pathFormula = pas.getPathFormula();
            SSAMapBuilder ssaMapBuilder = pathFormula.getSsa().builder();
            Set<String> containedVariables = ssaMapBuilder.allVariables();
            for (String variable : loopTerminationConditionVariables) {
              if (containedVariables.contains(variable)) {
                CType type = ssaMapBuilder.getType(variable);
                int freshIndex = ssaMapBuilder.getFreshIndex(variable);
                ssaMapBuilder.setIndex(variable, type, freshIndex);
              }
            }

            AbstractState newLoopHeadState = cpa.getInitialState(loopHead, StateSpacePartition.getDefaultPartition());

            PredicateAbstractState newPAS = extractStateByType(newLoopHeadState, PredicateAbstractState.class);
            newPAS.setPathFormula(pfmgr.makeNewPathFormula(pathFormula, ssaMapBuilder.build()));

            pReachedSet.add(newLoopHeadState, cpa.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition()));
          }
        } else {
          Precision precision = cpa.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition());
          pReachedSet.add(cpa.getInitialState(loopHead, StateSpacePartition.getDefaultPartition()), precision);
        }
      }
    }
  }

  private Collection<String> getTerminationConditionVariables(Loop pLoop) throws CPATransferException, InterruptedException {
    Collection<String> result = new HashSet<>();
    result.add(CFASingleLoopTransformation.PROGRAM_COUNTER_VAR_NAME);
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    for (CFANode loopHead : pLoop.getLoopHeads()) {
      waitlist.offer(loopHead);
      visited.add(loopHead);
    }
    while (!waitlist.isEmpty()) {
      CFANode current = waitlist.poll();
      assert pLoop.getLoopNodes().contains(current);
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
        CFANode successor = leavingEdge.getSuccessor();
        if (!isLoopExitEdge(leavingEdge, pLoop)) {
          if (visited.add(successor)) {
            waitlist.offer(successor);
          }
        } else {
          PathFormula formula = pfmgr.makeFormulaForPath(Collections.singletonList(leavingEdge));
          result.addAll(fmgr.extractVariableNames(fmgr.uninstantiate(formula.getFormula())));
        }
      }
    }
    return result;
  }

  private static boolean isLoopExitEdge(CFAEdge pEdge, Loop pLoop) {
    return !pLoop.getLoopNodes().contains(pEdge.getSuccessor());
  }

  /**
   * Excludes the given edges from the given precision if the EdgeExclusionCPA
   * is activated to allow for such edge exclusions.
   *
   * @param pPrecision the precision to exclude the edges from.
   * @param pEdgesToIgnore the edges to be excluded.
   * @return the new precision.
   */
  private Precision excludeEdges(Precision pPrecision, Iterable<CFAEdge> pEdgesToIgnore) {
    EdgeExclusionPrecision oldPrecision = Precisions.extractPrecisionByType(pPrecision, EdgeExclusionPrecision.class);
    if (oldPrecision != null) {
      EdgeExclusionPrecision newPrecision = oldPrecision.excludeMoreEdges(pEdgesToIgnore);
      return Precisions.replaceByType(pPrecision, newPrecision, Predicates.instanceOf(EdgeExclusionPrecision.class));
    }
    return pPrecision;
  }

  private static Set<CFANode> getStopLocations(ReachedSet pReachedSet) {
    return from(pReachedSet).filter(BMCAlgorithm.IS_STOP_STATE).transform(AbstractStates.EXTRACT_LOCATION).toSet();
  }

}
