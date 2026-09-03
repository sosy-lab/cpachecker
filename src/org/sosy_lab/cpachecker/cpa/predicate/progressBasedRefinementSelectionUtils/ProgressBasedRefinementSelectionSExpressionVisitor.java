// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils;

import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionSExpressionAtom;
import org.sosy_lab.cpachecker.cpa.predicate.progressBasedRefinementSelectionUtils.ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSelectionSExpressionOperator;

/**
 * Visitor for traversing a {@link
 * ProgressBasedRefinementSelectionAST.ProgressBasedRefinementSExpression} tree.
 *
 * @param <R> result type produced by the visitor
 */
public interface ProgressBasedRefinementSelectionSExpressionVisitor<R> {
  R visitAtom(ProgressBasedRefinementSelectionSExpressionAtom atom);

  R visitOperator(ProgressBasedRefinementSelectionSExpressionOperator operator);
}
