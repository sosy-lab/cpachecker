// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

/**
 * Sealed interface for nodes of a normalized s-expression tree. A node is either an atomic leaf
 * {@link DelegatingRefinerSExpressionAtom} or a compound operator {@link
 * DelegatingRefinerSExpressionSExpressionOperator}. Implementations support visitor-based traversal
 * for rule-matching via {@code accept()} by the {@link DelegatingRefinerMatchingVisitor}.
 */
public sealed interface DelegatingRefinerSExpression
    permits DelegatingRefinerSExpressionAtom, DelegatingRefinerSExpressionSExpressionOperator {
  <R> R accept(DelegatingRefinerSExpressionVisitor<R> pVisitor);
}
