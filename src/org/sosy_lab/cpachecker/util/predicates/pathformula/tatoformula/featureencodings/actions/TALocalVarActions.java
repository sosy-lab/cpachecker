// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TADiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TALocalVarActions implements TAActions {
  private final TimedAutomatonView automata;
  private final TADiscreteFeatureEncoding<TaVariable> encoding;
  private final FormulaManagerView fmgr;

  public TALocalVarActions(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TADiscreteFeatureEncoding<TaVariable> pEncoding) {
    automata = pAutomata;
    encoding = pEncoding;
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return encoding.makeEqualsFormula(pAutomaton, pVariableIndex, pVariable);
  }

  @Override
  public BooleanFormula makeActionOccursInStepFormula(int pVariableIndex, TaVariable pVariable) {
    var automatonPerformsActionFormulas =
        from(automata.getAutomataWithAction(pVariable))
            .transform(automaton -> makeActionEqualsFormula(automaton, pVariableIndex, pVariable));
    var anyAutomatonPerformsAction =
        fmgr.getBooleanFormulaManager().or(automatonPerformsActionFormulas.toSet());
    return anyAutomatonPerformsAction;
  }
}
