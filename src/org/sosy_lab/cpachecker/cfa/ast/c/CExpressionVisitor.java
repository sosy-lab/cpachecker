// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

public interface CExpressionVisitor<R, X extends Exception> extends CLeftHandSideVisitor<R, X> {

  R visit(CBinaryExpression pIastBinaryExpression) throws X;

  R visit(CCastExpression pIastCastExpression) throws X;

  R visit(CCharLiteralExpression pIastCharLiteralExpression) throws X;

  R visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws X;

  R visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) throws X;

  R visit(CStringLiteralExpression pIastStringLiteralExpression) throws X;

  R visit(CTypeIdExpression pIastTypeIdExpression) throws X;

  R visit(CUnaryExpression pIastUnaryExpression) throws X;

  R visit(CImaginaryLiteralExpression PIastLiteralExpression) throws X;

  R visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws X;
}
