// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a declarative pattern rule describing how to recognize and categorize
 * a formula pattern.
 *
 * <p>Each instance corresponds to one entry in the redundancy rules DSL (JSON) and defines.Rules
 * are loaded via the {@link DelegatingRefinerDslLoader}, parsed into {@link
 * DelegatingRefinerPatternNode} trees by the {@link DelegatingRefinerParser}, and used by the
 * {@link DelegatingRefinerMatchingVisitor}.
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

  static DelegatingRefinerPatternRule of(
      String pPatternMatch, String pNormalizedPattern, String pId, String pCategory) {
    return new DelegatingRefinerPatternRule(
        pPatternMatch, pNormalizedPattern, checkNotNull(pId), checkNotNull(pCategory));
  }
}
