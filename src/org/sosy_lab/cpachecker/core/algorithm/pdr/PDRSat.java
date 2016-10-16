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
package org.sosy_lab.cpachecker.core.algorithm.pdr;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.pdr.transition.Block;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
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
 * The central class with all PDR related methods that require SMT-solving. Takes care of predicate
 * abstraction.
 */
public class PDRSat {

  private final FrameSet frameSet;
  private final Solver solver;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final PathFormulaManager pfmgr;
  private final PredicatePrecisionManager abstractionManager;

  public PDRSat(
      FrameSet pFrameSet,
      Solver pSolver,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      PredicatePrecisionManager pAbstractionManager) {
    this.frameSet = pFrameSet;
    this.solver = pSolver;
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.pfmgr = pPfmgr;
    this.abstractionManager = pAbstractionManager;
  }

  /**
   * Tries to find a direct error predecessor state. Checks if the block transition is possible,
   * starting from the most recent overapproximation frame for the predecessor location in the
   * block. Note that the successor location in the block is assumed to be an error location.
   *
   * <p>In short : Is [F(maxLevel,pBlock.predLoc) & T(pBlock.predLoc -> pBlock.succLoc)] sat?
   *
   * @param pBlock a block describing the transition from a location to an error location
   * @return An Optional containing a formula describing an error predecessor state, if one was
   *     found. An empty Optional is no predecessor exists for the given block.
   */
  public Optional<BooleanFormula> getCTI(Block pBlock)
      throws SolverException, InterruptedException {
    try (ProverEnvironment prover = solver.newProverEnvironment(ProverOptions.GENERATE_MODELS)) {

      // Push frame clauses
      for (BooleanFormula frameClause :
          frameSet.getStatesForLocation(pBlock.getPredecessorLocation(), frameSet.getMaxLevel())) {
        prover.push(PDRUtils.asUnprimed(frameClause, pBlock, fmgr));
      }

      // Push transition
      prover.push(pBlock.getFormula());

      if (prover.isUnsat()) {
        return Optional.empty();
      }

      Model model = prover.getModel();
      BooleanFormula concreteState = getSatisfyingState(model, pBlock.getUnprimedContext());
      return Optional.of(concreteState);
    }
  }

  /**
   * Checks if the negation of a formula is inductive relative to states in a specific frame.
   *
   * <p>In short : Is [F(pLevel,pBlock.predLoc) & not(pState) & T(pBlock.predLoc -> pBlock.succLoc)
   * => not(pState)'] valid?
   *
   * @param pLevel the frame level
   * @param pBlock the block describing the 2 locations and the local transition between them
   * @param pState the state whose negation shall be tested for relative inductivity
   * @return A ConsecutionResult that describes if not(pState) is inductive or not. In the former
   *     case, it contains a formula representing a whole set of states whose negation is inductive,
   *     in the latter case a predecessor of pState that prevents its inductivity.
   */
  public ConsecutionResult consecution(int pLevel, Block pBlock, BooleanFormula pState)
      throws SolverException, InterruptedException {
    try (InterpolatingProverEnvironment<?> interpolatingProver =
            solver.newProverEnvironmentWithInterpolation();
        ProverEnvironment proverWithUnsatCore =
            solver.newProverEnvironment(
                ProverOptions.GENERATE_MODELS, ProverOptions.GENERATE_UNSAT_CORE)) {
      return consecution(pLevel, pBlock, pState, interpolatingProver, proverWithUnsatCore);
    }
  }

