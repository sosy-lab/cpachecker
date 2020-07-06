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
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.BooleanVarFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BooleanVarActionEncoding extends BooleanVarFeatureEncoding<TaVariable>
    implements ActionEncoding {
  private TaVariable delayAction;
  private TaVariable idleAction;
  private Map<TaDeclaration, TaVariable> localDummyActions;

  public BooleanVarActionEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    super(pFmgr);
    localDummyActions = new HashMap<>();

    var allAutomata =
        from(pCfa.getAllFunctions().values())
            .filter(instanceOf(TCFAEntryNode.class))
            .transform(entry -> (TaDeclaration) entry.getFunction());
    allAutomata.forEach(
        automaton -> {
          automaton.getActions().forEach(action -> addEntry(automaton, action, action.getName()));

          var localDummyAction = new TaVariable("#dummy", automaton.getName(), true);
          addEntry(automaton, localDummyAction, localDummyAction.getName());
          localDummyActions.put(automaton, localDummyAction);
        });

    delayAction = createDummyEntry("delay#dummy");
    idleAction = createDummyEntry("idle#dummy");
  }

  private TaVariable createDummyEntry(String pVarName) {
    var dummyVar = new TaVariable(pVarName, "global", false);
    addEntryToAllAutomata(dummyVar, pVarName);
    return dummyVar;
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return makeEqualsFormula(pVariable, pAutomaton, pVariableIndex);
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
