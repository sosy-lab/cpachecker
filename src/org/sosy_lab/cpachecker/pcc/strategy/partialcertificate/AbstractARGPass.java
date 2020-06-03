// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public abstract class AbstractARGPass {

  private final boolean visitMultipleTimes;

  protected AbstractARGPass(final boolean pMultipleVisits) {
    visitMultipleTimes = pMultipleVisits;
  }

  public void passARG(ARGState root) {
    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> toVisit = new ArrayDeque<>();
    ARGState currentNode;
    boolean childKnown;

    toVisit.add(root);
    seen.add(root);

    while (!toVisit.isEmpty()) {
      currentNode = toVisit.pollLast();
      visitARGNode(currentNode);

      if (!stopPathDiscovery(currentNode)) {
        for (ARGState child : currentNode.getChildren()) {
          childKnown = seen.contains(child);
          if (!childKnown) {
            toVisit.addLast(child);
            seen.add(child);
          }
          if (visitMultipleTimes && childKnown) {
            visitARGNode(child);
          }
        }
      }
    }
  }

  public abstract void visitARGNode(ARGState node);

  public abstract boolean stopPathDiscovery(ARGState node);
}
