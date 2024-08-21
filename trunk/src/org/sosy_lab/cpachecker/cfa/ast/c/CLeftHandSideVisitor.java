// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

public interface CLeftHandSideVisitor<R, X extends Exception> {

  R visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws X;

  R visit(CFieldReference pIastFieldReference) throws X;

  R visit(CIdExpression pIastIdExpression) throws X;

  R visit(CPointerExpression pointerExpression) throws X;

  R visit(CComplexCastExpression complexCastExpression) throws X;
}
