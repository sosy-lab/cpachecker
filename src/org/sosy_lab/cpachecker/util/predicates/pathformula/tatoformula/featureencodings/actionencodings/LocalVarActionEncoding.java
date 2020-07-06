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

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.LocalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class LocalVarActionEncoding extends LocalVarDiscreteFeatureEncoding<TaVariable>
    implements ActionEncoding {
  private Map<TaDeclaration, TaVariable> delayActions;
  private Map<TaDeclaration, TaVariable> idleActions;
  private Map<TaDeclaration, TaVariable> localDummyActions;

  public LocalVarActionEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    super(pFmgr, "action");

    var allAutomata =
        from(pCfa.getAllFunctions().values())
            .filter(instanceOf(TCFAEntryNode.class))
            .transform(entry -> (TaDeclaration) entry.getFunction());
    var allActions = allAutomata.transformAndConcat(automaton -> automaton.getActions()).toSet();
    allActions.forEach(action -> addEntry(action));

    delayActions = createDummyEntries(allAutomata, "delay");
    idleActions = createDummyEntries(allAutomata, "idle");
    localDummyActions = createDummyEntries(allAutomata, "dummy");
  }

  private Map<TaDeclaration, TaVariable> createDummyEntries(
      Iterable<TaDeclaration> pAutomata, String pVarName) {
    Map<TaDeclaration, TaVariable> result = new HashMap<>();
    pAutomata.forEach(
        automaton -> {
          var dummyVar = new TaVariable(pVarName + "#dummy", automaton.getName(), true);
          result.put(automaton, dummyVar);
          addEntry(dummyVar);
        });

    return ImmutableMap.copyOf(result);
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return makeEqualsFormula(pVariable, pAutomaton, pVariableIndex);
  }

  @Override
  public BooleanFormula makeDelayActionFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    return makeActionEqualsFormula(pAutomaton, pVariableIndex, delayActions.get(pAutomaton));
  }

  @Override
  public BooleanFormula makeIdleActionFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    return makeActionEqualsFormula(pAutomaton, pVariableIndex, idleActions.get(pAutomaton));
  }

  @Override
  public BooleanFormula makeLocalDummyActionFormula(TaDeclaration pAutomaton, int pVariableIndex) {
    return makeActionEqualsFormula(pAutomaton, pVariableIndex, localDummyActions.get(pAutomaton));
  }
}
