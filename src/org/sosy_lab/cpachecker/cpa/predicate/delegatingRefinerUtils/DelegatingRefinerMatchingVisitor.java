// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Matches normalized s-expressions against a set of DSL-rules.The Visitor traverses {@link
 * DelegatingRefinerSExpression} trees, and, for each visited node, evaluates the {@link
 * DelegatingRefinerPatternRule} rules. On a match, a {@link DelegatingRefinerNormalizedFormula} is
 * produced, containing the rule's id, normalized pattern and category. Multiple rules can match the
 * same node; results are returned in rule list order.
 */
public final class DelegatingRefinerMatchingVisitor
    implements DelegatingRefinerSExpressionVisitor<
        ImmutableList<DelegatingRefinerNormalizedFormula>> {
  private final ImmutableList<CompiledRule> compiledRules;

  /**
   * Constructs a matching visitor using provided DSL pattern rules. Each rule is parsed into a
   * pattern tree for structural redundancy matching.
   *
   * @param pPatternRules a list of pattern rules to apply during matching
   */
  public DelegatingRefinerMatchingVisitor(
      ImmutableList<DelegatingRefinerPatternRule> pPatternRules) {

    ImmutableList.Builder<CompiledRule> compiledRuleBuilder = ImmutableList.builder();
    for (DelegatingRefinerPatternRule rule : pPatternRules) {
      DelegatingRefinerPatternNode root =
          DelegatingRefinerParser.parseExpression(rule.patternMatch());
      compiledRuleBuilder.add(new CompiledRule(rule, root));
    }
    this.compiledRules = compiledRuleBuilder.build();
  }

  /**
   * Matches an atomic s-expression against all compiled DSL rules.
   *
   * @param atom the atomic s-expression to assess
   * @return a list of matched formulas
   */
  @Override
  public ImmutableList<DelegatingRefinerNormalizedFormula> visitAtom(
      DelegatingRefinerSExpressionAtom atom) {
    return matchAll(atom);
  }

  /**
   * Matches an s-expression node against all compiled DSL rules.
   *
   * @param operator the operator node to assess
   * @return a list of matched formulas
   */
  @Override
  public ImmutableList<DelegatingRefinerNormalizedFormula> visitOperator(
      DelegatingRefinerSExpressionSExpressionOperator operator) {
    return matchAll(operator);
  }

  private ImmutableList<DelegatingRefinerNormalizedFormula> matchAll(
      DelegatingRefinerSExpression pExpression) {
    ImmutableList.Builder<DelegatingRefinerNormalizedFormula> matches = ImmutableList.builder();
    for (CompiledRule cRule : compiledRules) {
      Map<String, DelegatingRefinerSExpression> bindings = new HashMap<>();
      if (matchNode(pExpression, cRule.root, bindings)) {
        DelegatingRefinerNormalizedFormula pFormula =
            new DelegatingRefinerNormalizedFormula(
                cRule.rule.normalizedPattern(), cRule.rule.id(), cRule.rule.category());
        matches.add(pFormula);
      }
    }
    return matches.build();
  }

  private boolean matchNode(
      DelegatingRefinerSExpression pExpression,
      DelegatingRefinerPatternNode pNode,
      Map<String, DelegatingRefinerSExpression> pBindings) {
    if (pNode instanceof DelegatingRefinerDelegatingRefinerPatternAtom leaf) {
      String key = leaf.name();
      DelegatingRefinerSExpression existing = pBindings.get(key);
      if (existing == null) {
        pBindings.put(key, pExpression);
        return true;
      }
      return isStructuralEqual(existing, pExpression);
    }

    if (pNode instanceof DelegatingRefinerPatternOperator operator) {
      if (!(pExpression instanceof DelegatingRefinerSExpressionSExpressionOperator current)) {
        return false;
      }
      if (!operator.operator().equals(current.operator())) {
        return false;
      }
      if (operator.sExpressionList().size() != current.sExpressionList().size()) {
        return false;
      }
      for (int i = 0; i < operator.sExpressionList().size(); i++) {
        if (!matchNode(
            current.sExpressionList().get(i), operator.sExpressionList().get(i), pBindings)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private static boolean isStructuralEqual(
      DelegatingRefinerSExpression pFirstExpression,
      DelegatingRefinerSExpression pSecondExpression) {
    if (pFirstExpression == pSecondExpression) {
      return true;
    }
    if (pFirstExpression == null
        || pSecondExpression == null
        || pFirstExpression.getClass() != pSecondExpression.getClass()) {
      return false;
    }

    if (pFirstExpression instanceof DelegatingRefinerSExpressionSExpressionOperator firstIsOp
        && pSecondExpression
            instanceof DelegatingRefinerSExpressionSExpressionOperator secondIsOp) {
      if (!Objects.equals(firstIsOp.operator(), secondIsOp.operator())) {
        return false;
      }
      ImmutableList<DelegatingRefinerSExpression> firstChildren = firstIsOp.sExpressionList();
      ImmutableList<DelegatingRefinerSExpression> secondChildren = secondIsOp.sExpressionList();
      if (firstChildren.size() != secondChildren.size()) {
        return false;
      }
      for (int i = 0; i < firstChildren.size(); i++) {
        if (!isStructuralEqual(firstChildren.get(i), secondChildren.get(i))) {
          return false;
        }
      }
      return true;
    }

    if (pFirstExpression instanceof DelegatingRefinerSExpressionAtom firstIsAtom
        && pSecondExpression instanceof DelegatingRefinerSExpressionAtom secondIsAtom) {
      return Objects.equals(firstIsAtom.toString(), secondIsAtom.toString());
    }

    return false;
  }

  private static final class CompiledRule {
    final DelegatingRefinerPatternRule rule;
    final DelegatingRefinerPatternNode root;

    CompiledRule(DelegatingRefinerPatternRule pRule, DelegatingRefinerPatternNode pNode) {
      this.rule = pRule;
      this.root = pNode;
    }
  }
}
