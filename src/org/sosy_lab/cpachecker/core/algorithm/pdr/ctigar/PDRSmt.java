/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pdr.ctigar;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.ForwardTransition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.InterpolatingProverEnvironment;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext.ProverOptions;
import org.sosy_lab.java_smt.api.SolverException;

/**
 * The central class with PDR-related methods that require SMT-solving for queries that involve
 * relative inductiveness. Takes care of predicate abstraction.
 */
public class PDRSmt {

  private static enum ReductionMode {
    CONSECUTION,
    LIFTING
  }

  private final FrameSet frameSet;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final PathFormulaManager pfmgr;
  private final PredicatePrecisionManager abstractionManager;
  private final TransitionSystem transition;
  private final PDRSatStatistics stats;
  private final LogManager logger;
  private final PDROptions options;
  private final ForwardTransition forward;
  private final ShutdownNotifier shutdownNotifier;

  /**
   * Creates a new PDRSmt instance.
   *
   * @param pFrameSet The frames relative to which the induction queries are formed.
   * @param pSolver The solver that is used in all queries.
   * @param pFmgr The formula manager used for instantiating formulas.
   * @param pPfmgr The path formula manager used for creating variables.
   * @param pAbstractionManager The component that handles predicate abstraction.
   * @param pTransition The transition system that defines the transition formula.
   * @param pCompStats The statistics delegator that this class should be registered at. It takes
   *     care of printing PDRSmt statistics.
   * @param pLogger The logging component.
   * @param pForward The stepwise transition that computes the blocks.
   * @param pOptions The container for all relevant configuration options.
   * @param pShutdown The notifier that can interrupt the methods if they take too long.
   */
  public PDRSmt(
      FrameSet pFrameSet,
      Solver pSolver,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      PredicatePrecisionManager pAbstractionManager,
      TransitionSystem pTransition,
      StatisticsDelegator pCompStats,
      LogManager pLogger,
      ForwardTransition pForward,
      PDROptions pOptions,
      ShutdownNotifier pShutdown) {
    this.stats = new PDRSatStatistics();
    Objects.requireNonNull(pCompStats).register(stats);

    this.frameSet = Objects.requireNonNull(pFrameSet);
    this.solver = Objects.requireNonNull(pSolver);
    this.fmgr = Objects.requireNonNull(pFmgr);
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.pfmgr = Objects.requireNonNull(pPfmgr);
    this.abstractionManager = Objects.requireNonNull(pAbstractionManager);
    this.transition = Objects.requireNonNull(pTransition);
    this.logger = Objects.requireNonNull(pLogger);
    this.forward = Objects.requireNonNull(pForward);
    this.options = Objects.requireNonNull(pOptions);
    this.shutdownNotifier = pShutdown;
  }

