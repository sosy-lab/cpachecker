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

import com.google.common.base.Splitter;
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

public class ControlFLowDistanceMetric {

  private DistanceCalculationHelper distanceHelper;

  public ControlFLowDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    this.distanceHelper = pDistanceCalculationHelper;
  }

  List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample)
      throws SolverException, InterruptedException {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);
    // compare all the paths with their distance
    List<CFAEdge> closest_suc_path = comparePaths(branches_ce, distanceHelper.convertPathsToEdges(safePaths));
    return closest_suc_path;
  }


  /**
   * Starts the path generator technique with the distance metric - ALIGNMENTS -
   *
   * @param safePaths is a List with all the Safe Paths
   * @param b         is the path of the counterexample
   */
  List<CFAEdge> startPathGenerator(List<ARGPath> safePaths, ARGPath counterexample)
      throws SolverException, InterruptedException {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);
    // auto path generator
    List<List<CFAEdge>> suc_path_generated = pathGenerator(branches_ce, ce);

    // check the number of the suc_path_generated
    List<CFAEdge> final_generated_path = null;
    if (suc_path_generated == null) {
      final_generated_path = null;
    } else if (suc_path_generated.isEmpty()) {
      return comparePaths(branches_ce, distanceHelper.convertPathsToEdges(safePaths));
    } else if (suc_path_generated.size() == 1) {
      final_generated_path = suc_path_generated.get(0);
    } else if (suc_path_generated.size() > 1) {
      final_generated_path = comparePaths(branches_ce, suc_path_generated);
    }

    return final_generated_path;

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
    List<Event> ce_events = new ArrayList<>();
    for (int i = 0; i < ce.size(); i++) {
      ce_events.add(new Event(ce.get(i), ce));
    }

    // create Events for all the safe paths
    List<List<Event>> sp_events = new ArrayList<>();
    for (int i = 0; i < safe_path_branches_list.size(); i++) {
      List<Event> events = new ArrayList<>();
      for (int j = 0; j < safe_path_branches_list.get(i).size(); j++) {
        events.add(new Event(safe_path_branches_list.get(i).get(j), safePaths.get(i)));
      }
      sp_events.add(events);
    }

    // compute the distances
    // The number of events equals here the number of safe paths
    List<List<Event>> distances = new ArrayList<>();
    for (List<Event> events : sp_events) {
      distances.add(distance(ce_events, events));
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

  private List<List<Event>> getUsefulSafePaths(
      List<List<Event>> pDistances,
      List<List<CFAEdge>> safepaths) {
    for (int i = 0; i < pDistances.size(); i++) {
      if (pDistances.get(i).size() == 0) {
        pDistances.remove(i);
        safepaths.remove(i);
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

    // Stream ?
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
   * @param pCe_events the events of the counterexample
   * @param pEvents    the events of all the safe paths
   * @return the Distance := List of events that are alligned but have a different outcome
   */
  private List<Event> distance(List<Event> pCe_events, List<Event> pEvents) {
    List<Event> deltas = new ArrayList<>();
    // wait list for the events to be aligned
    List<Event> eventsWaitList = new ArrayList<>(pEvents);
    List<Event> pCe_events1 = new ArrayList<>();
    List<Event> pEvents1 = new ArrayList<>();

    // TODO: LinkedList guengstiger ?
    //  This alignment is slightly different from the other one
    // 1 - Set mit allen aligned Events -> set.containes ?
    // Oder Event.aligned boolean ?
    // MAKING ALIGNMENTS
    for (int i = 0; i < pCe_events.size(); i++) {
      for (int j = 0; j < eventsWaitList.size(); j++) {
        if (pCe_events.get(i).getNode().getNodeNumber() == eventsWaitList.get(j).getNode()
            .getNodeNumber()) {
          pCe_events1.add(pCe_events.get(i));
          pEvents1.add(eventsWaitList.get(j));
          // remove the aligned events from the waitlist
          eventsWaitList.remove(j);
          break;
        }
      }
    }

    for (int i = 0; i < pCe_events1.size(); i++) {
      for (int j = 0; j < pEvents1.size(); j++) {
        if (pCe_events1.get(i).getLine() == pEvents1.get(j).getLine()) {
          if (!pCe_events1.get(i).getStatement().equals(pEvents1.get(j).getStatement())) {
            deltas.add(pCe_events1.get(i));
            pEvents1.remove(j);
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
   * @param pBranches_ce the control flow of the counterexample
   */
  private List<List<CFAEdge>> pathGenerator(List<CFAEdge> pBranches_ce, List<CFAEdge> ce) {
    if (pBranches_ce.isEmpty() || ce.isEmpty()) {
      return null;
    }

    // Get the last branch of the counterexample - the one closer to the Error -
    CFAEdge b_last = pBranches_ce.get(pBranches_ce.size() - 1);
    // change the flow of b_last
    int edges = b_last.getPredecessor().getNumLeavingEdges();

    if (edges != 2) {
      return null;
    }

    CFAEdge diff;
    if (b_last.equals(b_last.getPredecessor().getLeavingEdge(0))) {
      diff = b_last.getPredecessor().getLeavingEdge(1);
    } else {
      diff = b_last.getPredecessor().getLeavingEdge(0);
    }

    return buildNewPath(ce, b_last, diff);
  }


  /**
   * Building the closest to the target successful safe path
   * @param ce List of the CFAEdges of the counterexample
   * @param b_last the last branch of the Counterexample
   * @param diff the leaving edge of b_last that is the one that we want to explore
   * @return all the feasible safe paths that were built
   */
  private List<List<CFAEdge>> buildNewPath(List<CFAEdge> ce, CFAEdge b_last, CFAEdge diff) {
    assert b_last.getEdgeType().equals(CFAEdgeType.AssumeEdge);
    List<CFAEdge> result = new ArrayList<>();
    // start building the new path
    for (CFAEdge pCFAEdge : ce) {
      if (b_last.equals(pCFAEdge)) {
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
   * @param lastState the last safe state
   */
  private List<List<CFAEdge>> findAllPaths(List<CFAEdge> current) {
    List<List<CFAEdge>> paths = new ArrayList<>();
    paths.add(current);
    // is the last
    List<CFAEdge> currentPath = paths.get(0);
    CFAEdge edge_now;
    boolean finish = false;

    for (int i = 0; i < paths.size(); i++) {
      finish = false;
      currentPath = paths.get(i);
      edge_now = currentPath.get(currentPath.size() - 1);

      Set<CFANode> visited = new HashSet<>();
      Deque<CFAEdge> waitList = new ArrayDeque<>();
      waitList.add(edge_now);

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

    List<List<CFAEdge>> final_list = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      if (!isTarget(paths.get(i))) {
        final_list.add(paths.get(i));
      }
    }
    return final_list;
  }

  /**
   * Checks a List<CFAEdge> for a Target state
   */
  private boolean isTarget(List<CFAEdge> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).getDescription().equals("Label: ERROR")) {
        return true;
      }
    }
    return false;
  }


  /**
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   *//*
  private List<CFAEdge> cleanPath(ARGPath path) {
    List<CFAEdge> flow = path.getFullPath();
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (code.size() > 0) {
          if (code.get(0).equals("__VERIFIER_assert")) {
            clean_flow.add(flow.get(i));
            return clean_flow;
          }
        }
      }
      clean_flow.add(flow.get(i));
    }
    return clean_flow;
  }*/

  /**
   * Filter the path to stop at the __Verifier__assert Node
   *
   * @return the new - clean of useless nodes - Path
   *//*
  private List<CFAEdge> cleanPath(List<CFAEdge> path) {
    List<CFAEdge> flow = path;
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        List<String> code = Splitter.onPattern("\\s*[()]\\s*").splitToList(flow.get(i).getCode());
        if (code.size() > 0) {
          if (code.get(0).equals("__VERIFIER_assert")) {
            clean_flow.add(flow.get(i));
            return clean_flow;
          }
        }
      }
      clean_flow.add(flow.get(i));
    }
    return clean_flow;
  }*/

  /**
   * Convert a list of ARGPaths to a List<List<CFAEdges>>
   *//*
  private List<List<CFAEdge>> convertPathsToEdges(List<ARGPath> paths) {
    List<List<CFAEdge>> result = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      result.add(paths.get(i).getFullPath());
    }
    return result;
  }*/
}
