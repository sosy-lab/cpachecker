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
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;

class EqualElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgr;

  EqualElimination(FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr;
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    if (pDecl.getKind().equals(FunctionDeclarationKind.EQ)) {
      List<BooleanFormula> split = fmgr.splitNumeralEqualityIfPossible(pAtom);

      if (split.size() == 1) {
        return split.get(0);

      } else if (split.size() == 2) {
        return fmgr.makeAnd(split.get(0), split.get(1));

      } else {
        throw new AssertionError();
      }

    } else {
      return super.visitAtom(pAtom, pDecl);
    }
  }
}
