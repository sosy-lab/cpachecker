// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.graph.Traverser;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CFAReversePostorder {

  // for checkIds
  private final Set<CFANode> visited = new HashSet<>();
  private int reversePostorderId2 = 0;

  @SuppressWarnings("unused")
  private boolean checkIds(CFANode node) {
    // This is the (original) recursive algorithm.
    // We keep this implementation as an assertion check
    // to check that the non-recursive algorithm assigns exactly(!) the same ids.
    // Even slight variations have been found to cause performance differences,
    // although we are not sure why.

    if (!visited.add(node)) {
      // already handled, do nothing
      return true;
    }

    for (CFANode successor : CFAUtils.successorsOf(node)) {
      checkIds(successor);
    }

    // node.setReversePostorderId(reversePostorderId2++);
    assert node.getReversePostorderId() == reversePostorderId2++
        : "Node "
            + node
            + " got "
            + node.getReversePostorderId()
            + ", but should get "
            + (reversePostorderId2 - 1);
    return true;
  }

  public static void assignIds(CFANode pStartNode, Collection<CFANode> pNodes) {

    int reversePostOrderId = pNodes.size() - 1; // highest reverse post-order ID
    Iterable<CFANode> nodesInPostOrder =
        Traverser.forGraph(CFAUtils::successorsOf).depthFirstPostOrder(pStartNode);

    // note that we iterate the nodes in post-order but assign _reverse_ post-order IDs
    for (CFANode node : nodesInPostOrder) {
      node.setReversePostorderId(reversePostOrderId--);
    }

    // disabled because the recursive algorithm causes stack overflows for large files
    // assert new CFAReversePostorder().checkIds(pStartNode);
  }
}
