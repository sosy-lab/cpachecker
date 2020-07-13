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
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.BooleanVarFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class BooleanVarActionEncoding extends BooleanVarFeatureEncoding<TaVariable>
    implements ActionEncoding {
  private final TimedAutomatonView automata;

  public BooleanVarActionEncoding(FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    super(pFmgr);
    automata = pAutomata;

    automata
        .getAllAutomata()
        .forEach(
            automaton -> {
              automata
                  .getActionsByAutomaton(automaton)
                  // useage of var names entails global vars (names are unique for shared vars)
                  .forEach(action -> addEntry(automaton, action, action.getName()));
            });
  }

  @Override
  public BooleanFormula makeActionEqualsFormula(
      TaDeclaration pAutomaton, int pVariableIndex, TaVariable pVariable) {
    return makeEqualsFormula(pAutomaton, pVariableIndex, pVariable);
  }

  @Override
  public BooleanFormula makeActionOccursInStepFormula(int pVariableIndex, TaVariable pVariable) {
    return makeEqualsFormula(pVariableIndex, pVariable);
  }
}
