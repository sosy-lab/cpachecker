// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Streams;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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

    var pathEncoding = pFMGR.getBooleanFormulaManager().and(formulaByAutomata.values());
    var globalSyncConstraint = getGlobalSyncConstraint(targetStates, pFMGR);

    return pFMGR.makeNot(pFMGR.getBooleanFormulaManager().and(pathEncoding, globalSyncConstraint));
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
      var localOccurenceCountVarName =
          TatoFormulaConverter.getActionOccurenceVariableName(automatonName, action);
      var localOccurenceCountVar =
          pFMGR.makeVariable(
              TatoFormulaConverter.getIntegerVariableType(),
              localOccurenceCountVarName,
              ssa.getIndex(localOccurenceCountVarName));
      var globalOccurenceCountVar =
          pFMGR.makeVariableWithoutSSAIndex(
              TatoFormulaConverter.getIntegerVariableType(), action + "#cnt");
      var actionOccurenceCountFormula =
          pFMGR.makeEqual(globalOccurenceCountVar, localOccurenceCountVar);
      formula = pFMGR.makeAnd(actionOccurenceCountFormula, formula);
    }

    // add final sync constraint
    var timeVariableName = TatoFormulaConverter.getTimeVariableNameForAutomaton(automatonName);
    var timeVariable =
        pFMGR.makeVariable(
            TatoFormulaConverter.getDecimalVariableType(),
            timeVariableName,
            ssa.getIndex(timeVariableName));
    var finalSyncAction =
        pFMGR.makeVariable(TatoFormulaConverter.getDecimalVariableType(), "#final_sync");
    var finalSyncConstraint = pFMGR.makeEqual(finalSyncAction, timeVariable);
    formula = pFMGR.makeAnd(finalSyncConstraint, formula);

    return formula;
  }

  /**
   * Produces a formula that ensures that the i-th occurence of action a happened at the same time
   * among all path formulas.
   */
  private BooleanFormula getGlobalSyncConstraint(
      Iterable<AbstractState> pTargetStates, FormulaManagerView pFMGR) {
    Map<String, Integer> maxSSAIndexOfAction = new HashMap<>();
    for (AbstractState aState : pTargetStates) {
      var predicateState = AbstractStates.extractStateByType(aState, PredicateAbstractState.class);
      var ssa = predicateState.getPathFormula().getSsa();
      for (var action : ssa.allVariables()) {
        var index = ssa.getIndex(action);
        var maxIndex = Math.max(index, maxSSAIndexOfAction.getOrDefault(action, 0));
        maxSSAIndexOfAction.put(action, maxIndex);
      }
    }

    var result = pFMGR.getBooleanFormulaManager().makeTrue();
    var allRelevantAutomata =
        Streams.stream(pTargetStates)
            .map(aState -> (TCFANode) AbstractStates.extractLocation(aState))
            .map(tcfaNode -> tcfaNode.getAutomatonDeclaration())
            .collect(Collectors.toSet());
    for (var automaton : allRelevantAutomata) {
      var actions = automaton.getActions();
      for (var action : actions) {
        for (int variableIndex = 0;
            variableIndex <= maxSSAIndexOfAction.get(action);
            variableIndex++) {
          for (int numberOfOccurences = 0;
              numberOfOccurences <= variableIndex;
              numberOfOccurences++) {
            var countVarName =
                TatoFormulaConverter.getActionOccurenceVariableName(automaton.getName(), action);
            var globalTimestampVarName = action + "#occurence#";
            var localTimestampVarName =
                TatoFormulaConverter.getActionVariableName(automaton.getName(), action);

            var countVar =
                pFMGR.makeVariable(
                    TatoFormulaConverter.getIntegerVariableType(), countVarName, variableIndex);
            var globalTimestampVar =
                pFMGR.makeVariable(
                    TatoFormulaConverter.getDecimalVariableType(),
                    globalTimestampVarName + numberOfOccurences);
            var localTimestampVar =
                pFMGR.makeVariable(
                    TatoFormulaConverter.getDecimalVariableType(),
                    localTimestampVarName,
                    variableIndex);
            var occurenceFormula =
                pFMGR.makeNumber(TatoFormulaConverter.getIntegerVariableType(), numberOfOccurences);

            var countVarValueFormula = pFMGR.makeEqual(countVar, occurenceFormula);
            var timeStampsEqualFormula = pFMGR.makeEqual(globalTimestampVar, localTimestampVar);
            var syncFormula =
                pFMGR
                    .getBooleanFormulaManager()
                    .implication(countVarValueFormula, timeStampsEqualFormula);

            result = pFMGR.makeAnd(syncFormula, result);
          }
        }
      }
    }

    return result;
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
