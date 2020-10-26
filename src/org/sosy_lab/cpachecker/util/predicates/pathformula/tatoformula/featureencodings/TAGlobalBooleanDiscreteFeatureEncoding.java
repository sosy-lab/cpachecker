// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAGlobalBooleanDiscreteFeatureEncoding<X>
    implements TAGlobalDiscreteFeatureEncoding<X> {
  private final String variableName;
  private final Map<X, Integer> values;
  private final BooleanFormulaManagerView bFmgr;
  private final int numberOfVariables;

  public TAGlobalBooleanDiscreteFeatureEncoding(
      FormulaManagerView pFmgr, String pVariableName, Collection<X> pDomain) {
    bFmgr = pFmgr.getBooleanFormulaManager();
    variableName = pVariableName;
    numberOfVariables = (int) Math.ceil(Math.log(pDomain.size()) / Math.log(2));

    var valueMap = new HashMap<X, Integer>();
    pDomain.forEach(value -> valueMap.put(value, valueMap.size()));
    values = ImmutableMap.copyOf(valueMap);
  }

  @Override
  public BooleanFormula makeEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, X pFeature) {
    return makeEqualsFormula(pVariableIndex, pFeature);
  }

  private BooleanFormula makeFormulaForNumber(int pNumber, int pVariableIndex) {
    var result = bFmgr.makeTrue();

    for (int power = 0; power < numberOfVariables; power++) {
      var variable = getBooleanVariable(power, pVariableIndex);
      if ((pNumber & 1) > 0) {
        result = bFmgr.and(result, variable);
      } else {
        result = bFmgr.and(result, bFmgr.not(variable));
      }
      pNumber /= 2;
    }

    return result;
  }

  private BooleanFormula getBooleanVariable(int power, int pVariableIndex) {
    return bFmgr.makeVariable(variableName + "#b_" + power, pVariableIndex);
  }

  @Override
  public BooleanFormula makeUnchangedFormula(TaDeclaration pAutomaton, int pIndexBefore) {
    var result = bFmgr.makeTrue();
    for (int power = 0; power < numberOfVariables; power++) {
      var variableBefore = getBooleanVariable(power, pIndexBefore);
      var variableAfter = getBooleanVariable(power, pIndexBefore + 1);
      result = bFmgr.and(result, bFmgr.equivalence(variableBefore, variableAfter));
    }

    return result;
  }

  @Override
  public BooleanFormula makeEqualsFormula(int pVariableIndex, X pFeature) {
    return makeFormulaForNumber(values.get(pFeature), pVariableIndex);
  }
}
