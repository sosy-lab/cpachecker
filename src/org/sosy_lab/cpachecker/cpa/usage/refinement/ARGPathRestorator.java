/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class ARGPathRestorator implements PathRestorator {

  private final Function<ARGState, Integer> getStateId;

  public ARGPathRestorator(@NonNull Function<ARGState, Integer> idExtractor) {
    getStateId = idExtractor;
  }

  @Override
  public ARGPath computePath(ARGState pLastElement) {
    // Temporary implementation
    return computePath(pLastElement, Collections.emptySet());
  }

  @Override
  public ARGPath computePath(ARGState pLastElement, Set<List<Integer>> pRefinedStates) {
    Set<ArrayDeque<Integer>> remainingStates = new HashSet<>();

    for (List<Integer> newList : pRefinedStates) {
      remainingStates.add(new ArrayDeque<>(newList));
    }
    List<ARGState> states = new ArrayList<>(); // reversed order
    Set<ARGState> seenElements = new HashSet<>();

    // each element of the path consists of the abstract state and the outgoing
    // edge to its successor

    ARGState currentARGState = pLastElement;
    states.add(currentARGState);
    seenElements.add(currentARGState);
    Deque<ARGState> backTrackPoints = new ArrayDeque<>();
    Deque<List<ARGState>> backTrackOptions = new ArrayDeque<>();

    while (!currentARGState.getParents().isEmpty()) {
      Iterator<ARGState> parents = currentARGState.getParents().iterator();

      ARGState parentElement = parents.next();
      while (seenElements.contains(parentElement) && parents.hasNext()) {
        // while seenElements already contained parentElement, try next parent
        parentElement = parents.next();
      }

      if (seenElements.contains(parentElement)) {
        // Backtrack
        if (backTrackPoints.isEmpty()) {
          throw new IllegalArgumentException("No ARG path from the target state to a root state.");
        }
        ARGState backTrackPoint = backTrackPoints.pop();
        ListIterator<ARGState> stateIterator = states.listIterator(states.size());
        while (stateIterator.hasPrevious() && !stateIterator.previous().equals(backTrackPoint)) {
          stateIterator.remove();
        }
        List<ARGState> options = backTrackOptions.pop();
        for (ARGState parent : backTrackPoint.getParents()) {
          if (!options.contains(parent)) {
            seenElements.add(parent);
          }
        }
        currentARGState = backTrackPoint;
      } else {
        // Record backtracking options
        if (parents.hasNext()) {
          List<ARGState> options = new ArrayList<>(1);
          while (parents.hasNext()) {
            ARGState parent = parents.next();
            if (!seenElements.contains(parent)) {
              options.add(parent);
            }
          }
          if (!options.isEmpty()) {
            backTrackPoints.push(currentARGState);
            backTrackOptions.push(options);
          }
        }

        if (checkRepeatitionOfState(parentElement, remainingStates)) {
          return null;
        }
        seenElements.add(parentElement);
        states.add(parentElement);

        currentARGState = parentElement;
      }
    }
    return new ARGPath(Lists.reverse(states));
  }

  private boolean
      checkRepeatitionOfState(ARGState currentElement, Set<ArrayDeque<Integer>> remainingStates) {
    if (currentElement != null && getStateId != null) {
      Integer currentId = getStateId.apply(currentElement);
      for (ArrayDeque<Integer> rest : remainingStates) {
        if (rest.getLast().equals(currentId)) {
          rest.removeLast();
          if (rest.isEmpty()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public PathIterator iterator(ARGState pTarget) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

}
