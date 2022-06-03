// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.graph.Traverser;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CFAReversePostorder {

  private CFAReversePostorder() {}

  private static boolean checkIds(CFANode pStartNode) {

    // This is the old iterative algorithm. We keep this implementation to check that the new
    // Guava-based algorithm assigns exactly(!) the same IDs. Even slight variations have been found
    // to cause performance differences, although we are not sure why.

    // The original recursive algorithm this iterative algorithm is based on (it should assign the
    // exact same IDs as the iterative implementation):
    // ---------------------------------------------------------------------------------------------
    // private final Set<CFANode> visited = new HashSet<>();
    // private int reversePostorderId = 0;
    //
    // private void assignIds(CFANode pNode) {
    //   if (!visited.add(pNode)) return;
    //   for (CFANode successor : CFAUtils.successorsOf(pNode)) assignIds(successor);
    //   pNode.setReversePostorderId(reversePostOrderId++);
    // }
    // ---------------------------------------------------------------------------------------------

    // We store the state of the function in two stacks:
    // - the current node (parameter `pNode` in the recursive algorithm)
    // - the iterator over the current node's successors (this is state hidden in the for-each loop
    //   of the recursive algorithm)
    // Together, these two items form a "stack frame".

    final Set<CFANode> finished = new HashSet<>();
    int reversePostorderId = 0;

    final Deque<CFANode> nodeStack = new ArrayDeque<>();
    @SuppressWarnings("JdkObsolete") // ArrayDeque doesn't work here because we store nulls
    final Deque<Iterator<CFANode>> iteratorStack = new LinkedList<>();

    nodeStack.push(pStartNode);
    iteratorStack.push(null);

    while (!nodeStack.isEmpty()) {
      assert nodeStack.size() == iteratorStack.size();

      final CFANode node = nodeStack.peek();
      Iterator<CFANode> successors = iteratorStack.peek();

      if (successors == null) {
        // Entering this stack frame.
        // This part of the code corresponds to the code in the recursive algorithm before the loop.

        if (!finished.add(node)) {
          // already handled, do nothing

          // Do a simulated "return".
          nodeStack.pop();
          iteratorStack.pop();
          continue;
        }

        // enter the for loop
        successors = CFAUtils.successorsOf(node).iterator();
        iteratorStack.pop();
        iteratorStack.push(successors);
      }

      if (successors.hasNext()) {
        // "recursive call"
        // This part of the code corresponds to the code in the recursive algorithm during the loop.

        CFANode successor = successors.next();

        // Do a simulated "function call" by pushing something on the stacks,
        // creating a new stack frame.
        nodeStack.push(successor);
        iteratorStack.push(null);

      } else {
        // All children handled.
        // This part of the code corresponds to the code in the recursive algorithm after the loop.

        if (node.getReversePostorderId() != reversePostorderId++) {
          return false; // IDs are not the same
        }

        // Do a simulated "return".
        nodeStack.pop();
        iteratorStack.pop();
      }
    }

    return true; // all IDs are exactly the same
  }

  public static void assignIds(CFANode pStartNode) {

    int reversePostOrderId = 0;
    Iterable<CFANode> nodesInPostOrder =
        Traverser.forGraph(CFAUtils::successorsOf).depthFirstPostOrder(pStartNode);

    for (CFANode node : nodesInPostOrder) {
      node.setReversePostorderId(reversePostOrderId++);
    }

    // check whether the new implementation assigns exactly the same IDs as the old implementation
    assert checkIds(pStartNode);
  }
}
