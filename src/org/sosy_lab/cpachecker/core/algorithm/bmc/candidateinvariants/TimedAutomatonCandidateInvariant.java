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

    Map<String, Integer> maxVariableIndex = new HashMap<>();
    for (PredicateAbstractState e :
        AbstractStates.projectToType(targetStates, PredicateAbstractState.class)) {
      var ssa = e.getPathFormula().getSsa();
      for (var variable : ssa.allVariables()) {
        maxVariableIndex.put(
            variable, Math.max(ssa.getIndex(variable), maxVariableIndex.getOrDefault(variable, 0)));
      }
    }

    var minusOne = pFMGR.makeNumber(TatoFormulaConverter.getClockVariableType(), -1);
    var finalSyncAction =
        pFMGR.makeVariable(TatoFormulaConverter.getClockVariableType(), "#final_sync");
    for (AbstractState aState : targetStates) {
      var location = (TCFANode) AbstractStates.extractLocation(aState);
      var automatonActions = location.getAutomatonDeclaration().getActions();
      var automatonName = location.getAutomatonDeclaration().getName();
      var predicateState = AbstractStates.extractStateByType(aState, PredicateAbstractState.class);
      var ssa = predicateState.getPathFormula().getSsa();
      var formula =
          pFMGR
              .getBooleanFormulaManager()
              .and(
                  predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
                  predicateState.getPathFormula().getFormula());

      // add constraints for action that are not present in the formula
      for (var action : automatonActions) {
        for (int nextIndex = ssa.getIndex(action) + 1;
            nextIndex <= maxVariableIndex.get(action);
            nextIndex++) {
          var actionVariable =
              pFMGR.makeVariable(TatoFormulaConverter.getClockVariableType(), action, nextIndex);
          var actionEqMinusOne = pFMGR.makeEqual(actionVariable, minusOne);
          formula = pFMGR.makeAnd(actionEqMinusOne, formula);
        }
      }

      // add final sync constraint
      var timeVariableName = TatoFormulaConverter.getTimeVariableNameForAutomaton(automatonName);
      var timeVariable =
          pFMGR.makeVariable(
              TatoFormulaConverter.getClockVariableType(),
              timeVariableName,
              ssa.getIndex(timeVariableName));
      var finalSyncConstraint = pFMGR.makeEqual(finalSyncAction, timeVariable);
      formula = pFMGR.makeAnd(finalSyncConstraint, formula);

      // add formula to formula that describes the component automaton
      var automatonFormula =
          formulaByAutomata.getOrDefault(
              automatonName, pFMGR.getBooleanFormulaManager().makeFalse());
      formulaByAutomata.put(automatonName, pFMGR.makeOr(formula, automatonFormula));
    }

    return pFMGR.makeNot(pFMGR.getBooleanFormulaManager().and(formulaByAutomata.values()));
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
