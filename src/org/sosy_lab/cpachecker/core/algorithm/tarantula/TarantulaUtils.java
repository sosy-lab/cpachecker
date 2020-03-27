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

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class TarantulaUtils {

  private static List<ARGState> targetStates;
  private static final int SAFE_PATH_VALUE = 0;
  private static final int FAILED_PATH_VALUE = 1;
  private static final int PASSED_PATH_CASE = 0;
  private static final int FAILED_PATH_CASE = 1;
  private static final String VERIFIER_ERROR = "__VERIFIER_error()";

  /** Helper class for TarantulaAlgorithm related utility methods. */
  private TarantulaUtils() {}
  /**
   * Gets how many error states are in ARG.
   *
   * @param reachedSet input.
   * @return Detected error states.
   */
  private static List<ARGState> getErrorStates(ReachedSet reachedSet) {
    return FluentIterable.from(reachedSet)
        .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
        .filter(ARGState::isTarget)
        .toList();
  }
  /**
   * Gets 2d CFAEdge list of the error paths.
   *
   * @param reachedSet input.
   * @return Detected error edges.
   */
  public static List<List<CFAEdge>> getEdgesOfErrorPaths(ReachedSet reachedSet) {
    List<List<CFAEdge>> errorEdges = new ArrayList<>();
    targetStates = getErrorStates(reachedSet);
    for (ARGState pTargetState : targetStates) {
      errorEdges.add(ARGUtils.getOnePathTo(pTargetState).getFullPath());
    }
    return errorEdges;
  }
  /**
   * Gets 2d CFAEdge list of the safe paths.
   *
   * @param reachedSet input.
   * @return Detected safe edges.
   */
  public static List<List<CFAEdge>> getEdgesOfSafePaths(ReachedSet reachedSet) {
    List<List<CFAEdge>> safeEdges = new ArrayList<>();
    targetStates = getErrorStates(reachedSet);
    if (TarantulaUtils.checkSafePath(reachedSet)) {
      safeEdges.add(TarantulaUtils.getSafePath(reachedSet));
    }

    return safeEdges;
  }
  /**
   * Extracts all states on the error paths.
   *
   * @param reachedSet input.
   * @return Collection of ARG states.
   */
  private static Collection<ARGState> extractAllStatesOnErrorPaths(ReachedSet reachedSet) {
    targetStates = getErrorStates(reachedSet);
    Collection<ARGState> states = null;
    Collection<Collection<ARGState>> result = new ArrayList<>();
    for (ARGState pTargetState : targetStates) {

      states = ARGUtils.getAllStatesOnPathsTo(pTargetState);
      result.add(states);
    }
    return result.stream().flatMap(Collection::stream).collect(Collectors.toList());
  }
  /**
   * Gets safe path from reachedSet.
   *
   * @param reachedSet input.
   * @return Full path of the detected safe path.
   */
  private static List<CFAEdge> getSafePath(ReachedSet reachedSet) {
    ARGPath safePath = null;

    Collection<ARGState> statesOnErrorPath =
        TarantulaUtils.extractAllStatesOnErrorPaths(reachedSet);

    for (AbstractState s : reachedSet) {
      ARGState currentState = AbstractStates.extractStateByType(s, ARGState.class);
      assert currentState != null;
      if (!statesOnErrorPath.contains(currentState) && currentState.getChildren().isEmpty()) {
        safePath = ARGUtils.getOnePathTo(currentState);
        break;
      }
    }

    assert safePath != null;
    return safePath.getFullPath();
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
      ARGState currentState = AbstractStates.extractStateByType(s, ARGState.class);
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
  public static List<Integer> coveredEdges(List<CFAEdge> path, List<CFAEdge> programEdges) {
    List<Integer> binaryResult = new ArrayList<>();
    for (int i = 0; i < targetStates.size(); i++) {
      if (path.get(path.size() - 1).getDescription().contains(VERIFIER_ERROR)) {
        binaryResult.add(0, FAILED_PATH_VALUE);
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
      List<List<CFAEdge>> path, List<CFAEdge> programEdges) {
    List<List<Integer>> binaryResult = new ArrayList<>();
    for (List<CFAEdge> pCFAEdges : path) {
      binaryResult.add(coveredEdges(pCFAEdges, programEdges));
    }
    return binaryResult;
  }
  /**
   * Checks whether there is a safe paths in the ARG or not.
   *
   * @param reachedSet input.
   * @return Returns <code>true</code> if the path exists otherwise returns <code>false</code>
   */
  public static boolean checkSafePath(ReachedSet reachedSet) {
    Collection<ARGState> statesOnErrorPath =
        TarantulaUtils.extractAllStatesOnErrorPaths(reachedSet);

    for (AbstractState s : reachedSet) {
      ARGState currentState = AbstractStates.extractStateByType(s, ARGState.class);
      assert currentState != null;
      if (!statesOnErrorPath.contains(currentState) && currentState.getChildren().isEmpty()) {
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
  public static boolean checkForErrorPath(ReachedSet reachedSet) {
    for (AbstractState state : reachedSet) {
      if (AbstractStates.isTargetState(state)) {
        return true;
      }
    }
    return false;
  }
  /**
   * Gets the 2 dimensional <code>ArrayList</code>.
   *
   * @param reachedSet input.
   * @return Converted covered edges into binary <code>ArrayList</code>.
   */
  public static List<List<Integer>> getTable(ReachedSet reachedSet) {
    return TarantulaUtils.convertingToBinary(
        TarantulaUtils.mergeInto2dArray(
            TarantulaUtils.getEdgesOfSafePaths(reachedSet),
            TarantulaUtils.getEdgesOfErrorPaths(reachedSet)),
        TarantulaUtils.getProgramEdges(reachedSet));
  }
}
