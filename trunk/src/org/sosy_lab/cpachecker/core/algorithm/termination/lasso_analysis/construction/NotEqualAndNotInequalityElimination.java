// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

class NotEqualAndNotInequalityElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgr;

  private final StrictInequalityTransformation strictInequalityTransformation;
  private final InvertInequalityTransformation invertInequalityTransformation;

  NotEqualAndNotInequalityElimination(FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr;
    strictInequalityTransformation = new StrictInequalityTransformation(pFmgr);
    invertInequalityTransformation = new InvertInequalityTransformation(pFmgr);
  }

  @Override
  public BooleanFormula visitNot(BooleanFormula pOperand) {
    List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pOperand);

    // Pattern matching on (NOT (= A B)).
    if (split.size() == 2) {
      return fmgr.makeOr(
          fmgr.visit(split.get(0), strictInequalityTransformation),
          fmgr.visit(split.get(1), strictInequalityTransformation));

      // handle <,<=, >, >=
    } else {
      return fmgr.visit(pOperand, invertInequalityTransformation);
    }
  }

  private static class StrictInequalityTransformation
      extends DefaultFormulaVisitor<BooleanFormula> {

    private final FormulaManagerView fmgr;

    private StrictInequalityTransformation(FormulaManagerView pFmgr) {
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
        return fmgr.makeGreaterThan(pNewArgs.get(0), pNewArgs.get(1), true);

      } else if (pFunctionDeclaration.getKind().equals(FunctionDeclarationKind.LTE)
          || pFunctionDeclaration.getName().equals("<=")) {
        assert pNewArgs.size() == 2;
        return fmgr.makeLessThan(pNewArgs.get(0), pNewArgs.get(1), true);

      } else {
        return super.visitFunction(pF, pNewArgs, pFunctionDeclaration);
      }
    }
  }
}
