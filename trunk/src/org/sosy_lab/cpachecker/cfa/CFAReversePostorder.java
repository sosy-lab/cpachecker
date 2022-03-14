// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CFAReversePostorder {

  // for assignSorting
  private int reversePostorderId = 0;

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

  public void assignSorting(final CFANode start) {
    // This is an iterative version of the original algorithm that is now in checkIds().
    // We store the state of the function in two stacks:
    // - the current node (variable "node" in checkIds())
    // - the iterator over the current node's successors (this is state hidden in the for-each loop
    // in checkIds())
    // Together, these two items form a "stack frame".

    final Set<CFANode> finished = new HashSet<>();

    final Deque<CFANode> nodeStack = new ArrayDeque<>();
    @SuppressWarnings("JdkObsolete") // ArrayDeque doesn't work here because we store nulls
    final Deque<Iterator<CFANode>> iteratorStack = new LinkedList<>();

    nodeStack.push(start);
    iteratorStack.push(null);

    while (!nodeStack.isEmpty()) {
      assert nodeStack.size() == iteratorStack.size();

      final CFANode node = nodeStack.peek();
      Iterator<CFANode> successors = iteratorStack.peek();

      if (successors == null) {
        // Entering this stack frame.
        // This part of the code corresponds to the code in checkIds()
        // before the for loop.

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
        // This part of the code corresponds to the code in checkIds()
        // during the loop.
        CFANode successor = successors.next();

        // Do a simulated "function call" by pushing something on the stacks,
        // creating a new stack frame.
        nodeStack.push(successor);
        iteratorStack.push(null);

      } else {
        // All children handled.
        // This part of the code corresponds to the code in checkIds()
        // after the loop.
        node.setReversePostorderId(reversePostorderId++);

        // Do a simulated "return".
        nodeStack.pop();
        iteratorStack.pop();
      }
    }

    // Disabled because the recursive algorithm throws StackOverflowError
    // for large files.
    // assert checkIds(start);
  }
}
