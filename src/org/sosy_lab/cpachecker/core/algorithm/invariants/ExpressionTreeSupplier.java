// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public interface ExpressionTreeSupplier {

  /**
   * Return an invariant that holds at a given node. This method should be relatively cheap and not
   * block (i.e., do not start an expensive invariant generation procedure).
   *
   * @param node The CFANode.
   * @return An invariant boolean expression over C expressions.
   */
  ExpressionTree<Object> getInvariantFor(CFANode node) throws InterruptedException;

  enum TrivialInvariantSupplier implements ExpressionTreeSupplier {
    INSTANCE;

    @Override
    public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
      return ExpressionTrees.getTrue();
    }
  }
}
