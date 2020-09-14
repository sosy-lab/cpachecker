// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * This Class contains 2 different Techniques but through the same distance metric
 *
 * <p>TODO: Explain the metric in more detail
 *
 * <p>Technique 1: ControlFlow Metric Technique 2: Closest Successful Execution Generator
 */
public class ControlFlowDistanceMetric implements DistanceMetric {

  private final DistanceCalculationHelper distanceHelper;

  public ControlFlowDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    this.distanceHelper = pDistanceCalculationHelper;
  }

  @Override
  public List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
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
  List<CFAEdge> generateClosestSuccessfulExecution(
      ARGPath counterexample, CounterexampleInfo ceInfo) {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branchesCe = findBranches(ce);
    // auto path generator
    List<List<CFAEdge>> successfulGeneratedPath =
        pathGenerator(branchesCe, counterexample.asStatesList());

    if (successfulGeneratedPath == null) {
      return null;
    }

    List<List<CFAEdge>> replace = new ArrayList<>();
    for (List<CFAEdge> pCFAEdges : successfulGeneratedPath) {
      replace.add(distanceHelper.cleanPath(pCFAEdges));
    }
    successfulGeneratedPath = replace;
    // default location is 0 - the first node
    int a = 0;

    int spRootNodeNumber =
        successfulGeneratedPath
            .get(0)
            .get(0)
            .getPredecessor()
            .getEnteringEdge(0)
            .getPredecessor()
            .getNodeNumber();

    for (int i = 0; i < ce.size(); i++) {
      if (ce.get(i).getPredecessor().getNodeNumber() == spRootNodeNumber) {
        a = i;
        break;
      }
    }

    List<CFAEdge> finalCE = new ArrayList<>();
    for (int i = a; i < ce.size(); i++) {
      finalCE.add(ce.get(i));
    }

    // check the number of the successfulGeneratedPath
    List<CFAEdge> finalGeneratedPath;
    if (successfulGeneratedPath.size() == 1) {
      finalGeneratedPath = successfulGeneratedPath.get(0);
    } else if (successfulGeneratedPath.size() > 1) {
      finalGeneratedPath = comparePaths(branchesCe, successfulGeneratedPath);
    } else {
      // TODO: Unreachable Branch because of ANT - Compiler and Gitlab Pipeline
      finalGeneratedPath = null;
      return null;
    }

    new ExplainTool().explainDeltas(finalCE, finalGeneratedPath, ceInfo);
    // return finalGeneratedPath;
    return null;
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
  /** Get rid of safe paths with distance = 0 */
  private void eliminateZeroDistances(List<List<Event>> pDistances, List<List<CFAEdge>> safePaths) {
    for (int i = 0; i < pDistances.size(); i++) {
      if (pDistances.get(i).isEmpty()) {
        pDistances.remove(i);
        safePaths.remove(i);
      }
    }
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

    List<Event> closest =
        Collections.min(
            pDistances,
            new Comparator<List<Event>>() {
              @Override
              public int compare(List<Event> a, List<Event> b) {
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
        if (pCEvent.getNode().getNodeNumber() == eventsWaitList.get(j).getNode().getNodeNumber()) {
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
          if (!pCeAlignedEvent.getStatement().equals(safeAlignedEvents.get(j).getStatement())) {
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
   * @return the new Generated Path (maybe more than one found)
   */
  private List<List<CFAEdge>> pathGenerator(List<CFAEdge> pBranchesCE, List<ARGState> pARGStates) {
    if (pBranchesCE.isEmpty()) {
      return null;
    }
    // Get the last branch of the counterexample - the one closer to the Error -
    CFAEdge lastBranch = pBranchesCE.get(pBranchesCE.size() - 1);
    for (CFAEdge pCFAEdge : pBranchesCE) {
      if (pCFAEdge.getPredecessor().getNumLeavingEdges() == 0) {
        continue;
      } else if (pCFAEdge.getPredecessor().getNumLeavingEdges() == 2) {
        if (pCFAEdge.getPredecessor().getLeavingEdge(0).equals(pCFAEdge)) {
          lastBranch = pCFAEdge.getPredecessor().getLeavingEdge(1);
        } else {
          lastBranch = pCFAEdge.getPredecessor().getLeavingEdge(0);
        }
      } else {
        // in case of number of leaving edges >= 3, then:
        for (int i = 0; i < pCFAEdge.getPredecessor().getNumLeavingEdges(); i++) {
          if (!pCFAEdge.getPredecessor().getLeavingEdge(i).equals(pCFAEdge)) {
            lastBranch = pCFAEdge.getPredecessor().getLeavingEdge(i);
            break;
          }
        }
        break;
      }
    }

    ARGState lastBranchAsState = findEquivalentState(lastBranch, pARGStates);
    return buildNewPath(lastBranch, lastBranchAsState);
  }

  private ARGState findEquivalentState(CFAEdge pCFAEdge, List<ARGState> states) {
    ARGState finalState = null;
    for (ARGState state : states) {
      if (pCFAEdge
          .getPredecessor()
          .equals(
              AbstractStates.extractStateByType(state, AbstractStateWithLocation.class)
                  .getLocationNode())) {
        finalState = state;
        break;
      }
    }
    if (finalState == null) {
      return null;
    }
    List<ARGState> finalStatesChildren = new ArrayList<>(finalState.getChildren());
    for (int i = 0; i < finalState.getChildren().size(); i++) {
      if (!states.contains(finalStatesChildren.get(i))) {
        return finalStatesChildren.get(i);
      }
    }
    return null;
  }

  private List<List<CFAEdge>> buildNewPath(CFAEdge lastBranch, ARGState lastBranchAsState) {
    assert lastBranch.getEdgeType().equals(CFAEdgeType.AssumeEdge);
    // In Case that the last branch has more than one feasible safe paths
    // then this technique finds all of them and returns them in the form
    // of List<List<CFAEdge>>
    List<ARGPath> paths =
        distanceHelper.createPath(null, lastBranchAsState, false);
    List<List<CFAEdge>> filteredPaths = new ArrayList<>();

    for (ARGPath path : paths) {
      if (!path.getLastState().isTarget()) {
        filteredPaths.add(path.getFullPath());
      }
    }

    return filteredPaths;
  }
}
