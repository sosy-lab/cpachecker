// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BooleanVarFeatureEncoding<T> implements DiscreteFeatureEncoding<T> {
  private Map<T, String> variableNames;
  private Map<TaDeclaration, Collection<T>> elementsByAutomaton;
  protected final FormulaManagerView fmgr;

  public BooleanVarFeatureEncoding(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    variableNames = new HashMap<>();
    elementsByAutomaton = new HashMap<>();
  }

  public void addEntry(TaDeclaration pAutomaton, T pValue, String pVariableName) {
    variableNames.put(pValue, pVariableName);
    elementsByAutomaton.computeIfAbsent(pAutomaton, automaton -> new HashSet<>());
    elementsByAutomaton.get(pAutomaton).add(pValue);
  }

  private BooleanFormula makeVariableFormula(String variableName, int variableIndex) {
    return fmgr.getBooleanFormulaManager().makeVariable(variableName, variableIndex);
  }

  @Override
  public BooleanFormula makeEqualsFormula(TaDeclaration pAutomaton, int pVariableIndex, T feature) {
    return makeEqualsFormula(pVariableIndex, feature);
  }

  // This is a global encoding and in fact doesnt need automata.
  public BooleanFormula makeEqualsFormula(int pVariableIndex, T feature) {
    return makeVariableFormula(variableNames.get(feature), pVariableIndex);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var unchangedFormulas =
        from(elementsByAutomaton.get(pAutomaton))
            .transform(
                feature -> {
                  var variableBefore = makeEqualsFormula(pIndexBefore, feature);
                  var variableAfter = makeEqualsFormula(pIndexBefore + 1, feature);
                  return fmgr.makeEqual(variableAfter, variableBefore);
                });

    return fmgr.getBooleanFormulaManager().and(unchangedFormulas.toSet());
  }
}
