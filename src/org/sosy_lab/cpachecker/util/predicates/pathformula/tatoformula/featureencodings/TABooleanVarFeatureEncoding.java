// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TABooleanVarFeatureEncoding<T> implements TADiscreteFeatureEncoding<T> {
  private final Table<TaDeclaration, T, String> variableNames;
  private final Map<TaDeclaration, Collection<T>> elementsByAutomaton;
  private final FormulaManagerView fmgr;

  public TABooleanVarFeatureEncoding(
      FormulaManagerView pFmgr,
      Table<TaDeclaration, T, String> pVariableNames,
      Map<TaDeclaration, Collection<T>> pElementsByAutomaton) {
    fmgr = pFmgr;
    variableNames = ImmutableTable.copyOf(pVariableNames);
    elementsByAutomaton = ImmutableMap.copyOf(pElementsByAutomaton);
  }

  private BooleanFormula makeVariableFormula(String variableName, int variableIndex) {
    return fmgr.getBooleanFormulaManager().makeVariable(variableName, variableIndex);
  }

  @Override
  public BooleanFormula makeEqualsFormula(TaDeclaration pAutomaton, int pVariableIndex, T feature) {
    return makeVariableFormula(variableNames.get(pAutomaton, feature), pVariableIndex);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var unchangedFormulas =
        from(elementsByAutomaton.get(pAutomaton))
            .transform(feature -> makeUnchangedFormula(pAutomaton, pIndexBefore, feature));

    return fmgr.getBooleanFormulaManager().and(unchangedFormulas.toSet());
  }

  private BooleanFormula makeUnchangedFormula(
      TaDeclaration pAutomaton, int pIndexBefore, T feature) {
    var variableBefore = makeEqualsFormula(pAutomaton, pIndexBefore, feature);
    var variableAfter = makeEqualsFormula(pAutomaton, pIndexBefore + 1, feature);
    return fmgr.makeEqual(variableAfter, variableBefore);
  }
}
