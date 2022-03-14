// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class ExpressionTreesTest {

  private static final ExpressionTree<AExpression> LITERAL_A =
      LeafExpression.of(TestDataTools.makeVariable("a", CNumericTypes.INT));

  private static final ExpressionTree<AExpression> LITERAL_NOT_A =
      LeafExpression.of(TestDataTools.makeVariable("a", CNumericTypes.INT), false);

  private static final ExpressionTree<AExpression> LITERAL_B =
      LeafExpression.of(TestDataTools.makeVariable("b", CNumericTypes.INT));

  private static final ExpressionTree<AExpression> LITERAL_C =
      LeafExpression.of(TestDataTools.makeVariable("c", CNumericTypes.INT));

  private static final ExpressionTree<AExpression> COMPLEX_CNF =
      And.of(Or.of(LITERAL_A, LITERAL_B), Or.of(LITERAL_C, LITERAL_NOT_A));

  private static final ExpressionTree<AExpression> COMPLEX_DNF =
      Or.of(And.of(LITERAL_A, LITERAL_B), And.of(LITERAL_C, LITERAL_NOT_A));

  @Test
  public void testIsConstant() {
    assertThat(ExpressionTrees.isConstant(ExpressionTrees.getTrue())).isTrue();
    assertThat(ExpressionTrees.isConstant(ExpressionTrees.getFalse())).isTrue();
    assertThat(ExpressionTrees.isConstant(LITERAL_A)).isFalse();
    assertThat(ExpressionTrees.isConstant(LITERAL_NOT_A)).isFalse();
    assertThat(ExpressionTrees.isConstant(And.of(LITERAL_B, LITERAL_C))).isFalse();
    assertThat(ExpressionTrees.isConstant(Or.of(LITERAL_B, LITERAL_C))).isFalse();
  }

  @Test
  public void testIsLeaf() {
    assertThat(ExpressionTrees.isLeaf(ExpressionTrees.getTrue())).isTrue();
    assertThat(ExpressionTrees.isLeaf(ExpressionTrees.getFalse())).isTrue();
    assertThat(ExpressionTrees.isLeaf(LITERAL_A)).isTrue();
    assertThat(ExpressionTrees.isLeaf(LITERAL_NOT_A)).isTrue();
    assertThat(ExpressionTrees.isLeaf(And.of(LITERAL_B, LITERAL_C))).isFalse();
    assertThat(ExpressionTrees.isLeaf(Or.of(LITERAL_B, LITERAL_C))).isFalse();
  }

  @Test
  public void testIsOr() {
    assertThat(ExpressionTrees.isOr(ExpressionTrees.getTrue())).isFalse();
    assertThat(ExpressionTrees.isOr(ExpressionTrees.getFalse())).isFalse();
    assertThat(ExpressionTrees.isOr(LITERAL_A)).isFalse();
    assertThat(ExpressionTrees.isOr(LITERAL_NOT_A)).isFalse();
    assertThat(ExpressionTrees.isOr(And.of(LITERAL_B, LITERAL_C))).isFalse();
    assertThat(ExpressionTrees.isOr(Or.of(LITERAL_B, LITERAL_C))).isTrue();
  }

  @Test
  public void testIsAnd() {
    assertThat(ExpressionTrees.isAnd(ExpressionTrees.getTrue())).isFalse();
    assertThat(ExpressionTrees.isAnd(ExpressionTrees.getFalse())).isFalse();
    assertThat(ExpressionTrees.isAnd(LITERAL_A)).isFalse();
    assertThat(ExpressionTrees.isAnd(LITERAL_NOT_A)).isFalse();
    assertThat(ExpressionTrees.isAnd(And.of(LITERAL_B, LITERAL_C))).isTrue();
    assertThat(ExpressionTrees.isAnd(Or.of(LITERAL_B, LITERAL_C))).isFalse();
  }

  @Test
  public void testIsInCNF() {
    assertThat(ExpressionTrees.isInCNF(ExpressionTrees.getTrue())).isTrue();
    assertThat(ExpressionTrees.isInCNF(ExpressionTrees.getFalse())).isTrue();
    assertThat(ExpressionTrees.isInCNF(LITERAL_A)).isTrue();
    assertThat(ExpressionTrees.isInCNF(LITERAL_NOT_A)).isTrue();
    assertThat(ExpressionTrees.isInCNF(And.of(LITERAL_B, LITERAL_C))).isTrue();
    assertThat(ExpressionTrees.isInCNF(Or.of(LITERAL_B, LITERAL_C))).isTrue();

    assertThat(ExpressionTrees.isInCNF(COMPLEX_CNF)).isTrue();

    assertThat(ExpressionTrees.isInCNF(COMPLEX_DNF)).isFalse();
  }

  @Test
  public void testIsInDNF() {
    assertThat(ExpressionTrees.isInDNF(ExpressionTrees.getTrue())).isTrue();
    assertThat(ExpressionTrees.isInDNF(ExpressionTrees.getFalse())).isTrue();
    assertThat(ExpressionTrees.isInDNF(LITERAL_A)).isTrue();
    assertThat(ExpressionTrees.isInDNF(LITERAL_NOT_A)).isTrue();
    assertThat(ExpressionTrees.isInDNF(And.of(LITERAL_B, LITERAL_C))).isTrue();
    assertThat(ExpressionTrees.isInDNF(Or.of(LITERAL_B, LITERAL_C))).isTrue();

    assertThat(ExpressionTrees.isInDNF(COMPLEX_DNF)).isTrue();

    assertThat(ExpressionTrees.isInDNF(COMPLEX_CNF)).isFalse();
  }

  @Test
  public void testToDNF() {
    assertThat(ExpressionTrees.toDNF(ExpressionTrees.getTrue()))
        .isEqualTo(ExpressionTrees.getTrue());
    assertThat(ExpressionTrees.toDNF(ExpressionTrees.getFalse()))
        .isEqualTo(ExpressionTrees.getFalse());
    assertThat(ExpressionTrees.toDNF(LITERAL_A)).isEqualTo(LITERAL_A);
    assertThat(ExpressionTrees.toDNF(LITERAL_NOT_A)).isEqualTo(LITERAL_NOT_A);
    assertThat(ExpressionTrees.toDNF(And.of(LITERAL_B, LITERAL_C)))
        .isEqualTo(And.of(LITERAL_B, LITERAL_C));
    assertThat(ExpressionTrees.toDNF(Or.of(LITERAL_B, LITERAL_C)))
        .isEqualTo(Or.of(LITERAL_B, LITERAL_C));

    assertThat(ExpressionTrees.toDNF(COMPLEX_DNF)).isEqualTo(COMPLEX_DNF);

    assertThat(ExpressionTrees.isInDNF(ExpressionTrees.toDNF(COMPLEX_CNF))).isTrue();
  }

  @Test
  public void testToCNF() {
    assertThat(ExpressionTrees.toDNF(ExpressionTrees.getTrue()))
        .isEqualTo(ExpressionTrees.getTrue());
    assertThat(ExpressionTrees.toDNF(ExpressionTrees.getFalse()))
        .isEqualTo(ExpressionTrees.getFalse());
    assertThat(ExpressionTrees.toDNF(LITERAL_A)).isEqualTo(LITERAL_A);
    assertThat(ExpressionTrees.toDNF(LITERAL_NOT_A)).isEqualTo(LITERAL_NOT_A);
    assertThat(ExpressionTrees.toDNF(And.of(LITERAL_B, LITERAL_C)))
        .isEqualTo(And.of(LITERAL_B, LITERAL_C));
    assertThat(ExpressionTrees.toDNF(Or.of(LITERAL_B, LITERAL_C)))
        .isEqualTo(Or.of(LITERAL_B, LITERAL_C));

    assertThat(ExpressionTrees.toCNF(COMPLEX_CNF)).isEqualTo(COMPLEX_CNF);

    assertThat(ExpressionTrees.isInCNF(ExpressionTrees.toCNF(COMPLEX_DNF))).isTrue();
  }
}
