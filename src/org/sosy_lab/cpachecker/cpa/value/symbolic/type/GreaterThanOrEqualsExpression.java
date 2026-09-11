// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression.getCanonicalType;

import org.sosy_lab.cpachecker.cfa.types.Type;

public class GreaterThanOrEqualsExpression {

  /**
   * Creates a symbolic greater or equals (>=) expression based on a {@link
   * LessThanOrEqualExpression} with switched operands.
   */
  public static SymbolicExpression of(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {

    // represent 'a >= b' as 'b <= a' so we do need less classes
    return new LessThanOrEqualExpression(
        pOperand2, pOperand1, getCanonicalType(pType), getCanonicalType(pCalculationType));
  }
}
