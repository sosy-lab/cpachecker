/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;

public class SymbolicValueTest {
  @Test
  public void testSimplifyMinus() {
    // First create an expression in the form of "X - X"
    CVariableDeclaration decl = new CVariableDeclaration(null, true, CStorageClass.EXTERN, CNumericTypes.INT, "X", "X", "X", null);
    CIdExpression leftHand = new CIdExpression(null, CNumericTypes.INT, "X", decl);
    CIdExpression rightHand = new CIdExpression(null, CNumericTypes.INT, "X", decl);
    CBinaryExpression testExpression = new CBinaryExpression(null, CNumericTypes.INT, CNumericTypes.INT, leftHand, rightHand, CBinaryExpression.BinaryOperator.MINUS);

    // Now create a symbolic value representing "X - X" and simplify it
    SymbolicValue testValue = new SymbolicValue(testExpression);
    SymbolicValue simplifiedValue = testValue.simplify();

    // It should have been simplified to "0",
    // since the result type of the operation was an integer,
    // and X - X is 0 for all integers X.
    assertTrue(simplifiedValue.root instanceof CIntegerLiteralExpression);

    CIntegerLiteralExpression simplifiedExpr = (CIntegerLiteralExpression) simplifiedValue.root;
    assertEquals(simplifiedExpr.getValue().longValue(), 0);
  }
}
