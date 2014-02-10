/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
  private int                reversePostorderId = 0;

  // for checkIds
  private final Set<CFANode> visited   = new HashSet<>();
  private int                reversePostorderId2 = 0;

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

    //node.setReversePostorderId(reversePostorderId2++);
    assert node.getReversePostorderId() == reversePostorderId2++ : "Node " + node + " got " + node.getReversePostorderId() + ", but should get " + (reversePostorderId2-1);
    return true;
  }

  public void assignSorting(final CFANode start) {
    // This is an iterative version of the original algorithm that is now in checkIds().
    // We store the state of the function in two stacks:
    // - the current node (variable "node" in checkIds())
    // - the iterator over the current node's successors (this is state hidden in the for-each loop in checkIds())
    // Together, these two items form a "stack frame".

    final Set<CFANode> visited = new HashSet<>();

    final Deque<CFANode> nodeStack = new ArrayDeque<>();
    final Deque<Iterator<CFANode>> iteratorStack = new LinkedList<>(); // ArrayDeque doesn't work here because we store nulls

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

        if (!visited.add(node)) {
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
    assert checkIds(start);
  }
}
