// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.ExplicitTimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class ShallowSyncEncoding extends EncodingExtensionBase {
  private final ExplicitTimeEncoding time;
  private final ActionEncoding actions;
  private final FormulaType<?> timeFormulaType;
  private static final FormulaType<?> countFormulaType = FormulaType.IntegerType;
  private final TimedAutomatonView automata;

  public ShallowSyncEncoding(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      ExplicitTimeEncoding pTime,
      ActionEncoding pActions) {
    super(pFmgr);
    time = pTime;
    actions = pActions;
    timeFormulaType = time.getTimeFormulaType();
    automata = pAutomata;
  }

  private Formula makeOccurenceCountFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    var variableName = "occurence_count#" + pVariable.getName() + "#" + pAutomaton.getName();
    return fmgr.makeVariable(countFormulaType, variableName, pVariableIndex);
  }

  private BooleanFormula makeOccurenceCountEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable, int pCount) {
    var occurenceCountVarFormula = makeOccurenceCountFormula(pAutomaton, pVariableIndex, pVariable);
    var valueFormula = fmgr.makeNumber(countFormulaType, pCount);
    return fmgr.makeEqual(occurenceCountVarFormula, valueFormula);
  }

  private BooleanFormula makeOccurenceCountIncreaseFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable pVariable) {
    var occurenceCountVarBefore =
        makeOccurenceCountFormula(pAutomaton, pLastReachedIndex, pVariable);
    var occurenceCountVarAfter =
        makeOccurenceCountFormula(pAutomaton, pLastReachedIndex + 1, pVariable);
    var one = fmgr.makeNumber(countFormulaType, 1);
    return fmgr.makeEqual(occurenceCountVarAfter, fmgr.makePlus(occurenceCountVarBefore, one));
  }

  private Formula makeOccurenceTimeFormula(int pVariableIndex, TaVariable pVariable) {
    var variableName = "occurence_time#" + pVariable.getName();
    return fmgr.makeVariable(timeFormulaType, variableName, pVariableIndex);
  }

  private BooleanFormula makeOccurenceCountUnchangedFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable pAction) {
    var actionCountBefore = makeOccurenceCountFormula(pAutomaton, pLastReachedIndex, pAction);
    var actionCountAfter = makeOccurenceCountFormula(pAutomaton, pLastReachedIndex + 1, pAction);
    return fmgr.makeEqual(actionCountAfter, actionCountBefore);
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    for (var action : automata.getActionsByAutomaton(pAutomaton)) {
      var actionEqualsFormula =
          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);
      var occurenceCountFormulas = bFmgr.makeTrue();
      for (int actionOccurences = 1;
          actionOccurences <= pLastReachedIndex + 1;
          actionOccurences++) {
        var occurenceCount =
            makeOccurenceCountEqualsFormula(
                pAutomaton, pLastReachedIndex + 1, action, actionOccurences);
        var occurenceTimeVariable = makeOccurenceTimeFormula(pLastReachedIndex + 1, action);
        var occurenceTimeFormula =
            time.makeTimeEqualsVariable(pAutomaton, pLastReachedIndex, occurenceTimeVariable);
        occurenceCountFormulas =
            bFmgr.and(
                bFmgr.implication(occurenceCount, occurenceTimeFormula), occurenceCountFormulas);
      }

      result = bFmgr.and(bFmgr.implication(actionEqualsFormula, occurenceCountFormulas), result);
    }

    for (var action : automata.getActionsByAutomaton(pAutomaton)) {
      var actionOccurs = actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);
      var unchanged = makeOccurenceCountUnchangedFormula(pAutomaton, pLastReachedIndex, action);
      var increse = makeOccurenceCountIncreaseFormula(pAutomaton, pLastReachedIndex, action);

      var ifAction = bFmgr.implication(actionOccurs, increse);
      var ifNoAction = bFmgr.implication(bFmgr.not(actionOccurs), unchanged);

      result = bFmgr.and(ifAction, ifNoAction, result);
    }

    return result;
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    var initialCounts =
        from(automata.getActionsByAutomaton(pAutomaton))
            .transform(
                action -> makeOccurenceCountEqualsFormula(pAutomaton, pInitialIndex, action, 0));
    return bFmgr.and(initialCounts.toSet());
  }

  @Override
  public BooleanFormula makeFinalConditionForAutomaton(
      TaDeclaration pAutomaton, int pHighestReachedIndex) {

    var occurenceCounts = bFmgr.makeTrue();
    for (var action : automata.getActionsByAutomaton(pAutomaton)) {
      var localOccurenceCount = makeOccurenceCountFormula(pAutomaton, pHighestReachedIndex, action);
      var globalOccurenceCount = makeFinalOccurenceCountFormula(action);
      occurenceCounts =
          bFmgr.and(fmgr.makeEqual(globalOccurenceCount, localOccurenceCount), occurenceCounts);
    }

    var finalTime = makeFinalTimeFormula();
    return bFmgr.and(
        occurenceCounts, time.makeTimeEqualsVariable(pAutomaton, pHighestReachedIndex, finalTime));
  }

  private Formula makeFinalOccurenceCountFormula(TaVariable pVariable) {
    var variableName = "occurence_count_final#" + pVariable.getName();
    return fmgr.makeVariable(countFormulaType, variableName);
  }

  private Formula makeFinalTimeFormula() {
    var variableName = "#final_time";
    return fmgr.makeVariable(timeFormulaType, variableName);
  }
}
