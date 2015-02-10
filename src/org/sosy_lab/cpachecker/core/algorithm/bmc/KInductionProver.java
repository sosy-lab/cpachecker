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

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;

import javax.annotation.concurrent.GuardedBy;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.CFASingleLoopTransformation;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantGenerator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.edgeexclusion.EdgeExclusionPrecision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsPrecision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsState;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackCPA;
import org.sosy_lab.cpachecker.cpa.loopstack.LoopstackState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * Instances of this class are used to prove the safety of a program by
 * applying an inductive approach based on k-induction.
 */
class KInductionProver implements AutoCloseable {

  private final CFA cfa;

  private final LogManager logger;

  private final Algorithm algorithm;

  private final ConfigurableProgramAnalysis cpa;

  private final Boolean trivialResult;

  private final ReachedSet reachedSet;

  private final Loop loop;

  private final Solver stepCaseSolver;

  private final FormulaManagerView stepCaseFMGR;

  private final BooleanFormulaManagerView stepCaseBFMGR;

  private final PathFormulaManager stepCasePFMGR;

  private final BMCStatistics stats;

  private final ReachedSetFactory reachedSetFactory;

  private final boolean isProgramConcurrent;

  private final ReachedSetInitializer reachedSetInitializer = new ReachedSetInitializer() {

    @Override
    public void initialize(ReachedSet pReachedSet) throws CPAException, InterruptedException {
      ensureReachedSetInitialized(pReachedSet);
    }
  };

  private final InvariantGenerator invariantGenerator;

  private final boolean havocLoopTerminationConditionVariablesOnly;

  private final Predicate<? super AbstractState> isTargetState;

  private final Supplier<Integer> bmcKAccessor;

  private final Set<CandidateInvariant> knownLoopHeadInvariants = new CopyOnWriteArraySet<>();

  private ProverEnvironment prover = null;

  private UnmodifiableReachedSet invariantsReachedSet;

  private BooleanFormula loopHeadInvariants;

  private int stackDepth = 0;

  @GuardedBy("this")
  private ImmutableSet<CFANode> targetLocations = null;

  @GuardedBy("this")
  private boolean targetLocationsChanged = false;

  private BooleanFormula previousFormula = null;

  private int previousK = -1;

