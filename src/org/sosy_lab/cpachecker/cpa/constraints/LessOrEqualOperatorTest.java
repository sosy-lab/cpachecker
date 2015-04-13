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
package org.sosy_lab.cpachecker.cpa.constraints;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * Tests for {@link LessOrEqualOperator}.
 */
public class LessOrEqualOperatorTest {

  private final MemoryLocation memLoc1 = MemoryLocation.valueOf("a");
  private final MemoryLocation memLoc2 = MemoryLocation.valueOf("b");

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();
  private final Type defType = CNumericTypes.INT;

  private final SymbolicExpression locLessId1 = factory.asConstant(factory.newIdentifier(), defType);
  private final SymbolicExpression locLessId2 = factory.asConstant(factory.newIdentifier(), defType);

  private final SymbolicExpression number1 = factory.asConstant(new NumericValue(5), defType);

  private final SymbolicExpression locLessExp1 = factory.add(locLessId1, locLessId2, defType,
      defType);
  private final SymbolicExpression locLessExp2 = factory.add(locLessId1, number1, defType, defType);

  private final LessOrEqualOperator leqOp = LessOrEqualOperator.getInstance();

  @Test
  public void testhaveEqualMeaning() {
    SymbolicExpression exp1 = locLessExp1.copyForLocation(memLoc1);
    SymbolicExpression exp2 = locLessExp1.copyForLocation(memLoc2);
    SymbolicExpression exp3 = locLessExp2.copyForLocation(memLoc2);

    SymbolicExpression constr1 = factory.lessThan(exp1, exp3, defType, defType);
    SymbolicExpression constr2 = factory.lessThan(exp1, exp2, defType, defType);

    Assert.assertTrue(leqOp.haveEqualMeaning(exp1, exp1));
    Assert.assertTrue(leqOp.haveEqualMeaning(constr1, constr2));
    Assert.assertFalse(leqOp.haveEqualMeaning(exp1, exp2));
  }
}