  private <T> ConsecutionResult consecution(
      int pLevel,
      Block pBlock,
      BooleanFormula pConcrete,
      InterpolatingProverEnvironment<T> pConcrProver,
      ProverEnvironment pAbstrProver)
      throws InterruptedException, SolverException {

    CFANode predecessorLocation = pBlock.getPredecessorLocation();
    CFANode successorLocation = pBlock.getSuccessorLocation();
    BooleanFormula concrete = fmgr.uninstantiate(pConcrete);
    BooleanFormula abstracted = abstractionManager.computeAbstraction(successorLocation, concrete);
    List<T> partAforInterpolation = Lists.newLinkedList(); // Will contain F & not(s) & T
    T id;

    // Push F(pLevel, predLoc) [unprimed]
    for (BooleanFormula frameClause : frameSet.getStatesForLocation(predecessorLocation, pLevel)) {
      BooleanFormula unprimedFrameClause = PDRUtils.asUnprimed(frameClause, pBlock, fmgr);
      pAbstrProver.push(unprimedFrameClause);
      id = pConcrProver.push(unprimedFrameClause);
      partAforInterpolation.add(id);
    }

    // Push transition T
    BooleanFormula localTransition = pBlock.getFormula();
    pAbstrProver.push(localTransition);
    id = pConcrProver.push(localTransition);
    partAforInterpolation.add(id);

    // Push not(abstract_state) / not(concrete_state) [unprimed] if self-loop
    boolean isSelfLoop = predecessorLocation.equals(successorLocation);
    if (isSelfLoop) {
      BooleanFormula unprimedNegAbstract = PDRUtils.asUnprimed(bfmgr.not(abstracted), pBlock, fmgr);
      BooleanFormula unprimedNegConcrete = PDRUtils.asUnprimed(bfmgr.not(concrete), pBlock, fmgr);
      id = pConcrProver.push(unprimedNegConcrete);
      pAbstrProver.push(unprimedNegAbstract);
      partAforInterpolation.add(id);
    }

    // Push abstract/concrete state [primed]
    BooleanFormula primedAbstract = PDRUtils.asPrimed(abstracted, pBlock, fmgr);
    BooleanFormula primedConcrete = PDRUtils.asPrimed(concrete, pBlock, fmgr);
    pConcrProver.push(primedConcrete);
    pAbstrProver.push(primedAbstract);

    BooleanFormula toBeBlocked = abstracted;
    boolean abstractFail = !pAbstrProver.isUnsat();
    boolean wasRefined = false;

    if (abstractFail) {
      boolean concreteFail = !pConcrProver.isUnsat();
      if (concreteFail) {

        // Real predecessor exists
        Model model = pAbstrProver.getModel();
        BooleanFormula concretePred = getSatisfyingState(model, pBlock.getUnprimedContext());

        return new ConsecutionResult(false, concretePred); // Return predecessor
      } else {

        /*
         *  Abstract transition possible without concrete counterpart. Abstraction is too broad.
         *  Interpolate and refine abstraction.
         */
        BooleanFormula interpolant = pConcrProver.getInterpolant(partAforInterpolation);
        toBeBlocked =
            abstractionManager // TODO correct ?
                .refineAndComputeAbstraction(successorLocation, concrete, bfmgr.not(interpolant));
        wasRefined = true;

        // Prepare for generalization. Update abstractions of not(s) and s' on prover stack
        if (isSelfLoop) {
          pAbstrProver.pop();
        }
        pAbstrProver.pop();

        if (isSelfLoop) {
          pAbstrProver.push(PDRUtils.asUnprimed(bfmgr.not(toBeBlocked), pBlock, fmgr));
        }
        pAbstrProver.push(PDRUtils.asPrimed(toBeBlocked, pBlock, fmgr));
        boolean unsatAfterRefinement = pAbstrProver.isUnsat(); // Prepare unsat core
        assert unsatAfterRefinement;
      }
    }

    if (!abstractFail || wasRefined) { // Return generalized clause
      BooleanFormula generalized = dropUnusedLiterals(toBeBlocked, pAbstrProver.getUnsatCore());
      return new ConsecutionResult(true, generalized);
    }

    throw new AssertionError("Could neither prove nor disprove inductivity.");
  }

