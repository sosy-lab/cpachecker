// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

/**
 * Represents a normalized atomic formula extracted from a Boolean abstraction formula by the
 * DelegatingRefinerAtomNormalizer. Each atom encodes a canonical s-expression suitable for DSL rule
 * matching. Atoms may represent logical operators, bitvector expressions, equality comparisons, or
 * nested constructs such as negations and conjunctions. They are structurally comparable and serve
 * as input units for the DelegatingRefinerDslMatcher. The atoms are produced by the
 * DelegatingRefinerAtomNormalizer and reflect operator normalization, structural flattening and
 * operand stringification.
 */
record DelegatingRefinerNormalizedAtom(String pNormalizedExpression) {
  String toSExpr() {
    return pNormalizedExpression;
  }
}
