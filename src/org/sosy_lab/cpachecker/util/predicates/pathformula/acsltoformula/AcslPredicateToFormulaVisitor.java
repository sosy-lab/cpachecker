// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate.AcslUnaryExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

@SuppressWarnings("unused")
public class AcslPredicateToFormulaVisitor implements AcslPredicateVisitor<BooleanFormula, NoException> {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;

  public AcslPredicateToFormulaVisitor(FormulaManagerView pFmgr) {
    checkNotNull(pFmgr);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
  }

  @Override
  public BooleanFormula visit(AcslBinaryPredicate pBinaryExpression) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslUnaryPredicate pAcslUnaryPredicate) throws NoException {
    BooleanFormula operandFormula = ((AcslPredicate) pAcslUnaryPredicate.getOperand()).accept(this);

    return switch ((AcslUnaryExpressionOperator) pAcslUnaryPredicate.getOperator()) {
      case NEGATION -> bfmgr.not(operandFormula);
    };
  }

  @Override
  public BooleanFormula visit(AcslIdPredicate pAcslIdPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslOldPredicate pAcslOldPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate)
      throws NoException {
    return bfmgr.makeBoolean(pAcslBooleanLiteralPredicate.getValue());
  }

  @Override
  public BooleanFormula visit(AcslTernaryPredicate pAcslTernaryPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslValidPredicate pAcslValidPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslForallPredicate pForallPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslExistsPredicate pAcslExistsPredicate) throws NoException {
    return null;
  }

  @Override
  public BooleanFormula visit(AcslPredicateApplicationPredicate pAcslPredicateApplicationPredicate)
      throws NoException {
    return null;
  }
}
