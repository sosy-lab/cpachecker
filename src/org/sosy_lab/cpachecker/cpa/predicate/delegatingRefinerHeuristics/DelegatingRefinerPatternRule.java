// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableMap;

/**
 * Represents a declarative pattern rule used to normalize and categorize the different patterns
 * that can be discovered in the added predicates during refinement. Each rule has a match
 * expression, a normalized form, a semantic fingerprint, a category and a set of tags. The rules
 * are loaded from a DSL file and applied to the patterns to detect redundancy in the pattern set.
 */
record DelegatingRefinerPatternRule(
    String patternMatch,
    String normalizedPattern,
    String patternFingerprint,
    String id,
    ImmutableMap<String, String> tags,
    String category) {

  static DelegatingRefinerPatternRule of(
      String pPatternMatch,
      String pNormalizedPattern,
      String pPatternFingerprint,
      String pId,
      ImmutableMap<String, String> pTags,
      String pCategory) {
    return new DelegatingRefinerPatternRule(
        pPatternMatch,
        pNormalizedPattern,
        pPatternFingerprint,
        checkNotNull(pId),
        pTags,
        checkNotNull(pCategory));
  }

  @Override
  public String toString() {
    return String.format(
        "rule[%s] : %s -> %s [fingerprint: %s, category: %s, tags: %s]",
        id, patternMatch, normalizedPattern, patternFingerprint, category, tags);
  }
}
