// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public final class ExpressionSubstitution {

  private ExpressionSubstitution() {}

  /**
   * Applies the given substitution recursively to the given expression, using the given
   * binary-expression builder if necessary.
   *
   * @param pExpression the expression to substitute.
   * @param pSubstitution the substitution to apply.
   * @param pBinExpBuilder the binary-expression builder to be used to reconstruct binary
   *     expressions where an operand was modified by a substitution.
   * @return the substitute expression.
   * @throws SubstitutionException if the substitution fails.
   */
  public static CExpression applySubstitution(
      CExpression pExpression, Substitution pSubstitution, CBinaryExpressionBuilder pBinExpBuilder)
      throws SubstitutionException {
    SubstitutingVisitor substitutingVisitor =
        new SubstitutingVisitor(pSubstitution, pBinExpBuilder);
    return pExpression.accept(substitutingVisitor);
  }

  /**
   * Applies the given substitution recursively to the given expression, using the given
   * binary-expression builder if necessary.
   *
   * @param pExpression the expression to substitute.
   * @param pToSubstitute the (sub-)expression to substitute by something else.
   * @param pSubstitute the expression to substitute with.
   * @param pBinExpBuilder the binary-expression builder to be used to reconstruct binary
   *     expressions where an operand was modified by a substitution.
   * @return the substitute expression.
   * @throws SubstitutionException if the substitution fails.
   */
  public static CExpression applySubstitution(
      CExpression pExpression,
      CExpression pToSubstitute,
      CExpression pSubstitute,
      CBinaryExpressionBuilder pBinExpBuilder)
      throws SubstitutionException {
    Substitution substitution = pE -> pE.equals(pToSubstitute) ? pSubstitute : pE;
    return applySubstitution(pExpression, substitution, pBinExpBuilder);
  }

  public interface Substitution {

    /**
     * Substitutes the given expression by another expression.
     *
     * @param e the expression to substitute.
     * @return the substitute expression.
     * @throws SubstitutionException if the substitution fails.
     */
    CExpression substitute(CExpression e) throws SubstitutionException;
  }

  public static class SubstitutingVisitor
      implements CExpressionVisitor<CExpression, SubstitutionException> {

    private final Substitution substitution;

    private final CBinaryExpressionBuilder binExpBuilder;

    public SubstitutingVisitor(
        Substitution pSubstitution, CBinaryExpressionBuilder pBinExpBuilder) {
      substitution = pSubstitution;
      binExpBuilder = pBinExpBuilder;
    }

    @Override
    public CExpression visit(CArraySubscriptExpression pArraySubscriptExpression)
        throws SubstitutionException {
      CExpression arrayExpr = pArraySubscriptExpression.getArrayExpression().accept(this);
      CExpression subscExpr = pArraySubscriptExpression.getSubscriptExpression().accept(this);
      CExpression toSubstitute = pArraySubscriptExpression;
      if (arrayExpr != pArraySubscriptExpression.getArrayExpression()
          && subscExpr != pArraySubscriptExpression.getSubscriptExpression()) {
        toSubstitute =
            new CArraySubscriptExpression(
                pArraySubscriptExpression.getFileLocation(),
                pArraySubscriptExpression.getExpressionType(),
                arrayExpr,
                subscExpr);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CFieldReference pFieldReference) throws SubstitutionException {
      CExpression owner = pFieldReference.getFieldOwner().accept(this);
      CExpression toSubstitute = pFieldReference;
      if (owner != pFieldReference.getFieldOwner()) {
        toSubstitute =
            new CFieldReference(
                pFieldReference.getFileLocation(),
                pFieldReference.getExpressionType(),
                pFieldReference.getFieldName(),
                owner,
                pFieldReference.isPointerDereference());
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CIdExpression pIdExpression) throws SubstitutionException {
      return substitution.substitute(pIdExpression);
    }

    @Override
    public CExpression visit(CPointerExpression pPointerExpression) throws SubstitutionException {
      CExpression operand = pPointerExpression.getOperand().accept(this);
      CExpression toSubstitute = pPointerExpression;
      if (operand != pPointerExpression.getOperand()) {
        toSubstitute =
            new CPointerExpression(
                pPointerExpression.getFileLocation(),
                pPointerExpression.getExpressionType(),
                operand);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CComplexCastExpression pComplexCastExpression)
        throws SubstitutionException {
      CExpression operand = pComplexCastExpression.getOperand().accept(this);
      CExpression toSubstitute = pComplexCastExpression;
      if (operand != pComplexCastExpression.getOperand()) {
        toSubstitute =
            new CPointerExpression(
                pComplexCastExpression.getFileLocation(),
                pComplexCastExpression.getExpressionType(),
                operand);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CBinaryExpression pBinaryExpression) throws SubstitutionException {
      CExpression op1 = pBinaryExpression.getOperand1().accept(this);
      CExpression op2 = pBinaryExpression.getOperand2().accept(this);
      CExpression toSubstitute = pBinaryExpression;
      if (op1 != pBinaryExpression.getOperand1() || op2 != pBinaryExpression.getOperand2()) {
        try {
          toSubstitute =
              binExpBuilder.buildBinaryExpression(op1, op2, pBinaryExpression.getOperator());
        } catch (UnrecognizedCodeException e) {
          throw new SubstitutionException(e);
        }
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CCastExpression pCastExpression) throws SubstitutionException {
      CExpression operand = pCastExpression.getOperand().accept(this);
      CExpression toSubstitute = pCastExpression;
      if (operand != pCastExpression.getOperand()) {
        toSubstitute =
            new CCastExpression(
                pCastExpression.getFileLocation(), pCastExpression.getCastType(), operand);
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CCharLiteralExpression pCharLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pCharLiteralExpression);
    }

    @Override
    public CExpression visit(CFloatLiteralExpression pFloatLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pFloatLiteralExpression);
    }

    @Override
    public CExpression visit(CIntegerLiteralExpression pIntegerLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pIntegerLiteralExpression);
    }

    @Override
    public CExpression visit(CStringLiteralExpression pStringLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pStringLiteralExpression);
    }

    @Override
    public CExpression visit(CTypeIdExpression pTypeIdExpression) throws SubstitutionException {
      return substitution.substitute(pTypeIdExpression);
    }

    @Override
    public CExpression visit(CUnaryExpression pUnaryExpression) throws SubstitutionException {
      CExpression operand = pUnaryExpression.getOperand().accept(this);
      CExpression toSubstitute = pUnaryExpression;
      if (operand != pUnaryExpression.getOperand()) {
        toSubstitute =
            new CUnaryExpression(
                pUnaryExpression.getFileLocation(),
                pUnaryExpression.getExpressionType(),
                operand,
                pUnaryExpression.getOperator());
      }
      return substitution.substitute(toSubstitute);
    }

    @Override
    public CExpression visit(CImaginaryLiteralExpression pLiteralExpression)
        throws SubstitutionException {
      return substitution.substitute(pLiteralExpression);
    }

    @Override
    public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
        throws SubstitutionException {
      return substitution.substitute(pAddressOfLabelExpression);
    }
  }

  public static class SubstitutionException extends Exception {

    private static final long serialVersionUID = 1L;

    public SubstitutionException(String pMessage) {
      super(pMessage);
    }

    public SubstitutionException(Throwable pCause) {
      super(pCause);
    }

    public SubstitutionException(String pMessage, Throwable pCause) {
      super(pMessage, pCause);
    }
  }
}
