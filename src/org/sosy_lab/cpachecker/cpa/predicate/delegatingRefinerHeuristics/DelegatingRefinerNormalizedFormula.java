// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

/**
 * Represents the result of matching a DSL rule to a normalized atom.
 *
 * <p>Components:
 *
 * <ul>
 *   <li>normalizedPattern: the canonical, normalized form for the s-expression extracted from the
 *       Abstraction formula
 *   <li>id: a unique identifier for the rule
 *   <li>category: Semantic domain or category a rule belongs to
 * </ul>
 *
 * Each DelegatingRefinerNormalizedFormula is produced by the DelegatingRefinerDslMatcher when a DSL
 * rule successfully matches an SMT expression.
 */
record DelegatingRefinerNormalizedFormula(String normalizedPattern, String id, String category) {

  @Override
  public String toString() {
    return String.format(
        "NormalizedFormula [%s]: %s [category: %s]", id, normalizedPattern, category);
  }
}
