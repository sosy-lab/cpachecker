// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

/**
 * Leaf node in a normalized s-expression tree. Created by the {@link
 * DelegatingRefinerAtomNormalizer} for variables, constants, or certain indices, e.g., bit-slice
 * bounds. Supports visitor-based traversal for rule-matching by {@link
 * DelegatingRefinerMatchingVisitor}.
 */
public record DelegatingRefinerSExpressionAtom(String value)
    implements DelegatingRefinerSExpression {
  @Override
  public <R> R accept(DelegatingRefinerSExpressionVisitor<R> pVisitor) {
    return pVisitor.visitAtom(this);
  }
}
