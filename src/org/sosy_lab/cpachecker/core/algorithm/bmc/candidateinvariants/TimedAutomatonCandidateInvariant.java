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
package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TatoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Candidate invariant that represents the formula encoding of a timed automaton network. */
public enum TimedAutomatonCandidateInvariant implements CandidateInvariant {
  INSTANCE;

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext) {
    return pFMGR.getBooleanFormulaManager().makeFalse();
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws InterruptedException {
    Map<String, BooleanFormula> formulaByAutomata = new HashMap<>();
    Iterable<AbstractState> targetStates = filterApplicable(pReachedSet);

    // post-process each formula and build formula for each component automaton
    for (AbstractState aState : targetStates) {
      var location = (TCFANode) AbstractStates.extractLocation(aState);
      var allActionsOfAutomaton = location.getAutomatonDeclaration().getActions();
      var automatonName = location.getAutomatonDeclaration().getName();
      var predicateState = AbstractStates.extractStateByType(aState, PredicateAbstractState.class);
      var ssa = predicateState.getPathFormula().getSsa();
      var formula =
          pFMGR
              .getBooleanFormulaManager()
              .and(
                  predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
                  predicateState.getPathFormula().getFormula());

      var formulaSynced =
          getFormulaWithSyncConstraints(formula, pFMGR, allActionsOfAutomaton, automatonName, ssa);

      // add formula to formula that describes the component automaton
      var automatonFormula =
          formulaByAutomata.getOrDefault(
              automatonName, pFMGR.getBooleanFormulaManager().makeFalse());
      formulaByAutomata.put(automatonName, pFMGR.makeOr(formulaSynced, automatonFormula));
    }

    return pFMGR.makeNot(pFMGR.getBooleanFormulaManager().and(formulaByAutomata.values()));
  }

  /**
   * Adds constraints to the formula which ensure that the conjunction of formulas of the component
   * automata represent a consistent run in the product automaton: Number of occurence of each
   * action: Ensures that shared actions are executed equally often by each component automaton.
   * Final sync: Ensures that local clocks are synchronized (and thus consistent) in all final
   * states.
   */
  private BooleanFormula getFormulaWithSyncConstraints(
      BooleanFormula formula,
      FormulaManagerView pFMGR,
      Iterable<String> allActionsOfAutomaton,
      String automatonName,
      SSAMap ssa) {
    // add constraints for number of action occurences
    for (var action : allActionsOfAutomaton) {
      var actionOccurenceCountVar =
          pFMGR.makeVariableWithoutSSAIndex(
              TatoFormulaConverter.getClockVariableType(), action + "#cnt");
      var actionOccurenceCountVal =
          pFMGR.makeNumber(TatoFormulaConverter.getClockVariableType(), ssa.getIndex(action));
      var actionOccurenceCountFormula =
          pFMGR.makeEqual(actionOccurenceCountVar, actionOccurenceCountVal);
      formula = pFMGR.makeAnd(actionOccurenceCountFormula, formula);
    }

    // add final sync constraint
    var timeVariableName = TatoFormulaConverter.getTimeVariableNameForAutomaton(automatonName);
    var timeVariable =
        pFMGR.makeVariable(
            TatoFormulaConverter.getClockVariableType(),
            timeVariableName,
            ssa.getIndex(timeVariableName));
    var finalSyncAction =
        pFMGR.makeVariable(TatoFormulaConverter.getClockVariableType(), "#final_sync");
    var finalSyncConstraint = pFMGR.makeEqual(finalSyncAction, timeVariable);
    formula = pFMGR.makeAnd(finalSyncConstraint, formula);

    return formula;
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    Iterable<AbstractState> targetStates = filterApplicable(pReachedSet).toList();
    pReachedSet.removeAll(targetStates);
    for (ARGState s : from(targetStates).filter(ARGState.class)) {
      s.removeFromARG();
    }
  }

  @Override
  public String toString() {
    return "No target locations reachable";
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return true;
  }

  @Override
  public FluentIterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return from(pStates).filter(AbstractStates.IS_TARGET_STATE);
  }
}
