// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a declarative pattern rule used to normalize and categorize the
 * different patterns that can be discovered in the added predicates during refinement.
 *
 * <p>A DelegatingRefinerPatternRule corresponds to one entry in the redundancyRules.dsl and defines
 * how a SMT formula should be recognized and transformed into a normalized form. These rules are
 * used by the DelegatingRefinerDslMatcher.
 *
 * <p>Components:
 *
 * <ul>
 *   <li>patternMatch: the raw DSL match template, an s-expression with placeholders, extracted from
 *       the Abstraction formula
 *   <li>normalizedPattern: the canonical, normalized form for the s-expression extracted from the
 *       Abstraction formula
 *   <li>id: a unique identifier for the rule
 *   <li>category: Semantic domain or category a rule belongs to
 * </ul>
 */
record DelegatingRefinerPatternRule(
    String patternMatch, String normalizedPattern, String id, String category) {

  static DelegatingRefinerPatternRule of(
      String pPatternMatch, String pNormalizedPattern, String pId, String pCategory) {
    return new DelegatingRefinerPatternRule(
        pPatternMatch, pNormalizedPattern, checkNotNull(pId), checkNotNull(pCategory));
  }

  @Override
  public String toString() {
    return String.format(
        "rule[%s] : %s -> %s [category: %s]", id, patternMatch, normalizedPattern, category);
  }
}
