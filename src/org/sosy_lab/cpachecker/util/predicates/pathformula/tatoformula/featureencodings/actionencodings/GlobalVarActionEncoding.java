// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.GlobalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class GlobalVarActionEncoding extends GlobalVarDiscreteFeatureEncoding<TaVariable>
    implements ActionEncoding {
  private TaVariable delayAction;
  private TaVariable idleAction;
  private Map<TaDeclaration, TaVariable> localDummyActions;

  public GlobalVarActionEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    super(pFmgr, "global#action");
    localDummyActions = new HashMap<>();

    var allAutomata =
        from(pCfa.getAllFunctions().values())
            .filter(instanceOf(TCFAEntryNode.class))
            .transform(entry -> (TaDeclaration) entry.getFunction());
    var allActions = allAutomata.transformAndConcat(automaton -> automaton.getActions()).toSet();
    allActions.forEach(action -> addEntry(action));

    delayAction = createDummyEntry("delay");
    idleAction = createDummyEntry("idle");

    allAutomata.forEach(
        automaton -> {
          var localDummyAction = new TaVariable("#dummy", automaton.getName(), true);
          addEntry(localDummyAction);
          localDummyActions.put(automaton, localDummyAction);
        });
  }

  private TaVariable createDummyEntry(String pVarName) {
    var dummyVar = new TaVariable(pVarName + "#dummy", "global", false);
    addEntry(dummyVar);
    return dummyVar;
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return makeEqualsFormula(pVariable, pVariableIndex);
  }

  @Override
  public BooleanFormula makeDelayActionFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    return makeActionEqualsFormula(pAutomaton, pVariableIndex, delayAction);
  }

  @Override
  public BooleanFormula makeIdleActionFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    return makeActionEqualsFormula(pAutomaton, pVariableIndex, idleAction);
  }

  @Override
  public BooleanFormula makeLocalDummyActionFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    return makeActionEqualsFormula(pAutomaton, pVariableIndex, localDummyActions.get(pAutomaton));
  }
}
