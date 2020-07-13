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
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class EncodingExtensionBase implements EncodingExtension {
  protected FormulaManagerView fmgr;
  protected BooleanFormulaManagerView bFmgr;

  public EncodingExtensionBase(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
    bFmgr = fmgr.getBooleanFormulaManager();
  }

  @Override
  public BooleanFormula makeAutomatonStep(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeInitialFormula(TaDeclaration pAutomaton, int pInitialIndex) {
    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeFinalConditionForAutomaton(
      TaDeclaration pAutomaton, int pHighestReachedIndex) {
    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    return bFmgr.makeTrue();
  }

  @Override
  public BooleanFormula makeStepFormula(int pLastReachedIndex) {
    return bFmgr.makeTrue();
  }
}
