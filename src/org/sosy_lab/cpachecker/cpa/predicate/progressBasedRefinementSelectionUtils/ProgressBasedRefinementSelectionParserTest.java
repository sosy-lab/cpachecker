// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternAtom;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternNode;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternOperator;

public class ProgressBasedRefinementSelectionParserTest {

  /**
   * This test checks if the Parser returns a PatternOperatorNode with an empty subtree associated
   * with it for a single token.
   */
  @Test
  public void checkParseSingleTokenReturnZeroArityOperator() {
    ProgressBasedRefinementSelectionPatternNode node =
        ProgressBasedRefinementSelectionParser.parseExpression("x");
    assertThat(node instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator operator =
        (ProgressBasedRefinementSelectionPatternOperator) node;
    assertThat(operator.operator()).isEqualTo("x");
    assertThat(operator.sExpressionList()).isEmpty();
  }

  /**
   * This test checks if the Parser ignores noise such as newline or tab separators and whitespaces.
   */
  @Test
  public void checkParseNoisePatternCorrect() {
    ProgressBasedRefinementSelectionPatternNode node =
        ProgressBasedRefinementSelectionParser.parseExpression(
            "(\nbvlshl\t    <var>\t\n     <const>\t\n)");
    assertThat(node instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator bitVectorOperator =
        (ProgressBasedRefinementSelectionPatternOperator) node;
    assertThat(bitVectorOperator.operator()).isEqualTo("bvlshl");
    assertThat(bitVectorOperator.sExpressionList()).hasSize(2);
    assertThat(
            ((ProgressBasedRefinementSelectionPatternAtom)
                    bitVectorOperator.sExpressionList().getFirst())
                .name())
        .isEqualTo("var");
    assertThat(
            ((ProgressBasedRefinementSelectionPatternAtom)
                    bitVectorOperator.sExpressionList().getLast())
                .name())
        .isEqualTo("const");
  }

  /**
   * This test checks if the Parser returns a PatternAtom for a single wildcard such as {@code var}.
   */
  @Test
  public void checkParseWildcardAtomReturnsAtom() {
    ProgressBasedRefinementSelectionPatternNode node =
        ProgressBasedRefinementSelectionParser.parseExpression("<var>");
    assertThat(node instanceof ProgressBasedRefinementSelectionPatternAtom).isTrue();
    ProgressBasedRefinementSelectionPatternAtom atom =
        (ProgressBasedRefinementSelectionPatternAtom) node;
    assertThat(atom.name()).isEqualTo("var");
  }

  /**
   * This test checks if the Parser returns the correct expression tree for an associated operator.
   */
  @Test
  public void checkParseOperatorWithWildcardsBuildTree() {
    ProgressBasedRefinementSelectionPatternNode node =
        ProgressBasedRefinementSelectionParser.parseExpression("(bvadd <var> <const>)");
    assertThat(node instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator equalityPattern =
        (ProgressBasedRefinementSelectionPatternOperator) node;
    assertThat(equalityPattern.operator()).isEqualTo("bvadd");
    ImmutableList<ProgressBasedRefinementSelectionPatternNode> expressions =
        equalityPattern.sExpressionList();
    assertThat(expressions).hasSize(2);
    assertThat(expressions.getFirst() instanceof ProgressBasedRefinementSelectionPatternAtom)
        .isTrue();
    assertThat(((ProgressBasedRefinementSelectionPatternAtom) expressions.getFirst()).name())
        .isEqualTo("var");
    assertThat(expressions.get(1) instanceof ProgressBasedRefinementSelectionPatternAtom).isTrue();
    assertThat(((ProgressBasedRefinementSelectionPatternAtom) expressions.getLast()).name())
        .isEqualTo("const");
  }

  /**
   * This test checks if the Parser returns the correct expression trees for a nested expression.
   */
  @Test
  public void checkParseNestedBuildNestedTree() {
    ProgressBasedRefinementSelectionPatternNode node =
        ProgressBasedRefinementSelectionParser.parseExpression(
            "(and (= <var> <const>) (not <var2>))");
    assertThat(node instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator andOperator =
        (ProgressBasedRefinementSelectionPatternOperator) node;
    assertThat(andOperator.operator()).isEqualTo("and");
    assertThat(andOperator.sExpressionList()).hasSize(2);

    ProgressBasedRefinementSelectionPatternNode rightTree =
        andOperator.sExpressionList().getFirst();
    assertThat(rightTree instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator equalityOperator =
        (ProgressBasedRefinementSelectionPatternOperator) rightTree;
    assertThat(equalityOperator.operator()).isEqualTo("=");
    assertThat(equalityOperator.sExpressionList()).hasSize(2);
    assertThat(
            ((ProgressBasedRefinementSelectionPatternAtom)
                    equalityOperator.sExpressionList().getFirst())
                .name())
        .isEqualTo("var");
    assertThat(
            ((ProgressBasedRefinementSelectionPatternAtom)
                    equalityOperator.sExpressionList().getLast())
                .name())
        .isEqualTo("const");

    ProgressBasedRefinementSelectionPatternNode leftTree = andOperator.sExpressionList().getLast();
    assertThat(leftTree instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator notOperator =
        (ProgressBasedRefinementSelectionPatternOperator) leftTree;
    assertThat(notOperator.operator()).isEqualTo("not");
    assertThat(notOperator.sExpressionList()).hasSize(1);
    assertThat(
            ((ProgressBasedRefinementSelectionPatternAtom) notOperator.sExpressionList().getFirst())
                .name())
        .isEqualTo("var2");
  }

  /**
   * This test checks if the Parser correctly parses a deeply nested tree with more than one
   * subexpression.
   */
  @Test
  public void checkParseDeeplyNestedTree() {
    ProgressBasedRefinementSelectionPatternNode node =
        ProgressBasedRefinementSelectionParser.parseExpression(
            "(and (or (not (= <var> <const>)) (bvadd <const> <var>)))");
    assertThat(node instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator andOperator =
        (ProgressBasedRefinementSelectionPatternOperator) node;
    assertThat(andOperator.operator()).isEqualTo("and");
    assertThat(andOperator.sExpressionList()).hasSize(1);

    ProgressBasedRefinementSelectionPatternNode firstTree =
        andOperator.sExpressionList().getFirst();
    assertThat(firstTree instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator orOperator =
        (ProgressBasedRefinementSelectionPatternOperator) firstTree;
    assertThat(orOperator.operator()).isEqualTo("or");
    assertThat(orOperator.sExpressionList()).hasSize(2);

    ProgressBasedRefinementSelectionPatternNode secondTree =
        orOperator.sExpressionList().getFirst();
    assertThat(secondTree instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator notOperator =
        (ProgressBasedRefinementSelectionPatternOperator) secondTree;
    assertThat(notOperator.operator()).isEqualTo("not");
    assertThat(notOperator.sExpressionList()).hasSize(1);

    ProgressBasedRefinementSelectionPatternNode thirdTree = orOperator.sExpressionList().getLast();
    assertThat(thirdTree instanceof ProgressBasedRefinementSelectionPatternOperator).isTrue();
    ProgressBasedRefinementSelectionPatternOperator equalityOperator =
        (ProgressBasedRefinementSelectionPatternOperator) thirdTree;
    assertThat(equalityOperator.operator()).isEqualTo("bvadd");
    assertThat(equalityOperator.sExpressionList()).hasSize(2);
  }

  /**
   * This test checks if the Parser correctly identifies extra tokens after the end of an
   * expression.
   */
  @Test
  public void checkParseExtraTokens() {
    IllegalArgumentException extraArguments =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                ProgressBasedRefinementSelectionParser.parseExpression("(= <var1> <var2>) <var3>"));
    assertThat(extraArguments.getMessage()).contains("Extra tokens");
  }

  /** This test checks if the Parser correctly identifies an extra closing parenthesis. */
  @Test
  public void checkParseUnmatchedParentheses() {
    IllegalArgumentException unmatchedParentheses =
        assertThrows(
            IllegalArgumentException.class,
            () -> ProgressBasedRefinementSelectionParser.parseExpression(")"));
    assertThat(unmatchedParentheses.getMessage()).contains("Unmatched ')'");
  }

  /** This test checks if the Parser correctly identifies a missing closing parenthesis. */
  @Test
  public void checkParseMissingClosingParentheses() {
    IllegalArgumentException missingClosingParentheses =
        assertThrows(
            IllegalArgumentException.class,
            () -> ProgressBasedRefinementSelectionParser.parseExpression("(= <var>"));
    assertThat(missingClosingParentheses.getMessage()).contains("Missing ')'");
  }

  /** This test checks if the Parser correctly handles a set of empty parenthesis. */
  @Test
  public void checkParseMissingOperator() {
    IllegalArgumentException missingOperator =
        assertThrows(
            IllegalArgumentException.class,
            () -> ProgressBasedRefinementSelectionParser.parseExpression("()"));
    assertThat(missingOperator.getMessage()).contains("Missing ')'");
  }

  /** This test checks if the Parser correctly identifies an empty statement. */
  @Test
  public void checkParseMissingInput() {
    IllegalArgumentException missingInput =
        assertThrows(
            IllegalArgumentException.class,
            () -> ProgressBasedRefinementSelectionParser.parseExpression(""));
    assertThat(missingInput.getMessage()).contains("Pattern ended unexpectedly");
  }
}
