// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

/**
 * Visitor for traversing a {@link DelegatingRefinerSExpression} tree.
 *
 * @param <R> result type produced by the visitor
 */
public interface DelegatingRefinerSExpressionVisitor<R> {
  R visitAtom(DelegatingRefinerSExpressionAtom atom);

  R visitOperator(DelegatingRefinerSExpressionSExpressionOperator operator);
}
