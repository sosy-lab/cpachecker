/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value.symbolic.type;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

/**
 * Unit tests for {@link SymbolicExpression} sub types.
 */
public class SymbolicExpressionTest {

  private static final CType OP_TYPE = CNumericTypes.INT;
  private static final CType POINTER_TYPE = CNumericTypes.UNSIGNED_INT;
  private static final CType PROMOTED_OP_TYPE = CNumericTypes.INT;

  private static final ConstantSymbolicExpression CONSTANT_OP1
      = new ConstantSymbolicExpression(new NumericValue(1), OP_TYPE);

  private static final ConstantSymbolicExpression CONSTANT_OP2
      = new ConstantSymbolicExpression(new NumericValue(5), OP_TYPE);

  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  @Test
  public void testEquals_BinarySymbolicExpression() {
    AdditionExpression add1 = new AdditionExpression(CONSTANT_OP1,
        CONSTANT_OP2,
        PROMOTED_OP_TYPE,
        PROMOTED_OP_TYPE);
    AdditionExpression add2 = new AdditionExpression(CONSTANT_OP1,
        CONSTANT_OP2,
        PROMOTED_OP_TYPE,
        PROMOTED_OP_TYPE);
    SubtractionExpression sub1 = new SubtractionExpression(CONSTANT_OP1,
        CONSTANT_OP2,
        PROMOTED_OP_TYPE,
        PROMOTED_OP_TYPE);

    Assert.assertTrue(add1.equals(add2));
    Assert.assertFalse(add1.equals(sub1));
  }

  @SuppressWarnings("EqualsBetweenInconvertibleTypes")
  @Test
  public void testEquals_UnarySymbolicExpression() {
    NegationExpression neg1 = new NegationExpression(CONSTANT_OP1, POINTER_TYPE);
    NegationExpression neg2 = new NegationExpression(CONSTANT_OP1, POINTER_TYPE);
    PointerExpression ptr = new PointerExpression(CONSTANT_OP1, POINTER_TYPE);

    Assert.assertTrue(neg1.equals(neg2));
    Assert.assertFalse(neg1.equals(ptr));
  }
}
