// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class TALocalVarDiscreteFeatureEncoding<T> implements TADiscreteFeatureEncoding<T> {
  private final String variableName;
  private final FormulaType<?> variableType;
  private final Map<T, Integer> values;
  private final FormulaManagerView fmgr;

  public TALocalVarDiscreteFeatureEncoding(
      FormulaManagerView pFmgr,
      String pVariableName,
      Set<T> pDomain,
      FormulaType<?> pVariableType) {
    fmgr = pFmgr;
    variableName = pVariableName;
    variableType = pVariableType;

    var valueMap = new HashMap<T, Integer>();
    pDomain.forEach(value -> valueMap.put(value, valueMap.size()));
    values = ImmutableMap.copyOf(valueMap);
  }

  private Formula makeVariableFormula(TaDeclaration pAutomaton, int variableIndex) {
    var qualifiedName = pAutomaton.getName() + "#" + variableName;
    return fmgr.makeVariable(variableType, qualifiedName, variableIndex);
  }

  private Formula makeValueFormula(T feature) {
    assert values.containsKey(feature);
    return fmgr.makeNumber(variableType, values.get(feature));
  }

  @Override
  public BooleanFormula makeEqualsFormula(TaDeclaration pAutomaton, int pVariableIndex, T feature) {
    var variable = makeVariableFormula(pAutomaton, pVariableIndex);
    var value = makeValueFormula(feature);
    return fmgr.makeEqual(variable, value);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var variableBefore = makeVariableFormula(pAutomaton, pIndexBefore);
    var variableAfter = makeVariableFormula(pAutomaton, pIndexBefore + 1);
    return fmgr.makeEqual(variableAfter, variableBefore);
  }
}
