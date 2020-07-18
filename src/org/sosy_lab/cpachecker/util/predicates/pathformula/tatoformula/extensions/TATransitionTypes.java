// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;


import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TADiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TALocalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Adds for each automaton a variable that represents transition type (idle/delay/discrete). This
 * allows to track the transition types in each step. Note that this implies mutual exclusion of
 * different transition types for an automaton.
 */
public class TATransitionTypes extends TAEncodingExtensionBase {
  private final TADiscreteFeatureEncoding<String> variables;

  public TATransitionTypes(FormulaManagerView pFmgr) {
    super(pFmgr);
    var localVars = new TALocalVarDiscreteFeatureEncoding<String>(pFmgr, "transition_type");
    localVars.addEntry("DISCRETE");
    localVars.addEntry("DELAY");
    localVars.addEntry("IDLE");
    variables = localVars;
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    return variables.makeEqualsFormula(pAutomaton, pLastReachedIndex, "DISCRETE");
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return variables.makeEqualsFormula(pAutomaton, pLastReachedIndex, "DELAY");
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return variables.makeEqualsFormula(pAutomaton, pLastReachedIndex, "IDLE");
  }
}
