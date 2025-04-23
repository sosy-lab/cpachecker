// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface AcslExpressionVisitor<R, X extends Exception> {

  R visit(AcslBinaryPredicateExpression pBinaryExpression) throws X;

  R visit(AcslUnaryExpression pAcslUnaryExpression) throws X;

  R visit(AcslIdExpression pAcslIdExpression) throws X;

  R visit(AcslBinaryTermExpression pAcslBinaryTermExpression) throws X;

  R visit(AcslOldExpression pAcslOldExpression) throws X;

  R visit(AcslBooleanLiteralExpression pAcslBooleanLiteralExpression) throws X;

  R visit(AcslTernaryTermExpression pAcslTernaryTermExpression) throws X;

  R visit(AcslTernaryPredicateExpression pAcslTernaryPredicateExpression) throws X;
}
