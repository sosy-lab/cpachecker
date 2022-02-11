// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.java;

/**
 * Interface for the visitor pattern. Typically used with {@link org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor}
 * to evaluate expressions.
 *
 *
 * @param <R> the return type of an evaluation.
 * @param <X> the exception thrown, if there are errors while evaluating an expression.
 */
public interface JLeftHandSideVisitor<R, X extends Exception> {

  R visit(JArraySubscriptExpression pAArraySubscriptExpression) throws X;

  R visit(JIdExpression pJIdExpression) throws X;
}
