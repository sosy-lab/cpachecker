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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

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
  private final LogManager logger;
  private final CFA cfa;
  private final LoopTransitionFinder loopTransitionFinder;


  public FormulaSlicingManager(PathFormulaManager pPfmgr,
      FormulaManagerView pFmgr, Solver pSolver, LogManager pLogger, CFA pCfa,
      LoopTransitionFinder pLoopTransitionFinder) {
    pfmgr = pPfmgr;
    fmgr = pFmgr;
    solver = pSolver;
    logger = pLogger;
    cfa = pCfa;
    loopTransitionFinder = pLoopTransitionFinder;
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
  public Collection<? extends SlicingState> getAbstractSuccessors(
      SlicingState oldState, CFAEdge edge)
      throws CPATransferException, InterruptedException {

    SlicingIntermediateState iOldState;

    if (oldState.isAbstracted()) {
      iOldState = abstractStateToIntermediate(oldState.asAbstracted());
    } else {
      iOldState = oldState.asIntermediate();
    }

    PathFormula outPath = pfmgr.makeAnd(iOldState.getPathFormula(), edge);

    SlicingIntermediateState out = SlicingIntermediateState.of(
        outPath, iOldState.getAbstraction());

    return Collections.singleton(out);
  }

  @Override
  public SlicingState join(SlicingState newState,
      SlicingState oldState) throws CPAException, InterruptedException {
    Preconditions.checkState(oldState.isAbstracted() == newState.isAbstracted());

    SlicingState out;
    if (oldState.isAbstracted()) {
      out = joinAbstractedStates(oldState.asAbstracted(),
          newState.asAbstracted());
    } else {
      out = joinIntermediateStates(oldState.asIntermediate(),
          newState.asIntermediate());
    }

    return out;
  }

  /**
   * Slicing is performed in the {@code strengthen} call.
   */
  @Override
  public Collection<? extends SlicingState> strengthen(
      SlicingState state, List<AbstractState> otherState,
      CFAEdge pCFAEdge) throws CPATransferException, InterruptedException {

    CFANode successor = pCFAEdge.getSuccessor();

    if (shouldPerformAbstraction(successor)) {
      if (shouldPerformSlicing(pCFAEdge)) {

        // todo: this is the place we perform formula slicing!
        return null;

      } else {

        // Would it work properly?? Returning bottom when no abstraction is
        // necessary.
        return Collections.emptySet();
      }

    } else {
      return Collections.singleton(state);
    }
  }

  @Override
  public SlicingState getInitialState(CFANode node) {
    return SlicingAbstractedState.empty(bfmgr);
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(SlicingState state,
      UnmodifiableReachedSet states, AbstractState fullState)
      throws CPAException, InterruptedException {

    // todo: remove, we perform slicing in {@code strengthen} instead.
    return null;
  }


  @Override
  public boolean isLessOrEqual(SlicingState pState1,
      SlicingState pState2) {
    Preconditions.checkState(pState1.isAbstracted() == pState2.isAbstracted());

    if (!pState1.isAbstracted()) {

      // Use the coverage relation for intermediate states.
      SlicingIntermediateState iState1 = pState1.asIntermediate();
      SlicingIntermediateState iState2 = pState2.asIntermediate();
      return iState1.isMergedInto(iState2) &&
          iState1.getAbstraction().equals(iState2.getAbstraction());
    } else {
      // todo: Comparison for abstracted states?
      // Do we need to deal with "false" and "true" values explicitly?
      // Namely, "true" is the biggest, "false" is the smallest.
      if (pState1.asAbstracted().getAbstraction().equals(bfmgr.makeBoolean(false))) {

        // False is the smallest.
        // todo: would AST comparison work though?
        return true;
      }
      return false;
    }
  }

  private SlicingState joinIntermediateStates(
      SlicingIntermediateState newState,
      SlicingIntermediateState oldState) throws InterruptedException {
    Preconditions.checkState(newState.getAbstraction().equals(
        oldState.getAbstraction()));

    if (newState.isMergedInto(oldState)) {
      return oldState;
    } else if (oldState.isMergedInto(newState)) {
      return newState;
    }

    if (oldState.getPathFormula().equals(newState.getPathFormula())) {
      return newState;
    }
    PathFormula mergedPath = pfmgr.makeOr(newState.getPathFormula(),
        oldState.getPathFormula());

    SlicingIntermediateState out = SlicingIntermediateState.of(
        mergedPath, oldState.getAbstraction()
    );
    newState.setMergedInto(out);
    oldState.setMergedInto(out);
    return out;
  }

  private SlicingState joinAbstractedStates(
      SlicingAbstractedState newState,
      SlicingAbstractedState oldState) throws InterruptedException {

    logger.log(Level.INFO, "Joining abstracted states",
        "this should be quite rare");

    // HM what if I'll just set to bottom the generated abstract state if it
    // comes from within the loop? That would avoid the given problem.

    // Merging PointerTargetSet.
    PathFormula p1 = abstractStateToIntermediate(newState).getPathFormula();
    PathFormula p2 = abstractStateToIntermediate(oldState).getPathFormula();
    PointerTargetSet merged = pfmgr.makeOr(p1, p2).getPointerTargetSet();

    return SlicingAbstractedState.of(
        bfmgr.or(newState.getAbstraction(), oldState.getAbstraction()),
        newState.getSSA(), // arbitrary
        merged
    );
  }


  private SlicingIntermediateState abstractStateToIntermediate(
      SlicingAbstractedState pSlicingAbstractedState) {
    return SlicingIntermediateState.of(
        new PathFormula(
            bfmgr.makeBoolean(true),
            pSlicingAbstractedState.getSSA(),
            pSlicingAbstractedState.getPointerTargetSet(),
            0), pSlicingAbstractedState);
  }

  /**
   * <=>
   * 1) Target is a loop-head.
   * 2) {@code edge} is NOT a back edge.
   */
  private boolean shouldPerformSlicing(CFAEdge edge) {
    CFANode succ = edge.getSuccessor();
    return shouldPerformAbstraction(succ)
        && !loopTransitionFinder.getEdgesInLoop(succ).contains(edge);
  }

  private boolean shouldPerformAbstraction(CFANode node) {

    // Slicing is only performed on the loop heads.
    return cfa.getLoopStructure().get().getAllLoopHeads().contains(node);
  }


}
