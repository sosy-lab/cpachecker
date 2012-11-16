/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.fsmbdd.tests;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cpa.fsmbdd.ExpressionCache;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.UnrecognizedSyntaxException;


public class ExpressionCache2Test {

  @Test
  public void testLookupCachedExpressionVersion() throws UnrecognizedSyntaxException {
    ExpressionCache cache = new ExpressionCache();
    BinaryOperator opr = BinaryOperator.EQUALS;
    CExpression opA1 = new CIdExpression(null, null, "a", null);
    CBinaryExpression bin = new CBinaryExpression(null, null, opA1, opA1, opr);

    CExpression cached1 = cache.lookupCachedExpressionVersion(bin);
    CExpression cached2 = cache.lookupCachedExpressionVersion(bin);
    Assert.assertEquals(cached1, cached2);
  }

  @Test
  public void test2() throws UnrecognizedSyntaxException {
    ExpressionCache cache = new ExpressionCache();
    BinaryOperator opr = BinaryOperator.EQUALS;
    CExpression opA1 = new CIdExpression(null, null, "a", null);
    CExpression opA2 = new CIdExpression(null, null, "a", null);
    CBinaryExpression bin1 = new CBinaryExpression(null, null, opA1, opA1, opr);
    CBinaryExpression bin2 = new CBinaryExpression(null, null, opA2, opA2, opr);

    Assert.assertEquals(
        cache.lookupCachedExpressionVersion(bin1),
        cache.lookupCachedExpressionVersion(bin2));

    CExpression cached1 = cache.binaryExpression(BinaryOperator.LOGICAL_AND, bin1, bin2);
    CExpression cached2 = cache.binaryExpression(BinaryOperator.LOGICAL_AND, bin1, bin2);

    Assert.assertEquals(cached1, cached2);
  }

}
