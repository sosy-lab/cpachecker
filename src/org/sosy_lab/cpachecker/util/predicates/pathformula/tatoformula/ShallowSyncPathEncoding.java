// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TatoFormulaConverter.VariableType;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Implements the shallow sync encoding where formulas are created on paths. Thus, PredicateAnalysis
 * with location is used and the successor formulas are created there.
 */
public class ShallowSyncPathEncoding implements TAFormulaEncoding {
  private final FormulaManagerView fmgr;
  private final CFA cfa;

  private Map<String, Integer> maxSSAIndexOfAction = new HashMap<>();
  private Map<String, BooleanFormula> formulaByAutomata = new HashMap<>();
  private Set<AbstractState> processedTargetStates = new HashSet<>();
  private Set<String> automataWithErrorLocations = new HashSet<>();

  ShallowSyncPathEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    fmgr = pFmgr;
    cfa = pCfa;

    automataWithErrorLocations =
        cfa.getAllNodes().stream()
            .filter(node -> node instanceof TCFANode)
            .map(node -> (TCFANode) node)
            .filter(node -> node.isErrorLocation())
            .map(node -> node.getAutomatonDeclaration().getName())
            .collect(Collectors.toSet());
  }

  @Override
  public BooleanFormula getInitialFormula(CFANode pInitialNode, int pStepCount) {
    return fmgr.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pStepCount, CFAEdge pEdge) {
    return Collections.singleton(pPredecessor);
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pStepCount) {
    return Collections.singleton(pPredecessor);
  }

  @Override
  public BooleanFormula getFormulaFromReachedSet(Iterable<AbstractState> pReachedSet) {
    var targetStates = filterApplicable(pReachedSet).toList();

    if (targetStates.isEmpty()) {
      return fmgr.getBooleanFormulaManager().makeTrue();
    }

    if (formulaByAutomata.isEmpty()) {
      cfa.getAllFunctionNames()
          .forEach(
              automatonName ->
                  formulaByAutomata.put(
                      automatonName, fmgr.getBooleanFormulaManager().makeFalse()));
    }

    // post-process each formula and build formula for each component automaton
    for (AbstractState aState : targetStates) {
      var location = (TCFANode) AbstractStates.extractLocation(aState);
      var allActionsOfAutomaton = location.getAutomatonDeclaration().getActions();
      var automatonName = location.getAutomatonDeclaration().getName();
      var predicateState = AbstractStates.extractStateByType(aState, PredicateAbstractState.class);
      var ssa = predicateState.getPathFormula().getSsa();
      var formula =
          fmgr.getBooleanFormulaManager()
              .and(
                  predicateState.getAbstractionFormula().getBlockFormula().getFormula(),
                  predicateState.getPathFormula().getFormula());

      var formulaSynced =
          getFormulaWithSyncConstraints(formula, allActionsOfAutomaton, automatonName, ssa);

      // add formula to formula that describes the component automaton
      var automatonFormula = formulaByAutomata.get(automatonName);
      formulaByAutomata.put(automatonName, fmgr.makeOr(formulaSynced, automatonFormula));

      processedTargetStates.add(aState);
    }

    var pathEncoding = fmgr.getBooleanFormulaManager().and(formulaByAutomata.values());
    var globalSyncConstraint = getGlobalSyncConstraint(targetStates);

    return fmgr.getBooleanFormulaManager().and(pathEncoding, globalSyncConstraint);
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
      Iterable<TaVariable> allActionsOfAutomaton,
      String automatonName,
      SSAMap ssa) {
    // add constraints for number of action occurences
    for (var action : allActionsOfAutomaton) {
      var localOccurenceCountVarName =
          TatoFormulaConverter.getActionOccurenceVariableName(automatonName, action.getName());
      var localOccurenceCountVar =
          fmgr.makeVariable(
              VariableType.INTEGER.getFormulaType(),
              localOccurenceCountVarName,
              ssa.getIndex(localOccurenceCountVarName));
      var globalOccurenceCountVar =
          fmgr.makeVariableWithoutSSAIndex(
              VariableType.INTEGER.getFormulaType(), action.getName() + "#cnt");
      var actionOccurenceCountFormula =
          fmgr.makeEqual(globalOccurenceCountVar, localOccurenceCountVar);
      formula = fmgr.makeAnd(actionOccurenceCountFormula, formula);
    }

    // add final sync constraint
    var timeVariableName = TatoFormulaConverter.getTimeVariableNameForAutomaton(automatonName);
    var timeVariable =
        fmgr.makeVariable(
            VariableType.FLOAT.getFormulaType(), timeVariableName, ssa.getIndex(timeVariableName));
    var finalSyncAction = fmgr.makeVariable(VariableType.FLOAT.getFormulaType(), "#final_sync");
    var finalSyncConstraint = fmgr.makeEqual(finalSyncAction, timeVariable);
    formula = fmgr.makeAnd(finalSyncConstraint, formula);

    return formula;
  }

  /**
   * Produces a formula that ensures that the i-th occurence of action a happened at the same time
   * among all path formulas.
   */
  private BooleanFormula getGlobalSyncConstraint(Iterable<AbstractState> pTargetStates) {
    for (AbstractState aState : pTargetStates) {
      var predicateState = AbstractStates.extractStateByType(aState, PredicateAbstractState.class);
      var ssa = predicateState.getPathFormula().getSsa();
      for (var action : ssa.allVariables()) {
        var index = ssa.getIndex(action);
        var maxIndex = Math.max(index, maxSSAIndexOfAction.getOrDefault(action, 0));
        maxSSAIndexOfAction.put(action, maxIndex);
      }
    }

    var result = fmgr.getBooleanFormulaManager().makeTrue();
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
                fmgr.makeVariable(
                    VariableType.INTEGER.getFormulaType(), countVarName, variableIndex);
            var globalTimestampVar =
                fmgr.makeVariable(
                    VariableType.FLOAT.getFormulaType(),
                    globalTimestampVarName + numberOfOccurences);
            var localTimestampVar =
                fmgr.makeVariable(
                    VariableType.FLOAT.getFormulaType(), localTimestampVarName, variableIndex);
            var occurenceFormula =
                fmgr.makeNumber(VariableType.INTEGER.getFormulaType(), numberOfOccurences);

            var countVarValueFormula = fmgr.makeEqual(countVar, occurenceFormula);
            var timeStampsEqualFormula = fmgr.makeEqual(globalTimestampVar, localTimestampVar);
            var syncFormula =
                fmgr.getBooleanFormulaManager()
                    .implication(countVarValueFormula, timeStampsEqualFormula);

            result = fmgr.makeAnd(syncFormula, result);
          }
        }
      }
    }

    return result;
  }

  private FluentIterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
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
