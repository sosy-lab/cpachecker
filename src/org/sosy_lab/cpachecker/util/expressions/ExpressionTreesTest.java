/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.test.TestDataTools;


public class ExpressionTreesTest {

  private static final ExpressionTree<AExpression> LITERAL_A =
      LeafExpression.of((AExpression) TestDataTools.makeVariable("a", CNumericTypes.INT));

  private static final ExpressionTree<AExpression> LITERAL_NOT_A =
      LeafExpression.of((AExpression) TestDataTools.makeVariable("a", CNumericTypes.INT), false);

  private static final ExpressionTree<AExpression> LITERAL_B =
      LeafExpression.of((AExpression) TestDataTools.makeVariable("b", CNumericTypes.INT));

  private static final ExpressionTree<AExpression> LITERAL_C =
      LeafExpression.of((AExpression) TestDataTools.makeVariable("c", CNumericTypes.INT));

  private static final ExpressionTree<AExpression> COMPLEX_CNF =
      And.of(Or.of(LITERAL_A, LITERAL_B), Or.of(LITERAL_C, LITERAL_NOT_A));

  private static final ExpressionTree<AExpression> COMPLEX_DNF =
      Or.of(And.of(LITERAL_A, LITERAL_B), And.of(LITERAL_C, LITERAL_NOT_A));

  @Test
  public void testIsConstant() {
    Assert.assertTrue(ExpressionTrees.isConstant(ExpressionTrees.getTrue()));
    Assert.assertTrue(ExpressionTrees.isConstant(ExpressionTrees.getFalse()));
    Assert.assertFalse(ExpressionTrees.isConstant(LITERAL_A));
    Assert.assertFalse(ExpressionTrees.isConstant(LITERAL_NOT_A));
    Assert.assertFalse(ExpressionTrees.isConstant(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertFalse(ExpressionTrees.isConstant(Or.of(LITERAL_B, LITERAL_C)));
  }

  @Test
  public void testIsLeaf() {
    Assert.assertTrue(ExpressionTrees.isLeaf(ExpressionTrees.getTrue()));
    Assert.assertTrue(ExpressionTrees.isLeaf(ExpressionTrees.getFalse()));
    Assert.assertTrue(ExpressionTrees.isLeaf(LITERAL_A));
    Assert.assertTrue(ExpressionTrees.isLeaf(LITERAL_NOT_A));
    Assert.assertFalse(ExpressionTrees.isLeaf(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertFalse(ExpressionTrees.isLeaf(Or.of(LITERAL_B, LITERAL_C)));
  }

  @Test
  public void testIsOr() {
    Assert.assertFalse(ExpressionTrees.isOr(ExpressionTrees.getTrue()));
    Assert.assertFalse(ExpressionTrees.isOr(ExpressionTrees.getFalse()));
    Assert.assertFalse(ExpressionTrees.isOr(LITERAL_A));
    Assert.assertFalse(ExpressionTrees.isOr(LITERAL_NOT_A));
    Assert.assertFalse(ExpressionTrees.isOr(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertTrue(ExpressionTrees.isOr(Or.of(LITERAL_B, LITERAL_C)));
  }

  @Test
  public void testIsAnd() {
    Assert.assertFalse(ExpressionTrees.isAnd(ExpressionTrees.getTrue()));
    Assert.assertFalse(ExpressionTrees.isAnd(ExpressionTrees.getFalse()));
    Assert.assertFalse(ExpressionTrees.isAnd(LITERAL_A));
    Assert.assertFalse(ExpressionTrees.isAnd(LITERAL_NOT_A));
    Assert.assertTrue(ExpressionTrees.isAnd(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertFalse(ExpressionTrees.isAnd(Or.of(LITERAL_B, LITERAL_C)));
  }

  @Test
  public void testIsInCNF() {
    Assert.assertTrue(ExpressionTrees.isInCNF(ExpressionTrees.getTrue()));
    Assert.assertTrue(ExpressionTrees.isInCNF(ExpressionTrees.getFalse()));
    Assert.assertTrue(ExpressionTrees.isInCNF(LITERAL_A));
    Assert.assertTrue(ExpressionTrees.isInCNF(LITERAL_NOT_A));
    Assert.assertTrue(ExpressionTrees.isInCNF(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertTrue(ExpressionTrees.isInCNF(Or.of(LITERAL_B, LITERAL_C)));

    Assert.assertTrue(ExpressionTrees.isInCNF(COMPLEX_CNF));

    Assert.assertFalse(ExpressionTrees.isInCNF(COMPLEX_DNF));
  }

  @Test
  public void testIsInDNF() {
    Assert.assertTrue(ExpressionTrees.isInDNF(ExpressionTrees.getTrue()));
    Assert.assertTrue(ExpressionTrees.isInDNF(ExpressionTrees.getFalse()));
    Assert.assertTrue(ExpressionTrees.isInDNF(LITERAL_A));
    Assert.assertTrue(ExpressionTrees.isInDNF(LITERAL_NOT_A));
    Assert.assertTrue(ExpressionTrees.isInDNF(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertTrue(ExpressionTrees.isInDNF(Or.of(LITERAL_B, LITERAL_C)));

    Assert.assertTrue(ExpressionTrees.isInDNF(COMPLEX_DNF));

    Assert.assertFalse(ExpressionTrees.isInDNF(COMPLEX_CNF));
  }

  @Test
  public void testToDNF() {
    Assert.assertEquals(
        ExpressionTrees.getTrue(), ExpressionTrees.toDNF(ExpressionTrees.getTrue()));
    Assert.assertEquals(
        ExpressionTrees.getFalse(), ExpressionTrees.toDNF(ExpressionTrees.getFalse()));
    Assert.assertEquals(
        LITERAL_A,
        ExpressionTrees.toDNF(LITERAL_A));
    Assert.assertEquals(
        LITERAL_NOT_A,
        ExpressionTrees.toDNF(LITERAL_NOT_A));
    Assert.assertEquals(
        And.of(LITERAL_B, LITERAL_C), ExpressionTrees.toDNF(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertEquals(
        Or.of(LITERAL_B, LITERAL_C), ExpressionTrees.toDNF(Or.of(LITERAL_B, LITERAL_C)));

    Assert.assertEquals(COMPLEX_DNF, ExpressionTrees.toDNF(COMPLEX_DNF));

    Assert.assertTrue(ExpressionTrees.isInDNF(ExpressionTrees.toDNF(COMPLEX_CNF)));
  }

  @Test
  public void testToCNF() {
    Assert.assertEquals(
        ExpressionTrees.getTrue(), ExpressionTrees.toDNF(ExpressionTrees.getTrue()));
    Assert.assertEquals(
        ExpressionTrees.getFalse(), ExpressionTrees.toDNF(ExpressionTrees.getFalse()));
    Assert.assertEquals(
        LITERAL_A,
        ExpressionTrees.toDNF(LITERAL_A));
    Assert.assertEquals(
        LITERAL_NOT_A,
        ExpressionTrees.toDNF(LITERAL_NOT_A));
    Assert.assertEquals(
        And.of(LITERAL_B, LITERAL_C), ExpressionTrees.toDNF(And.of(LITERAL_B, LITERAL_C)));
    Assert.assertEquals(
        Or.of(LITERAL_B, LITERAL_C), ExpressionTrees.toDNF(Or.of(LITERAL_B, LITERAL_C)));

    Assert.assertEquals(COMPLEX_CNF, ExpressionTrees.toCNF(COMPLEX_CNF));

    Assert.assertTrue(ExpressionTrees.isInCNF(ExpressionTrees.toCNF(COMPLEX_DNF)));
  }

}
