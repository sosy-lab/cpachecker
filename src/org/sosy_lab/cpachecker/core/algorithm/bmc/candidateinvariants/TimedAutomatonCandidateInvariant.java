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
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TatoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TatoFormulaConverter.VariableType;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Candidate invariant that represents the formula encoding of a timed automaton network. */
public class TimedAutomatonCandidateInvariant implements CandidateInvariant {
  private final CFA cfa;
  private Map<String, Integer> maxSSAIndexOfAction = new HashMap<>();
  private Map<String, BooleanFormula> formulaByAutomata = new HashMap<>();
  private Set<AbstractState> processedTargetStates = new HashSet<>();
  private Set<String> automataWithErrorLocations = new HashSet<>();

  private static TimedAutomatonCandidateInvariant instance;

  public static TimedAutomatonCandidateInvariant getInstance(CFA pCFA) {
    if (instance == null) {
      instance = new TimedAutomatonCandidateInvariant(pCFA);
    }
    return instance;
  }

  private TimedAutomatonCandidateInvariant(CFA pCFA) {
    cfa = pCFA;
    automataWithErrorLocations =
        cfa.getAllNodes().stream()
            .filter(node -> node instanceof TCFANode)
            .map(node -> (TCFANode) node)
            .filter(node -> node.isErrorLocation())
            .map(node -> node.getAutomatonDeclaration().getName())
            .collect(Collectors.toSet());
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext) {
    return pFMGR.getBooleanFormulaManager().makeFalse();
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws InterruptedException {
    var targetStates = filterApplicable(pReachedSet).toList();

    if (targetStates.isEmpty()) {
      return pFMGR.getBooleanFormulaManager().makeTrue();
    }

    if (formulaByAutomata.isEmpty()) {
      cfa.getAllFunctionNames()
          .forEach(
              automatonName ->
                  formulaByAutomata.put(
                      automatonName, pFMGR.getBooleanFormulaManager().makeFalse()));
    }

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
      var automatonFormula = formulaByAutomata.get(automatonName);
      formulaByAutomata.put(automatonName, pFMGR.makeOr(formulaSynced, automatonFormula));

      processedTargetStates.add(aState);
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
      Iterable<TaVariable> allActionsOfAutomaton,
      String automatonName,
      SSAMap ssa) {
    // add constraints for number of action occurences
    for (var action : allActionsOfAutomaton) {
      var localOccurenceCountVarName =
          TatoFormulaConverter.getActionOccurenceVariableName(automatonName, action.getName());
      var localOccurenceCountVar =
          pFMGR.makeVariable(
              VariableType.INTEGER.getFormulaType(),
              localOccurenceCountVarName,
              ssa.getIndex(localOccurenceCountVarName));
      var globalOccurenceCountVar =
          pFMGR.makeVariableWithoutSSAIndex(
              VariableType.INTEGER.getFormulaType(), action.getName() + "#cnt");
      var actionOccurenceCountFormula =
          pFMGR.makeEqual(globalOccurenceCountVar, localOccurenceCountVar);
      formula = pFMGR.makeAnd(actionOccurenceCountFormula, formula);
    }

    // add final sync constraint
    var timeVariableName = TatoFormulaConverter.getTimeVariableNameForAutomaton(automatonName);
    var timeVariable =
        pFMGR.makeVariable(
            VariableType.FLOAT.getFormulaType(), timeVariableName, ssa.getIndex(timeVariableName));
    var finalSyncAction = pFMGR.makeVariable(VariableType.FLOAT.getFormulaType(), "#final_sync");
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
    for (var automaton : formulaByAutomata.keySet()) {
      var automatonDelcaration =
          (TaDeclaration) cfa.getAllFunctions().get(automaton).getFunctionDefinition();
      var actionsOfAutomaton =
          automatonDelcaration.getActions().stream()
              .map(TaVariable::getName)
              .collect(Collectors.toSet());
      var actions = Sets.intersection(actionsOfAutomaton, maxSSAIndexOfAction.keySet());
      for (var action : actions) {
        for (int variableIndex = 0;
            variableIndex <= maxSSAIndexOfAction.get(action);
            variableIndex++) {
          for (int numberOfOccurences = 0;
              numberOfOccurences <= variableIndex;
              numberOfOccurences++) {
            var countVarName =
                TatoFormulaConverter.getActionOccurenceVariableName(automaton, action);
            var globalTimestampVarName = action + "#occurence#";
            var localTimestampVarName =
                TatoFormulaConverter.getActionVariableName(automaton, action);

            var countVar =
                pFMGR.makeVariable(
                    VariableType.INTEGER.getFormulaType(), countVarName, variableIndex);
            var globalTimestampVar =
                pFMGR.makeVariable(
                    VariableType.FLOAT.getFormulaType(),
                    globalTimestampVarName + numberOfOccurences);
            var localTimestampVar =
                pFMGR.makeVariable(
                    VariableType.FLOAT.getFormulaType(), localTimestampVarName, variableIndex);
            var occurenceFormula =
                pFMGR.makeNumber(VariableType.INTEGER.getFormulaType(), numberOfOccurences);

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
    return;
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
    return from(pStates).filter(this::isTargetState);
  }

  private boolean isTargetState(AbstractState aState) {
    if (processedTargetStates.contains(aState)) {
      return false;
    }

    var location = AbstractStates.extractLocation(aState);
    if (!(location instanceof TCFANode)) {
      return false;
    }

    var tcfaLocation = (TCFANode) location;
    var automatonName = tcfaLocation.getAutomatonDeclaration().getName();
    var hasAutomatonErrorStates = automataWithErrorLocations.contains(automatonName);

    return !hasAutomatonErrorStates || tcfaLocation.isErrorLocation();
  }
}
