/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager.Tactic;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class FormulaSlicingManager implements IFormulaSlicingManager {
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManager bfmgr;
  private final Solver solver;


  public FormulaSlicingManager(PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, Solver pSolver) {
    pfmgr = pPfmgr;
    fmgr = pFmgr;
    solver = pSolver;
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  public BooleanFormula slice(PathFormula input, PathFormula transition) {

    // Step 1: get rid of intermediate variables in "input".

    // todo(optional): add quantifiers next to intermediate variables,
    // perform quantification, run QE_LIGHT to remove the ones we can.

    // ...remove atoms containing intermediate variables.
    BooleanFormula noIntermediate = SlicingPreprocessor
        .of(fmgr, input.getSsa()).visit(input.getFormula());

    BooleanFormula noIntermediateNNF = bfmgr.applyTactic(noIntermediate,
        Tactic.NNF);

    // Step 2: Annotate conjunctions.
    Set<BooleanFormula> selectionVars = new HashSet<>();
    BooleanFormula annotated = ConjunctionAnnotator.of(fmgr, selectionVars).visit(
        noIntermediateNNF);


    // This is possible since the formula does not have any intermediate
    // variables, hence the whole renaming would work just as expected.
    BooleanFormula primed =
        fmgr.instantiate(fmgr.uninstantiate(annotated),
            transition.getSsa());
    BooleanFormula negated = bfmgr.not(primed);

    // Inductiveness checking formula.
    BooleanFormula check = bfmgr.and(ImmutableList.of(annotated,
        transition.getFormula(),
        negated));

    return null;
  }

  /**
   * @param selectionVars List of selection variables.
   *    The order is very important and determines which MUS we will get out.
   *
   * @return An assignment to boolean variables:
   *         returned as a set of abstracted {@code selectionVars}
   */
  private Set<BooleanFormula> formulaSlicing(
      List<BooleanFormula> selectionVars,
      BooleanFormula query
  ) throws SolverException, InterruptedException {

    List<BooleanFormula> selection = selectionVars;


    try (ProverEnvironment env = solver.newProverEnvironment()) {
      env.push(query);

      // Make everything abstracted.
      BooleanFormula selectionFormula = bfmgr.and(selection);
      env.push(selectionFormula);
      Verify.verify(env.isUnsat());


      while (true) {
        // todo: this is actually not that easy to implement with the current
        // interface.

        // Remove the selection constraint.
        env.pop();

        int i = 0;
        boolean removed = false;
        for (BooleanFormula selVar : selectionVars) {

          // Remove this variable from the selection.
          List<BooleanFormula> newSelection = Lists.newArrayList(selectionVars);
          newSelection.remove(i);

          env.push(bfmgr.and(newSelection));
          i++;

          if (env.isUnsat()) {
            // Still unsat: keep that element non-abstracted.
            selection = newSelection;
            removed = true;
            break; // break out of the variable selection loop.
          } else {

            // Try to abstract away some other element.
            continue;
          }
        }

        if (!removed) {
          break;
        }
      }
    }


    return new HashSet<>(selection);
  }

  private static class SlicingPreprocessor
      extends BooleanFormulaTransformationVisitor {
    private final SSAMap finalSSA;
    private final FormulaManagerView fmgr;

    protected SlicingPreprocessor(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache, SSAMap pFinalSSA) {
      super(pFmgr, pCache);
      finalSSA = pFinalSSA;
      fmgr = pFmgr;
    }

    public static SlicingPreprocessor of(FormulaManagerView fmgr,
        SSAMap ssa) {
      return new SlicingPreprocessor(fmgr,
          new HashMap<BooleanFormula, BooleanFormula>(), ssa);
    }

    /**
     * Replace all atoms containing intermediate variables with "true".
     */
    @Override
    protected BooleanFormula visitAtom(BooleanFormula atom) {

      // todo: this does not deal with UFs.
      if (!fmgr.getDeadVariableNames(atom, finalSSA).isEmpty()) {
        return fmgr.getBooleanFormulaManager().makeBoolean(true);
      }
      return atom;
    }
  }

  /**
   * (and a_1 a_2 a_3 ...)
   * -> gets converted to ->
   * (and (or p_1 a_1) ...)
   */
  private static class ConjunctionAnnotator
      extends BooleanFormulaTransformationVisitor {
    private final UniqueIdGenerator controllerIdGenerator =
        new UniqueIdGenerator();
    private final BooleanFormulaManager bfmgr;
    private final Set<BooleanFormula> selectionVars;

    private static final String PROP_VAR = "_FS_PROP_";

    protected ConjunctionAnnotator(
        FormulaManagerView pFmgr,
        Map<BooleanFormula, BooleanFormula> pCache,
        Set<BooleanFormula> pSelectionVars) {
      super(pFmgr, pCache);
      bfmgr = pFmgr.getBooleanFormulaManager();
      selectionVars = pSelectionVars;
    }

    public static ConjunctionAnnotator of(FormulaManagerView pFmgr,
        Set<BooleanFormula> selectionVars) {
      return new ConjunctionAnnotator(pFmgr,
          new HashMap<BooleanFormula, BooleanFormula>(),
          selectionVars);
    }

    @Override
    protected BooleanFormula visitAnd(BooleanFormula... pOperands) {
      List<BooleanFormula> args = new ArrayList<>(pOperands.length);
      for (BooleanFormula arg : pOperands) {
        BooleanFormula controller = makeFreshSelector();
        args.add(bfmgr.or(controller, arg));
      }
      return bfmgr.and(args);
    }

    private BooleanFormula makeFreshSelector() {
      BooleanFormula selector = bfmgr
          .makeVariable(PROP_VAR + controllerIdGenerator.getFreshId());
      selectionVars.add(selector);
      return selector;
    }
  }

  /** ... utility stuff below ... **/

  @Override
  public FormulaSlicingState join(FormulaSlicingState oldState,
      FormulaSlicingState newState) throws CPAException, InterruptedException {
    return null;
  }

  @Override
  public Collection<? extends FormulaSlicingState> getAbstractSuccessors(
      FormulaSlicingState state, CFAEdge edge)
      throws CPAException, InterruptedException {
    return null;
  }

  @Override
  public Collection<? extends FormulaSlicingState> strengthen(
      FormulaSlicingState state, List<AbstractState> otherState,
      CFAEdge pCFAEdge) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  public FormulaSlicingState getInitialState(CFANode node) {
    return null;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(FormulaSlicingState state,
      UnmodifiableReachedSet states, AbstractState pArgState)
      throws CPAException, InterruptedException {
    return null;
  }

  @Override
  public boolean isLessOrEqual(FormulaSlicingState pState1,
      FormulaSlicingState pState2) {
    return false;
  }
}
