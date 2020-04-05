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
import java.util.Collection;
import java.util.List;
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
  private static final int SAFE_PATH_VALUE = 0;
  private static final int FAILED_PATH_VALUE = 1;
  private static final int PASSED_PATH_CASE = 0;
  private static final int FAILED_PATH_CASE = 1;

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
        .transform(s -> getRoot(s))
        .filter(ARGState::isTarget)
        .toList();
  }
  /**
   * Gets error states in case of loop (safe leaves) from ARG.
   *
   * @param pRoot root at ARG.
   * @return Detected error loop states.
   */
  public static List<ARGState> getErrorLoopState(ARGState pRoot) {

    ImmutableList<ARGState> leaves =
        from(pRoot.getSubgraph())
            .filter(
                s -> {
                  assert s != null;
                  return s.getChildren().isEmpty() && s.isCovered();
                })
            .toList();

    List<ARGState> errorLoopStates = new ArrayList<>();

    for (ARGState leaf : leaves) {
      if (AbstractStates.isTargetState(leaf)) {
        errorLoopStates.add(leaf);
      }
    }

    return errorLoopStates;
  }
  /**
   * Merges error states and error states in case of loop from ARG together.
   *
   * @param reachedSet Input.
   * @return Detected all error states.
   */
  private static List<ARGState> mergeAllErrorStates(ReachedSet reachedSet) {
    List<List<ARGState>> result = new ArrayList<>();
    result.add(getErrorStates(reachedSet));
    result.add(getErrorLoopState(getRoot(reachedSet.getFirstState())));

    return result.stream().flatMap(Collection::stream).collect(Collectors.toList());
  }
  /**
   * Gets safe states (safe leaves) from ARG.
   *
   * @param pRoot root at ARG.
   * @return Detected safe states.
   */
  public static ARGState getSafeState(ARGState pRoot) {
    FluentIterable<ARGState> leaves =
        from(pRoot.getSubgraph())
            .filter(
                s -> {
                  assert s != null;
                  return s.getChildren().isEmpty() && !s.isCovered();
                });
    ARGState safeState = null;

    for (ARGState leaf : leaves) {
      if (!AbstractStates.isTargetState(leaf)) {
        safeState = leaf;
      }
    }

    return safeState;
  }
  /**
   * Gets safe states in case of loop (safe leaves) from ARG.
   *
   * @param pRoot root at ARG.
   * @return Detected safe loop states.
   */
  public static ARGState getSafeLoopState(ARGState pRoot) {
    FluentIterable<ARGState> leaves =
        from(pRoot.getSubgraph())
            .filter(
                s -> {
                  assert s != null;
                  return s.getChildren().isEmpty() && s.isCovered();
                });
    ARGState loopSafeState = null;

    for (ARGState leaf : leaves) {
      if (!AbstractStates.isTargetState(leaf)) {
        loopSafeState = leaf;
      }
    }
    return loopSafeState;
  }
  /**
   * Merges safe states and safe states in case of loop from ARG together.
   *
   * @param reachedSet Input.
   * @return Merged all safe states.
   */
  private static List<ARGState> mergeAllSafeStates(ReachedSet reachedSet) {
    List<ARGState> mergedAllSafeStates = new ArrayList<>();
    root = getRoot(reachedSet.getFirstState());

    mergedAllSafeStates.add(getSafeState(root));
    if (getSafeLoopState(root) != null) {
      mergedAllSafeStates.add(getSafeLoopState(root));
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

    targetStates = mergeAllErrorStates(reachedSet);
    List<List<CFAEdge>> errorEdges;
    List<List<List<CFAEdge>>> allPathsTogether = new ArrayList<>();

    for (ARGState targetState : targetStates) {
      allPathsTogether.add(getAllPaths(reachedSet, targetState));
    }
    errorEdges = allPathsTogether.stream().flatMap(Collection::stream).collect(Collectors.toList());
    return errorEdges;
  }

  /**
   * Gets two dimensional CFAEdge list of the safe paths.
   *
   * @param reachedSet input.
   * @return Detected safe edges.
   */
  public static List<List<CFAEdge>> getEdgesOfSafePaths(ReachedSet reachedSet) {
    List<List<CFAEdge>> safeEdges;
    List<List<List<CFAEdge>>> allPathsTogether = new ArrayList<>();

    for (ARGState safePath : mergeAllSafeStates(reachedSet)) {
      if (checkForSafePath(reachedSet)) {

        allPathsTogether.add(getAllPaths(reachedSet, safePath));
      }
    }
    safeEdges = allPathsTogether.stream().flatMap(Collection::stream).collect(Collectors.toList());

    return safeEdges;
  }

  /**
   * Gets all program edges reachedSet.
   *
   * @param reachedSet input.
   * @return Amount of program edges.
   */
  public static List<CFAEdge> getProgramEdges(ReachedSet reachedSet) {
    List<CFAEdge> programEdges = new ArrayList<>();

    for (AbstractState s : reachedSet) {
      ARGState currentState = getRoot(s);
      assert currentState != null;
      Collection<CFAEdge> edge =
          currentState.getParents().stream()
              .map(p -> p.getEdgeToChild(currentState))
              .filter(x -> x != null && x.getLineNumber() != 0)
              .collect(Collectors.toList());

      programEdges.addAll(edge);
    }

    return programEdges;
  }

  /**
   * Converts path into binary list (1 or 0) such that 1 means its covered and 0 its not covered.
   *
   * @param path type of the path, error path or safe path.
   * @param programEdges list of program edges.
   * @return binary result.
   */
  public static List<Integer> coveredEdges(
      List<CFAEdge> path, List<CFAEdge> programEdges, ReachedSet reachedSet) {
    List<Integer> binaryResult = new ArrayList<>();
    targetStates = mergeAllErrorStates(reachedSet);
    for (AbstractState targetState : targetStates) {
      CFANode nodeOfTargetState = AbstractStates.extractLocation(targetState);
      if (path.get(path.size() - 1).getSuccessor().equals(nodeOfTargetState)) {
        binaryResult.add(0, FAILED_PATH_VALUE);
        break;
      } else {
        binaryResult.add(0, SAFE_PATH_VALUE);
      }
    }
    for (int i = 1; i < programEdges.size(); i++) {

      if (path.contains(programEdges.get(i))) {
        binaryResult.add(i, FAILED_PATH_CASE);
      } else {
        binaryResult.add(i, PASSED_PATH_CASE);
      }
    }
    return binaryResult;
  }

  /**
   * Gets the two dimensional <code>ArrayList</code>.
   *
   * @param reachedSet input.
   * @return Converted covered edges into binary <code>ArrayList</code>.
   */
  public static List<List<Integer>> getTable(ReachedSet reachedSet) {

    return convertingToBinary(
        mergeInto2dArray(getEdgesOfSafePaths(reachedSet), getEdgesOfErrorPaths(reachedSet)),
        getProgramEdges(reachedSet),
        reachedSet);
  }
  /**
   * Merges into two dimensional <code>ArrayList</code>.
   *
   * @param safePaths safe paths.
   * @param errorPaths error paths
   * @return merged ArrayList.
   */
  private static List<List<CFAEdge>> mergeInto2dArray(
      List<List<CFAEdge>> safePaths, List<List<CFAEdge>> errorPaths) {

    return Stream.concat(safePaths.stream(), errorPaths.stream()).collect(Collectors.toList());
  }

  /**
   * Converts all paths into binary list <code>(1 or 0)</code> based on the program edges.
   *
   * @param path type of the path, error path or safe path.
   * @param programEdges list of program edges.
   * @return binary result.
   */
  private static List<List<Integer>> convertingToBinary(
      List<List<CFAEdge>> path, List<CFAEdge> programEdges, ReachedSet reachedSet) {
    List<List<Integer>> binaryResult = new ArrayList<>();

    for (List<CFAEdge> pCFAEdges : path) {
      binaryResult.add(coveredEdges(pCFAEdges, programEdges, reachedSet));
    }

    return binaryResult;
  }

  /**
   * Checks whether there is a safe paths in the ARG or not.
   *
   * @param reachedSet input.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public static boolean checkForSafePath(ReachedSet reachedSet) {
    root = getRoot(reachedSet.getFirstState());

    for (AbstractState state : reachedSet) {
      assert root != null;
      if (getSafeState(root) == state) {
        return true;
      }
    }

    return false;
  }
  /**
   * Extract root state from ARG.
   *
   * @param pPFirstState First state of ARG.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  private static ARGState getRoot(AbstractState pPFirstState) {
    return AbstractStates.extractStateByType(pPFirstState, ARGState.class);
  }

  /**
   * Checks whether there is a false paths in the ARG or not.
   *
   * @param reachedSet input.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public static boolean checkForErrorPath(ReachedSet reachedSet) {

    for (AbstractState state : reachedSet) {
      if (AbstractStates.isTargetState(state)) {
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
    List<List<ARGState>> getAllErrorState = getAllStatesReversed(reachedSet, chosenState);
    List<List<CFAEdge>> paths = new ArrayList<>();

    for (List<ARGState> pARGStates : getAllErrorState) {
      paths.add(new ARGPath(Lists.reverse(pARGStates)).getFullPath());
    }

    return paths;
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

    root = getRoot(reachedSet.getFirstState());
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
        List<ARGState> tmp = new ArrayList<>(List.copyOf(curPath));
        tmp.add(parentElement);
        paths.add(tmp);
      }
    }

    return results;
  }
}
