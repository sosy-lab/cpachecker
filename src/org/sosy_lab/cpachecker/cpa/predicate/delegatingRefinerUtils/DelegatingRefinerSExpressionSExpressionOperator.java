// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import com.google.common.collect.ImmutableList;

/**
 * Compound node in a normalized s-expression tree.Represents an operator, e.g. {@code and}, {@code
 * bvextract}, and its children. Produced by the {@link DelegatingRefinerAtomNormalizer}. Supports
 * visitor-based traversal for rule-matching by {@link DelegatingRefinerMatchingVisitor}.
 */
public record DelegatingRefinerSExpressionSExpressionOperator(
    String operator, ImmutableList<DelegatingRefinerSExpression> sExpressionList)
    implements DelegatingRefinerSExpression {
  @Override
  public <R> R accept(DelegatingRefinerSExpressionVisitor<R> pVisitor) {
    return pVisitor.visitOperator(this);
  }
}
