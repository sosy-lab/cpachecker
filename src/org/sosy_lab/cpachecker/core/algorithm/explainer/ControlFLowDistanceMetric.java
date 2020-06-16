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
import com.google.common.collect.ImmutableList;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.MultiStatistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.Model.ValueAssignment;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverException;

public class ControlFLowDistanceMetric {

  public ControlFLowDistanceMetric() {
  }

  List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample)
      throws SolverException, InterruptedException {
    /*ln("NUMBER OF SAFEPATHS IS: " + safePaths.size());
    ln("AND OUR COUNTEREXAMPLE IS");
    ln(cleanPath(counterexample));
    ln();
*/
    List<CFAEdge> ce = cleanPath(counterexample);

    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);

    // compare all the paths with their distance
    List<CFAEdge> closest_suc_path = comparePaths(branches_ce, convertPathsToEdges(safePaths));


    return closest_suc_path;


  }


  /**
   *Starts the path generator technique with the distance metric - ALIGNMENTS -
   * @param safePaths is a List with all the Safe Paths
   * @param b is the path of the counterexample
   */
  List<CFAEdge> startPathGenerator(List<ARGPath> safePaths, ARGPath counterexample)
      throws SolverException, InterruptedException {
   /* ln("NUMBER OF SAFEPATHS IS: " + safePaths.size());
    ln("AND OUR COUNTEREXAMPLE IS");
    ln(cleanPath(counterexample));
    ln();*/

    List<CFAEdge> ce = cleanPath(counterexample);

    // find all Branches in Counterexample
    List<CFAEdge> branches_ce = findBranches(ce);

    // auto path generator
    List<List<CFAEdge>> suc_path_generated = pathGenerator(branches_ce, ce);

    // check the number of the suc_path_generated
    List<CFAEdge> final_generated_path;
    if (suc_path_generated.isEmpty()) {
      return comparePaths(branches_ce, convertPathsToEdges(safePaths));
    } else if (suc_path_generated.size() == 1) {
      final_generated_path = suc_path_generated.get(0);
    } else if (suc_path_generated.size() > 1) {
      final_generated_path = comparePaths(branches_ce, suc_path_generated);
    } else {
      //ln("NO CLOSE SUCCESSFUL EXECUTION COULD BE FOUND");
      final_generated_path = null;
    }

    return final_generated_path;

  }

  /**
   * Compares all the paths and finds the one with the smallest distance from the counterexample
   * @param ce the counterexample branches
   * @param safePaths list with all the safe paths
   */
  private List<CFAEdge> comparePaths(List<CFAEdge> ce, List<List<CFAEdge>> safePaths) {
    List<List<CFAEdge>> safe_path_branches_list = new ArrayList<>();

    if (safePaths.isEmpty()) {
      //ln("THERE ARE NO SAFE PATHS");
      return null;
    }

    // find all branches in safe paths
    for (int i = 0; i < safePaths.size(); i++) {
      safe_path_branches_list.add(findBranches(cleanPath(safePaths.get(i))));
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
    List<List<Event>> distances = new ArrayList<>();
    for (int i = 0; i < sp_events.size(); i++) {
      distances.add(distance(ce_events, sp_events.get(i)));
    }

    // get rid of useless safepaths with distance = 0
    getUsefulSafePaths(distances, safePaths);

    // find the closest successful execution
    int closestSuccessfulRunIndex = closestSuccessfulRun(distances);

    if (closestSuccessfulRunIndex == -1) {
      //ln("NO CLOSEST SUCCESSFUL EXECUTION");
      return null;
    }

    return safePaths.get(closestSuccessfulRunIndex);

  }

  private List<List<Event>> getUsefulSafePaths(List<List<Event>> pDistances, List<List<CFAEdge>> safepaths) {
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
   * @param pDistances the list of distances
   * @return the distance - List of the different events - of the closest safe path
   */
  private int closestSuccessfulRun(List<List<Event>> pDistances) {
    if (pDistances.isEmpty()) {
      return -1;
    }
    // TODO: Break if the distances list is empty
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

    // TODO: beschwert sich ueber Stack

    // Convert to Stack
    /*Stack<Event> closest = new Stack<>();
    closest.addAll(pClosest);
    Stack<Event> current = new Stack<>();
    current.addAll(pCurrent);



    int closest_distance = 0;
    for (int i = 0; i < closest.size(); i++) {
      closest_distance += closest.pop().getDistanceFromTheEnd();
    }
    int current_distance = 0;
    for (int i = 0; i < current.size(); i++) {
      current_distance += current.pop().getDistanceFromTheEnd();
    }*/

    // UNCHECKED !!
    ArrayDeque<Event> closest = new ArrayDeque<>();
    closest.addAll(pClosest);
    ArrayDeque<Event> current = new ArrayDeque<>(pCurrent);

    int closest_distance = 0;
    for (int i = 0; i < closest.size(); i++) {
      closest_distance += closest.pop().getDistanceFromTheEnd();
    }
    int current_distance = 0;
    for (int i = 0; i < current.size(); i++) {
      current_distance += current.pop().getDistanceFromTheEnd();
    }

    return (current_distance <= closest_distance);

  }

  /**
   * Find the distance between all safe paths and the counterexample
   * @param pCe_events the events of the counterexample
   * @param pEvents the events of all the safe paths
   * @return the Distance := List of events that are alligned but have a different outcome
   */
  private List<Event> distance(List<Event> pCe_events, List<Event> pEvents) {
    List<Event> deltas = new ArrayList<>();
    List<Event> pCe_events2 = new ArrayList<>(pCe_events);
    List<Event> pEvents2 = new ArrayList<>(pEvents);
    List<Event> pCe_events1 = new ArrayList<>();
    List<Event> pEvents1 = new ArrayList<>();

    // MAKING ALIGNMENTS
    for (int i = 0; i < pCe_events2.size(); i++) {
      for (int j = 0; j < pEvents2.size(); j++) {
        if (pCe_events2.get(i).getNode().getNodeNumber() == pEvents2.get(j).getNode().getNodeNumber()) {
          pCe_events1.add(pCe_events2.get(i));
          pEvents1.add(pEvents2.get(j));
          // and delete them
          pCe_events2.remove(i);
          pEvents2.remove(j);
          i = i - 1;
          break;
        }
      }
    }

    for (int i = 0; i < pCe_events1.size(); i++) {
      for (int j = 0; j < pEvents1.size(); j++) {
        if (pCe_events1.get(i).getLine() == pEvents1.get(j).getLine()) {
          if (!pCe_events1.get(i).getStatement().equals(pEvents1.get(j).getStatement())) {
            deltas.add(pCe_events1.get(i));
            pCe_events1.remove(i);
            pEvents1.remove(j);
            i--;
            break;
          }
        }
      }
    }

    return deltas;
  }

  /**
   * Finds the control flow branches of the path
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
   * @param pBranches_ce the control flow of the counterexample
   */
  private List<List<CFAEdge>> pathGenerator(List<CFAEdge> pBranches_ce, List<CFAEdge> ce) {
    if (pBranches_ce.isEmpty() && ce.isEmpty()) {
      return null;
    }

    //List<CFAEdge> safePath = ce;

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
   *
   * @param ce
   * @param b_last
   * @param diff
   * @param lastState
   * @return
   */
  private List<List<CFAEdge>> buildNewPath(List<CFAEdge> ce, CFAEdge b_last, CFAEdge diff) {
    List<CFAEdge> result = new ArrayList<>();
    // start building the new path
    for (CFAEdge pCFAEdge : ce) {
      if (pCFAEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        if (b_last.equals(pCFAEdge)) {
          result.add(diff);
          break;
        }
      }
      result.add(pCFAEdge);
    }

    List<List<CFAEdge>> paths = findAllPaths(result);
    /*ln("SIZE IS");
    ln(paths.size());
    ln("AND");

    ln();
    ln();*/
    for (int i = 0; i < paths.size(); i++) {
      //ln(paths.get(i));
    }
    //ln();
    //ln();


    return paths;

  }

  /**
   *
   * @param current
   * @param lastState the last safe state
   * @return
   */
  private List<List<CFAEdge>> findAllPaths(List<CFAEdge> current) {
    List<List<CFAEdge>> paths = new ArrayList<>();
    paths.add(current);
    // is the last
    List<CFAEdge> path_now = paths.get(0);
    CFAEdge edge_now;
    boolean finish = false;

    for (int i = 0; i < paths.size(); i++) {
      finish = false;
      path_now = paths.get(i);
      edge_now = path_now.get(path_now.size() - 1);

      while(!finish) {
        if(edge_now.getSuccessor().getNumLeavingEdges() == 1) {
          path_now.add(edge_now.getSuccessor().getLeavingEdge(0));
          edge_now = edge_now.getSuccessor().getLeavingEdge(0);
        } else if (edge_now.getSuccessor().getNumLeavingEdges() == 2) {
          List<CFAEdge> extra = new ArrayList<>(path_now);
          extra.add(edge_now.getSuccessor().getLeavingEdge(1));
          paths.add(extra);
          path_now.add(edge_now.getSuccessor().getLeavingEdge(0));
          edge_now = edge_now.getSuccessor().getLeavingEdge(0);
        } else if (edge_now.getSuccessor().getNumLeavingEdges() == 0) {
          finish = true;
        }
      }
    }

    //ln("SIZE BEFORE: " + paths.size());
    List<List<CFAEdge>> final_list = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      if (!isTarget(paths.get(i))) {
        final_list.add(paths.get(i));
      }
    }
    //ln("SIZE AFTER: " + paths.size());
    return final_list;
  }

  /**
   * Checks a List<CFAEdge> for a Target state
   * @param list
   * @return
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
   * @param path
   * @return the new - clean of useless nodes - Path
   */
  private List<CFAEdge> cleanPath(ARGPath path) {
    List<CFAEdge> flow = path.getFullPath();
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        //String[] code = flow.get(i).getCode().split("\\s*[()]\\s*");
        /*if (code.length > 0) {
          if (code[0].equals("__VERIFIER_assert")) {
            clean_flow.add(flow.get(i));
            return clean_flow;
          }
        }*/
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
  }

  /**
   * Filter the path to stop at the __Verifier__assert Node
   * @param path
   * @return the new - clean of useless nodes - Path
   */
  private List<CFAEdge> cleanPath(List<CFAEdge> path) {
    List<CFAEdge> flow = path;
    List<CFAEdge> clean_flow = new ArrayList<>();

    for (int i = 0; i < flow.size(); i++) {
      if (flow.get(i).getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
        //String[] code = flow.get(i).getCode().split("\\s*[()]\\s*");
        /*if (code.length > 0) {
          if (code[0].equals("__VERIFIER_assert")) {
            clean_flow.add(flow.get(i));
            return clean_flow;
          }
        }*/
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
  }

  /**
   * Convert a list of ARGPaths to a List<List<CFAEdges>>
   * @param paths
   * @return
   */
  private List<List<CFAEdge>> convertPathsToEdges(List<ARGPath> paths) {
    List<List<CFAEdge>> result = new ArrayList<>();
    for (int i = 0; i < paths.size(); i++) {
      result.add(paths.get(i).getFullPath());
    }
    return result;
  }
}
