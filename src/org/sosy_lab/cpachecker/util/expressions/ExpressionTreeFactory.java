// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import java.util.Collection;

public interface ExpressionTreeFactory<LeafType> {

  ExpressionTree<LeafType> leaf(LeafType pLeafType);

  ExpressionTree<LeafType> leaf(LeafType pLeafType, boolean pAssumeTruth);

  ExpressionTree<LeafType> and(ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2);

  ExpressionTree<LeafType> and(Collection<ExpressionTree<LeafType>> pOperands);

  ExpressionTree<LeafType> or(ExpressionTree<LeafType> pOp1, ExpressionTree<LeafType> pOp2);

  ExpressionTree<LeafType> or(Collection<ExpressionTree<LeafType>> pOperands);
}
