// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBooleanLiteralPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslExistsPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslForallPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslOldPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateApplicationPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicateVisitor;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTernaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslUnaryPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslValidPredicate;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings("unused")
public class AcslPredicateToFormulaVisitor
    implements AcslPredicateVisitor<BooleanFormula, NoException> {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final AcslTermToFormulaVisitor termVisitor;
  private final SSAMapBuilder currentSsa; // ToDo where do we get this from??
  private final @Nullable SSAMap
      functionEntrySsa; // Optional SSA map for function-entry state (\old)

  public AcslPredicateToFormulaVisitor(FormulaManagerView pFmgr, SSAMapBuilder pCurrentSsa) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor = new AcslTermToFormulaVisitor(pFmgr, pCurrentSsa);
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = null;
  }

  public AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr, SSAMapBuilder pCurrentSsa, SSAMap pFunctionEntrySsa) {
    checkNotNull(pFmgr);
    checkNotNull(pCurrentSsa);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor = new AcslTermToFormulaVisitor(pFmgr, pCurrentSsa, pFunctionEntrySsa);
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = pFunctionEntrySsa;
  }

  protected AcslPredicateToFormulaVisitor(
      FormulaManagerView pFmgr,
      AcslTermToFormulaVisitor pTermVisitor,
      SSAMapBuilder pCurrentSsa,
      SSAMap pFunctionEntrySsa) {
    checkNotNull(pFmgr);
    checkNotNull(pTermVisitor);
    this.fmgr = pFmgr;
    this.bfmgr = fmgr.getBooleanFormulaManager();
    this.termVisitor = pTermVisitor;
    this.currentSsa = pCurrentSsa;
    this.functionEntrySsa = pFunctionEntrySsa;
  }

  @Override
  public BooleanFormula visit(AcslBinaryPredicate pBinaryExpression) throws NoException {
    BooleanFormula operand1Formula = pBinaryExpression.getOperand1().accept(this);
    BooleanFormula operand2Formula = pBinaryExpression.getOperand2().accept(this);

    return switch (pBinaryExpression.getOperator()) {
      case IMPLICATION -> bfmgr.implication(operand1Formula, operand2Formula);
      case EQUIVALENT -> bfmgr.equivalence(operand1Formula, operand2Formula);
      case AND -> bfmgr.and(operand1Formula, operand2Formula);
      case OR -> bfmgr.or(operand1Formula, operand2Formula);
    };
  }

  @Override
  public BooleanFormula visit(AcslUnaryPredicate pAcslUnaryPredicate) throws NoException {
    BooleanFormula operandFormula = pAcslUnaryPredicate.getOperand().accept(this);

    return switch (pAcslUnaryPredicate.getOperator()) {
      case NEGATION -> bfmgr.not(operandFormula);
    };
  }

  @Override
  public BooleanFormula visit(AcslIdPredicate pAcslIdPredicate) throws NoException {
    AcslPredicateDeclaration declaration = pAcslIdPredicate.getDeclaration();
    return bfmgr.makeVariable(declaration.getName());
  }

  @Override
  public BooleanFormula visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate) throws NoException {
    Formula operand1Formula = pAcslBinaryTermPredicate.getOperand1().accept(termVisitor);
    Formula operand2Formula = pAcslBinaryTermPredicate.getOperand2().accept(termVisitor);

    // TODO revisit if signed=true is safe or if we could have a case where we get something
    // unsigned in the bitvector case
    return switch (pAcslBinaryTermPredicate.getOperator()) {
      case EQUALS -> fmgr.makeEqual(operand1Formula, operand2Formula);
      case NOT_EQUALS -> bfmgr.not(fmgr.makeEqual(operand1Formula, operand2Formula));
      case LESS_EQUAL -> fmgr.makeLessOrEqual(operand1Formula, operand2Formula, true);
      case GREATER_EQUAL -> fmgr.makeGreaterOrEqual(operand1Formula, operand2Formula, true);
      case LESS_THAN -> fmgr.makeLessThan(operand1Formula, operand2Formula, true);
      case GREATER_THAN -> fmgr.makeGreaterThan(operand1Formula, operand2Formula, true);
    };
  }

  @Override
  public BooleanFormula visit(AcslOldPredicate pAcslOldPredicate) throws NoException {
    if (functionEntrySsa == null) {
      throw new UnsupportedOperationException(
          "\\old is not available without a SSA map at function entry");
    }

    // TODO: implementation
    return null;
  }

  @Override
  public BooleanFormula visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate)
      throws NoException {
    return bfmgr.makeBoolean(pAcslBooleanLiteralPredicate.getValue());
  }

  @Override
  public BooleanFormula visit(AcslTernaryPredicate pAcslTernaryPredicate) throws NoException {
    BooleanFormula conditionFormula = pAcslTernaryPredicate.getCondition().accept(this);
    BooleanFormula ifTrueFormula = pAcslTernaryPredicate.getResultIfTrue().accept(this);
    BooleanFormula ifFalseFormula = pAcslTernaryPredicate.getResultIfFalse().accept(this);

    if (bfmgr.isTrue(conditionFormula)) {
      return ifTrueFormula;
    }
    if (bfmgr.isFalse(conditionFormula)) {
      return ifFalseFormula;
    }

    // (condition AND trueFormula) OR ((NOT condition) AND falseFormula)
    return bfmgr.or(
        bfmgr.and(conditionFormula, ifTrueFormula),
        bfmgr.and(bfmgr.not(conditionFormula), ifFalseFormula));
  }

  @Override
  public BooleanFormula visit(AcslValidPredicate pAcslValidPredicate) throws NoException {
    throw new UnsupportedOperationException("Not yet implemented");
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
