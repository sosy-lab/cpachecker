// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

/**
 * Visitor for {@link SymbolicValue}s.
 *
 * @param <T> return type of visit methods
 */
public interface SymbolicValueVisitor<T> {

  T visit(SymbolicIdentifier pValue);

  T visit(ConstantSymbolicExpression pExpression);

  T visit(AdditionExpression pExpression);

  T visit(SubtractionExpression pExpression);

  T visit(MultiplicationExpression pExpression);

  T visit(DivisionExpression pExpression);

  T visit(ModuloExpression pExpression);

  T visit(BinaryAndExpression pExpression);

  T visit(BinaryNotExpression pExpression);

  T visit(BinaryOrExpression pExpression);

  T visit(BinaryXorExpression pExpression);

  T visit(ShiftRightExpression pExpression);

  T visit(ShiftLeftExpression pExpression);

  T visit(LogicalNotExpression pExpression);

  T visit(LessThanOrEqualExpression pExpression);

  T visit(LessThanExpression pExpression);

  T visit(EqualsExpression pExpression);

  T visit(LogicalOrExpression pExpression);

  T visit(LogicalAndExpression pExpression);

  T visit(CastExpression pExpression);

  T visit(PointerExpression pExpression);

  T visit(AddressOfExpression pExpression);

  T visit(NegationExpression pExpression);
}
