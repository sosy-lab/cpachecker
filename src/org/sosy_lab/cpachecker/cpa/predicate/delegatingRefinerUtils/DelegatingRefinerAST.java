// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

public class DelegatingRefinerAST {

  /**
   * Sealed interface for nodes in a parsed DSL pattern tree. A {@link DelegatingRefinerPatternNode}
   * can either be a wildcard leaf {@link DelegatingRefinerDelegatingRefinerPatternAtom} or a
   * compound node {@link DelegatingRefinerPatternOperator}.Produced by the {@link
   * DelegatingRefinerParser} and consumed by the {@link DelegatingRefinerMatchingVisitor}.
   */
  public sealed interface DelegatingRefinerPatternNode
      permits DelegatingRefinerDelegatingRefinerPatternAtom, DelegatingRefinerPatternOperator {}

  /**
   * Sealed interface for nodes of a normalized s-expression tree. A node is either an atomic leaf
   * {@link DelegatingRefinerSExpressionAtom} or a compound operator {@link
   * DelegatingRefinerSExpressionSExpressionOperator}. Implementations support visitor-based
   * traversal for rule-matching via {@code accept()} by the {@link
   * DelegatingRefinerMatchingVisitor}.
   */
  public sealed interface DelegatingRefinerSExpression
      permits DelegatingRefinerSExpressionAtom, DelegatingRefinerSExpressionSExpressionOperator {
    <R> R accept(DelegatingRefinerSExpressionVisitor<R> pVisitor);
  }

  /**
   * Leaf node in a normalized s-expression tree. Created by the {@link
   * DelegatingRefinerAtomNormalizer} for variables, constants, or certain indices, e.g., bit-slice
   * bounds. Supports visitor-based traversal for rule-matching by {@link
   * DelegatingRefinerMatchingVisitor}.
   */
  public record DelegatingRefinerSExpressionAtom(String value)
      implements DelegatingRefinerSExpression {
    @Override
    public <R> R accept(DelegatingRefinerSExpressionVisitor<R> pVisitor) {
      return pVisitor.visitAtom(this);
    }
  }

  /**
   * Compound node in a normalized s-expression tree.Represents an operator, e.g. {@code and},
   * {@code bvextract}, and its children. Produced by the {@link DelegatingRefinerAtomNormalizer}.
   * Supports visitor-based traversal for rule-matching by {@link DelegatingRefinerMatchingVisitor}.
   */
  public record DelegatingRefinerSExpressionSExpressionOperator(
      String operator, ImmutableList<DelegatingRefinerSExpression> sExpressionList)
      implements DelegatingRefinerSExpression {
    @Override
    public <R> R accept(DelegatingRefinerSExpressionVisitor<R> pVisitor) {
      return pVisitor.visitOperator(this);
    }
  }

  /**
   * Leaf node in a parsed DSL pattern tree representing a wildcard placeholder. Produced by the
   * {@link DelegatingRefinerParser} for tokens such as {@code <var>} in the {@code patternMatch}
   * string of a {@link DelegatingRefinerPatternRule}. Consumed by the {@link
   * DelegatingRefinerMatchingVisitor} during recursive matching.
   */
  public record DelegatingRefinerDelegatingRefinerPatternAtom(String name)
      implements DelegatingRefinerPatternNode {}

  /**
   * Compound node in a parsed DSL pattern tree. Represents an operator and its child nodes. A DSL
   * String such as {@code (= <var> <term>)} is represented by a {@link
   * DelegatingRefinerPatternOperator} with the operator {@code "="} and two children of the type
   * {@link DelegatingRefinerPatternNode}. Produced by the {@link DelegatingRefinerParser} and
   * consumed by the {@link DelegatingRefinerMatchingVisitor} during recursive matching.
   */
  public record DelegatingRefinerPatternOperator(
      String operator, ImmutableList<DelegatingRefinerPatternNode> sExpressionList)
      implements DelegatingRefinerPatternNode {}

  /**
   * Represents the result of matching a DSL rule to a normalized s-expression. Instances are
   * produced by {@link DelegatingRefinerMatchingVisitor} when a rule matches an s-expression. Each
   * record includes:
   *
   * <ul>
   *   <li>normalizedPattern: the canonical, human-readable form of the matched s-expression.
   *   <li>id: a unique identifier for the rule
   *   <li>category: Semantic category of the rule: Equality, Logical, or Bitvector.
   * </ul>
   */
  public record DelegatingRefinerNormalizedFormula(
      String normalizedPattern, String id, String category) {}

  /**
   * Immutable representation of a declarative pattern rule describing how to recognize and
   * categorize a formula pattern.
   *
   * <p>Each instance corresponds to one entry in the redundancy rules DSL (JSON) and defines.Rules
   * are loaded via the {@link DelegatingRefinerDslLoader}, {@link
   * DelegatingRefinerAST.DelegatingRefinerPatternNode} trees by the {@link
   * DelegatingRefinerParser}, and used by the trees by the {@link DelegatingRefinerParser}, and
   * used by the {@link DelegatingRefinerMatchingVisitor}.
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
  public record DelegatingRefinerPatternRule(
      String patternMatch, String normalizedPattern, String id, String category) {

    public static DelegatingRefinerPatternRule of(
        String pPatternMatch, String pNormalizedPattern, String pId, String pCategory) {
      return new DelegatingRefinerPatternRule(
          pPatternMatch, pNormalizedPattern, checkNotNull(pId), checkNotNull(pCategory));
    }
  }
}
