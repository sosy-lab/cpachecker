// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction;

import java.util.List;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView.BooleanFormulaTransformationVisitor;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

/**
 * Replaces strict inequalities over integers by non-strict ones, i.e., {@code a < b} by {@code a +
 * 1 <= b}. LassoRanker synthesizes termination arguments over the reals, where, e.g., {@code y < 0}
 * does not bound the decrease of {@code -y} away from zero.
 */
class StrictIntegerInequalityElimination extends BooleanFormulaTransformationVisitor {

  private final FormulaManagerView fmgr;
  private final IntegerFormulaManagerView ifmgr;

  StrictIntegerInequalityElimination(FormulaManagerView pFmgr) {
    super(pFmgr);
    fmgr = pFmgr;
    ifmgr = pFmgr.getIntegerFormulaManager();
  }

  @Override
  public BooleanFormula visitAtom(BooleanFormula pAtom, FunctionDeclaration<BooleanFormula> pDecl) {
    if (!pDecl.getArgumentTypes().stream().allMatch(FormulaType::isIntegerType)) {
      return pAtom;
    }
    FunctionDeclarationKind kind = pDecl.getKind();
    if (kind == FunctionDeclarationKind.LT) {
      List<Formula> args = getArguments(pAtom);
      return ifmgr.lessOrEquals(plusOne(args.getFirst()), (IntegerFormula) args.get(1));
    } else if (kind == FunctionDeclarationKind.GT) {
      List<Formula> args = getArguments(pAtom);
      return ifmgr.greaterOrEquals((IntegerFormula) args.getFirst(), plusOne(args.get(1)));
    }
    return pAtom;
  }

  private IntegerFormula plusOne(Formula pFormula) {
    return ifmgr.add((IntegerFormula) pFormula, ifmgr.makeNumber(1));
  }

  private List<Formula> getArguments(BooleanFormula pAtom) {
    return fmgr.visit(
        pAtom,
        new DefaultFormulaVisitor<List<Formula>>() {
          @Override
          protected List<Formula> visitDefault(Formula pF) {
            throw new AssertionError("Unexpected atom " + pF);
          }

          @Override
          public List<Formula> visitFunction(
              Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
            return pArgs;
          }
        });
  }
}
