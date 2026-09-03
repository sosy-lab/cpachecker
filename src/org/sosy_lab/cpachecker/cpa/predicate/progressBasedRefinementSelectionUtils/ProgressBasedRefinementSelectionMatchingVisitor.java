// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSExpression;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionNormalizedFormula;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternAtom;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternNode;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternOperator;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternRule;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionSExpressionAtom;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionSExpressionOperator;

/**
 * Matches normalized s-expressions against a set of DSL-rules.The Visitor traverses {@link
 * ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSExpression} trees, and, for each
 * visited node, evaluates the {@link
 * ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionPatternRule} rules. On a
 * match, a {@link
 * ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionNormalizedFormula} is
 * produced, containing the rule's id, normalized pattern and category. Multiple rules can match the
 * same node; results are returned in rule list order.
 */
public final class ProgressBasedRefinementSelectionMatchingVisitor
    implements ProgressBasedRefinementSelectionSExpressionVisitor<
        ImmutableList<ProgressBasedRefinementSelectionNormalizedFormula>> {
  private final ImmutableList<CompiledRule> compiledRules;

  /**
   * Constructs a matching visitor using provided DSL pattern rules. Each rule is parsed into a
   * pattern tree for structural redundancy matching.
   *
   * @param pPatternRules a list of pattern rules to apply during matching
   */
  public ProgressBasedRefinementSelectionMatchingVisitor(
      ImmutableList<ProgressBasedRefinementSelectionPatternRule> pPatternRules) {

    ImmutableList.Builder<CompiledRule> compiledRuleBuilder = ImmutableList.builder();
    for (ProgressBasedRefinementSelectionPatternRule rule : pPatternRules) {
      ProgressBasedRefinementSelectionPatternNode root =
          ProgressBasedRefinementSelectionParser.parseExpression(rule.patternMatch());
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
  public ImmutableList<ProgressBasedRefinementSelectionNormalizedFormula> visitAtom(
      ProgressBasedRefinementSelectionSExpressionAtom atom) {
    return matchAll(atom);
  }

  /**
   * Matches an s-expression node against all compiled DSL rules.
   *
   * @param operator the operator node to assess
   * @return a list of matched formulas
   */
  @Override
  public ImmutableList<ProgressBasedRefinementSelectionNormalizedFormula> visitOperator(
      ProgressBasedRefinementSelectionSExpressionOperator operator) {
    return matchAll(operator);
  }

  private ImmutableList<ProgressBasedRefinementSelectionNormalizedFormula> matchAll(
      ProgressBasedRefinementSExpression pExpression) {
    ImmutableList.Builder<ProgressBasedRefinementSelectionNormalizedFormula> matches =
        ImmutableList.builder();
    for (CompiledRule cRule : compiledRules) {
      Map<String, ProgressBasedRefinementSExpression> bindings = new HashMap<>();
      if (matchNode(pExpression, cRule.root, bindings)) {
        ProgressBasedRefinementSelectionNormalizedFormula pFormula =
            new ProgressBasedRefinementSelectionNormalizedFormula(
                cRule.rule.normalizedPattern(), cRule.rule.id(), cRule.rule.category());
        matches.add(pFormula);
      }
    }
    return matches.build();
  }

  private boolean matchNode(
      ProgressBasedRefinementSExpression pExpression,
      ProgressBasedRefinementSelectionPatternNode pNode,
      Map<String, ProgressBasedRefinementSExpression> pBindings) {
    if (pNode instanceof ProgressBasedRefinementSelectionPatternAtom leaf) {
      String key = leaf.name();
      ProgressBasedRefinementSExpression existing = pBindings.get(key);
      if (existing == null) {
        pBindings.put(key, pExpression);
        return true;
      }
      return isStructuralEqual(existing, pExpression);
    }

    if (pNode instanceof ProgressBasedRefinementSelectionPatternOperator operator) {
      if (!(pExpression instanceof ProgressBasedRefinementSelectionSExpressionOperator current)) {
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
      ProgressBasedRefinementSExpression pFirstExpression,
      ProgressBasedRefinementSExpression pSecondExpression) {
    if (pFirstExpression == pSecondExpression) {
      return true;
    }
    if (pFirstExpression == null
        || pSecondExpression == null
        || pFirstExpression.getClass() != pSecondExpression.getClass()) {
      return false;
    }

    if (pFirstExpression instanceof ProgressBasedRefinementSelectionSExpressionOperator firstIsOp
        && pSecondExpression
            instanceof ProgressBasedRefinementSelectionSExpressionOperator secondIsOp) {
      if (!Objects.equals(firstIsOp.operator(), secondIsOp.operator())) {
        return false;
      }
      ImmutableList<ProgressBasedRefinementSExpression> firstChildren = firstIsOp.sExpressionList();
      ImmutableList<ProgressBasedRefinementSExpression> secondChildren =
          secondIsOp.sExpressionList();
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

    if (pFirstExpression instanceof ProgressBasedRefinementSelectionSExpressionAtom firstIsAtom
        && pSecondExpression
            instanceof ProgressBasedRefinementSelectionSExpressionAtom secondIsAtom) {
      return Objects.equals(firstIsAtom.toString(), secondIsAtom.toString());
    }

    return false;
  }

  private static final class CompiledRule {
    final ProgressBasedRefinementSelectionPatternRule rule;
    final ProgressBasedRefinementSelectionPatternNode root;

    CompiledRule(
        ProgressBasedRefinementSelectionPatternRule pRule,
        ProgressBasedRefinementSelectionPatternNode pNode) {
      this.rule = pRule;
      this.root = pNode;
    }
  }
}
