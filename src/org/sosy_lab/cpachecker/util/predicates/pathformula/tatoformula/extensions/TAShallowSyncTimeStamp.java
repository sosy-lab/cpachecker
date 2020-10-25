// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import static com.google.common.collect.FluentIterable.from;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class TAShallowSyncTimeStamp extends TAEncodingExtensionBase {
  private final TAVariables time;
  private final TAActions actions;
  private final FormulaType<?> countFormulaType;
  private final FormulaType<?> occurenceStepFormulaType;
  private final TimedAutomatonView automata;
  private Map<TaDeclaration, TaVariable> timeStampVariables;

  private static final String LOCAL_TIMESTAMP_VARIABLE = "#local_timestamp";

  public TAShallowSyncTimeStamp(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TAActions pActions) {
    super(pFmgr);
    time = pTime;
    actions = pActions;
    automata = pAutomata;
    countFormulaType = FormulaType.IntegerType;
    occurenceStepFormulaType = FormulaType.IntegerType;
    timeStampVariables = new HashMap<>();
    addLocalTimeStampVariables();
  }

  private void addLocalTimeStampVariables() {
    automata
        .getAllAutomata()
        .forEach(
            automaton -> {
              var clockVariable =
                  automata.addClockToAutomaton(
                      automaton, LOCAL_TIMESTAMP_VARIABLE + "#" + automaton.getName());
              timeStampVariables.put(automaton, clockVariable);
            });
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var actionOccurenceFormulas =
        from(automata.getSharedActionsByAutomaton(pAutomaton))
            .transform(action -> makeActionOccurenceFormula(pAutomaton, pLastReachedIndex, action));
    var actionOccurencesFormula = bFmgr.and(actionOccurenceFormulas.toSet());

    var counterStepFormulas =
        from(automata.getSharedActionsByAutomaton(pAutomaton))
            .transform(action -> makeCounterStepFormula(pAutomaton, pLastReachedIndex, action));
    var counterStepsFormula = bFmgr.and(counterStepFormulas.toSet());

    return bFmgr.and(actionOccurencesFormula, counterStepsFormula);
  }

  private BooleanFormula makeActionOccurenceFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable action) {
    var actionTimeStampFormulas =
        IntStream.rangeClosed(1, pLastReachedIndex + 1)
            .mapToObj(
                occurenceCount ->
                    makeActionTimeStampForOccurenceFormula(
                        pAutomaton, pLastReachedIndex, action, occurenceCount));
    var actionTimeStampsFormula = bFmgr.and(actionTimeStampFormulas.collect(Collectors.toSet()));

    var actionOccurs = actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);

    return bFmgr.implication(actionOccurs, actionTimeStampsFormula);
  }

  private BooleanFormula makeActionTimeStampForOccurenceFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable action, int occurenceCount) {
    var occurenceCountFormula =
        makeOccurenceCountEqualsFormula(pAutomaton, pLastReachedIndex + 1, action, occurenceCount);

    var timeOfOccurence =
        makeOccurenceTimeStampFormula(occurenceCount, action, pAutomaton, pLastReachedIndex);

    var globalOrderVariable = makeGlobalActionOrderVariable(action, occurenceCount);
    var oldLocalOrderVariable = makeLocalOrderVariable(pAutomaton, pLastReachedIndex);
    var newLocalOrderVariable = makeLocalOrderVariable(pAutomaton, pLastReachedIndex + 1);
    var f1 = fmgr.makeEqual(globalOrderVariable, oldLocalOrderVariable);
    var f2 = fmgr.makeGreaterThan(newLocalOrderVariable, oldLocalOrderVariable, false);
    var f3 = bFmgr.and(f1, f2);

    return bFmgr.implication(occurenceCountFormula, bFmgr.and(f3, timeOfOccurence));
  }

  private Formula makeGlobalActionOrderVariable(TaVariable pAction, int pOccurenceCount) {
    var variableName = "order#" + pAction.getName();
    return fmgr.makeVariable(occurenceStepFormulaType, variableName, pOccurenceCount);
  }

  private Formula makeLocalOrderVariable(TaDeclaration pAutomaton, int pLastReaachedIndex) {
    var variableName = pAutomaton.getName() + "#order";
    return fmgr.makeVariable(occurenceStepFormulaType, variableName, pLastReaachedIndex);
  }

  private BooleanFormula makeCounterStepFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable action) {
    var actionOccurs = actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);
    var unchanged = makeOccurenceCountUnchangedFormula(pAutomaton, pLastReachedIndex, action);
    var increase = makeOccurenceCountIncreaseFormula(pAutomaton, pLastReachedIndex, action);

    var ifAction = bFmgr.implication(actionOccurs, increase);
    var ifNoAction = bFmgr.implication(bFmgr.not(actionOccurs), unchanged);

    return bFmgr.and(ifAction, ifNoAction);
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    var initialCounts =
        from(automata.getSharedActionsByAutomaton(pAutomaton))
            .transform(
                action -> makeOccurenceCountEqualsFormula(pAutomaton, pInitialIndex, action, 0));
    return bFmgr.and(initialCounts.toSet());
  }

  @Override
  public BooleanFormula makeFinalConditionForAutomaton(
      TaDeclaration pAutomaton, int pMaxUnrolling) {

    var occurenceCounts = bFmgr.makeTrue();
    for (var action : automata.getSharedActionsByAutomaton(pAutomaton)) {
      var localOccurenceCount = makeOccurenceCountFormula(pAutomaton, pMaxUnrolling, action);
      var globalOccurenceCount = makeFinalOccurenceCountVariable(action);
      occurenceCounts =
          bFmgr.and(fmgr.makeEqual(globalOccurenceCount, localOccurenceCount), occurenceCounts);
    }

    var finalTimeSyncFormula = makeFinalTimeVariableSyncFormula(pAutomaton, pMaxUnrolling);
    return bFmgr.and(occurenceCounts, finalTimeSyncFormula);
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

  private BooleanFormula makeOccurenceTimeStampFormula(
      int pOccurenceCount, TaVariable pVariable, TaDeclaration pAutomaton, int pTimeVariableIndex) {
    var occurenceTimeStampName = "occurence_time#" + pOccurenceCount + "#" + pVariable.getName();
    var occurenceTimeStamp = fmgr.makeVariable(time.getClockVariableType(), occurenceTimeStampName);
    var localTimeStampValue =
        time.evaluateClock(pAutomaton, pTimeVariableIndex, timeStampVariables.get(pAutomaton));
    return fmgr.makeEqual(occurenceTimeStamp, localTimeStampValue);
  }

  private BooleanFormula makeOccurenceCountUnchangedFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable pAction) {
    var actionCountBefore = makeOccurenceCountFormula(pAutomaton, pLastReachedIndex, pAction);
    var actionCountAfter = makeOccurenceCountFormula(pAutomaton, pLastReachedIndex + 1, pAction);
    return fmgr.makeEqual(actionCountAfter, actionCountBefore);
  }

  private Formula makeFinalOccurenceCountVariable(TaVariable pVariable) {
    var variableName = "occurence_count_final#" + pVariable.getName();
    return fmgr.makeVariable(countFormulaType, variableName);
  }

  private BooleanFormula makeFinalTimeVariableSyncFormula(
      TaDeclaration pAutomaton, int pTimeVariableIndex) {
    var finalTimeVariable = fmgr.makeVariable(time.getClockVariableType(), "#final_time");
    var localTimeStampValue =
        time.evaluateClock(pAutomaton, pTimeVariableIndex, timeStampVariables.get(pAutomaton));
    return fmgr.makeEqual(finalTimeVariable, localTimeStampValue);
  }
}
