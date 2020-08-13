// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import java.util.Set;

/**
 * This Class contains 2 different Techniques but through the same distance metric
 *
 * <p>TODO: Explain the metric in more detail
 *
 * <p>Technique 1: ControlFlow Metric Technique 2: Closest Successful Execution Generator
 */
public class ControlFlowDistanceMetric {

  private final DistanceCalculationHelper distanceHelper;

  public ControlFlowDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    this.distanceHelper = pDistanceCalculationHelper;
  }

  List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);
    // compare all the paths with their distance
    return comparePaths(branches_ce, distanceHelper.convertPathsToEdges(safePaths));
  }

  /**
   * Starts the path generator technique using the Control Flow metric
   *
   * @param counterexample is the path of the counterexample
   * @return the new generated Path
   */
  List<CFAEdge> generateClosestSuccessfulExecution(ARGPath counterexample) {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branchesCe = findBranches(ce);
    // auto path generator
    List<List<CFAEdge>> successfulGeneratedPath = pathGenerator(branchesCe, ce);

    // check the number of the successfulGeneratedPath
    List<CFAEdge> finalGeneratedPath;
    if (successfulGeneratedPath == null) {
      finalGeneratedPath = null;
    } else if (successfulGeneratedPath.size() == 1) {
      finalGeneratedPath = successfulGeneratedPath.get(0);
    } else if (successfulGeneratedPath.size() > 1) {
      finalGeneratedPath = comparePaths(branchesCe, successfulGeneratedPath);
    } else {
      // TODO: Unreachable Branch because of ANT - Compiler and Gitlab Pipeline
      finalGeneratedPath = null;
    }

    return finalGeneratedPath;
  }

  /**
   * Compares all the paths and finds the one with the smallest distance from the counterexample
   *
   * @param ce the counterexample branches
   * @param safePaths list with all the safe paths 
   */
  private List<CFAEdge> comparePaths(List<CFAEdge> ce, List<List<CFAEdge>> safePaths) {
    List<List<CFAEdge>> safePathBranchesList = new ArrayList<>();

    if (safePaths.isEmpty()) {
      return null;
    }

    // find all branches in safe paths
    for (List<CFAEdge> safePath : safePaths) {
      safePathBranchesList.add(findBranches(distanceHelper.cleanPath(safePath)));
    }

    // create Events for the counterexample
    List<Event> ceEvents = new ArrayList<>();
    for (int i = 0; i < ce.size(); i++) {
      ceEvents.add(new Event(ce.get(i), ce));
    }

    // create Events for all the Safe Paths
    List<List<Event>> safeEvents = new ArrayList<>();
    for (int i = 0; i < safePathBranchesList.size(); i++) {
      List<Event> events = new ArrayList<>();
      for (int j = 0; j < safePathBranchesList.get(i).size(); j++) {
        events.add(new Event(safePathBranchesList.get(i).get(j), safePaths.get(i)));
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
    eliminateZeroDistances(distances, safePaths);

    // find the closest successful execution
    int closestSuccessfulRunIndex = closestSuccessfulRun(distances);

    // TODO: Review: closestSuccessfulRun works with List<Events> (i.e. the Control Flow of a Path)
    // <---> I want here the whole Path
    if (closestSuccessfulRunIndex == -1) {
      // Index -1 INDICATES THAT
      // NO CLOSEST SUCCESSFUL EXECUTION WAS FOUND
      return null;
    }

    return safePaths.get(closestSuccessfulRunIndex);
  }

  // TODO: Check if this is safe
  /**
   * Get rid of safe paths with distance = 0
   *
   * @return the safe paths with distance != 0
   */
  private List<List<Event>> eliminateZeroDistances(
      List<List<Event>> pDistances, List<List<CFAEdge>> safePaths) {
    for (int i = 0; i < pDistances.size(); i++) {
      if (pDistances.get(i).isEmpty()) {
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

    List<Event> closest = Collections.min(pDistances, new Comparator<List<Event>>() {
      @Override
      public int compare(
          List<Event> a, List<Event> b) {
        int aSum = a.stream().map(e -> e.getDistanceFromTheEnd()).reduce(0, Integer::sum);
        int bSum = b.stream().map(e -> e.getDistanceFromTheEnd()).reduce(0, Integer::sum);
        return Integer.compare(aSum, bSum);
      }
    });

    return pDistances.indexOf(closest);
  }

  /**
   * Find the distance between all safe paths and the counterexample
   *
   * @param pCEvents the events of the counterexample
   * @param pSafeEvents the events of all the safe paths
   * @return the Distance := List of events that are aligned but have a different outcome
   */
  private List<Event> distance(List<Event> pCEvents, List<Event> pSafeEvents) {
    List<Event> deltas = new ArrayList<>();
    // wait list for the events to be aligned
    List<Event> eventsWaitList = new ArrayList<>(pSafeEvents);
    List<Event> ceAlignedEvents = new ArrayList<>();
    List<Event> safeAlignedEvents = new ArrayList<>();

    // MAKING ALIGNMENTS
    for (Event pCEvent : pCEvents) {
      for (int j = 0; j < eventsWaitList.size(); j++) {
        if (pCEvent.getNode().getNodeNumber()
            == eventsWaitList.get(j).getNode().getNodeNumber()) {
          ceAlignedEvents.add(pCEvent);
          safeAlignedEvents.add(eventsWaitList.get(j));
          // remove the aligned events from the wait-list
          eventsWaitList.remove(j);
          break;
        }
      }
    }

    // Find Differences - Distance Calculation in the form of a List of Events
    for (Event pCeAlignedEvent : ceAlignedEvents) {
      for (int j = 0; j < safeAlignedEvents.size(); j++) {
        if (pCeAlignedEvent.getLine() == safeAlignedEvents.get(j).getLine()) {
          if (!pCeAlignedEvent
              .getStatement()
              .equals(safeAlignedEvents.get(j).getStatement())) {
            deltas.add(pCeAlignedEvent);
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
    for (CFAEdge ceEdge : pCe) {
      if (ceEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        branches.add(ceEdge);
      }
    }
    return branches;
  }

  /**
   * Auto generator of the closest successful safe path
   *
   * @param pBranchesCE the control flow of the counterexample
   * @param ce the actual counterexample path as List of CFAEdges
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
   * @param ce List of the CFAEdges of the counterexample
   * @param lastBranch the last branch of the Counterexample
   * @param diff the leaving edge of lastBranch that is the one that we want to explore
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
      visited.add(currentEdge.getPredecessor());
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
   *
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
