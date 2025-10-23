// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.expressions;

import com.google.common.collect.ImmutableList;

public class ExpressionTreeUtil {

  @SafeVarargs
  public static <LeafType> ImmutableList<ExpressionTree<LeafType>> toExpressionTree(
      LeafType... pLeafs) {

    ImmutableList.Builder<ExpressionTree<LeafType>> rTree = ImmutableList.builder();
    for (LeafType leaf : pLeafs) {
      rTree.add(LeafExpression.of(leaf));
    }
    return rTree.build();
  }

  public static <LeafType> ImmutableList<ExpressionTree<LeafType>> toExpressionTree(
      Iterable<LeafType> pLeafs) {

    ImmutableList.Builder<ExpressionTree<LeafType>> rTree = ImmutableList.builder();
    for (LeafType leaf : pLeafs) {
      rTree.add(LeafExpression.of(leaf));
    }
    return rTree.build();
  }
}