  /**
   * Checks if the propagation of the given formula from the block predecessor to its successor is
   * possible. Note that this doesn't factor in other predecessors of the successor.
   *
   * <p>In short : Is [F(pLevel,pBlock.predLoc) & T(pBlock.predLoc -> pBlock.succLoc) => pFormula]
   * valid?
   *
   * @param pFormula the formula to propagate
   * @param pLevel the level of the frame relative to which propagation is checked
   * @param pBlock the block containing the locations and the transition between them
   * @return true if propagation is possible, false otherwise
   */
  public boolean canPropagate(BooleanFormula pFormula, int pLevel, Block pBlock)
      throws SolverException, InterruptedException {
    CFANode predecessor = pBlock.getPredecessorLocation();
    try (ProverEnvironment prover = solver.newProverEnvironment()) {

      for (BooleanFormula frameClause : frameSet.getStatesForLocation(predecessor, pLevel)) {
        prover.push(PDRUtils.asUnprimed(frameClause, pBlock, fmgr));
      }
      prover.push(pBlock.getFormula());
      prover.push(PDRUtils.asPrimed(bfmgr.not(pFormula), pBlock, fmgr));

      return prover.isUnsat();
    }
  }

  /**
   * Checks whether the first formula implies the second one.
   *
   * @return true if (pF1 => pF2) is a tautology, false otherwise
   */
  public boolean subsumes(BooleanFormula pF1, BooleanFormula pF2)
      throws SolverException, InterruptedException {
    BooleanFormula implicationAsUnsat = bfmgr.not(bfmgr.implication(pF1, pF2));
    return solver.isUnsat(implicationAsUnsat);
  }

  /**
   * Extracts a concrete state from the model. The given context contains the variables to be looked
   * at. The returned formula is a pure conjunction of the form (variable=value). The returned
   * formula is uninstantiated.
   */
  private BooleanFormula getSatisfyingState(Model pModel, PathFormula pUnprimedContext) {
    BooleanFormula satisfyingState = bfmgr.makeTrue();
    // TODO context always unprimed? (see case without not(s))
    for (String variableName : pUnprimedContext.getSsa().allVariables()) {

      // Make variable
      CType type = pUnprimedContext.getSsa().getType(variableName);
      BitvectorFormula unprimedVar =
          (BitvectorFormula)
              pfmgr.makeFormulaForVariable(pUnprimedContext, variableName, type, false);

      // Make value
      BitvectorFormula value =
          fmgr.getBitvectorFormulaManager()
              .makeBitvector(fmgr.getFormulaType(unprimedVar), pModel.evaluate(unprimedVar));

      // Conjoin (variable=value) to existing formula
      satisfyingState =
          bfmgr.and(
              satisfyingState,
              fmgr.getBitvectorFormulaManager().equal(fmgr.uninstantiate(unprimedVar), value));
    }

    return satisfyingState;
  }

  private BooleanFormula dropUnusedLiterals(
      BooleanFormula pFormula, @SuppressWarnings("unused") List<BooleanFormula> pUnsatCore) {
    return pFormula; // TODO implement
  }

  //---------------------------------inner class-----------------------------

  /**
   * Contains information on the result of the call to {@link PDRSat#consecution(int, Block,
   * BooleanFormula)}.
   *
   * <p>Firstly, if it was successful. Secondly, provides a formula whose meaning is related to the
   * success. If it worked, the formula describes a set of states derived from the initial argument
   * to {@link PDRSat#consecution(int, Block, BooleanFormula)}. All those states obey consecution.
   * If it failed, the formula describes a predecessor state of the one given in the call. This
   * predecessor is one reason the state was not inductive.
   */
  public static class ConsecutionResult {

    private final boolean consecutionSuccessful;
    private final BooleanFormula formula;

    private ConsecutionResult(boolean pSuccess, BooleanFormula pFormula) {
      this.consecutionSuccessful = pSuccess;
      this.formula = pFormula;
    }

    /**
     * Returns whether or not the consecution attempt succeeded.
     *
     * @return true if consecution succeeded, false otherwise
     */
    public boolean consecutionSuccess() {
      return consecutionSuccessful;
    }

    /**
     * Returns the result of the consecution attempt. If it succeeded, the returned formula
     * describes a whole set of states that obey consecution. If it failed, the returned formula
     * describes a predecessor state that is responsible for the failure.
     *
     * @return a formula describing either a set of states that passed consecution, or a predecessor
     *     that is one reason for the failure
     */
    public BooleanFormula getResultFormula() {
      return formula;
    }
  }
}
