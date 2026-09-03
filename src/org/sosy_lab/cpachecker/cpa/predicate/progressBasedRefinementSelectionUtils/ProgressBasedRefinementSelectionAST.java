// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

public class ProgressBasedRefinementSelectionAST {

  /**
   * Sealed interface for nodes in a parsed DSL pattern tree. A {@link
   * ProgressBasedRefinementSelectionPatternNode} can either be a wildcard leaf {@link
   * ProgressBasedRefinementSelectionPatternAtom} or a compound node {@link
   * ProgressBasedRefinementSelectionPatternOperator}.Produced by the {@link
   * ProgressBasedRefinementSelectionParser} and consumed by the {@link
   * ProgressBasedRefinementSelectionMatchingVisitor}.
   */
  public sealed interface ProgressBasedRefinementSelectionPatternNode
      permits ProgressBasedRefinementSelectionPatternAtom,
          ProgressBasedRefinementSelectionPatternOperator {}

  /**
   * Sealed interface for nodes of a normalized s-expression tree. A node is either an atomic leaf
   * {@link ProgressBasedRefinementSelectionSExpressionAtom} or a compound operator {@link
   * ProgressBasedRefinementSelectionSExpressionOperator}. Implementations support visitor-based
   * traversal for rule-matching via {@code accept()} by the {@link
   * ProgressBasedRefinementSelectionMatchingVisitor}.
   */
  public sealed interface ProgressBasedRefinementSExpression
      permits ProgressBasedRefinementSelectionSExpressionAtom,
          ProgressBasedRefinementSelectionSExpressionOperator {
    <R> R accept(ProgressBasedRefinementSelectionSExpressionVisitor<R> pVisitor);
  }

  /**
   * Leaf node in a normalized s-expression tree. Created by the {@link
   * ProgressBasedRefinementSelectionAtomNormalizer} for variables, constants, or certain indices,
   * e.g., bit-slice bounds. Supports visitor-based traversal for rule-matching by {@link
   * ProgressBasedRefinementSelectionMatchingVisitor}.
   */
  public record ProgressBasedRefinementSelectionSExpressionAtom(String value)
      implements ProgressBasedRefinementSExpression {
    @Override
    public <R> R accept(ProgressBasedRefinementSelectionSExpressionVisitor<R> pVisitor) {
      return pVisitor.visitAtom(this);
    }
  }

  /**
   * Compound node in a normalized s-expression tree.Represents an operator, e.g. {@code and},
   * {@code bvextract}, and its children. Produced by the {@link
   * ProgressBasedRefinementSelectionAtomNormalizer}. Supports visitor-based traversal for
   * rule-matching by {@link ProgressBasedRefinementSelectionMatchingVisitor}.
   */
  public record ProgressBasedRefinementSelectionSExpressionOperator(
      String operator, ImmutableList<ProgressBasedRefinementSExpression> sExpressionList)
      implements ProgressBasedRefinementSExpression {
    @Override
    public <R> R accept(ProgressBasedRefinementSelectionSExpressionVisitor<R> pVisitor) {
      return pVisitor.visitOperator(this);
    }
  }

  /**
   * Leaf node in a parsed DSL pattern tree representing a wildcard placeholder. Produced by the
   * {@link ProgressBasedRefinementSelectionParser} for tokens such as {@code <var>} in the {@code
   * patternMatch} string of a {@link ProgressBasedRefinementSelectionPatternRule}. Consumed by the
   * {@link ProgressBasedRefinementSelectionMatchingVisitor} during recursive matching.
   */
  public record ProgressBasedRefinementSelectionPatternAtom(String name)
      implements ProgressBasedRefinementSelectionPatternNode {}

  /**
   * Compound node in a parsed DSL pattern tree. Represents an operator and its child nodes. A DSL
   * String such as {@code (= <var> <term>)} is represented by a {@link
   * ProgressBasedRefinementSelectionPatternOperator} with the operator {@code "="} and two children
   * of the type {@link ProgressBasedRefinementSelectionPatternNode}. Produced by the {@link
   * ProgressBasedRefinementSelectionParser} and consumed by the {@link
   * ProgressBasedRefinementSelectionMatchingVisitor} during recursive matching.
   */
  public record ProgressBasedRefinementSelectionPatternOperator(
      String operator, ImmutableList<ProgressBasedRefinementSelectionPatternNode> sExpressionList)
      implements ProgressBasedRefinementSelectionPatternNode {}

  /**
   * Represents the result of matching a DSL rule to a normalized s-expression. Instances are
   * produced by {@link ProgressBasedRefinementSelectionMatchingVisitor} when a rule matches an
   * s-expression. Each record includes:
   *
   * <ul>
   *   <li>normalizedPattern: the canonical, human-readable form of the matched s-expression.
   *   <li>id: a unique identifier for the rule
   *   <li>category: Semantic category of the rule: Equality, Logical, or Bitvector.
   * </ul>
   */
  public record ProgressBasedRefinementSelectionNormalizedFormula(
      String normalizedPattern, String id, String category) {}

  /**
   * Immutable representation of a declarative pattern rule describing how to recognize and
   * categorize a formula pattern.
   *
   * <p>Each instance corresponds to one entry in the redundancy rules DSL (JSON) and defines.Rules
   * are loaded via the {@link ProgressBasedRefinementSelectionDslLoader}, {@link
   * ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternNode} trees by the
   * {@link ProgressBasedRefinementSelectionParser}, and used by the trees by the {@link
   * ProgressBasedRefinementSelectionParser}, and used by the {@link
   * ProgressBasedRefinementSelectionMatchingVisitor}.
   *
   * <p>Each instance includes:
   *
   * <ul>
   *   <li>patternMatch: the raw DSL match template, an s-expression with wildcards, e.g. {@code (=
   *       <var> <const>)}.
   *   <li>normalizedPattern: canonical, normalized form used in logs and for traceability, e.g.
   *       {@code x = 1}
   *   <li>id: a unique identifier for the rule, e.g. {@code EqVarConst}
   *   <li>category: Semantic category of the rule to, e.g. {@code Equality }
   * </ul>
   */
  public record ProgressBasedRefinementSelectionPatternRule(
      String patternMatch, String normalizedPattern, String id, String category) {

    public static ProgressBasedRefinementSelectionPatternRule of(
        String pPatternMatch, String pNormalizedPattern, String pId, String pCategory) {
      return new ProgressBasedRefinementSelectionPatternRule(
          pPatternMatch, pNormalizedPattern, checkNotNull(pId), checkNotNull(pCategory));
    }
  }
}
