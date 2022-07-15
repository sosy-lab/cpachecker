// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

/** Unit tests for {@link SymbolicExpression} sub types. */
public class SymbolicExpressionTest {

  private static final CType OP_TYPE = CNumericTypes.INT;
  private static final CType POINTER_TYPE = CNumericTypes.UNSIGNED_INT;
  private static final CType PROMOTED_OP_TYPE = CNumericTypes.INT;

  private static final ConstantSymbolicExpression CONSTANT_OP1 =
      new ConstantSymbolicExpression(new NumericValue(1), OP_TYPE);

  private static final ConstantSymbolicExpression CONSTANT_OP2 =
      new ConstantSymbolicExpression(new NumericValue(5), OP_TYPE);

  @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "unlikely-arg-type"})
  @Test
  public void testEquals_BinarySymbolicExpression() {
    AdditionExpression add1 =
        new AdditionExpression(CONSTANT_OP1, CONSTANT_OP2, PROMOTED_OP_TYPE, PROMOTED_OP_TYPE);
    AdditionExpression add2 =
        new AdditionExpression(CONSTANT_OP1, CONSTANT_OP2, PROMOTED_OP_TYPE, PROMOTED_OP_TYPE);
    SubtractionExpression sub1 =
        new SubtractionExpression(CONSTANT_OP1, CONSTANT_OP2, PROMOTED_OP_TYPE, PROMOTED_OP_TYPE);

    assertThat(add1).isEqualTo(add2);
    assertThat(add1).isNotEqualTo(sub1);
  }

  @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "unlikely-arg-type"})
  @Test
  public void testEquals_UnarySymbolicExpression() {
    NegationExpression neg1 = new NegationExpression(CONSTANT_OP1, POINTER_TYPE);
    NegationExpression neg2 = new NegationExpression(CONSTANT_OP1, POINTER_TYPE);
    PointerExpression ptr = new PointerExpression(CONSTANT_OP1, POINTER_TYPE);

    assertThat(neg1).isEqualTo(neg2);
    assertThat(neg1).isNotEqualTo(ptr);
  }
}
