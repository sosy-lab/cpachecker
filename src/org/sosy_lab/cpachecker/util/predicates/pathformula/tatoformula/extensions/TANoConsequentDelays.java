// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions;

import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TANoConsequentDelays extends TAEncodingExtensionBase {
  private final boolean isLocal;

  public TANoConsequentDelays(FormulaManagerView pFmgr, boolean pIsLocal) {
    super(pFmgr);
    isLocal = pIsLocal;
  }

  private BooleanFormula getDelayMarker(TaDeclaration pAutomaton, int pVariableIndex) {
    var variableName = "#delay_occured#" + (isLocal ? pAutomaton.getName() : "");
    return fmgr.getBooleanFormulaManager().makeVariable(variableName, pVariableIndex);
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    return bFmgr.not(getDelayMarker(pAutomaton, pInitialIndex));
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return getDelayMarker(pAutomaton, pLastReachedIndex + 1);
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return bFmgr.implication(
        getDelayMarker(pAutomaton, pLastReachedIndex),
        bFmgr.not(getDelayMarker(pAutomaton, pLastReachedIndex + 1)));
  }
}
