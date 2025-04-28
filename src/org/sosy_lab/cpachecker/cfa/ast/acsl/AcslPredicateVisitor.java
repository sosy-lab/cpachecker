// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

public interface AcslPredicateVisitor<R, X extends Exception> {

  R visit(AcslBinaryPredicate pBinaryExpression) throws X;

  R visit(AcslUnaryPredicate pAcslUnaryPredicate) throws X;

  R visit(AcslIdPredicate pAcslIdPredicate) throws X;

  R visit(AcslBinaryTermPredicate pAcslBinaryTermPredicate) throws X;

  R visit(AcslOldPredicate pAcslOldPredicate) throws X;

  R visit(AcslBooleanLiteralPredicate pAcslBooleanLiteralPredicate) throws X;

  R visit(AcslTernaryPredicate pAcslTernaryPredicate) throws X;

  R visit(AcslValidPredicate pAcslValidPredicate) throws X;
}
