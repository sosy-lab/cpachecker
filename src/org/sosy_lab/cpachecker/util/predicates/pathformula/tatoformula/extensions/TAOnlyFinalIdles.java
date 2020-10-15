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
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TAOnlyFinalIdles extends TAEncodingExtensionBase {
  private final boolean isLocal = true;

  public TAOnlyFinalIdles(FormulaManagerView pFmgr) {
    super(pFmgr);
  }

  private BooleanFormula getHasIdledMarker(TaDeclaration pAutomaton, int pVariableIndex) {
    var variableName = "#has_idled#" + (isLocal ? pAutomaton.getName() : "");
    return fmgr.getBooleanFormulaManager().makeVariable(variableName, pVariableIndex);
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    return bFmgr.not(getHasIdledMarker(pAutomaton, pInitialIndex));
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge edge) {
    return bFmgr.not(getHasIdledMarker(pAutomaton, pLastReachedIndex));
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return bFmgr.not(getHasIdledMarker(pAutomaton, pLastReachedIndex));
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return getHasIdledMarker(pAutomaton, pLastReachedIndex + 1);
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return bFmgr.implication(
        getHasIdledMarker(pAutomaton, pLastReachedIndex),
        getHasIdledMarker(pAutomaton, pLastReachedIndex + 1));
  }
}
