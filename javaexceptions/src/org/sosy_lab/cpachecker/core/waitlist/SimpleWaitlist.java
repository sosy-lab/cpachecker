// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.waitlist;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Waitlist implementation that supports either a breadth-first (BFS) or depth-first (DFS) strategy
 * for pop().
 */
public class SimpleWaitlist extends AbstractWaitlist<Deque<AbstractState>> {

  private final TraversalMethod traversal;

  protected SimpleWaitlist(TraversalMethod pTraversal) {
    super(new ArrayDeque<>());
    Preconditions.checkArgument(
        pTraversal == TraversalMethod.BFS || pTraversal == TraversalMethod.DFS);
    traversal = pTraversal;
  }

  @Override
  public AbstractState pop() {
    switch (traversal) {
      case BFS:
        return waitlist.removeFirst();

      case DFS:
        return waitlist.removeLast();

      default:
        throw new AssertionError();
    }
  }
}
