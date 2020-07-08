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
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.java_smt.api.SolverException;
import java.util.Set;

/**
 * This Class contains 2 different Techniques but
 * through the same distance metric
 * Technique 1: ControlFlow Metric
 * Technique 2: Closest Successful Execution Generator
 */
public class ControlFLowDistanceMetric {

  private final DistanceCalculationHelper distanceHelper;

  public ControlFLowDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    this.distanceHelper = pDistanceCalculationHelper;
  }

  List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);
    // compare all the paths with their distance
    List<CFAEdge> closest_suc_path =
        comparePaths(branches_ce, distanceHelper.convertPathsToEdges(safePaths));
    return closest_suc_path;
  }


  /**
   * Starts the path generator technique with the distance metric - ALIGNMENTS -
   * @param safePaths is a List with all the Safe paths
   * @param counterexample is the path of the counterexample
   * @return the new generated Path
   * @throws SolverException for Error
   * @throws InterruptedException for Error
   */
  List<CFAEdge> startPathGenerator(List<ARGPath> safePaths, ARGPath counterexample)
      throws SolverException, InterruptedException {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);
    // auto path generator
    List<List<CFAEdge>> successfulGeneratedPath = pathGenerator(branches_ce, ce);

    // check the number of the successfulGeneratedPath
    List<CFAEdge> finalGeneratedPath;
    if (successfulGeneratedPath == null) {
      finalGeneratedPath = null;
    } else if (successfulGeneratedPath.isEmpty()) {
      return comparePaths(branches_ce, distanceHelper.convertPathsToEdges(safePaths));
    } else if (successfulGeneratedPath.size() == 1) {
      finalGeneratedPath = successfulGeneratedPath.get(0);
    } else if (successfulGeneratedPath.size() > 1) {
      finalGeneratedPath = comparePaths(branches_ce, successfulGeneratedPath);
    } else {
      finalGeneratedPath = null;
    }

    return finalGeneratedPath;

  }

  /**
   * Compares all the paths and finds the one with the smallest distance from the counterexample
   *
   * @param ce        the counterexample branches
   * @param safePaths list with all the safe paths
   */
  private List<CFAEdge> comparePaths(List<CFAEdge> ce, List<List<CFAEdge>> safePaths) {
    List<List<CFAEdge>> safe_path_branches_list = new ArrayList<>();

    if (safePaths.isEmpty()) {
      return null;
    }

    // find all branches in safe paths
    for (int i = 0; i < safePaths.size(); i++) {
      safe_path_branches_list.add(findBranches(distanceHelper.cleanPath(safePaths.get(i))));
    }

    // create Events for the counterexample
    List<Event> ceEvents = new ArrayList<>();
    for (int i = 0; i < ce.size(); i++) {
      ceEvents.add(new Event(ce.get(i), ce));
    }

    // create Events for all the Safe Paths
    List<List<Event>> safeEvents = new ArrayList<>();
    for (int i = 0; i < safe_path_branches_list.size(); i++) {
      List<Event> events = new ArrayList<>();
      for (int j = 0; j < safe_path_branches_list.get(i).size(); j++) {
        events.add(new Event(safe_path_branches_list.get(i).get(j), safePaths.get(i)));
      }
      safeEvents.add(events);
    }

    // compute the distances
    // The number of events equals here the number of safe paths
    List<List<Event>> distances = new ArrayList<>();
    for (List<Event> events : safeEvents) {
      distances.add(distance(ceEvents, events));
    }

    // get rid of useless safe paths with distance = 0
    getUsefulSafePaths(distances, safePaths);

    // find the closest successful execution
    int closestSuccessfulRunIndex = closestSuccessfulRun(distances);

    if (closestSuccessfulRunIndex == -1) {
      // Index -1 INDICATES THAT
      // NO CLOSEST SUCCESSFUL EXECUTION WAS FOUND
      return null;
    }

    return safePaths.get(closestSuccessfulRunIndex);

  }

  /**
   * Get rid of safe paths with distance = 0
   *
   * @return the safe paths with distance != 0
   */
  private List<List<Event>> getUsefulSafePaths(
      List<List<Event>> pDistances,
      List<List<CFAEdge>> safePaths) {
    for (int i = 0; i < pDistances.size(); i++) {
      if (pDistances.get(i).size() == 0) {
        pDistances.remove(i);
        safePaths.remove(i);
      }
    }
    return pDistances;
  }

  /**
   * Finds the closest successful execution to the counterexample
   *
   * @param pDistances the list of distances
   * @return the distance - List of the different events - of the closest safe path
   */
  private int closestSuccessfulRun(List<List<Event>> pDistances) {
    if (pDistances.isEmpty()) {
      return -1;
    }

    // init
    List<Event> closest = pDistances.get(0);
    int index = 0;
    // compare the distances
    for (int i = 1; i < pDistances.size(); i++) {
      if (isCloserThan(pDistances.get(i), closest)) {
        closest = pDistances.get(i);
        index = i;
      }
    }
    return index;
  }

  /**
   * Finds if a path is closer to the counterexample than the current closest one
   *
   * @param pCurrent the new path that we have to evaluate
   * @param pClosest the current closest path to the counterexample
   * @return true, if the current path is indeed closer to the counterexample than the other
   */
  private boolean isCloserThan(List<Event> pCurrent, List<Event> pClosest) {
    // fewer changes --> the closest remains the same
    if (pClosest.size() < pCurrent.size()) {
      return false;
    } else if (pClosest.size() > pCurrent.size()) {
      return true;
    }
    Deque<Event> closest = new ArrayDeque<>(pClosest);
    Deque<Event> current = new ArrayDeque<>(pCurrent);

    int closest_distance = 0;
    for (Event event : closest) {
      closest_distance += event.getDistanceFromTheEnd();
    }
    int current_distance = 0;
    for (Event event : current) {
      current_distance += event.getDistanceFromTheEnd();
    }

    return (current_distance <= closest_distance);

  }

  /**
   * Find the distance between all safe paths and the counterexample
   *
   * @param pCEevents   the events of the counterexample
   * @param pSafeEvents the events of all the safe paths
   * @return the Distance := List of events that are aligned but have a different outcome
   */
  private List<Event> distance(List<Event> pCEevents, List<Event> pSafeEvents) {
    List<Event> deltas = new ArrayList<>();
    // wait list for the events to be aligned
    List<Event> eventsWaitList = new ArrayList<>(pSafeEvents);
    List<Event> ceAlignedEvents = new ArrayList<>();
    List<Event> safeAlignedEvents = new ArrayList<>();

    // MAKING ALIGNMENTS
    for (int i = 0; i < pCEevents.size(); i++) {
      for (int j = 0; j < eventsWaitList.size(); j++) {
        if (pCEevents.get(i).getNode().getNodeNumber() == eventsWaitList.get(j).getNode()
            .getNodeNumber()) {
          ceAlignedEvents.add(pCEevents.get(i));
          safeAlignedEvents.add(eventsWaitList.get(j));
          // remove the aligned events from the wait-list
          eventsWaitList.remove(j);
          break;
        }
      }
    }

    // Find Differences - Distance Calculation in the form of a List of Events
    for (int i = 0; i < ceAlignedEvents.size(); i++) {
      for (int j = 0; j < safeAlignedEvents.size(); j++) {
        if (ceAlignedEvents.get(i).getLine() == safeAlignedEvents.get(j).getLine()) {
          if (!ceAlignedEvents.get(i).getStatement()
              .equals(safeAlignedEvents.get(j).getStatement())) {
            deltas.add(ceAlignedEvents.get(i));
            safeAlignedEvents.remove(j);
            break;
          }
        }
      }
    }

    return deltas;
  }

  /**
   * Finds the control flow branches of the path
   *
   * @param pCe a list of CFAEdges
   * @return a list with all control flow branches of the path
   */
  private List<CFAEdge> findBranches(List<CFAEdge> pCe) {
    List<CFAEdge> branches = new ArrayList<>();
    for (int i = 0; i < pCe.size(); i++) {
      if (pCe.get(i).getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        branches.add(pCe.get(i));
      }
    }
    return branches;
  }

  /**
   * Auto generator of the closest successful safe path
   *
   * @param pBranchesCE the control flow of the counterexample
   * @param ce          the actual counterexample path as List of CFAEdges
   * @return the new Generated Path (maybe more than one found)
   */
  private List<List<CFAEdge>> pathGenerator(List<CFAEdge> pBranchesCE, List<CFAEdge> ce) {
    if (pBranchesCE.isEmpty() || ce.isEmpty()) {
      return null;
    }

    // Get the last branch of the counterexample - the one closer to the Error -
    CFAEdge lastBranch = pBranchesCE.get(pBranchesCE.size() - 1);
    // change the flow of lastBranch
    int edges = lastBranch.getPredecessor().getNumLeavingEdges();

    if (edges != 2) {
      return null;
    }

    CFAEdge diff;
    if (lastBranch.equals(lastBranch.getPredecessor().getLeavingEdge(0))) {
      diff = lastBranch.getPredecessor().getLeavingEdge(1);
    } else {
      diff = lastBranch.getPredecessor().getLeavingEdge(0);
    }

    return buildNewPath(ce, lastBranch, diff);
  }


  /**
   * Building the closest to the target successful safe path
   *
   * @param ce         List of the CFAEdges of the counterexample
   * @param lastBranch the last branch of the Counterexample
   * @param diff       the leaving edge of lastBranch that is the one that we want to explore
   * @return all the feasible safe paths that were built
   */
  private List<List<CFAEdge>> buildNewPath(List<CFAEdge> ce, CFAEdge lastBranch, CFAEdge diff) {
    assert lastBranch.getEdgeType().equals(CFAEdgeType.AssumeEdge);
    List<CFAEdge> result = new ArrayList<>();
    // start building the new path
    for (CFAEdge pCFAEdge : ce) {
      if (lastBranch.equals(pCFAEdge)) {
        result.add(diff);
        break;
      }
      result.add(pCFAEdge);
    }

    // In Case that the last branch has more than one feasible safe paths
    // then this technique finds all of them and returns them in the form
    // of List<List<CFAEdge>>
    List<List<CFAEdge>> paths = findAllPaths(result);

    return paths;

  }

  /**
   * Finds all outgoing paths from the last branch
   *
   * @param current the current Path
   * @return all the found (feasible) Paths
   */
  private List<List<CFAEdge>> findAllPaths(List<CFAEdge> current) {
    List<List<CFAEdge>> paths = new ArrayList<>();
    paths.add(current);
    // is the last
    List<CFAEdge> currentPath = paths.get(0);
    CFAEdge currentEdge;

    for (int i = 0; i < paths.size(); i++) {
      currentPath = paths.get(i);
      currentEdge = currentPath.get(currentPath.size() - 1);

      Set<CFANode> visited = new HashSet<>();
      Deque<CFAEdge> waitList = new ArrayDeque<>();
      waitList.add(currentEdge);

      while (!waitList.isEmpty()) {
        CFANode successor = waitList.pop().getSuccessor();
        if (visited.contains(successor)) {
          continue;
        }
        if (successor.getNumLeavingEdges() == 1) {
          currentPath.add(successor.getLeavingEdge(0));
          waitList.add(successor.getLeavingEdge(0));
        } else if (successor.getNumLeavingEdges() == 2) {
          List<CFAEdge> extra = new ArrayList<>(currentPath);
          extra.add(successor.getLeavingEdge(1));
          paths.add(extra);
          currentPath.add(successor.getLeavingEdge(0));
          waitList.add(successor.getLeavingEdge(0));
        }
        visited.add(successor);
      }
    }

    // Feasibility Check
    List<List<CFAEdge>> finalList = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      if (!isTarget(paths.get(i))) {
        finalList.add(paths.get(i));
      }
    }
    return finalList;
  }

  /**
   * Checks if the list contains a Target State
   * @param list a list with CFAEdges that has to be checked
   * @return true if it has no target states, otherwise false
   */
  private boolean isTarget(List<CFAEdge> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).getDescription().equals("Label: ERROR")) {
        return true;
      }
    }
    return false;
  }
}
