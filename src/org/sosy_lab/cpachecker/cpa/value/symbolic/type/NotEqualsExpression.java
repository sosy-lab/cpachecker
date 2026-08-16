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

public class NotEqualsExpression {

  /** Builds a not equals expression, i.e. !=, out of a logically negated equality expression. */
  public static SymbolicExpression of(
      SymbolicExpression pOperand1,
      SymbolicExpression pOperand2,
      Type pType,
      Type pCalculationType) {
    return LogicalNotExpression.of(
        EqualsExpression.of(
            pOperand1, pOperand2, getCanonicalType(pType), getCanonicalType(pCalculationType)),
        pType);
  }
}
