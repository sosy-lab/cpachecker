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

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class TarantulaUtils {

  private static List<ARGState> targetStates;
  private static ARGState root;

  /** Helper class for TarantulaAlgorithm related utility methods. */
  private TarantulaUtils() {}
  /**
   * Gets error states (error leaves) from ARG.
   *
   * @param reachedSet input.
   * @return Detected error states.
   */
  private static List<ARGState> getErrorStates(ReachedSet reachedSet) {

    return FluentIterable.from(reachedSet)
        .transform(s -> getRootState(s))
        .filter(ARGState::isTarget)
        .toList();
  }

  /**
   * Gets safe states (safe leaves) from ARG.
   *
   * @param pRoot root at ARG.
   * @return Detected safe states.
   */
  public static List<ARGState> getSafeStates(ARGState pRoot) {
    return from(pRoot.getSubgraph())
        .filter(
            e -> {
              assert e != null;
              return !e.isCovered() && !e.isTarget();
            })
        .filter(
            s -> {
              assert s != null;
              return s.getChildren().isEmpty();
            })
        .toList();
  }
  /**
   * Gets safe states in case of loop (safe leaves) from ARG.
   *
   * @param pRoot root at ARG.
   * @return Detected safe loop states.
   */
  public static List<ARGState> getSafeLoopStates(ARGState pRoot) {

    return from(pRoot.getSubgraph())
        .filter(
            e -> {
              assert e != null;
              return e.isCovered() && !e.isTarget();
            })
        .filter(
            s -> {
              assert s != null;
              return s.getChildren().isEmpty();
            })
        .toList();
  }
  /**
   * Merges safe states and safe states in case of loop from ARG together.
   *
   * @param reachedSet Input.
   * @return Merged all safe states.
   */
  private static List<ARGState> mergeAllSafeStates(ReachedSet reachedSet) {

    root = getRootState(reachedSet.getFirstState());
    List<ARGState> mergedAllSafeStates = new ArrayList<>(getSafeStates(root));

    List<ARGState> getSafeStatesOfLoop = getSafeLoopStates(root);
    if (getSafeStatesOfLoop != null) {
      mergedAllSafeStates.addAll(getSafeStatesOfLoop);
    }
    return mergedAllSafeStates;
  }
  /**
   * Gets two dimensional CFAEdge list of the error paths.
   *
   * @param reachedSet input.
   * @return Detected error edges.
   */
  public static List<List<CFAEdge>> getEdgesOfErrorPaths(ReachedSet reachedSet) {

    targetStates = getErrorStates(reachedSet);
    List<List<CFAEdge>> allErrorPathsTogether = new ArrayList<>();

    for (ARGState targetState : targetStates) {
      allErrorPathsTogether.addAll(getAllPaths(reachedSet, targetState));
    }
    return allErrorPathsTogether;
  }

  /**
   * Gets two dimensional CFAEdge list of the safe paths.
   *
   * @param reachedSet input.
   * @return Detected safe edges.
   */
  public static List<List<CFAEdge>> getEdgesOfSafePaths(ReachedSet reachedSet) {

    List<List<CFAEdge>> allSafePathsTogether = new ArrayList<>();
    for (ARGState safePath : mergeAllSafeStates(reachedSet)) {

      if (existsSafePath(reachedSet)) {
        allSafePathsTogether.addAll(getAllPaths(reachedSet, safePath));
      }
    }
    return allSafePathsTogether;
  }

  /**
   * Gets the <code> HashMap<CFAEdge, int[]> </code>.
   *
   * @param reachedSet Input.
   * @return Covered edges.
   */
  public static Map<CFAEdge, Pair> getTable(ReachedSet reachedSet) {

    return coverageInformation(
        mergeInto2dArray(getEdgesOfSafePaths(reachedSet), getEdgesOfErrorPaths(reachedSet)),
        reachedSet);
  }

  public static List<List<CFAEdge>> getAllPossiblePaths(ReachedSet reachedSet) {

    return mergeInto2dArray(getEdgesOfSafePaths(reachedSet), getEdgesOfErrorPaths(reachedSet));
  }

  public static List<List<CFAEdge>> mergeInto2dArray(
      List<List<CFAEdge>> safePaths, List<List<CFAEdge>> errorPaths) {

    return Stream.concat(safePaths.stream(), errorPaths.stream()).collect(Collectors.toList());
  }

  /**
   * Checks whether there is a safe paths in the ARG or not.
   *
   * @param reachedSet input.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public static boolean existsSafePath(ReachedSet reachedSet) {
    root = getRootState(reachedSet.getFirstState());

    for (AbstractState state : reachedSet) {
      if (getSafeStates(root).contains(state)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks whether there is a false paths in the ARG or not.
   *
   * @param reachedSet input.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public static boolean existsErrorPath(ReachedSet reachedSet) {

    for (AbstractState state : reachedSet) {
      if (AbstractStates.isTargetState(state)) {
        return true;
      }
    }

    return false;
  }
  /**
   * Checks whether the path is a failed path or not.
   *
   * @param reachedSet input.
   * @param path The chosen path.
   * @return <code>boolean</code>
   */
  public static boolean isFailedPath(List<CFAEdge> path, ReachedSet reachedSet) {
    targetStates = getErrorStates(reachedSet);

    for (AbstractState targetState : targetStates) {
      CFANode nodeOfTargetState = AbstractStates.extractLocation(targetState);
      if (path.get(path.size() - 1).getSuccessor().equals(nodeOfTargetState)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Gets all paths from the possible leaves (whether targetStates or safeStates)to the root.
   *
   * @param reachedSet input.
   * @param chosenState whether targetStates or safeStates
   * @return Full paths
   */
  public static List<List<CFAEdge>> getAllPaths(ReachedSet reachedSet, ARGState chosenState) {
    List<List<ARGState>> getAllStates = getAllStatesReversed(reachedSet, chosenState);
    List<List<CFAEdge>> paths = new ArrayList<>();

    for (List<ARGState> pARGStates : getAllStates) {
      paths.add(new ARGPath(Lists.reverse(pARGStates)).getFullPath());
    }

    return paths;
  }
  /**
   * Extract root state from ARG.
   *
   * @param pPFirstState First state of ARG.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  private static ARGState getRootState(AbstractState pPFirstState) {
    return AbstractStates.extractStateByType(pPFirstState, ARGState.class);
  }

  /**
   * Counts how many failed case / passed case has each Edges. For example <code>
   * line 5: N2 -{[cond == 0]},[2,1]</code> means that this specific Edges has `2` failed cases and
   * only one passed case.
   *
   * @param path The whole path contains all error paths and passed paths.
   * @param reachedSet input.
   * @return result as <code>Map<code/>.
   */
  public static Map<CFAEdge, Pair> coverageInformation(
      List<List<CFAEdge>> path, ReachedSet reachedSet) {

    Map<CFAEdge, Pair> map = new LinkedHashMap<>();
    for (List<CFAEdge> individualArray : path) {
      for (int j = 0; j < individualArray.size(); j++) {
        Pair pair = new Pair(0, 0);
        if (map.containsKey(individualArray.get(j))) {
          pair = map.get(individualArray.get(j));
          if (isFailedPath(individualArray, reachedSet)) {
            pair.setFailedCases(pair.getFailedCases() + 1);
          } else {
            pair.setPassedCases(pair.getPassedCases() + 1);
          }

        } else {
          if (isFailedPath(individualArray, reachedSet)) {
            pair.setFailedCases(1);
          } else {
            pair.setPassedCases(1);
          }
        }
        // Skipp the "none" line numbers.
        if (individualArray.get(j).getLineNumber() != 0) {
          map.put(individualArray.get(j), pair);
        }
      }
    }

    return map;
  }
  /**
   * Gets all states on the possible paths from the possible leaves (whether targetStates or
   * safeStates)to the root.
   *
   * @param reachedSet input.
   * @param chosenState whether targetStates or safeStates.
   * @return States on the paths.
   */
  private static List<List<ARGState>> getAllStatesReversed(
      ReachedSet reachedSet, ARGState chosenState) {

    root = getRootState(reachedSet.getFirstState());
    List<ARGState> states = new ArrayList<>();
    List<List<ARGState>> results = new ArrayList<>();
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
        results.add(curPath);
        continue;
      }
      // Expand the path

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
