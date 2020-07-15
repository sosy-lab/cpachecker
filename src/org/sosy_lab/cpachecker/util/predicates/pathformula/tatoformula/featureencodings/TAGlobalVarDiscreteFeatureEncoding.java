// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;

public class TAGlobalVarDiscreteFeatureEncoding<T> implements TADiscreteFeatureEncoding<T> {
  private final String VARIABLE_NAME;
  private static final FormulaType<?> VARIABLE_TYPE = FormulaType.IntegerType;
  private Map<T, Integer> ids;
  protected final FormulaManagerView fmgr;

  public TAGlobalVarDiscreteFeatureEncoding(FormulaManagerView pFmgr, String variableName) {
    fmgr = pFmgr;
    VARIABLE_NAME = variableName;
    ids = new HashMap<>();
  }

  protected void addEntry(T pValue) {
    ids.putIfAbsent(pValue, ids.size());
  }

  private Formula makeVariableFormula(int variableIndex) {
    return fmgr.makeVariable(VARIABLE_TYPE, VARIABLE_NAME, variableIndex);
  }

  private Formula makeValueFormula(T feature) {
    assert ids.containsKey(feature);
    return fmgr.makeNumber(VARIABLE_TYPE, ids.get(feature));
  }

  @Override
  public BooleanFormula makeEqualsFormula(TaDeclaration pAutomaton, int pVariableIndex, T feature) {
    return makeEqualsFormula(pVariableIndex, feature);
  }

  // This is a global encoding - it doesnt need automata information
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
