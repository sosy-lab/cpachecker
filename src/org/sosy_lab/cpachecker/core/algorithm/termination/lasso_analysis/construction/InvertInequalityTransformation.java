// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

class InvertInequalityTransformation extends DefaultFormulaVisitor<BooleanFormula> {

  private final FormulaManagerView fmgr;

  InvertInequalityTransformation(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  @Override
  protected BooleanFormula visitDefault(Formula pF) {
    return (BooleanFormula) pF;
  }

  @Override
  public BooleanFormula visitFunction(
      Formula pF, List<Formula> pNewArgs, FunctionDeclaration<?> pFunctionDeclaration) {

    if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GTE)
        || pFunctionDeclaration.getName().equals(">=")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeLessThan(pNewArgs.get(0), pNewArgs.get(1), true);

    } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LTE)
        || pFunctionDeclaration.getName().equals("<=")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeGreaterThan(pNewArgs.get(0), pNewArgs.get(1), true);

    } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.GT)
        || pFunctionDeclaration.getName().equals(">")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeLessOrEqual(pNewArgs.get(0), pNewArgs.get(1), true);

    } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LT)
        || pFunctionDeclaration.getName().equals("<")) {
      assert pNewArgs.size() == 2;
      return fmgr.makeGreaterOrEqual(pNewArgs.get(0), pNewArgs.get(1), true);

    } else {
      return super.visitFunction(pF, pNewArgs, pFunctionDeclaration);
    }
  }
}
