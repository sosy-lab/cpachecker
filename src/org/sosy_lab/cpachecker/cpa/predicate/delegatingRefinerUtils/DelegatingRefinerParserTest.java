// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

public class DelegatingRefinerParserTest {

  @Test
  public void parseSingleToken_returnZeroArityOperator() {
    DelegatingRefinerPatternNode node = DelegatingRefinerParser.parseExpression("x");
    assertThat(node instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator operator = (DelegatingRefinerPatternOperator) node;
    assertThat(operator.operator()).isEqualTo("x");
    assertThat(operator.sExpressionList()).isEmpty();
  }

  @Test
  public void parseNoise_patternCorrect() {
    DelegatingRefinerPatternNode node =
        DelegatingRefinerParser.parseExpression("(\nbvlshl\t    <var>\t\n     <const>\t\n)");
    assertThat(node instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator bitVectorOperator = (DelegatingRefinerPatternOperator) node;
    assertThat(bitVectorOperator.operator()).isEqualTo("bvlshl");
    assertThat(bitVectorOperator.sExpressionList()).hasSize(2);
    assertThat(
            ((DelegatingRefinerDelegatingRefinerPatternAtom)
                    bitVectorOperator.sExpressionList().getFirst())
                .name())
        .isEqualTo("var");
    assertThat(
            ((DelegatingRefinerDelegatingRefinerPatternAtom)
                    bitVectorOperator.sExpressionList().getLast())
                .name())
        .isEqualTo("const");
  }

  @Test
  public void parseWildcardAtom_returnsAtom() {
    DelegatingRefinerPatternNode node = DelegatingRefinerParser.parseExpression("<var>");
    assertThat(node instanceof DelegatingRefinerDelegatingRefinerPatternAtom).isTrue();
    DelegatingRefinerDelegatingRefinerPatternAtom atom =
        (DelegatingRefinerDelegatingRefinerPatternAtom) node;
    assertThat(atom.name()).isEqualTo("var");
  }

  @Test
  public void parseOperatorWithWildcards_buildTree() {
    DelegatingRefinerPatternNode node =
        DelegatingRefinerParser.parseExpression("(bvadd <var> <const>)");
    assertThat(node instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator equalityPattern = (DelegatingRefinerPatternOperator) node;
    assertThat(equalityPattern.operator()).isEqualTo("bvadd");
    ImmutableList<DelegatingRefinerPatternNode> expressions = equalityPattern.sExpressionList();
    assertThat(expressions).hasSize(2);
    assertThat(expressions.getFirst() instanceof DelegatingRefinerDelegatingRefinerPatternAtom)
        .isTrue();
    assertThat(((DelegatingRefinerDelegatingRefinerPatternAtom) expressions.getFirst()).name())
        .isEqualTo("var");
    assertThat(expressions.get(1) instanceof DelegatingRefinerDelegatingRefinerPatternAtom)
        .isTrue();
    assertThat(((DelegatingRefinerDelegatingRefinerPatternAtom) expressions.getLast()).name())
        .isEqualTo("const");
  }

  @Test
  public void parseNested_buildNestedTree() {
    DelegatingRefinerPatternNode node =
        DelegatingRefinerParser.parseExpression("(and (= <var> <const>) (not <var2>))");
    assertThat(node instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator andOperator = (DelegatingRefinerPatternOperator) node;
    assertThat(andOperator.operator()).isEqualTo("and");
    assertThat(andOperator.sExpressionList()).hasSize(2);

    DelegatingRefinerPatternNode rightTree = andOperator.sExpressionList().getFirst();
    assertThat(rightTree instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator equalityOperator =
        (DelegatingRefinerPatternOperator) rightTree;
    assertThat(equalityOperator.operator()).isEqualTo("=");
    assertThat(equalityOperator.sExpressionList()).hasSize(2);
    assertThat(
            ((DelegatingRefinerDelegatingRefinerPatternAtom)
                    equalityOperator.sExpressionList().getFirst())
                .name())
        .isEqualTo("var");
    assertThat(
            ((DelegatingRefinerDelegatingRefinerPatternAtom)
                    equalityOperator.sExpressionList().getLast())
                .name())
        .isEqualTo("const");

    DelegatingRefinerPatternNode leftTree = andOperator.sExpressionList().getLast();
    assertThat(leftTree instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator notOperator = (DelegatingRefinerPatternOperator) leftTree;
    assertThat(notOperator.operator()).isEqualTo("not");
    assertThat(notOperator.sExpressionList()).hasSize(1);
    assertThat(
            ((DelegatingRefinerDelegatingRefinerPatternAtom)
                    notOperator.sExpressionList().getFirst())
                .name())
        .isEqualTo("var2");
  }

  @Test
  public void parseDeeplyNestedTree() {
    DelegatingRefinerPatternNode node =
        DelegatingRefinerParser.parseExpression(
            "(and (or (not (= <var> <const>)) (bvadd <const> <var>)))");
    assertThat(node instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator andOperator = (DelegatingRefinerPatternOperator) node;
    assertThat(andOperator.operator()).isEqualTo("and");
    assertThat(andOperator.sExpressionList()).hasSize(1);

    DelegatingRefinerPatternNode firstTree = andOperator.sExpressionList().getFirst();
    assertThat(firstTree instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator orOperator = (DelegatingRefinerPatternOperator) firstTree;
    assertThat(orOperator.operator()).isEqualTo("or");
    assertThat(orOperator.sExpressionList()).hasSize(2);

    DelegatingRefinerPatternNode secondTree = orOperator.sExpressionList().getFirst();
    assertThat(secondTree instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator notOperator = (DelegatingRefinerPatternOperator) secondTree;
    assertThat(notOperator.operator()).isEqualTo("not");
    assertThat(notOperator.sExpressionList()).hasSize(1);

    DelegatingRefinerPatternNode thirdTree = orOperator.sExpressionList().getLast();
    assertThat(thirdTree instanceof DelegatingRefinerPatternOperator).isTrue();
    DelegatingRefinerPatternOperator equalityOperator =
        (DelegatingRefinerPatternOperator) thirdTree;
    assertThat(equalityOperator.operator()).isEqualTo("bvadd");
    assertThat(equalityOperator.sExpressionList()).hasSize(2);
  }

  @Test
  public void parse_extraTokens() {
    IllegalArgumentException extraArguments =
        assertThrows(
            IllegalArgumentException.class,
            () -> DelegatingRefinerParser.parseExpression("(= <var1> <var2>) <var3>"));
    assertThat(extraArguments.getMessage()).contains("Extra tokens");
  }

  @Test
  public void parse_unmatchedParentheses() {
    IllegalArgumentException unmatchedParentheses =
        assertThrows(
            IllegalArgumentException.class, () -> DelegatingRefinerParser.parseExpression(")"));
    assertThat(unmatchedParentheses.getMessage()).contains("Unmatched ')'");
  }

  @Test
  public void parse_missingClosingParentheses() {
    IllegalArgumentException missingClosingParentheses =
        assertThrows(
            IllegalArgumentException.class,
            () -> DelegatingRefinerParser.parseExpression("(= <var>"));
    assertThat(missingClosingParentheses.getMessage()).contains("Missing ')'");
  }

  @Test
  public void parse_missingOperator() {
    IllegalArgumentException missingOperator =
        assertThrows(
            IllegalArgumentException.class, () -> DelegatingRefinerParser.parseExpression("()"));
    assertThat(missingOperator.getMessage()).contains("Missing ')'");
  }

  @Test
  public void parse_missingInput() {
    IllegalArgumentException missingInput =
        assertThrows(
            IllegalArgumentException.class, () -> DelegatingRefinerParser.parseExpression(""));
    assertThat(missingInput.getMessage()).contains("Pattern ended unexpectedly");
  }
}
