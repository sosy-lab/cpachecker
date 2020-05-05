/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tarantula;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class TarantulaUtils {

  /** Helper class for TarantulaAlgorithm related utility methods. */
  private TarantulaUtils() {}

  /**
   * Gets all possible paths from the possible leaves (whether targetStates or safeStates)to the
   * root.
   *
   * @param reachedSet input.
   * @param chosenState whether targetStates or safeStates.
   * @return States on the paths.
   */
  public static Set<ARGPath> getAllPaths(ReachedSet reachedSet, ARGState chosenState) {

    ARGState root = AbstractStates.extractStateByType(reachedSet.getFirstState(), ARGState.class);
    List<ARGState> states = new ArrayList<>();
    Set<ARGPath> results = new HashSet<>();
    List<List<ARGState>> paths = new ArrayList<>();

    states.add(chosenState);
    paths.add(states);

    // This is assuming from each node there is a way to go to the start
    // Go on until all the paths got the start
    while (!paths.isEmpty()) {
      // Expand the last path

      List<ARGState> curPath = paths.remove(paths.size() - 1);
      // If there is no more to expand - add this path and continue
      if (curPath.get(curPath.size() - 1) == root) {
        results.add(new ARGPath(Lists.reverse(curPath)));

        continue;
      }
      // Add all parents
      for (ARGState parentElement : curPath.get(curPath.size() - 1).getParents()) {
        List<ARGState> tmp = new ArrayList<>(ImmutableList.copyOf(curPath));
        tmp.add(parentElement);
        paths.add(tmp);
      }
    }

    return results;
  }
}
