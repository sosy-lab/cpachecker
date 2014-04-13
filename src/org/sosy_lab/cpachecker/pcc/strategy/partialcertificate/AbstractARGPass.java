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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.arg.ARGState;


public abstract class AbstractARGPass {

  private final boolean visitMultipleTimes;

  public AbstractARGPass(final boolean pMultipleVisits) {
    visitMultipleTimes = pMultipleVisits;
  }

  public void passARG(ARGState root) {
    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> toVisit = new ArrayDeque<>();
    ARGState currentNode;
    boolean childKnown;

    while (!toVisit.isEmpty()) {
      currentNode = toVisit.pollLast();
      visitARGNode(currentNode);

      if (!stopPathDiscovery(currentNode)) {
        for (ARGState child : currentNode.getChildren()) {
          childKnown = seen.contains(child);
          if (!childKnown) {
            toVisit.addLast(child);
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
