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
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
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
 * The central class with PDR related methods that require SMT-solving for queries that involve
 * relative inductivity. Takes care of predicate abstraction.
 */
public class PDRSmt {

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
  private final ShutdownNotifier shutDown;

  /**
   * Creates a new PDRSmt instance.
   *
   * @param pFrameSet The frames relative to which the induction queries formed.
   * @param pSolver The solver that is to be used in all queries.
   * @param pFmgr The used formula manager.
   * @param pPfmgr The used path formula manager.
   * @param pAbstractionManager The component that handles predicate abstraction.
   * @param pTransition The global transition relation that defines the transition formula.
   * @param pCompStats The statistics delegator that this class should be registered at. It takes
   *     care of printing PDRSmt statistics.
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
    this.shutDown = pShutdown;
  }

  /**
   * Tries to find direct error predecessor states. Checks if the transition to error states is
   * still possible, starting inside the most recent overapproximation frame.
   *
   * <p>In short : Get states satisfying [F(maxLevel) & T & not(SafetyProperty)'].
   *
   * @return An Optional containing a formula describing direct error predecessor states, if they
   *     exist. An empty Optional is no predecessors exists.
   */
  public Optional<ConsecutionResult> getCTI()
      throws SolverException, InterruptedException, CPAException {
    stats.consecutionTimer.start();
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      // Push F(maxLevel) & T & not(P)'
      for (BooleanFormula frameClause : frameSet.getStates(frameSet.getMaxLevel())) {
        prover.push(frameClause);
      }
      prover.push(transition.getTransitionRelationFormula());
      BooleanFormula notSafetyPrimed =
          PDRUtils.asPrimed(bfmgr.not(transition.getSafetyProperty()), fmgr, transition);
      prover.push(notSafetyPrimed);

      if (timedUnsatCall(prover, stats.pureConsecutionSatTimer)) {
        stats.numberSuccessfulConsecutions++;
        return Optional.empty();
      }

      stats.numberFailedConsecutions++;
      StatesWithLocation directErrorPredecessor = getSatisfyingState(prover.getModel());
      BooleanFormula concreteState = directErrorPredecessor.getFormula();
      CFANode correspondingErrorLocation =
          PDRUtils.getBlockToNextTargetLocation(
                  directErrorPredecessor, transition, forward, fmgr, bfmgr, solver)
              .orElseThrow(IllegalStateException::new)
              .getSuccessorLocation();
      BooleanFormula liftedAbstractState =
          abstractLift(
              concreteState,
              notSafetyPrimed,
              directErrorPredecessor.getLocation(),
              correspondingErrorLocation);
      assert ctiOK(liftedAbstractState);
      return Optional.of(
          new ConsecutionResult(
              false,
              new StatesWithLocation(
                  liftedAbstractState,
                  directErrorPredecessor.getLocation(),
                  directErrorPredecessor.getConcrete())));
    } finally {
      stats.consecutionTimer.stop();
    }
  }

  private boolean ctiOK(BooleanFormula pLiftedAbstractedState)
      throws SolverException, InterruptedException {
    try (ProverEnvironment prover = solver.newProverEnvironment()) {

      for (BooleanFormula frameClause : frameSet.getStates(frameSet.getMaxLevel())) {
        prover.push(frameClause);
      }
      prover.push(pLiftedAbstractedState);
      prover.push(transition.getTransitionRelationFormula());
      prover.push(PDRUtils.asPrimed(bfmgr.not(transition.getSafetyProperty()), fmgr, transition));
      return !prover.isUnsat();
    }
  }

  /**
   * Checks if the given formula describes initial states. This is based on the transition relation
   * given in the constructor of this instance.
   *
   * @param pStates The formula describing a set of states.
   * @return True, if the provided states are initial, and false otherwise.
   */
  public boolean isInitial(BooleanFormula pStates) throws SolverException, InterruptedException {

    // States are initial iff : SAT[I & states]
    BooleanFormula initialCondAndStates =
        bfmgr.and(PDRUtils.asUnprimed(transition.getInitialCondition(), fmgr, transition), pStates);
    return !solver.isUnsat(initialCondAndStates);
  }

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
   * [pConcreteState & T & not(pSuccessorStates)'] is unsat. Abstract pConcreteStates to 'abstr' and
   * try with [abstr & T & not(pSuccessorStates)']. If it is sat, there is a spurious transition and
   * the domain is refined with an interpolant. Trying again with the refined 'abstr' must now
   * succeed. After the abstract query is unsat (with or without refinement), drop unused literals
   * of 'abstr' and return that.
   *
   * @throws CPAException if the analysis creating the blocks encounters an exception.
   */
  private <T> BooleanFormula abstractLift(
      BooleanFormula pConcreteState,
      BooleanFormula pSuccessorStates,
      CFANode pPredLoc,
      CFANode pSuccLoc,
      InterpolatingProverEnvironment<T> pConcrProver,
      ProverEnvironment pAbstrProver)
      throws InterruptedException, SolverException, CPAException {
    logger.log(Level.ALL, "Concrete : ", pConcreteState);
    BooleanFormula abstractState = abstractionManager.computeAbstraction(pConcreteState);
    logger.log(Level.ALL, "Abstract : ", abstractState);

    // Push unsatisfiable formula (state & T & not(successor)'). Push predecessor state last,
    // so it can be popped and replaced with a refined version later when necessary.
    pAbstrProver.push(transition.getTransitionRelationFormula());
    pConcrProver.push(transition.getTransitionRelationFormula());
    pAbstrProver.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));
    pConcrProver.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));
    T idForInterpolation = pConcrProver.push(pConcreteState);
    pAbstrProver.push(abstractState);

    boolean concreteQueryUnsat = timedUnsatCall(pConcrProver, stats.pureLiftingSatTimer);

    // Concrete query could be sat if successor contains non-deterministically assigned
    // variables or if predecessor has more than one successor. If the query is still sat
    // after variables have been dropped, lifting cannot work. Just return abstract state
    // as it is.
    if (!concreteQueryUnsat) {
      idForInterpolation =
          removeNondetVariables(pConcreteState, pSuccessorStates, pPredLoc, pSuccLoc, pConcrProver);
    }
    if (!timedUnsatCall(pConcrProver, stats.pureLiftingSatTimer)) {
      stats.numberImpossibleLifts++;
      return abstractState;
    }

    // Abstraction was too broad => Refine.
    if (!timedUnsatCall(pAbstrProver, stats.pureLiftingSatTimer)) {
      stats.numberFailedLifts++;

      BooleanFormula interpolant =
          pConcrProver.getInterpolant(Collections.singletonList(idForInterpolation));
      assert interpolant.equals(PDRUtils.asUnprimed(interpolant, fmgr, transition));
      abstractState = abstractionManager.refineAndComputeAbstraction(pConcreteState, interpolant);
      logger.log(Level.ALL, "Abstract after refinement : ", abstractState);

      // Update abstraction on prover.
      pAbstrProver.pop();
      pAbstrProver.push(abstractState);
      assert pAbstrProver.isUnsat();
    } else {
      stats.numberSuccessfulLifts++;
    }

    // Get used literals from query with abstraction. Query must be unsat at this point.
    BooleanFormula reduced = reduceByUnsatCore(abstractState, pAbstrProver, false);
    if (options.shouldDropLiteralsAfterLiftingWithUnsatCore()) {
      reduced = dropLits(reduced, pAbstrProver, false);
    }
    assert liftOK(reduced, pSuccessorStates);
    logger.log(Level.ALL, "Abstract after reduction: ", reduced);
    return reduced;
  }

  private boolean liftOK(BooleanFormula pLiftedStates, BooleanFormula pSuccessorStates)
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

  // Assumes that the prover already contains the unsatisfiable query and the formula at the top of
  // the prover stack is pFormula and the one to reduce. Afterwards, the prover will contain the new
  // reduced formula instead.
  private BooleanFormula reduceByUnsatCore(
      BooleanFormula pFormula, ProverEnvironment pProver, boolean consecutionMode)
      throws SolverException, InterruptedException {

    logger.log(Level.ALL, "Formula before unsat core reduction : ", pFormula);
    pProver.pop(); // Remove old (unreduced) formula.
    Set<BooleanFormula> conjuncts = bfmgr.toConjunctionArgs(pFormula, true);

    if (consecutionMode) {
      stats.partsBeforeCoreGen += conjuncts.size();
    } else {
      stats.partsBeforeCoreLift += conjuncts.size();
    }

    // Mapping of activation literals to conjuncts
    Map<BooleanFormula, BooleanFormula> actToConjuncts = new HashMap<>();

    // Create activation literals A <=> conjunct, push equivalences and check which literals
    // are in core (over assumptions with all literals).
    for (BooleanFormula conjunct : conjuncts) {

      // Activation literals must have a unique name! Conjuncts are distinct from another, so use
      // toString as unique name.
      BooleanFormula act = bfmgr.makeVariable(conjunct.toString());
      BooleanFormula equiv = bfmgr.equivalence(act, conjunct);
      actToConjuncts.put(act, conjunct);
      pProver.push(equiv);
    }

    Optional<List<BooleanFormula>> usedLits;
    Timer timer = consecutionMode ? stats.pureConsecutionSatTimer : stats.pureLiftingSatTimer;
    timer.start();
    try {
      usedLits = pProver.unsatCoreOverAssumptions(actToConjuncts.keySet());
    } finally {
      timer.stop();
    }

    shutDown.shutdownIfNecessary();
    assert usedLits.isPresent(); // Must be unsat
    Set<BooleanFormula> usedConjuncts =
        actToConjuncts
            .entrySet()
            .stream()
            .filter(entry -> usedLits.get().contains(entry.getKey()))
            .map(entry -> entry.getValue())
            .collect(Collectors.toSet());

    BooleanFormula reduced = bfmgr.and(usedConjuncts);

    int dropped = conjuncts.size() - usedConjuncts.size();
    if (consecutionMode) {
      stats.droppedAfterCoreGen += dropped;
    } else {
      stats.droppedAfterCoreLift += dropped;
    }

    // If reduction is initial, pc was dropped. Re-add it.
    if (isInitial(PDRUtils.asUnprimed(reduced, fmgr, transition))) {
      reduced = bfmgr.and(getPcComponent(pFormula), reduced);
      if (consecutionMode) {
        stats.droppedAfterCoreGen--;
      } else {
        stats.droppedAfterCoreLift--;
      }

    }
    logger.log(Level.ALL, "Formula after unsat core reduction : ", reduced);
    assert !isInitial(PDRUtils.asUnprimed(reduced, fmgr, transition));

    // Rebuild prover : remove equivalences and push new reduced formula.
    for (int i = 0; i < conjuncts.size(); ++i) {
      pProver.pop();
    }
    pProver.push(reduced);
    return reduced;
  }

  private BooleanFormula getPcComponent(BooleanFormula pFormula) throws InterruptedException {
    boolean isPrimed = pFormula.equals(PDRUtils.asPrimed(pFormula, fmgr, transition));
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
   * For consecution, the prover should contain from bottom to top: F, T, not(state), state'.<br>
   * For lifting, the prover should contain from bottom to top: successor', T, predecessor.<br>
   * Both queries must return unsat!<br>
   * Removes conjunctive parts of the formula on top and checks if query still unsat. In consecution
   * mode, not(state) and state' are updated. In lifting mode, predecessor is updated. The prover
   * will contain the final shrunk formulas afterwards instead of their originals.
   */
  private BooleanFormula dropLits(
      BooleanFormula pFormula, ProverEnvironment pProver, boolean consecutionMode)
      throws SolverException, InterruptedException {

    logger.log(Level.ALL, "Formula before manual drop: ", pFormula);

    Set<BooleanFormula> remainingLits = bfmgr.toConjunctionArgs(pFormula, true);
    if (consecutionMode) {
      stats.partsBeforeManualGen += remainingLits.size();
    } else {
      stats.partsBeforeManualLift += remainingLits.size();
    }

    Iterator<BooleanFormula> litIterator = remainingLits.iterator();
    int droppedLits = 0;
    int numberOfTries = 0;

    while (numberOfTries < options.maxAttemptsAtDroppingLiterals()
        && droppedLits < options.maxLiteralsToDrop()
        && litIterator.hasNext()) {
      shutDown.shutdownIfNecessary();
      numberOfTries++;
      BooleanFormula currentLit = litIterator.next();

      // Remove lit from formula
      Set<BooleanFormula> litsWithoutCurrent =
          Sets.filter(remainingLits, bf -> !bf.equals(currentLit));
      BooleanFormula formulaWithoutLit =
          PDRUtils.asUnprimed(bfmgr.and(litsWithoutCurrent), fmgr, transition);

      // If removal makes states initial, continue with next lit.
      if (isInitial(formulaWithoutLit)) {
        continue;
      }

      // Now check if still unsat with reduced formula. Yes -> remove lit from set.
      // Consecution mode : push not(reduced) and reduced'
      // Lift mode : push reduced
      if (consecutionMode) {
        pProver
            .pop(); // Prover contains old versions of not(states) and states'. Remove them first.
        pProver.pop();
        pProver.push(bfmgr.not(formulaWithoutLit));
        pProver.push(PDRUtils.asPrimed(formulaWithoutLit, fmgr, transition));
      } else {
        pProver.pop(); // Prover contains old version of states. Remove it first.
        pProver.push(formulaWithoutLit);
      }


      if (timedUnsatCall(
          pProver, consecutionMode ? stats.pureConsecutionSatTimer : stats.pureLiftingSatTimer)) {
        litIterator.remove();

        if (consecutionMode) {
          stats.droppedAfterManualGen++;
        } else {
          stats.droppedAfterManualLift++;
        }
        droppedLits++;
      }
    }

    // All unneeded conjuncts/literals are removed at this point. The conjunction of the remaining ones
    // is the reduced formula.
    BooleanFormula result = bfmgr.and(remainingLits);
    logger.log(Level.ALL, "Formula after manual drop: ", result);
    return result;
  }

  private Set<Formula> nondetVarsOfConnectingBlock(CFANode predLoc, CFANode succLoc)
      throws CPAException, InterruptedException {

    FluentIterable<Block> connectingBlocks =
        forward.getBlocksFrom(predLoc).filter(b -> b.getSuccessorLocation().equals(succLoc));
    // Just get the block with the most disjunction args.
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

  private <T> T removeNondetVariables(
      BooleanFormula pPred,
      BooleanFormula pSucc,
      CFANode pPredLoc,
      CFANode pSuccLoc,
      InterpolatingProverEnvironment<T> pConcrProver)
      throws InterruptedException, CPAException {

    // Remove non-deterministically assigned variables from pSuccessorStates.
    // Pop pred and succ first (in that order!).
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
   * Checks if the given states are inductive relative to the frame with the given level. In short:
   * Is [F_pLevel & not(pStates) & T & pStates'] unsat or not.
   *
   * <p>If it is, the returned ConsecutionResult contains an inductive formula consisting of a
   * subset of the used literals in the original formula.<br>
   * If it is not, the returned ConsecutionResult contains formula describing predecessors of the
   * original states. Those predecessors are one reason why the original states are not inductive.
   *
   * <p>The given states should be instantiated as unprimed.
   *
   * @param pLevel The frame level relative to which consecution should be checked.
   * @param pStates The states whose inductivity should be checked.
   * @return A set of states that are inductive, or predecessors of the original states.
   * @throws SolverException If the solver failed during consecution.
   * @throws InterruptedException If the process was interrupted.
   */
  public ConsecutionResult consecution(int pLevel, StatesWithLocation pStates)
      throws SolverException, InterruptedException, CPAException {
    stats.consecutionTimer.start();
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

    BooleanFormula abstr = pStates.getFormula();
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
        timedUnsatCall(pAbstractProver, stats.pureConsecutionSatTimer);
    boolean concreteConsecutionWorks =
        timedUnsatCall(pConcreteProver, stats.pureConsecutionSatTimer);

    if (!abstractConsecutionWorks) {
      if (concreteConsecutionWorks) {

        // Refine
        BooleanFormula interpolant = pConcreteProver.getInterpolant(idsForInterpolation);
        BooleanFormula forRefinement = bfmgr.not(interpolant);
        abstr = abstractionManager.refineAndComputeAbstraction(concrete, forRefinement);
        logger.log(Level.ALL, "Abstract after refinement : ", abstr);

        // Update not(s) and s'
        pAbstractProver.pop();
        pAbstractProver.pop();
        pAbstractProver.push(bfmgr.not(abstr));
        pAbstractProver.push(PDRUtils.asPrimed(abstr, fmgr, transition));

        abstractConsecutionWorks = timedUnsatCall(pAbstractProver, stats.pureConsecutionSatTimer);
        assert abstractConsecutionWorks;
        assert consecutionOK(pLevel, abstr);
      } else {

        // Real predecessor found
        stats.numberFailedConsecutions++;
        StatesWithLocation predecessorState = getSatisfyingState(pConcreteProver.getModel());

        // No need to lift and abstract if state is initial.
        // PDR will terminate with counterexample anyway.
        if (isInitial(predecessorState.getConcrete())) {
          return new ConsecutionResult(false, predecessorState);
        }
        BooleanFormula abstractLiftedPredecessor =
            abstractLift(
                predecessorState.getFormula(),
                concrete,
                predecessorState.getLocation(),
                pStates.getLocation());
        return new ConsecutionResult(
            false,
            new StatesWithLocation(
                abstractLiftedPredecessor,
                predecessorState.getLocation(),
                predecessorState.getConcrete()));
      }
    }

    assert abstractConsecutionWorks;

    // Generalize
    stats.numberSuccessfulConsecutions++;
    BooleanFormula generalized =
        PDRUtils.asUnprimed(
            reduceByUnsatCore(PDRUtils.asPrimed(abstr, fmgr, transition), pAbstractProver, true),
            fmgr,
            transition);
    generalized = dropLits(generalized, pAbstractProver, true);
    assert consecutionOK(pLevel, generalized);
    return new ConsecutionResult(
        true, new StatesWithLocation(generalized, pStates.getLocation(), pStates.getConcrete()));
  }

  private static <T> boolean timedUnsatCall(BasicProverEnvironment<T> pProver, Timer pTimer)
      throws SolverException, InterruptedException {
    pTimer.start();
    try {
      return pProver.isUnsat();
    } finally {
      pTimer.stop();
    }
  }

  private boolean consecutionOK(int pLevel, BooleanFormula pGeneralizedStates)
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
   * Extracts a concrete state from the model. The given context contains all program variables as
   * they are provided by the transition relation. The returned formula is a pure conjunction of the
   * form (variable=value).
   */
  private StatesWithLocation getSatisfyingState(Model pModel) throws InterruptedException {
    BitvectorFormulaManagerView bvfmgr = fmgr.getBitvectorFormulaManager();
    PathFormula unprimedContext = transition.getUnprimedContext();
    BooleanFormula satisfyingState = bfmgr.makeTrue();
    CFANode location = null;

    for (String variableName : unprimedContext.getSsa().allVariables()) {
      shutDown.shutdownIfNecessary();

      // Make variable
      CType type = unprimedContext.getSsa().getType(variableName);
      BitvectorFormula unprimedVar =
          (BitvectorFormula)
              pfmgr.makeFormulaForVariable(unprimedContext, variableName, type, false);

      // Make value
      BigInteger val = pModel.evaluate(unprimedVar);

      /*
       * Null means there is no unprimed variable. To still get a full state, assign
       * the same value v that the primed variable has. The actual value shouldn't matter,
       * because it is re-assigned to v any way. This way the chosen value automatically
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

    return new StatesWithLocation(satisfyingState, location, satisfyingState);
  }

  //---------------------------------Inner classes-----------------------------

  /**
   * Contains information on the result of the call to {@link PDRSmt#consecution(int,
   * StatesWithLocation)} and {@link PDRSmt#getCTI()}.
   *
   * <p>If consecution failed or a CTI was found, contains a formula representing predecessors -
   * states that can reach either the ones that were checked for consecution, or error states in
   * case of getCTI().
   *
   * <p>If consecution succeeded, the contained formula stands for a set of states that also obey
   * consecution, in addition to the original states themselves.
   */
  public static class ConsecutionResult {

    private final boolean consecutionSuccessful;
    private final StatesWithLocation state;

    private ConsecutionResult(boolean pSuccess, StatesWithLocation pState) {
      this.consecutionSuccessful = pSuccess;
      this.state = pState;
    }

    /**
     * Returns whether or not the consecution attempt succeeded.
     *
     * @return True, if consecution succeeded. False otherwise.
     */
    public boolean consecutionSuccess() {
      return consecutionSuccessful;
    }

    /**
     * Returns the result of the consecution attempt. If it succeeded, the returned formula
     * describes a whole set of states that obey consecution. If it failed, the returned formula
     * describes a predecessor state that is responsible for the failure.
     *
     * @return A formula describing either a set of states that passed consecution, or a predecessor
     *     that is one reason for the failure.
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
