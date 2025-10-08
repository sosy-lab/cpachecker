// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

/**
 * Represents the result of matching a DSL rule to a normalized s-expression. Instances are produced
 * by {@link DelegatingRefinerMatchingVisitor} when a rule matches an s-expression. Each record
 * includes:
 *
 * <ul>
 *   <li>normalizedPattern: the canonical, human-readable form of the matched s-expression.
 *   <li>id: a unique identifier for the rule
 *   <li>category: Semantic category of the rule: Equality, Logical, or Bitvector.
 * </ul>
 */
public record DelegatingRefinerNormalizedFormula(
    String normalizedPattern, String id, String category) {}
