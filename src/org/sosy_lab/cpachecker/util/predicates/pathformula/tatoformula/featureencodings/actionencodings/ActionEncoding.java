// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface ActionEncoding {
  BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable);

  BooleanFormula makeDelayActionFormula(TaDeclaration pAutomaton, int pVariableIndex);

  BooleanFormula makeIdleActionFormula(TaDeclaration pAutomaton, int pVariableIndex);

  BooleanFormula makeLocalDummyActionFormula(TaDeclaration pAutomaton, int pVariableIndex);

  Iterable<BooleanFormula> makeAllActionFormulas(
      int pVariableIndex, boolean pIncludeDelay, boolean pIncludeIdle);
}