  @GuardedBy("this")
  private ImmutableSet<CandidateInvariant> potentialLoopHeadInvariants = ImmutableSet.of();

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
      boolean pIsProgramConcurrent,
      TargetLocationProvider pTargetLocationProvider,
      boolean pHavocLoopTerminationConditionVariablesOnly,
      Supplier<Integer> pBMCKAccessor,
      Predicate<? super AbstractState> pIsTargetStatePredicate,
      ShutdownNotifier pShutdownNotifier) {
    Preconditions.checkNotNull(pCFA);
    Preconditions.checkNotNull(pLogger);
    Preconditions.checkNotNull(pAlgorithm);
    Preconditions.checkNotNull(pCPA);
    Preconditions.checkNotNull(pInvariantGenerator);
    Preconditions.checkNotNull(pStats);
    Preconditions.checkNotNull(pReachedSetFactory);
    Preconditions.checkNotNull(pBMCKAccessor);
    Preconditions.checkNotNull(pIsTargetStatePredicate);
    Preconditions.checkNotNull(pShutdownNotifier);
    cfa = pCFA;
    logger = pLogger;
    algorithm = pAlgorithm;
    cpa = pCPA;
    invariantGenerator = pInvariantGenerator;
    stats = pStats;
    reachedSetFactory = pReachedSetFactory;
    isProgramConcurrent = pIsProgramConcurrent;
    havocLoopTerminationConditionVariablesOnly = pHavocLoopTerminationConditionVariablesOnly;
    bmcKAccessor = pBMCKAccessor;
    isTargetState = pIsTargetStatePredicate;
    List<CFAEdge> incomingEdges = null;
    ReachedSet reachedSet = null;
    Loop loop = null;
    if (!cfa.getLoopStructure().isPresent()) {
      logger.log(Level.WARNING, "Could not use induction for proving program safety, loop structure of program could not be determined.");
      trivialResult = false;
    } else {
      LoopStructure loops = cfa.getLoopStructure().get();

      // Induction is currently only possible if there is a single loop.
      // This check can be weakened in the future,
      // e.g. it is ok if there is only a single loop on each path.
      if (loops.getCount() > 1) {
        logger.log(Level.WARNING, "Could not use induction for proving program safety, program has too many loops");
        invariantGenerator.cancel();
        trivialResult = false;
      } else if (loops.getCount() == 0) {
        // induction is unnecessary, program has no loops
        invariantGenerator.cancel();
        trivialResult = true;
      } else {
        stats.inductionPreparation.start();

        loop = Iterables.getOnlyElement(loops.getAllLoops());
        // function edges do not count as incoming/outgoing edges
        incomingEdges = from(loop.getIncomingEdges()).filter(not(instanceOf(CFunctionReturnEdge.class))).toList();

        if (incomingEdges.size() > 1) {
          logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many incoming edges", incomingEdges);
          trivialResult = false;
        } else if (loop.getLoopHeads().size() > 1) {
          logger.log(Level.WARNING, "Could not use induction for proving program safety, loop has too many loop heads");
          trivialResult = false;
        } else {
          trivialResult = null;
          reachedSet = reachedSetFactory.create();
          if (!isProgramConcurrent) {
            targetLocations = pTargetLocationProvider.tryGetAutomatonTargetLocations(cfa.getMainFunction());
          }
        }
        stats.inductionPreparation.stop();
      }
    }

    PredicateCPA stepCasePredicateCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
    stepCaseSolver = stepCasePredicateCPA.getSolver();
    stepCaseFMGR = stepCaseSolver.getFormulaManager();
    stepCaseBFMGR = stepCaseFMGR.getBooleanFormulaManager();
    stepCasePFMGR = stepCasePredicateCPA.getPathFormulaManager();
    loopHeadInvariants = stepCaseBFMGR.makeBoolean(true);

    invariantsReachedSet = reachedSetFactory.create();
    this.reachedSet = reachedSet;
    this.loop = loop;
  }

  public Collection<CandidateInvariant> getKnownLoopHeadInvariants() {
    return knownLoopHeadInvariants;
  }

  public ImmutableSet<CandidateInvariant> setPotentialLoopHeadInvariants(ImmutableSet<CandidateInvariant> pPotentialLoopHeadInvariants) {
    synchronized (this) {
      return this.potentialLoopHeadInvariants = from(pPotentialLoopHeadInvariants).filter(not(in(knownLoopHeadInvariants))).toSet();
    }
  }

  private ImmutableSet<CandidateInvariant> getPotentialLoopHeadInvariants() {
    synchronized (this) {
      return from(this.potentialLoopHeadInvariants).filter(not(in(knownLoopHeadInvariants))).toSet();
    }
  }

  /**
   * Checks if the result of the k-induction check has been determined to
   * be trivial by the constructor.
   *
   * @return {@code true} if the constructor was able to determine a constant
   * result for the k-induction check, {@code false} otherwise.
   */
  boolean isTrivial() {
    return this.trivialResult != null;
  }

  /**
   * If available, gets the constant result of the k-induction check as
   * determined by the constructor. Do not call this function if there is no
   * such trivial constant result. This can be checked by calling
   * {@link isTrivial}.
   *
   * @return the trivial constant result of the k-induction check.
   */
  private boolean getTrivialResult() {
    Preconditions.checkState(isTrivial(), "The proof is non-trivial.");
    return trivialResult;
  }

  /**
   * Gets the current reached set describing the loop iterations unrolled for
   * the inductive step. The reached set is only available if no trivial
   * constant result for the k-induction check was determined by the
   * constructor, as can be checked by calling {@link isTrivial}.
   *
   * @return the current reached set describing the loop iterations unrolled
   * for the inductive step.
   */
  private ReachedSet getCurrentReachedSet() {
    Preconditions.checkState(!isTrivial(), "No reached set created, because the proof is trivial.");
    assert reachedSet != null;
    return reachedSet;
  }

  /**
   * Gets the single loop of the program. This loop is only available if no
   * trivial constant result for the k-induction check was determined by the
   * constructor, as can be checked by calling {@link isTrivial}.
   *
   * @return the single loop of the program.
   */
  Loop getLoop() {
    Preconditions.checkState(!isTrivial(), "No loop computed, because the proof is trivial.");
    assert loop != null;
    return loop;
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
  private ProverEnvironment getProver() throws CPAException, InterruptedException {
    UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
    if (currentInvariantsReachedSet != invariantsReachedSet || !isProverInitialized()) {
      CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
      invariantsReachedSet = currentInvariantsReachedSet;
      // get global invariants
      BooleanFormula invariants = getCurrentLoopHeadInvariants();
      injectInvariants(currentInvariantsReachedSet, loopHead);
      if (isProverInitialized()) {
        pop();
      } else {
        prover = stepCaseSolver.newProverEnvironmentWithModelGeneration();
      }
      invariants = stepCaseFMGR.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
      push(invariants);
    }
    assert isProverInitialized();
    return prover;
  }

  public UnmodifiableReachedSet getCurrentInvariantsReachedSet() {
    if (!invariantGenerationRunning) {
      return invariantsReachedSet;
    }
    try {
      return invariantGenerator.get();
    } catch (CPAException e) {
      logger.log(Level.FINE, "Invariant generation encountered an exception.", e);
      invariantGenerationRunning = false;
      return invariantsReachedSet;
    } catch (InterruptedException e) {
      logger.log(Level.FINE, "Invariant generation has terminated:", e);
      invariantGenerationRunning = false;
      return invariantsReachedSet;
    }
  }

  /**
   * Gets the most current invariants generated by the invariant generator.
   *
   * @return the most current invariants generated by the invariant generator.
   * @throws InterruptedException
   * @throws CPATransferException
   */
  private BooleanFormula getCurrentLoopHeadInvariants() throws CPATransferException, InterruptedException {
    if (!stepCaseBFMGR.isFalse(loopHeadInvariants) && invariantGenerationRunning) {
      CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
      loopHeadInvariants = getCurrentLocationInvariants(loopHead);
    }
    return loopHeadInvariants;
  }

  public BooleanFormula getCurrentLocationInvariants(CFANode pLocation, FormulaManagerView pFMGR) throws CPATransferException, InterruptedException {
    if (invariantGenerationRunning) {
      UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
      if (currentInvariantsReachedSet != invariantsReachedSet) {
        return extractInvariantsAt(currentInvariantsReachedSet, pLocation, pFMGR);
      }
    }
    return extractInvariantsAt(invariantsReachedSet, pLocation, pFMGR);
  }

  private BooleanFormula getCurrentLocationInvariants(CFANode pLocation) throws CPATransferException, InterruptedException {
    if (invariantGenerationRunning) {
      UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
      if (currentInvariantsReachedSet != invariantsReachedSet) {
        return extractInvariantsAt(currentInvariantsReachedSet, pLocation);
      }
    }
    return extractInvariantsAt(invariantsReachedSet, pLocation);
  }

  /**
   * Attempts to inject the generated invariants into the bounded analysis
   * CPAs to improve their performance.
   *
   * Currently, this is only supported for the InvariantsCPA. If the
   * InvariantsCPA is not activated for both the bounded analysis as well as
   * the invariant generation, this function does nothing.
   *
   * @param pReachedSet the invariant generation reached set.
   * @param pLocation the location for which to extract and re-inject the
   * invariants.
   */
  private void injectInvariants(UnmodifiableReachedSet pReachedSet, CFANode pLocation) {
    InvariantsCPA invariantsCPA = CPAs.retrieveCPA(cpa, InvariantsCPA.class);
    if (invariantsCPA == null) {
      return;
    }
    InvariantsState invariant = null;
    for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
      InvariantsState disjunctivePart = AbstractStates.extractStateByType(locState, InvariantsState.class);
      if (disjunctivePart != null) {
        if (invariant == null) {
          invariant = disjunctivePart;
        } else {
          invariant = invariant.join(disjunctivePart, InvariantsPrecision.getEmptyPrecision());
        }
      } else {
        return;
      }
    }
    if (invariant != null) {
      invariantsCPA.injectInvariant(pLocation, invariant.asFormula());
    }
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
   * Extracts the generated invariants for the given location from the
   * given reached set produced by the invariant generator.
   *
   * @param pReachedSet the reached set produced by the invariant generator.
   * @param pLocation the location to extract the invariants for.
   *
   * @return the extracted invariants as a boolean formula.
   * @throws InterruptedException
   * @throws CPATransferException
   */
  private BooleanFormula extractInvariantsAt(UnmodifiableReachedSet pReachedSet, CFANode pLocation) throws CPATransferException, InterruptedException {

    BooleanFormula result = stepCaseBFMGR.makeBoolean(true);

    if (pLocation == loop.getLoopHeads().iterator().next()) {
      for (CandidateInvariant ci : getKnownLoopHeadInvariants()) {
        result = stepCaseBFMGR.and(result, ci.getCandidate(stepCaseFMGR, stepCasePFMGR));
      }
    }

    return stepCaseBFMGR.and(extractInvariantsAt(pReachedSet, pLocation, stepCaseFMGR), result);
  }

  /**
   * Extracts the generated invariants for the given location from the
   * given reached set produced by the invariant generator.
   *
   * @param pReachedSet the reached set produced by the invariant generator.
   * @param pLocation the location to extract the invariants for.
   *
   * @return the extracted invariants as a boolean formula.
   * @throws InterruptedException
   * @throws CPATransferException
   */
  private BooleanFormula extractInvariantsAt(UnmodifiableReachedSet pReachedSet, CFANode pLocation, FormulaManagerView pFMGR) throws CPATransferException, InterruptedException {

    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();

    if (invariantGenerationRunning && pReachedSet.isEmpty()) {
      return bfmgr.makeBoolean(true); // no invariants available
    }

    Set<CFANode> targetLocations = getCurrentPotentialTargetLocations();
    // Check if the invariant generation was able to prove correctness for the program
    if (targetLocations != null && AbstractStates.filterLocations(pReachedSet, targetLocations).isEmpty()) {
      logger.log(Level.INFO, "Invariant generation found no target states.");
      invariantGenerator.cancel();
      return bfmgr.makeBoolean(false);
    }

    BooleanFormula invariant = bfmgr.makeBoolean(false);

    for (AbstractState locState : AbstractStates.filterLocation(pReachedSet, pLocation)) {
      BooleanFormula f = AbstractStates.extractReportedFormulas(pFMGR, locState);
      logger.log(Level.ALL, "Invariant:", f);

      invariant = bfmgr.or(invariant, f);
    }
    return invariant;
  }

  public ImmutableSet<CFANode> getCurrentPotentialTargetLocations() {
    synchronized (this) {
      return this.targetLocations;
    }
  }

  private void setCurrentPotentialTargetLocations(ImmutableSet<CFANode> pTargetLocations) {
    synchronized (this) {
      this.targetLocationsChanged = pTargetLocations.equals(this.targetLocations);
      this.targetLocations = pTargetLocations;
    }
  }

  public boolean haveCurrentPotentialTargetLocationsChanged() {
    synchronized (this) {
      return this.targetLocationsChanged;
    }
  }

  /**
   * Attempts to perform the inductive check.
   *
   * @return <code>true</code> if k-induction successfully proved the
   * correctness, <code>false</code> if the attempt was inconclusive.
   *
   * @throws CPAException if the bounded analysis constructing the step case
   * encountered an exception.
   * @throws InterruptedException if the bounded analysis constructing the
   * step case was interrupted.
   */
  public final boolean check() throws CPAException, InterruptedException {

    if (!getPotentialLoopHeadInvariants().isEmpty()) {
      return check1();
    }

    // Early return if there is a trivial result for the inductive approach
    if (isTrivial()) {
      return getTrivialResult();
    }

    // Early return if the invariant generation proved the program correct
    if (stepCaseBFMGR.isFalse(getCurrentLoopHeadInvariants())) {
      return true;
    }

    stats.inductionPreparation.start();

    // Proving program safety with induction consists of two parts:
    // 1) Prove all paths safe that go only one iteration through the loop.
    //    This is part of the classic bounded model checking done above,
    //    so we don't care about this here.
    // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.

    // Create initial reached set:
    // Run algorithm in order to create formula (A & B)
    logger.log(Level.INFO, "Running algorithm to create induction hypothesis");

    int k = bmcKAccessor.get();
    LoopstackCPA stepCaseLoopstackCPA = CPAs.retrieveCPA(cpa, LoopstackCPA.class);

    BooleanFormula safePredecessors;
    ReachedSet reached = getCurrentReachedSet();

    // Initialize the reached set if necessary
    ensureReachedSetInitialized(reached);

    // Create the formula asserting the safety for k consecutive predecessors
    if (previousFormula != null && this.previousK == k) {
      safePredecessors = stepCaseBFMGR.not(previousFormula);
    } else {
      final Iterable<AbstractState> predecessorTargetStates;
      if (k < 1) {
        predecessorTargetStates = Collections.emptySet();
      } else {
        stepCaseLoopstackCPA.setMaxLoopIterations(k);

        BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);
        predecessorTargetStates = from(reached).filter(isTargetState);
      }
      safePredecessors = stepCaseBFMGR.not(BMCHelper.createFormulaFor(predecessorTargetStates, stepCaseBFMGR));
    }
    stepCaseLoopstackCPA.setMaxLoopIterations(k + 1);

    Iterable<AbstractState> loopHeadStates =
        AbstractStates.filterLocations(reached, loop.getLoopHeads()).filter(new Predicate<AbstractState>() {

          @Override
          public boolean apply(AbstractState pArg0) {
            if (pArg0 == null) {
              return false;
            }
            LoopstackState ls = AbstractStates.extractStateByType(pArg0, LoopstackState.class);
            return ls != null && ls.getIteration() <= 1;
          }});

    BooleanFormula loopHeadInv = stepCaseBFMGR.and(from(BMCHelper.assertAt(loopHeadStates, getCurrentLoopHeadInvariants(), stepCaseFMGR)).toList());

    // Create the formula asserting the faultiness of the successor
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);
    Set<AbstractState> targetStates = from(reached).filter(isTargetState).toSet();
    BooleanFormula unsafeSuccessor = BMCHelper.createFormulaFor(from(targetStates), stepCaseBFMGR);
    this.previousFormula = unsafeSuccessor;

    ProverEnvironment prover = getProver();

    this.previousK = k + 1;

    ImmutableSet<CFANode> newTargetLocations = from(targetStates).transform(AbstractStates.EXTRACT_LOCATION).toSet();
    setCurrentPotentialTargetLocations(newTargetLocations);

    stats.inductionPreparation.stop();

    logger.log(Level.INFO, "Starting induction check...");

    stats.inductionCheck.start();

    push(safePredecessors); // k consecutive iterations are SAFE
    push(loopHeadInv); // loop invariant holds for predecessors
    push(unsafeSuccessor); // Check if the successor is UNSAFE
    boolean sound = prover.isUnsat();

    if (!sound && logger.wouldBeLogged(Level.ALL)) {
      logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
    }

    UnmodifiableReachedSet localInvariantsReachedSet = invariantsReachedSet;
    UnmodifiableReachedSet currentInvariantsReachedSet = getCurrentInvariantsReachedSet();

    while (!sound && currentInvariantsReachedSet != localInvariantsReachedSet) {
      localInvariantsReachedSet = currentInvariantsReachedSet;
      BooleanFormula invariants = getCurrentLoopHeadInvariants();
      invariants = stepCaseFMGR.instantiate(invariants, SSAMap.emptySSAMap().withDefault(1));
      push(invariants);
      sound = prover.isUnsat();

      if (!sound && logger.wouldBeLogged(Level.ALL)) {
        logger.log(Level.ALL, "Model returned for induction check:", prover.getModel());
      }

      pop();
      currentInvariantsReachedSet = getCurrentInvariantsReachedSet();
    }

    pop(); // pop UNSAFE successor
    pop(); // pop loop invariant assertion for predecessors
    pop(); // pop SAFE predecessors

    stats.inductionCheck.stop();

    logger.log(Level.FINER, "Soundness after induction check:", sound);

    return sound;
  }

  /**
   * Attempts to perform the inductive check.
   *
   * @return <code>true</code> if k-induction successfully proved the
   * correctness, <code>false</code> if the attempt was inconclusive.
   *
   * @throws CPAException if the bounded analysis constructing the step case
   * encountered an exception.
   * @throws InterruptedException if the bounded analysis constructing the
   * step case was interrupted.
   */
  public final boolean check1() throws CPAException, InterruptedException {
    // Early return if there is a trivial result for the inductive approach
    if (isTrivial()) {
      return getTrivialResult();
    }

    // Early return if the invariant generation proved the program correct
    if (stepCaseBFMGR.isFalse(getCurrentLoopHeadInvariants())) {
      return true;
    }

    stats.inductionPreparation.start();

    // Proving program safety with induction consists of two parts:
    // 1) Prove all paths safe that go only one iteration through the loop.
    //    This is part of the classic bounded model checking done above,
    //    so we don't care about this here.
    // 2) Assume that one loop iteration is safe and prove that the next one is safe, too.

    // Create initial reached set:
    // Run algorithm in order to create formula (A & B)
    logger.log(Level.INFO, "Running algorithm to create induction hypothesis");

    int k = bmcKAccessor.get();
    LoopstackCPA stepCaseLoopstackCPA = CPAs.retrieveCPA(cpa, LoopstackCPA.class);

    ReachedSet reached = getCurrentReachedSet();

    // Initialize the reached set if necessary
    ensureReachedSetInitialized(reached);

    Map<CandidateInvariant, BooleanFormula> assumptionsAtState = new HashMap<>();

    Iterable<AbstractState> loopHeadStates = AbstractStates.filterLocations(reached, loop.getLoopHeads());

    BooleanFormula combinedPotentialLoopHeadInvariantAssertion = stepCaseBFMGR.makeBoolean(true);
    ImmutableSet<CandidateInvariant> potentialLoopHeadInvariants = getPotentialLoopHeadInvariants();
    for (CandidateInvariant potentialLoopHeadInvariant : potentialLoopHeadInvariants) {
      BooleanFormula potentialLoopHeadInvariantAssertion = stepCaseBFMGR.and(from(BMCHelper.assertAt(loopHeadStates, potentialLoopHeadInvariant.getCandidate(stepCaseFMGR, stepCasePFMGR), stepCaseFMGR)).toList());
      combinedPotentialLoopHeadInvariantAssertion = stepCaseBFMGR.and(combinedPotentialLoopHeadInvariantAssertion, potentialLoopHeadInvariantAssertion);
      assumptionsAtState.put(potentialLoopHeadInvariant, potentialLoopHeadInvariantAssertion);
    }

    stepCaseLoopstackCPA.setMaxLoopIterations(k + 1);

    // Create the formula asserting the faultiness of the successor
    BMCHelper.unroll(logger, reached, reachedSetInitializer, algorithm, cpa);

    ProverEnvironment prover = getProver();



    loopHeadStates = AbstractStates.filterLocations(reached, loop.getLoopHeads());
    BooleanFormula combinedPotentialLoopHeadInvariantContradiction = stepCaseBFMGR.makeBoolean(false);
    for (CandidateInvariant potentialLoopHeadInvariant : potentialLoopHeadInvariants) {

      BooleanFormula potentialLoopHeadInvariantAssertion = assumptionsAtState.get(potentialLoopHeadInvariant);
      BooleanFormula potentialLoopHeadInvariantContradiction = stepCaseBFMGR.not(stepCaseBFMGR.and(from(BMCHelper.assertAt(loopHeadStates, potentialLoopHeadInvariant.getCandidate(stepCaseFMGR, stepCasePFMGR), stepCaseFMGR)).toList()));
      combinedPotentialLoopHeadInvariantContradiction = stepCaseBFMGR.or(combinedPotentialLoopHeadInvariantContradiction, potentialLoopHeadInvariantContradiction);

      // Try to prove the loop head invariant itself
      push(potentialLoopHeadInvariantAssertion);
      push(potentialLoopHeadInvariantContradiction);
      stats.inductionPreparation.stop();
      stats.inductionCheck.start();
      boolean isInvariant = prover.isUnsat();
      stats.inductionCheck.stop();
      if (isInvariant) {
        knownLoopHeadInvariants.add(potentialLoopHeadInvariant);
        if (invariantGenerator instanceof CPAInvariantGenerator) {
          CPAInvariantGenerator invGen = (CPAInvariantGenerator) invariantGenerator;
          InvariantsCPA invariantsCPA = CPAs.retrieveCPA(invGen.getCPAs(), InvariantsCPA.class);
          Optional<AssumeEdge> assumption = potentialLoopHeadInvariant.getAssumeEdge();
          if (invariantsCPA != null && assumption.isPresent()) {
            invariantsCPA.injectInvariant(loop.getLoopHeads().iterator().next(), assumption.get());
          }
        }
      }
      stats.inductionPreparation.start();
      // Pop loop invariant contradiction
      pop();
      // Pop loop invariant predecessor safety assertion
      pop();
    }
    stats.inductionPreparation.stop();

    this.previousK = k + 1;

    return getPotentialLoopHeadInvariants().isEmpty();
  }

  private void ensureReachedSetInitialized(ReachedSet pReachedSet) throws InterruptedException, CPAException {
    if (pReachedSet.size() > 1) {
      return;
    }
    CFANode loopHead = Iterables.getOnlyElement(getLoop().getLoopHeads());
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
        newPAS.setPathFormula(stepCasePFMGR.makeNewPathFormula(pathFormula, ssaMapBuilder.build()));

        pReachedSet.add(newLoopHeadState, cpa.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition()));
      }
    } else {
      Precision precision = cpa.getInitialPrecision(loopHead, StateSpacePartition.getDefaultPartition());
      pReachedSet.add(cpa.getInitialState(loopHead, StateSpacePartition.getDefaultPartition()), precision);
    }
  }

  private Collection<String> getTerminationConditionVariables(Loop pLoop) throws CPATransferException, InterruptedException {
    Collection<String> result = new HashSet<>();
    result.add(CFASingleLoopTransformation.PROGRAM_COUNTER_VAR_NAME);
    CFANode loopHead = Iterables.getOnlyElement(pLoop.getLoopHeads());
    Set<CFANode> visited = new HashSet<>();
    Queue<CFANode> waitlist = new ArrayDeque<>();
    waitlist.offer(loopHead);
    visited.add(loopHead);
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
          PathFormula formula = stepCasePFMGR.makeFormulaForPath(Collections.singletonList(leavingEdge));
          result.addAll(stepCaseFMGR.extractVariableNames(stepCaseFMGR.uninstantiate(formula.getFormula())));
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

}
