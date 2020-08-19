// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TABooleanVarFeatureEncoding;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TABooleanVarActions implements TAActions {
  private final TABooleanVarFeatureEncoding<TaVariable> encoding;

  public TABooleanVarActions(TABooleanVarFeatureEncoding<TaVariable> pEncoding) {
    encoding = pEncoding;
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return encoding.makeEqualsFormula(pAutomaton, pVariableIndex, pVariable);
  }

  @Override
  public BooleanFormula makeActionOccursInStepFormula(int pVariableIndex, TaVariable pVariable) {
    return encoding.makeEqualsFormula(pVariableIndex, pVariable);
  }
}
