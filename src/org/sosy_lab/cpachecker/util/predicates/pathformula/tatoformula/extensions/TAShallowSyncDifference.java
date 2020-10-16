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

public class TAShallowSyncDifference extends TAEncodingExtensionBase {
  private final TAVariables time;
  private final TAActions actions;
  private final FormulaType<?> countFormulaType;
  private final FormulaType<?> actionOrderFormulaType;
  private final TimedAutomatonView automata;
  private Map<TaDeclaration, TaVariable> timeStampVariables;

  private static final String LOCAL_TIMESTAMP_VARIABLE = "#local_timestamp";

  public TAShallowSyncDifference(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TAActions pActions) {
    super(pFmgr);
    time = pTime;
    actions = pActions;
    automata = pAutomata;
    countFormulaType = FormulaType.IntegerType;
    actionOrderFormulaType = FormulaType.IntegerType;
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
        from(automata.getActionsByAutomaton(pAutomaton))
            .transform(action -> makeActionOccurenceFormula(pAutomaton, pLastReachedIndex, action));
    var actionOccurencesFormula = bFmgr.and(actionOccurenceFormulas.toSet());

    var counterStepFormulas =
        from(automata.getActionsByAutomaton(pAutomaton))
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
    var f1 = makeDifferenceEqualsFormula(globalOrderVariable, oldLocalOrderVariable, 0);
    var f2 = fmgr.makeLessThan(oldLocalOrderVariable, newLocalOrderVariable, false);
    var f3 = bFmgr.and(f1, f2);

    return bFmgr.implication(occurenceCountFormula, bFmgr.and(f3, timeOfOccurence));
  }

  private BooleanFormula makeDifferenceEqualsFormula(
      Formula variable1, Formula variable2, int constant) {
    var difference1 = fmgr.makeMinus(variable1, variable2);
    var difference2 = fmgr.makeMinus(variable2, variable1);
    var constantFormula1 = fmgr.makeNumber(fmgr.getFormulaType(variable1), constant);
    var constantFormula2 = fmgr.makeNumber(fmgr.getFormulaType(variable2), -constant);

    return bFmgr.and(
        fmgr.makeLessOrEqual(difference1, constantFormula1, true),
        fmgr.makeLessOrEqual(difference2, constantFormula2, true));
  }

  private Formula makeGlobalActionOrderVariable(TaVariable pAction, int pOccurenceCount) {
    var variableName = "order#" + pAction.getName();
    return fmgr.makeVariable(actionOrderFormulaType, variableName, pOccurenceCount);
  }

  private Formula makeLocalOrderVariable(TaDeclaration pAutomaton, int pLastReaachedIndex) {
    var variableName = pAutomaton.getName() + "#order";
    return fmgr.makeVariable(actionOrderFormulaType, variableName, pLastReaachedIndex);
  }

  private BooleanFormula makeCounterStepFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable action) {
    var actionOccurs = actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action);
    var unchanged = makeOccurenceCountUnchangedFormula(pAutomaton, pLastReachedIndex, action);
    var increse = makeOccurenceCountIncreaseFormula(pAutomaton, pLastReachedIndex, action);

    var ifAction = bFmgr.implication(actionOccurs, increse);
    var ifNoAction = bFmgr.implication(bFmgr.not(actionOccurs), unchanged);

    return bFmgr.and(ifAction, ifNoAction);
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
      TaDeclaration pAutomaton, int pMaxUnrolling) {

    var occurenceCounts = bFmgr.makeTrue();
    for (var action : automata.getActionsByAutomaton(pAutomaton)) {
      var localOccurenceCount = makeOccurenceCountVariable(pAutomaton, pMaxUnrolling, action);
      var globalOccurenceCount = makeFinalOccurenceCountVariable(action);
      occurenceCounts =
          bFmgr.and(
              makeDifferenceEqualsFormula(globalOccurenceCount, localOccurenceCount, 0),
              occurenceCounts);
    }

    var finalTimeSyncFormula = makeFinalTimeVariableSyncFormula(pAutomaton, pMaxUnrolling);
    return bFmgr.and(occurenceCounts, finalTimeSyncFormula);
  }

  private Formula makeOccurenceCountVariable(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    var variableName = "occurence_count#" + pVariable.getName() + "#" + pAutomaton.getName();
    return fmgr.makeVariable(countFormulaType, variableName, pVariableIndex);
  }

  private Formula makeOccurenceCountZeroVariable() {
    return fmgr.makeVariable(countFormulaType, "#occurence_count_zero#");
  }

  private BooleanFormula makeOccurenceCountEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable, int pCount) {
    var occurenceCountVar = makeOccurenceCountVariable(pAutomaton, pVariableIndex, pVariable);
    var occurenceCountZeroVar = makeOccurenceCountZeroVariable();
    return makeDifferenceEqualsFormula(occurenceCountVar, occurenceCountZeroVar, pCount);
  }

  private BooleanFormula makeOccurenceCountIncreaseFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex, TaVariable pVariable) {
    var occurenceCountVarBefore =
        makeOccurenceCountVariable(pAutomaton, pLastReachedIndex, pVariable);
    var occurenceCountVarAfter =
        makeOccurenceCountVariable(pAutomaton, pLastReachedIndex + 1, pVariable);
    return makeDifferenceEqualsFormula(occurenceCountVarAfter, occurenceCountVarBefore, 1);
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
    var actionCountBefore = makeOccurenceCountVariable(pAutomaton, pLastReachedIndex, pAction);
    var actionCountAfter = makeOccurenceCountVariable(pAutomaton, pLastReachedIndex + 1, pAction);
    return makeDifferenceEqualsFormula(actionCountAfter, actionCountBefore, 0);
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