  /**
   * Tries to find direct error predecessor states in the current frontier frame.
   *
   * <p>In short : Gets states satisfying [F_frontierLevel &amp; T &amp; not(SafetyProperty)'].
   *
   * @return An Optional containing a formula describing direct error predecessor states if they
   *     exist. An empty Optional is no such predecessors exists.
   */
  public Optional<ConsecutionResult> getCTIinFrontierFrame()
      throws SolverException, InterruptedException, CPAException {
    stats.consecutionTimer.start();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      // Push F_frontierLevel & T & not(P)'
      for (BooleanFormula frameClause : frameSet.getStates(frameSet.getFrontierLevel())) {
        prover.push(frameClause);
      }
      prover.push(transition.getTransitionRelationFormula());
      BooleanFormula notSafetyPrimed =
          PDRUtils.asPrimed(bfmgr.not(transition.getSafetyProperty()), fmgr, transition);
      prover.push(notSafetyPrimed);

      if (PDRUtils.isUnsat(prover, stats.pureConsecutionSatTimer)) {
        stats.numberSuccessfulConsecutions++;
        return Optional.empty();
      }

      stats.numberFailedConsecutions++;
      StatesWithLocation directErrorPredecessor = getSatisfyingState(prover.getModel());
      BooleanFormula concreteState = directErrorPredecessor.getConcrete();

      // Get reached target-location
      CFANode correspondingTargetLocation =
          PDRUtils.getDirectBlockToTargetLocation(
                  directErrorPredecessor, transition, forward, fmgr, solver)
              .orElseThrow(IllegalArgumentException::new)
              .getSuccessorLocation();

      BooleanFormula liftedAbstractState =
          abstractLift(
              concreteState,
              notSafetyPrimed,
              directErrorPredecessor.getLocation(),
              correspondingTargetLocation);
      assert isValidFrontierCTI(liftedAbstractState);
      return Optional.of(
          new ConsecutionResult(
              false,
              new StatesWithLocation(
                  liftedAbstractState,
                  directErrorPredecessor.getConcrete(),
                  directErrorPredecessor.getLocation())));
    } finally {
      stats.consecutionTimer.stop();
    }
  }

  // Simple double check to assert that the formula is indeed a frontier-CTI.
  private boolean isValidFrontierCTI(BooleanFormula pLiftedAbstractedState)
      throws SolverException, InterruptedException {
    try (ProverEnvironment prover = solver.newProverEnvironment()) {

      for (BooleanFormula frameClause : frameSet.getStates(frameSet.getFrontierLevel())) {
        prover.push(frameClause);
      }
      prover.push(pLiftedAbstractedState);
      prover.push(transition.getTransitionRelationFormula());
      prover.push(PDRUtils.asPrimed(bfmgr.not(transition.getSafetyProperty()), fmgr, transition));
      return !prover.isUnsat();
    }
  }

  /**
   * Checks if the given formula describes initial states which are defined by the transition
   * system.
   *
   * @param pStates The formula describing a set of states.
   * @return True if the provided states are initial, false otherwise.
   */
  public boolean isInitial(BooleanFormula pStates) throws SolverException, InterruptedException {

    // States are initial iff : SAT [InitialCondition & states]
    BooleanFormula initialCondAndStates =
        bfmgr.and(PDRUtils.asUnprimed(transition.getInitialCondition(), fmgr, transition), pStates);
    return !solver.isUnsat(initialCondAndStates);
  }

  // Wrapper method to capture the wildcard type of InterpolatingProverEnvironment.
  private BooleanFormula abstractLift(
      BooleanFormula pConcretePredecessor,
      BooleanFormula pSuccessors,
      CFANode pPredLoc,
      CFANode pSuccLoc)
      throws InterruptedException, SolverException, CPAException {

    stats.liftingTimer.start();
    try (InterpolatingProverEnvironment<?> concreteProver =
            solver.newProverEnvironmentWithInterpolation();
        ProverEnvironment abstractProver = solver.newProverEnvironment()) {
      return abstractLift(
          pConcretePredecessor, pSuccessors, pPredLoc, pSuccLoc, concreteProver, abstractProver);
    } finally {
      stats.liftingTimer.stop();
    }
  }

  /**
   * [pConcreteState &amp; T &amp: not(pSuccessorStates)'] is unsat. Create abstraction <i>abstr</i>
   * of pConcreteStates and try [abstr &amp; T &amp; not(pSuccessorStates)']. <br>
   * If it is sat, there is a spurious transition and the domain is refined with an interpolant.
   * After the abstract query is unsat (with or without refinement), drop unused literals of
   * <i>abstr</i> and return this lifted abstract state.
   */
  private <T> BooleanFormula abstractLift(
      BooleanFormula pConcreteState,
      BooleanFormula pSuccessorStates,
      CFANode pPredLoc,
      CFANode pSuccLoc,
      InterpolatingProverEnvironment<T> pConcrProver,
      ProverEnvironment pAbstrProver)
      throws InterruptedException, SolverException, CPAException {
    BooleanFormula abstractState = abstractionManager.computeAbstraction(pConcreteState);

    // Push unsatisfiable formula (state & T & not(successor)'). Push state last,
    // so it can be popped and replaced with a refined version later if necessary.
    pAbstrProver.push(transition.getTransitionRelationFormula());
    pConcrProver.push(transition.getTransitionRelationFormula());
    pAbstrProver.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));
    pConcrProver.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));
    T idForInterpolation = pConcrProver.push(pConcreteState);
    pAbstrProver.push(abstractState);

    boolean isConcreteQueryUnsat = PDRUtils.isUnsat(pConcrProver, stats.pureLiftingSatTimer);

    /*
     * Concrete query could be sat if pSuccessorStates contain non-deterministically assigned
     * variables or if pConcreteState has more than one successor. If the query is still sat
     * after nondet variables have been dropped, lifting cannot work. In that case, just return the abstract state
     * as it is, potentially permitting spurious transitions.
     */
    if (!isConcreteQueryUnsat) {
      idForInterpolation =
          removeNondetVariables(pConcreteState, pSuccessorStates, pPredLoc, pSuccLoc, pConcrProver);
    }
    if (!PDRUtils.isUnsat(pConcrProver, stats.pureLiftingSatTimer)) {
      logger.log(
          Level.ALL,
          "Could not lift the state ",
          pConcreteState,
          " based on its successor-states ",
          pSuccessorStates,
          ". Expected cause: The state has successors other than those in the transition relation.");
      stats.numberImpossibleLifts++;
      return abstractState;
    }

    if (!PDRUtils.isUnsat(pAbstrProver, stats.pureLiftingSatTimer)) {

      // Abstraction was too broad => Refine.
      stats.numberFailedLifts++;
      BooleanFormula interpolant =
          pConcrProver.getInterpolant(Collections.singletonList(idForInterpolation));
      assert PDRUtils.isUnprimed(interpolant, fmgr, transition);
      abstractState = abstractionManager.refineAndComputeAbstraction(pConcreteState, interpolant);

      // Update abstraction on prover.
      pAbstrProver.pop();
      pAbstrProver.push(abstractState);
      assert pAbstrProver.isUnsat();
    } else {
      stats.numberSuccessfulLifts++;
    }

    // Get used conjunctive parts from query with abstraction. Query must be unsat at this point.
    BooleanFormula reduced = reduceByUnsatCore(abstractState, pAbstrProver, ReductionMode.LIFTING);
    reduced = dropIrrelevantConjunctiveParts(reduced, pAbstrProver, ReductionMode.LIFTING);
    assert isValidLifting(reduced, pSuccessorStates);
    return reduced;
  }

  // Simple double check to assert that the lifting procedure was sound.
  private boolean isValidLifting(BooleanFormula pLiftedStates, BooleanFormula pSuccessorStates)
      throws SolverException, InterruptedException {
    if (isInitial(pLiftedStates)) {
      return false;
    }

    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      prover.push(pLiftedStates);
      prover.push(transition.getTransitionRelationFormula());
      prover.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));
      return prover.isUnsat();
    }
  }

  /**
   * Drops irrelevant literals in pFormula based on an unsat-core. Assumes that the prover already
   * contains the unsatisfiable query and the formula at the top of the prover stack is pFormula.
   * The new formula will replace the original one on the prover.
   */
  private BooleanFormula reduceByUnsatCore(
      BooleanFormula pFormula, ProverEnvironment pProver, ReductionMode pMode)
      throws SolverException, InterruptedException {

    if (pMode == ReductionMode.LIFTING && !options.shouldDropLiteralsAfterLiftingWithUnsatCore()) {
      return pFormula;
    }

    pProver.pop(); // Remove old (unreduced) formula.
    Set<BooleanFormula> conjuncts = bfmgr.toConjunctionArgs(pFormula, true);

    if (pMode == ReductionMode.CONSECUTION) {
      stats.partsBeforeCoreGen += conjuncts.size();
    } else {
      stats.partsBeforeCoreLift += conjuncts.size();
    }

    /*
     *  To determine irrelevant conjuncts, pop pFormula from prover and push equivalences
     *  A_i <=> conjunct_i for all conjuncts of pFormula. A_i is an arbitrary unique identifier
     *  for the i'th conjunct and acts as activation literal.
     *  The unsat core over the set {A_1, A_2,...}
     *  of assumptions yields the corresponding RELEVANT conjuncts.
     */
    Map<BooleanFormula, BooleanFormula> actToConjuncts = new HashMap<>();

    // Push equivalences A_i <=> conjunct_i
    for (BooleanFormula conjunct : conjuncts) {

      // Activation literals must have a unique name! Conjuncts are distinct from another, so use
      // toString as unique name.
      BooleanFormula act = bfmgr.makeVariable(conjunct.toString());
      BooleanFormula equiv = bfmgr.equivalence(act, conjunct);
      actToConjuncts.put(act, conjunct);
      pProver.push(equiv);
    }

    // Get core
    Optional<List<BooleanFormula>> unsatCore;
    Timer timer =
        pMode == ReductionMode.CONSECUTION
            ? stats.pureConsecutionSatTimer
            : stats.pureLiftingSatTimer;
    timer.start();
    try {
      unsatCore = pProver.unsatCoreOverAssumptions(actToConjuncts.keySet());
    } finally {
      timer.stop();
    }

    shutdownNotifier.shutdownIfNecessary();
    assert unsatCore.isPresent(); // Must be unsat
    Set<BooleanFormula> relevantConjuncts =
        actToConjuncts
            .entrySet()
            .stream()
            .filter(entry -> unsatCore.get().contains(entry.getKey()))
            .map(entry -> entry.getValue())
            .collect(Collectors.toSet());

    BooleanFormula reduced = bfmgr.and(relevantConjuncts);

    int dropped = conjuncts.size() - relevantConjuncts.size();
    if (pMode == ReductionMode.CONSECUTION) {
      stats.droppedAfterCoreGen += dropped;
    } else {
      stats.droppedAfterCoreLift += dropped;
    }

    // If reduced state is an initial state, pc was dropped. Re-add it.
    if (isInitial(PDRUtils.asUnprimed(reduced, fmgr, transition))) {
      reduced = bfmgr.and(getPcLiteral(pFormula), reduced);
      if (pMode == ReductionMode.CONSECUTION) {
        stats.droppedAfterCoreGen--;
      } else {
        stats.droppedAfterCoreLift--;
      }

    }

    assert !isInitial(PDRUtils.asUnprimed(reduced, fmgr, transition));

    // Rebuild prover : remove equivalences and push new reduced formula.
    for (int i = 0; i < conjuncts.size(); ++i) {
      pProver.pop();
    }
    pProver.push(reduced);
    return reduced;
  }

  /** Extracts the program-counter literal from a formula. */
  private BooleanFormula getPcLiteral(BooleanFormula pFormula) throws InterruptedException {
    boolean isPrimed = PDRUtils.isPrimed(pFormula, fmgr, transition);
    BooleanFormula pcPart =
        fmgr.filterLiterals(
            fmgr.uninstantiate(pFormula),
            literal ->
                fmgr.extractVariableNames(literal).contains(transition.programCounterName()));
    return isPrimed
        ? PDRUtils.asPrimed(pcPart, fmgr, transition)
        : PDRUtils.asUnprimed(pcPart, fmgr, transition);
  }

  /**
   * Drops conjunctive parts in pFormula one by one if the prover with the updated pFormula still
   * returns unsat without them.
   *
   * @param pFormula The formula to be reduced.
   * @return The version of pFormula without irrelevant conjunctive parts.
   */
  private BooleanFormula dropIrrelevantConjunctiveParts(
      BooleanFormula pFormula, ProverEnvironment pProver, ReductionMode pMode)
      throws SolverException, InterruptedException {

    Set<BooleanFormula> remainingConjuncts = bfmgr.toConjunctionArgs(pFormula, true);
    if (pMode == ReductionMode.CONSECUTION) {
      stats.partsBeforeManualGen += remainingConjuncts.size();
    } else {
      stats.partsBeforeManualLift += remainingConjuncts.size();
    }

    Iterator<BooleanFormula> conjIterator = remainingConjuncts.iterator();
    int numberOfDroppedConjuncts = 0;
    int numberOfAttempts = 0;

    while (numberOfAttempts < options.maxAttemptsAtDroppingLiterals()
        && numberOfDroppedConjuncts < options.maxLiteralsToDrop()
        && conjIterator.hasNext()) {
      shutdownNotifier.shutdownIfNecessary();
      numberOfAttempts++;
      BooleanFormula current = conjIterator.next();

      // Remove conjunct from formula
      Set<BooleanFormula> conjunctsWithoutCurrent =
          Sets.filter(remainingConjuncts, bf -> !bf.equals(current));
      BooleanFormula formulaWithoutCurrent =
          PDRUtils.asUnprimed(bfmgr.and(conjunctsWithoutCurrent), fmgr, transition);

      // If removal makes states initial, continue with next conjunct.
      if (isInitial(formulaWithoutCurrent)) {
        continue;
      }

      /*
       *  Prover contains old version(s) 'old' of pFormula.
       *  Consecution mode: not(old), old'.
       *  Lifting mode: old.
       *  Replace with reduced formula and check if still unsat.
       */
      if (pMode == ReductionMode.CONSECUTION) {
        pProver.pop();
        pProver.pop();
        pProver.push(bfmgr.not(formulaWithoutCurrent));
        pProver.push(PDRUtils.asPrimed(formulaWithoutCurrent, fmgr, transition));
      } else {
        pProver.pop();
        pProver.push(formulaWithoutCurrent);
      }

      Timer timer =
          pMode == ReductionMode.CONSECUTION
              ? stats.pureConsecutionSatTimer
              : stats.pureLiftingSatTimer;
      if (PDRUtils.isUnsat(pProver, timer)) {
        conjIterator.remove(); // Conjunct was irrelevant -> remove permanently

        if (pMode == ReductionMode.CONSECUTION) {
          stats.droppedAfterManualGen++;
        } else {
          stats.droppedAfterManualLift++;
        }
        numberOfDroppedConjuncts++;
      }
    }

    // All irrelevant conjuncts are removed at this point. The conjunction of the remaining ones
    // is the reduced formula.
    BooleanFormula result = bfmgr.and(remainingConjuncts);
    return result;
  }

  private Set<Formula> nondetVarsOfConnectingBlock(CFANode pPredLoc, CFANode pSuccLoc)
      throws CPAException, InterruptedException {

    FluentIterable<Block> connectingBlocks =
        forward.getBlocksFrom(pPredLoc).filter(b -> b.getSuccessorLocation().equals(pSuccLoc));
    /*
     * If there are multiple blocks between the same two locations, the additional blocks just represent
     * different branches. One of the blocks contains all branches in a single disjunctive formula.
     * Get that one.
     */
    Block block =
        connectingBlocks
            .toList()
            .stream()
            .max(
                new Comparator<Block>() {

                  @Override
                  public int compare(Block pArg0, Block pArg1) {
                    Set<BooleanFormula> d0 = bfmgr.toDisjunctionArgs(pArg0.getFormula(), true);
                    Set<BooleanFormula> d1 = bfmgr.toDisjunctionArgs(pArg1.getFormula(), true);
                    return d0.size() - d1.size();
                  }
                })
            .get();
    return block.getUnconstrainedNondeterministicVariables();
  }

  /**
   * Removes literals from pSuccessorStates that contain non-deterministically assigned variables
   * defined in the block from pPredLoc to pSuccLoc.
   *
   * <p>The prover is supposed to already contain the lifting query.
   */
  private <T> T removeNondetVariables(
      BooleanFormula pPred,
      BooleanFormula pSucc,
      CFANode pPredLoc,
      CFANode pSuccLoc,
      InterpolatingProverEnvironment<T> pConcrProver)
      throws InterruptedException, CPAException {

    // Pop pPred and pSucc first (in that order!).
    pConcrProver.pop();
    pConcrProver.pop();

    Set<Formula> nondetVars = nondetVarsOfConnectingBlock(pPredLoc, pSuccLoc);
    List<Formula> nondetsAsPrimed =
        fmgr.instantiate(nondetVars, transition.getPrimedContext().getSsa());
    Set<String> nondetNames =
        nondetsAsPrimed
            .stream()
            .flatMap(f -> fmgr.extractVariableNames(f).stream())
            .collect(Collectors.toSet());

    BooleanFormula succWithoutNondet =
        fmgr.filterLiterals(pSucc, lit -> !nondetNames.containsAll(fmgr.extractVariableNames(lit)));

    // Re-push adjusted not(succ)' and old pred (in that order!).
    pConcrProver.push(PDRUtils.asPrimed(bfmgr.not(succWithoutNondet), fmgr, transition));
    T idForInterpolation = pConcrProver.push(pPred);

    return idForInterpolation;
  }

  /**
   * Checks if the given abstract states in pStates are inductive relative to the frame with the
   * given level. In other words, checks if [F_pLevel &amp; not(abstr) &amp; T &amp; abstr'] is
   * unsat or not.
   *
   * <p>If it is, the returned ConsecutionResult contains an inductive formula consisting of a
   * subset of the used conjuncts in the original formula.<br>
   * If it is not, the returned ConsecutionResult contains a formula describing predecessors of the
   * original states. Those predecessors represent a reason why the original states are not
   * inductive.
   *
   * <p>The given states should be instantiated as unprimed.
   *
   * @param pLevel The frame level relative to which consecution should be checked.
   * @param pStates Contains the abstract states that should be checked for inductiveness.
   * @return A result encapsulating a single formula representing a set of states that are
   *     inductive, or predecessors of the original states.
   * @throws SolverException If the solver failed during consecution.
   * @throws InterruptedException If the process was interrupted.
   */
  public ConsecutionResult consecution(int pLevel, StatesWithLocation pStates)
      throws SolverException, InterruptedException, CPAException {
    stats.consecutionTimer.start();

    // Wrapper method to capture the wildcard type.
    try (InterpolatingProverEnvironment<?> concreteProver =
            solver.newProverEnvironmentWithInterpolation();
        ProverEnvironment abstractProver =
            solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {
      return consecution(pLevel, pStates, concreteProver, abstractProver);
    } finally {
      stats.consecutionTimer.stop();
    }
  }

  private <T> ConsecutionResult consecution(
      int pLevel,
      StatesWithLocation pStates,
      InterpolatingProverEnvironment<T> pConcreteProver,
      ProverEnvironment pAbstractProver)
      throws SolverException, InterruptedException, CPAException {

    BooleanFormula abstr = pStates.getAbstract();
    BooleanFormula concrete = pStates.getConcrete();
    List<T> idsForInterpolation = new ArrayList<>();

    // Push consecution query (F_pLevel & not(s) & T & s')
    for (BooleanFormula frameClause : frameSet.getStates(pLevel)) {
      pAbstractProver.push(frameClause);
      idsForInterpolation.add(pConcreteProver.push(frameClause));
    }
    pAbstractProver.push(transition.getTransitionRelationFormula());
    idsForInterpolation.add(pConcreteProver.push(transition.getTransitionRelationFormula()));
    pAbstractProver.push(bfmgr.not(abstr));
    idsForInterpolation.add(pConcreteProver.push(bfmgr.not(concrete)));
    pAbstractProver.push(PDRUtils.asPrimed(abstr, fmgr, transition));
    pConcreteProver.push(PDRUtils.asPrimed(concrete, fmgr, transition));

    boolean abstractConsecutionWorks =
        PDRUtils.isUnsat(pAbstractProver, stats.pureConsecutionSatTimer);
    boolean concreteConsecutionWorks =
        PDRUtils.isUnsat(pConcreteProver, stats.pureConsecutionSatTimer);

    if (!abstractConsecutionWorks) {
      if (concreteConsecutionWorks) {

        // Concrete states and abstract states disagree. Abstraction was too broad => Refine
        BooleanFormula interpolant = pConcreteProver.getInterpolant(idsForInterpolation);
        BooleanFormula forRefinement = bfmgr.not(interpolant);
        abstr = abstractionManager.refineAndComputeAbstraction(concrete, forRefinement);

        // Update not(s) and s'
        pAbstractProver.pop();
        pAbstractProver.pop();
        pAbstractProver.push(bfmgr.not(abstr));
        pAbstractProver.push(PDRUtils.asPrimed(abstr, fmgr, transition));

        abstractConsecutionWorks = PDRUtils.isUnsat(pAbstractProver, stats.pureConsecutionSatTimer);
        assert abstractConsecutionWorks;
        assert isConsecutionValid(pLevel, abstr);
      } else {

        // Concrete states and abstract states both agree that there is a predecessor.
        stats.numberFailedConsecutions++;
        StatesWithLocation predecessorState = getSatisfyingState(pConcreteProver.getModel());

        // No need to lift and abstract if states are initial.
        // PDR will terminate with counterexample anyway.
        if (isInitial(predecessorState.getConcrete())) {
          return new ConsecutionResult(false, predecessorState);
        }
        BooleanFormula abstractLiftedPredecessor =
            abstractLift(
                predecessorState.getAbstract(),
                concrete,
                predecessorState.getLocation(),
                pStates.getLocation());
        return new ConsecutionResult(
            false,
            new StatesWithLocation(
                abstractLiftedPredecessor,
                predecessorState.getConcrete(),
                predecessorState.getLocation()));
      }
    }

    // Must work at this point. Real predecessor would have been found before.
    assert abstractConsecutionWorks;

    // Generalize: Drop parts of abstr' that are not in unsat-core.
    stats.numberSuccessfulConsecutions++;
    BooleanFormula generalized =
        PDRUtils.asUnprimed(
            reduceByUnsatCore(
                PDRUtils.asPrimed(abstr, fmgr, transition),
                pAbstractProver,
                ReductionMode.CONSECUTION),
            fmgr,
            transition);
    generalized =
        dropIrrelevantConjunctiveParts(generalized, pAbstractProver, ReductionMode.CONSECUTION);
    assert isConsecutionValid(pLevel, generalized);
    return new ConsecutionResult(
        true, new StatesWithLocation(generalized, pStates.getConcrete(), pStates.getLocation()));
  }

  // Simple double check to assert that consecution for the given formula works.
  private boolean isConsecutionValid(int pLevel, BooleanFormula pGeneralizedStates)
      throws SolverException, InterruptedException {

    if (isInitial(pGeneralizedStates)) {
      return false;
    }

    try (ProverEnvironment prover = solver.newProverEnvironment()) {
      for (BooleanFormula frameClause : frameSet.getStates(pLevel)) {
        prover.push(frameClause);
      }
      prover.push(transition.getTransitionRelationFormula());
      prover.push(bfmgr.not(pGeneralizedStates));
      prover.push(PDRUtils.asPrimed(pGeneralizedStates, fmgr, transition));
      return prover.isUnsat();
    }
  }

  /**
   * Extracts a concrete state from the model. The formula is a pure conjunction of the form
   * (variable=value) for all unprimed variables in the transition system. The returned
   * StatesWithLocation contains the same state as both abstract and concrete formula as well as the
   * CFANode corresponding to the program-counter value in the state.
   */
  private StatesWithLocation getSatisfyingState(Model pModel) throws InterruptedException {
    BitvectorFormulaManagerView bvfmgr = fmgr.getBitvectorFormulaManager();
    PathFormula unprimedContext = transition.getUnprimedContext();
    BooleanFormula satisfyingState = bfmgr.makeTrue();
    CFANode location = null;

    for (String variableName : unprimedContext.getSsa().allVariables()) {
      shutdownNotifier.shutdownIfNecessary();

      // Make variable
      CType type = unprimedContext.getSsa().getType(variableName);
      BitvectorFormula unprimedVar =
          (BitvectorFormula)
              pfmgr.makeFormulaForVariable(unprimedContext, variableName, type, false);

      // Make value
      BigInteger val = pModel.evaluate(unprimedVar);

      /*
       * Null means there is no unprimed variable. To still get a full state, assign
       * the same value v that the primed variable has. The actual value shouldn't matter
       * because it is re-assigned to v any way. This way, the chosen value automatically
       * has the correct type.
       */
      if (val == null) {
        BitvectorFormula primedVar =
            (BitvectorFormula)
                pfmgr.makeFormulaForVariable(
                    transition.getPrimedContext(), variableName, type, false);
        val = pModel.evaluate(primedVar);
      }

      BitvectorFormula value = bvfmgr.makeBitvector(fmgr.getFormulaType(unprimedVar), val);

      if (variableName.equals(transition.programCounterName())) {
        location = transition.getNodeForID(val.intValue()).get();
      }

      // Conjoin (variable=value) to existing formula
      satisfyingState = bfmgr.and(satisfyingState, bvfmgr.equal(unprimedVar, value));
    }

    return new StatesWithLocation(satisfyingState, satisfyingState, location);
  }

  //---------------------------------Inner classes-----------------------------

  /**
   * Contains information about the result of the call to {@link PDRSmt#consecution(int,
   * StatesWithLocation)} and {@link PDRSmt#getCTIinFrontierFrame()}.
   *
   * <p>If consecution failed, it contains a formula representing predecessors --- states that can
   * reach the ones that were checked for consecution.
   *
   * <p>If consecution succeeded, the contained formula stands for a set of states that also obey
   * consecution, in addition to the original states themselves.
   */
  public static class ConsecutionResult {

    private final boolean wasConsecutionSuccessful;
    private final StatesWithLocation state;

    private ConsecutionResult(boolean pSuccess, StatesWithLocation pState) {
      this.wasConsecutionSuccessful = pSuccess;
      this.state = pState;
    }

    /**
     * Returns whether or not the consecution attempt succeeded.
     *
     * @return True if consecution succeeded. False otherwise.
     */
    public boolean wasConsecutionSuccessful() {
      return wasConsecutionSuccessful;
    }

    /**
     * Returns the result of the consecution attempt. If it succeeded, the returned formula
     * describes a set of states that obey consecution. If it failed, the returned formula describes
     * predecessor states that are responsible for the failure.
     *
     * @return A formula describing either a set of states that passed consecution, or predecessors
     *     that are a reason for the failure.
     */
    public StatesWithLocation getResult() {
      return state;
    }
  }

  private static class PDRSatStatistics implements Statistics {

    // Number of method calls
    private int numberSuccessfulConsecutions = 0;
    private int numberFailedConsecutions = 0;
    private int numberSuccessfulLifts = 0;
    private int numberFailedLifts = 0;
    private int numberImpossibleLifts = 0;

    // Unsat core reduction stats
    private long partsBeforeCoreGen = 0;
    private long partsBeforeCoreLift = 0;
    private long droppedAfterCoreGen = 0;
    private long droppedAfterCoreLift = 0;

    // Manual reduction stats
    private long partsBeforeManualGen = 0;
    private long partsBeforeManualLift = 0;
    private long droppedAfterManualGen = 0;
    private long droppedAfterManualLift = 0;

    // Timing
    private final Timer consecutionTimer = new Timer();
    private final Timer pureConsecutionSatTimer = new Timer();
    private final Timer pureLiftingSatTimer = new Timer();
    private final Timer liftingTimer = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {

      // Consecution stats
      pOut.println(
          "Total number of consecution queries:                "
              + String.valueOf(numberFailedConsecutions + numberSuccessfulConsecutions));
      pOut.println(
          "  Successful consecution queries:                   " + numberSuccessfulConsecutions);
      pOut.println(
          "  Failed consecution queries:                       " + numberFailedConsecutions);
      if (consecutionTimer.getNumberOfIntervals() > 0) {
        pOut.println("Total time for consecution calls:               " + consecutionTimer);
        pOut.println(
            "  Average time for consecution calls:           "
                + consecutionTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
      }
      if (pureConsecutionSatTimer.getNumberOfIntervals() > 0) {
        pOut.println("Total time spent in solver during consecution:  " + pureConsecutionSatTimer);
        pOut.println(
            "  Average time in solver during consecution:    "
                + pureConsecutionSatTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
      }

      // Lifting stats
      int totalLifts = numberFailedLifts + numberSuccessfulLifts + numberImpossibleLifts;
      pOut.println("Total number of lifting queries:                    " + totalLifts);
      pOut.println("  Successful attempts:                              " + numberSuccessfulLifts);
      pOut.println("  Failed attempts:                                  " + numberFailedLifts);
      pOut.println("  Impossible attempts:                              " + numberImpossibleLifts);
      if (liftingTimer.getNumberOfIntervals() > 0) {
        pOut.println("Total time for lifting calls:                   " + liftingTimer);
        pOut.println(
            "  Average time for lifting calls:               "
                + liftingTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
      }
      if (pureLiftingSatTimer.getNumberOfIntervals() > 0) {
        pOut.println("Total time spent in solver during lifting:      " + pureLiftingSatTimer);
        pOut.println(
            "  Average time in solver during lifting:        "
                + pureLiftingSatTimer.getAvgTime().formatAs(TimeUnit.SECONDS));
      }

      // Unsat core stats
      long totalDroppedByCore = droppedAfterCoreGen + droppedAfterCoreLift;
      pOut.println("Number of dropped parts with unsat core:            " + totalDroppedByCore);
      pOut.println(
          "  Dropped with core during generalization:          "
              + getPercentage(droppedAfterCoreGen, partsBeforeCoreGen));
      pOut.println(
          "  Dropped with core during lifting:                 "
              + getPercentage(droppedAfterCoreLift, partsBeforeCoreLift));

      // Manual reduction stats
      long totalManuallyDropped = droppedAfterManualGen + droppedAfterManualLift;
      pOut.println("Number of manually dropped after unsat core:        " + totalManuallyDropped);
      pOut.println(
          "  Manually dropped during generalization:           "
              + getPercentage(droppedAfterManualGen, partsBeforeManualGen));
      pOut.println(
          "  Manually dropped during lifting:                  "
              + getPercentage(droppedAfterManualLift, partsBeforeManualLift));
    }

    private String getPercentage(long pDividend, long pDivisor) {
      double ratio = (double) pDividend / pDivisor;
      return Math.round(ratio * 100) + "%";
    }

    @Override
    public @Nullable String getName() {
      return "SMT queries";
    }
  }

}
