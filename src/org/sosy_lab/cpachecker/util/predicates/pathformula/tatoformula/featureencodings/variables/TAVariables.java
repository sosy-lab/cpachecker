// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface TAVariables {
  BooleanFormula makeConditionFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariableCondition pCondition);

  BooleanFormula makeVariablesDoNotChangeFormula(
      TaDeclaration pAutomaton, int pVariableIndexBefore, Iterable<TaVariable> pClocks);

  BooleanFormula makeTimeElapseFormula(TaDeclaration pAutomaton, int pIndexBefore);

  BooleanFormula makeEqualsZeroFormula(
      TaDeclaration pAutomaton, int pVariableIndex, Iterable<TaVariable> pClocks);
}
