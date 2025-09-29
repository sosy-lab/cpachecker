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
import java.util.Objects;

/**
 * Represents a declarative pattern rule used to normalize and categorize the different patterns
 * that can be discovered in the added predicates during refinement. Each rule has a match
 * expression, a normalized form, a semantic fingerprint, a category and a set of tags. The rules
 * are loaded from a DSL file and applied to the patterns to detect redundancy in the pattern set.
 */
final class DelegatingRefinerPatternRule {
  private final String patternMatch;
  private final String normalizedPattern;
  private final String patternFingerprint;
  private final String id;
  private final String category;
  private final ImmutableMap<String, String> tags;

  static DelegatingRefinerPatternRule of(
      String pPatternMatch,
      String pNormalizedPattern,
      String pPatternFingerprint,
      String pId,
      ImmutableMap<String, String> pTags,
      String pCategory) {
    return new DelegatingRefinerPatternRule(
        pPatternMatch, pNormalizedPattern, pPatternFingerprint, pId, pTags, pCategory);
  }

  private DelegatingRefinerPatternRule(
      String pPatternMatch,
      String pNormalizedPattern,
      String pPatternFingerprint,
      String pId,
      ImmutableMap<String, String> pTags,
      String pCategory) {

    this.patternMatch = checkNotNull(pPatternMatch, "Pattern Match must not be empty.");
    this.normalizedPattern = pNormalizedPattern;
    this.patternFingerprint = pPatternFingerprint;
    this.id = checkNotNull(pId, "Rule ID must not be empty.");
    this.tags = pTags;
    this.category = pCategory;
  }

  String getPatternMatch() {
    return patternMatch;
  }

  String getNormalizedPattern() {
    return normalizedPattern;
  }

  String getPatternFingerprint() {
    return patternFingerprint;
  }

  String getCategory() {
    return category;
  }

  String getId() {
    return id;
  }

  ImmutableMap<String, String> getTags() {
    return tags;
  }

  @Override
  public String toString() {
    return String.format(
        "rule[%s] : %s -> %s [fingerprint: %s, category: %s, tags: %s]",
        id, patternMatch, normalizedPattern, patternFingerprint, category, tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, patternMatch, normalizedPattern, patternFingerprint, tags, category);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof DelegatingRefinerPatternRule other)) {
      return false;
    }
    other = (DelegatingRefinerPatternRule) pO;
    return Objects.equals(id, other.id)
        && Objects.equals(patternMatch, other.patternMatch)
        && Objects.equals(normalizedPattern, other.normalizedPattern)
        && Objects.equals(patternFingerprint, other.patternFingerprint)
        && Objects.equals(tags, other.tags)
        && Objects.equals(category, other.category);
  }
}
