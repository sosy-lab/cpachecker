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

public class TAGlobalVarDiscreteFeatureEncoding<T> implements TAGlobalDiscreteFeatureEncoding<T> {
  private final String variableName;
  private final FormulaType<?> variableType;
  private final Map<T, Integer> values;
  private final FormulaManagerView fmgr;

  public TAGlobalVarDiscreteFeatureEncoding(
      FormulaManagerView pFmgr, String pVariableName, Set<T> pDomain) {
    fmgr = pFmgr;
    variableName = pVariableName;
    var bitVectorSize = (int) (Math.log(pDomain.size()) / Math.log(2) + 1);
    variableType = FormulaType.getBitvectorTypeWithSize(bitVectorSize);

    var valueMap = new HashMap<T, Integer>();
    pDomain.forEach(value -> valueMap.put(value, valueMap.size()));
    values = ImmutableMap.copyOf(valueMap);
  }

  private Formula makeVariableFormula(int variableIndex) {
    return fmgr.makeVariable(variableType, variableName, variableIndex);
  }

  private Formula makeValueFormula(T feature) {
    assert values.containsKey(feature);
    return fmgr.makeNumber(variableType, values.get(feature));
  }

  @Override
  public BooleanFormula makeEqualsFormula(TaDeclaration pAutomaton, int pVariableIndex, T feature) {
    return makeEqualsFormula(pVariableIndex, feature);
  }

  @Override
  public BooleanFormula makeEqualsFormula(int pVariableIndex, T feature) {
    var variable = makeVariableFormula(pVariableIndex);
    var value = makeValueFormula(feature);
    return fmgr.makeEqual(variable, value);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var variableBefore = makeVariableFormula(pIndexBefore);
    var variableAfter = makeVariableFormula(pIndexBefore + 1);
    return fmgr.makeEqual(variableAfter, variableBefore);
  }
}
