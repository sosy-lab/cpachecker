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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;

import org.sosy_lab.cpachecker.core.interfaces.PostProcessor;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import com.google.common.base.Preconditions;


public class ARGDuplicateEdgeRemover implements PostProcessor {

  @Override
  public void postProcess(ReachedSet pReached) throws InterruptedException {
    // assume that used transfer relation is indeed a transfer function
    Collection<ARGState> visited = new HashSet<>();
    Queue<ARGState> toVisit = new ArrayDeque<>();
    ARGState currentVisit;
    boolean mayVisit;
    List<ARGState> removeChildren = new ArrayList<>(2);

    toVisit.add((ARGState) pReached.getFirstState());

    while (!toVisit.isEmpty()) {
      currentVisit = toVisit.poll();
      removeChildren.clear();

      nextChild: for (ARGState child : currentVisit.getChildren()) {
        mayVisit = true;
        for (ARGState otherChild : currentVisit.getChildren()) {
          if(child==otherChild) {
            continue;
          }
          if (currentVisit.getEdgeToChild(child).equals(currentVisit.getEdgeToChild(otherChild))) {
            Preconditions.checkState(child.isCovered() || otherChild.isCovered(),
                    "Has at least two childs for some edge and none of it is covered. Preparation for ARG translation to C file failed. Retry using CallstackPCC instead of Callstack CPA or enable option cpa.arg.deleteInCPAEnabledAnalysis.");

            if (child.isCovered() &&
                (child.getCoveringState() == otherChild
                || (otherChild.isCovered() && child.isOlderThan(otherChild)))) {
              removeChildren.add(child);
              mayVisit = false;
              continue nextChild;
            }
          }
        }

        if (mayVisit && !visited.contains(child)) {
          toVisit.add(child);
          visited.add(child);
        }
      }

      for (ARGState childRemove : removeChildren) {
        currentVisit.deleteChild(childRemove);
        // TODO also remove from reached set?
      }
    }
  }

}
