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

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cpa.fsmbdd.ExpressionToString;


public class ExpressionToStringTest {

  @Test
  public void test() {
    ExpressionToString v = new ExpressionToString();
    CIdExpression a = new CIdExpression(null, null, "a", null);
    CIdExpression b = new CIdExpression(null, null, "b", null);
    CBinaryExpression bin1 = new CBinaryExpression(null, null, a, b, BinaryOperator.LOGICAL_AND);
    CBinaryExpression bin2 = new CBinaryExpression(null, null, bin1, b, BinaryOperator.LOGICAL_AND);

    System.out.println(bin2.accept(v));
  }

}
