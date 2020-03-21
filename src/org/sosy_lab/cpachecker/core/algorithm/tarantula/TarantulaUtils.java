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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class TarantulaUtils {

  private static ARGState targetState;

  /** Helper class for TarantulaAlgorithm related utility methods. */
  private TarantulaUtils() {}

  private static List<CFAEdge> getErrorPath(ReachedSet reached) {

    targetState = getErrorState(reached);

    ARGPath currentFailurePath = ARGUtils.getOnePathTo(targetState);
    return currentFailurePath.getFullPath();
  }

  private static List<CFAEdge> getSafePath(ReachedSet reachedSet) {
    ARGPath safePath = null;

    targetState = getErrorState(reachedSet);

    Collection<ARGState> statesOnErrorPath = ARGUtils.getAllStatesOnPathsTo(targetState);
    for (AbstractState s : reachedSet) {
      ARGState currentState = AbstractStates.extractStateByType(s, ARGState.class);

      if (!statesOnErrorPath.contains(currentState)) {
        safePath = ARGUtils.getOnePathTo(currentState);
      }
    }
    return safePath.getFullPath();
  }

  public static boolean checkSafePath(ReachedSet reachedSet) {
    targetState = getErrorState(reachedSet);

    Collection<ARGState> statesOnErrorPath = ARGUtils.getAllStatesOnPathsTo(targetState);
    for (AbstractState s : reachedSet) {
      ARGState currentState = AbstractStates.extractStateByType(s, ARGState.class);

      if (!statesOnErrorPath.contains(currentState)) {
        return true;
      }
    }
    return false;
  }

  public static boolean checkForErrorPath(ReachedSet reachedSet) {
    for (AbstractState state : reachedSet) {
      if (AbstractStates.isTargetState(state)) {
        return true;
      }
    }
    return false;
  }

  private static ARGState getErrorState(ReachedSet reachedSet) {
    return FluentIterable.from(reachedSet)
            .transform(s -> AbstractStates.extractStateByType(s, ARGState.class))
            .filter(ARGState::isTarget)
            .first()
            .get();
  }

  private static List<Integer> getLinesFromPath(
          List<CFAEdge> path, ArrayList<Integer> programLines, int pPassedPath) {
    ArrayList<Integer> lines = new ArrayList<>();

    for (int i = 1; i < path.size(); i++) {
      if (path.get(i).getLineNumber() != 0) {
        lines.add(path.get(i).getLineNumber());
      }
    }
    List<Integer> listOutput =
            programLines.stream().map(i -> lines.contains(i) ? i : 0).collect(Collectors.toList());
    listOutput.add(0, pPassedPath);
    return listOutput;
  }
  // get lines from the input program
  public static ArrayList<Integer> getProgramLines(ReachedSet reachedSet) {
    ArrayList<Integer> result = new ArrayList<>();
    for (AbstractState s : reachedSet) {
      ARGState currentState = AbstractStates.extractStateByType(s, ARGState.class);
      Collection<CFAEdge> e =
              currentState.getParents().stream()
                      .map(p -> p.getEdgeToChild(currentState))
                      .filter(x -> x != null && x.getLineNumber() != 0)
                      .collect(Collectors.toList());
      Iterator<CFAEdge> iterator = e.iterator();
      while (iterator.hasNext()) {
        result.add(iterator.next().getLineNumber());
      }
    }
    LinkedHashSet<Integer> hashSet = new LinkedHashSet<>(result);
    ArrayList<Integer> listWithoutDuplicates = new ArrayList<>(hashSet);
    return listWithoutDuplicates;
  }

  public static List<Integer> CoveredLines(List<Integer> path) {
    boolean asc = path.get(1) - path.get(0) == 1;
    for (int i = 1; i < path.size() - 1; i++) {
      if (path.get(i + 1) - path.get(i) == 1) {
        path.set(i, 1);
        asc = true;
      } else {
        if (asc) {
          asc = false;
          path.set(i, 1);
        } else {
          path.set(i, 0);
        }
      }
    }
    path.set(path.size() - 1, asc ? 1 : 0);
    return path;
  }

  private static List<List<Integer>> convertingToTable(
          ArrayList<Integer> getProgramLines, List<Integer> safePaths, List<Integer> errorPaths) {
    List<Integer> safeCodeLines = TarantulaUtils.CoveredLines(safePaths);
    List<Integer> errorCodeLines = TarantulaUtils.CoveredLines(errorPaths);
    List<List<Integer>> table = new ArrayList<>();
    table.add(getProgramLines);
    table.add(safeCodeLines);
    table.add(errorCodeLines);
    return table;
  }

  public static List<Integer> linesFromSafePath(int pathValue, ReachedSet reachedSet) {
    return TarantulaUtils.getLinesFromPath(
            TarantulaUtils.getSafePath(reachedSet),
            TarantulaUtils.getProgramLines(reachedSet),
            pathValue);
  }

  public static List<Integer> linesFromErrorPath(int pathValue, ReachedSet reachedSet) {
    return TarantulaUtils.getLinesFromPath(
            TarantulaUtils.getErrorPath(reachedSet),
            TarantulaUtils.getProgramLines(reachedSet),
            pathValue);
  }

  public static List<List<Integer>> getTable(
          ReachedSet reachedSet, List<Integer> passedLines, List<Integer> failedLines) {
    return convertingToTable(TarantulaUtils.getProgramLines(reachedSet), passedLines, failedLines);
  }
}
