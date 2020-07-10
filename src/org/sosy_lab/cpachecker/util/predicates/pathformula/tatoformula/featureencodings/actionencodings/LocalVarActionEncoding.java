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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
  private final Collection<TaDeclaration> automata;

  public LocalVarActionEncoding(FormulaManagerView pFmgr, CFA pCfa) {
    super(pFmgr, "action");

    automata =
        from(pCfa.getAllFunctions().values())
            .filter(instanceOf(TCFAEntryNode.class))
            .transform(entry -> (TaDeclaration) entry.getFunction())
            .toSet();
    var allActions = from(automata).transformAndConcat(automaton -> automaton.getActions()).toSet();
    allActions.forEach(action -> addEntry(action));

    delayActions = createDummyEntries(automata, "delay");
    idleActions = createDummyEntries(automata, "idle");
    localDummyActions = createDummyEntries(automata, "dummy");
  }

  private Map<TaDeclaration, TaVariable> createDummyEntries(
      Iterable<TaDeclaration> pAutomata, String pVarName) {
    Map<TaDeclaration, TaVariable> result = new HashMap<>();
    pAutomata.forEach(
        automaton -> {
          var dummyVar =
              TaVariable.createDummyVariable(pVarName + "#dummy", automaton.getName(), true);
          result.put(automaton, dummyVar);
          addEntry(dummyVar);
        });

    return ImmutableMap.copyOf(result);
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return makeEqualsFormula(pAutomaton, pVariableIndex, pVariable);
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

  @Override
  public Iterable<BooleanFormula> makeAllActionFormulas(
      int pVariableIndex, boolean pIncludeDelay, boolean pIncludeIdle) {
    var result = new HashSet<BooleanFormula>();
    var processedActions = new HashSet<TaVariable>();
    for (var automaton : automata) {
      for (var action : automaton.getActions()) {
        // it is possible that automata share an action. However, the automaton definition is needed
        // for formula cration. Thus remember actions that have been processed by another automaton
        if (!processedActions.contains(action)) {
          result.add(makeActionEqualsFormula(automaton, pVariableIndex, action));
          processedActions.add(action);
        }
      }
      result.add(makeLocalDummyActionFormula(automaton, pVariableIndex));
      // dummy and delay are globally unique, add only once
      if (pIncludeIdle) {
        result.add(makeIdleActionFormula(automaton, pVariableIndex));
      }
      if (pIncludeDelay) {
        result.add(makeDelayActionFormula(automaton, pVariableIndex));
      }
    }

    return result;
  }
}
