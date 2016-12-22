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

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
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
      StatisticsDelegator pCompStats) {
    this.stats = new PDRSatStatistics();
    Objects.requireNonNull(pCompStats).register(stats);

    this.frameSet = Objects.requireNonNull(pFrameSet);
    this.solver = Objects.requireNonNull(pSolver);
    this.fmgr = Objects.requireNonNull(pFmgr);
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.pfmgr = Objects.requireNonNull(pPfmgr);
    this.abstractionManager = Objects.requireNonNull(pAbstractionManager);
    this.transition = Objects.requireNonNull(pTransition);
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
  public Optional<ConsecutionResult> getCTI() throws SolverException, InterruptedException {
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      // Push F(maxLevel) & T & not(P)'
      for (BooleanFormula frameClause : frameSet.getStates(frameSet.getMaxLevel())) {
        prover.push(frameClause);
      }
      prover.push(transition.getTransitionRelationFormula());
      BooleanFormula notSafetyPrimed =
          PDRUtils.asPrimed(bfmgr.not(transition.getSafetyProperty()), fmgr, transition);
      prover.push(notSafetyPrimed);

      if (prover.isUnsat()) {
        return Optional.empty();
      }

      Model model = prover.getModel();
      StatesWithLocation directErrorPredecessor = getSatisfyingState(model);
      BooleanFormula concreteState = directErrorPredecessor.getFormula();
      BooleanFormula liftedAbstractState = abstractLift(concreteState, notSafetyPrimed);
      return Optional.of(
          new ConsecutionResult(
              false,
              new StatesWithLocation(liftedAbstractState, directErrorPredecessor.getLocation())));
    }
  }

  /**
   * Checks if the given formula describes initial states. This is based on the transition relation
   * given in the constructor of this instance.
   *
   * @param pStates The formula describing a set of states.
   * @return True, if the provided states are initial, and false otherwise.
   */
  public boolean isInitial(BooleanFormula pStates) {
    // TODO what is initial? SAT[InitialCondition & pStates]? (pc=startlocationID) in formula/
    // no pc in formula at all?
    return bfmgr
        .toConjunctionArgs(pStates, true)
        .stream()
        .anyMatch(
            literal ->
                literal.equals(
                    PDRUtils.asUnprimed(transition.getInitialCondition(), fmgr, transition)));
  }

  private BooleanFormula abstractLift(
      BooleanFormula pConcretePredecessor, BooleanFormula pSuccessors)
      throws InterruptedException, SolverException {
    stats.liftingTimer.start();
    try (InterpolatingProverEnvironment<?> concreteProver =
            solver.newProverEnvironmentWithInterpolation();
        ProverEnvironment abstractProver =
            solver.newProverEnvironment(ProverOptions.GENERATE_UNSAT_CORE)) {
      return abstractLift(pConcretePredecessor, pSuccessors, concreteProver, abstractProver);
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
   */
  private <T> BooleanFormula abstractLift(
      BooleanFormula pConcreteState,
      BooleanFormula pSuccessorStates,
      InterpolatingProverEnvironment<T> pConcrProver,
      ProverEnvironment pAbstrProver)
      throws InterruptedException, SolverException {

    BooleanFormula abstractState = abstractionManager.computeAbstraction(pConcreteState);

    // Push unsatisfiable formula (state & T & not(successor)')
    pAbstrProver.push(transition.getTransitionRelationFormula());
    pConcrProver.push(transition.getTransitionRelationFormula());
    pAbstrProver.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));
    pConcrProver.push(PDRUtils.asPrimed(bfmgr.not(pSuccessorStates), fmgr, transition));

    // Push predecessor state last, so it can be popped and replaced with a refined version later
    // when necessary.
    T id = pConcrProver.push(pConcreteState);
    Set<BooleanFormula> conjunctiveParts = bfmgr.toConjunctionArgs(abstractState, true);
    int numParts = conjunctiveParts.size();
    for (BooleanFormula part : conjunctiveParts) {
      pAbstrProver.push(part);
    }
    assert pConcrProver.isUnsat();

    // Abstraction was too broad => Prepare interpolating prover and refine.
    if (!pAbstrProver.isUnsat()) {
      stats.numberFailedLifts++;
      boolean unsat = pConcrProver.isUnsat();
      assert unsat;

      BooleanFormula interpolant = pConcrProver.getInterpolant(Collections.singletonList(id));
      abstractState = abstractionManager.refineAndComputeAbstraction(pConcreteState, interpolant);

      // Update abstraction on prover.
      for (int i = 0; i < numParts; ++i) {
        pAbstrProver.pop();
      }
      conjunctiveParts = bfmgr.toConjunctionArgs(abstractState, true);
      numParts = conjunctiveParts.size();
      for (BooleanFormula part : conjunctiveParts) {
        pAbstrProver.push(part);
      }
      unsat = pAbstrProver.isUnsat();
      assert unsat;
    } else {
      stats.numberSuccessfulLifts++;
    }

    // Get used literals from query with abstraction. Query must be unsat at this point.
    BooleanFormula liftedAbstractState =
        dropUnusedLiterals(conjunctiveParts, pAbstrProver, numParts);
    return liftedAbstractState;
  }

  // TODO special handling if reduced formula is initial
  /**
   * Builds a boolean formula that consists of exactly those parts in pConjArgs that are also in the
   * unsat core of the prover.
   */
  private BooleanFormula dropUnusedLiterals(
      Set<BooleanFormula> pConjArgs, ProverEnvironment pProver, int pNumPops)
      throws SolverException, InterruptedException {

    List<BooleanFormula> usedLiterals =
        pProver
            .getUnsatCore()
            .stream()
            .filter(bf -> pConjArgs.contains(bf))
            .collect(Collectors.toList());
    BooleanFormula generalized = bfmgr.and(usedLiterals);
    assert !isInitial(generalized);
    assert unsatWith(generalized, pProver, pNumPops);
    return generalized;
  }

  private boolean unsatWith(BooleanFormula pFormula, ProverEnvironment pProver, int pNumPops)
      throws SolverException, InterruptedException {
    for (int i = 0; i < pNumPops; ++i) {
      pProver.pop();
    }
    pProver.push(pFormula);
    return pProver.isUnsat();
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
      throws SolverException, InterruptedException {
    stats.consecutionTimer.start();
    try (ProverEnvironment prover =
        solver.newProverEnvironment(
            ProverOptions.GENERATE_MODELS, ProverOptions.GENERATE_UNSAT_CORE)) {
      BooleanFormula states = pStates.getFormula();

      // Push (F_pLevel & not(s) & T & s')
      for (BooleanFormula frameClause : frameSet.getStates(pLevel)) {
        prover.push(frameClause);
      }
      prover.push(transition.getTransitionRelationFormula());
      prover.push(bfmgr.not(states));
      BooleanFormula primed = PDRUtils.asPrimed(states, fmgr, transition);

      // Push conjunctive parts of s' separately. Favors minimal unsat core.
      Set<BooleanFormula> conjunctiveParts = bfmgr.toConjunctionArgs(primed, true);
      for (BooleanFormula part : conjunctiveParts) {
        prover.push(part);
      }

      // If successful, return generalized version of states.
      if (prover.isUnsat()) {
        stats.numberSuccessfulConsecutions++;
        BooleanFormula generalized =
            dropUnusedLiterals(conjunctiveParts, prover, conjunctiveParts.size());
        return new ConsecutionResult(
            true,
            new StatesWithLocation(
                PDRUtils.asUnprimed(generalized, fmgr, transition), pStates.getLocation()));
      }

      // If unsuccessful, return abstracted and lifted predecessor.
      stats.numberFailedConsecutions++;
      Model model = prover.getModel();
      StatesWithLocation predecessorState = getSatisfyingState(model);

      // No need to lift and abstract if state is initial.
      // PDR will terminate with counterexample anyway.
      if (isInitial(predecessorState.getFormula())) {
        return new ConsecutionResult(false, predecessorState);
      }
      BooleanFormula abstractLiftedPredecessor =
          abstractLift(predecessorState.getFormula(), states);
      return new ConsecutionResult(
          false, new StatesWithLocation(abstractLiftedPredecessor, predecessorState.getLocation()));
    } finally {
      stats.consecutionTimer.stop();
    }
  }

  /**
   * Extracts a concrete state from the model. The given context contains all program variables as
   * they are provided by the transition relation. The returned formula is a pure conjunction of the
   * form (variable=value).
   */
  private StatesWithLocation getSatisfyingState(Model pModel) {
    BitvectorFormulaManagerView bvfmgr = fmgr.getBitvectorFormulaManager();
    PathFormula unprimedContext = transition.getUnprimedContext();
    BooleanFormula satisfyingState = bfmgr.makeTrue();
    CFANode location = null;

    for (String variableName : unprimedContext.getSsa().allVariables()) {

      // Make variable
      CType type = unprimedContext.getSsa().getType(variableName);
      BitvectorFormula unprimedVar =
          (BitvectorFormula)
              pfmgr.makeFormulaForVariable(unprimedContext, variableName, type, false);

      // Make value
      BigInteger val = pModel.evaluate(unprimedVar);
      BitvectorFormula value = bvfmgr.makeBitvector(fmgr.getFormulaType(unprimedVar), val);

      if (variableName.equals(transition.programCounterName())) {
        location = transition.getNodeForID(val.intValue()).get();
      }

      // Conjoin (variable=value) to existing formula
      satisfyingState = bfmgr.and(satisfyingState, bvfmgr.equal(unprimedVar, value));
    }

    return new StatesWithLocation(satisfyingState, location);
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

    private int numberSuccessfulConsecutions = 0;
    private int numberFailedConsecutions = 0;
    private int numberSuccessfulLifts = 0;
    private int numberFailedLifts = 0;
    private Timer consecutionTimer = new Timer();
    private Timer liftingTimer = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println(
          "Total number of consecution queries:           "
              + String.valueOf(numberFailedConsecutions + numberSuccessfulConsecutions));
      pOut.println(
          "Number of successful consecution queries:      " + numberSuccessfulConsecutions);
      pOut.println("Number of failed consecution queries:          " + numberFailedConsecutions);
      if (consecutionTimer.getNumberOfIntervals() > 0) {
        pOut.println(
            "Total time for consecution queries:            " + consecutionTimer.getSumTime());
        pOut.println(
            "Average time for consecution queries:          " + consecutionTimer.getAvgTime());
      }
      pOut.println(
          "Total number of lifting queries:               "
              + String.valueOf(numberFailedLifts + numberSuccessfulLifts));
      pOut.println(
          "Number of successful lifting queries:          " + numberSuccessfulConsecutions);
      pOut.println("Number of failed lifting queries:              " + numberFailedConsecutions);
      if (liftingTimer.getNumberOfIntervals() > 0) {
        pOut.println(
            "Total time for lifting queries:                " + consecutionTimer.getSumTime());
        pOut.println(
            "Average time for lifting queries:              " + consecutionTimer.getAvgTime());
      }
    }

    @Override
    public @Nullable String getName() {
      return "SMT queries";
    }
  }
}
