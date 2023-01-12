// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

public interface ExpressionTreeVisitor<LeafType, T, E extends Throwable> {

  T visit(And<LeafType> pAnd) throws E;

  T visit(Or<LeafType> pOr) throws E;

  T visit(LeafExpression<LeafType> pLeafExpression) throws E;

  T visitTrue() throws E;

  T visitFalse() throws E;
}
