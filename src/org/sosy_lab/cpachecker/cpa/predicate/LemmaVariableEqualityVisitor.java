// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

public class LemmaVariableEqualityVisitor extends DefaultFormulaVisitor<BooleanFormula> {
  private final Formula predArg;
  private final FormulaManagerView fmgr;

  public LemmaVariableEqualityVisitor(Formula pArg, FormulaManagerView pFmgr) {
    predArg = pArg;
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula visitFreeVariable(Formula f, String name) {
    return fmgr.makeEqual(predArg, f);
  }

  @Override
  protected BooleanFormula visitDefault(Formula pFormula) {
    return fmgr.getBooleanFormulaManager().makeTrue();
  }
}
