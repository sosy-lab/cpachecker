// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import com.google.common.collect.ImmutableMap;

/**
 * Represents the result of matching a DSL rule to a normalized atom. Encodes the normalized
 * pattern, a semantic fingerprint and rule metadata. Used by the
 * DelegatingRefinerRedundantPredicates Heuristic to check for redundancy in the predicate set.
 */
record DelegatingRefinerNormalizedFormula(
    String normalizedPattern,
    String patternFingerprint,
    String id,
    ImmutableMap<String, String> tags,
    String category) {

  @Override
  public String toString() {
    return String.format(
        "NormalizedFormula [%s]: %s [fingerprint: %s, category: %s, tags: %s]",
        id, normalizedPattern, patternFingerprint, category, tags);
  }
}
