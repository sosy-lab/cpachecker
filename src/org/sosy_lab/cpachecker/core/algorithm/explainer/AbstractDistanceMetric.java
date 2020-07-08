// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AbstractDistanceMetric {

  private DistanceCalculationHelper distanceHelper;

  public AbstractDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    this.distanceHelper = pDistanceCalculationHelper;
  }

  /** Start Method */
  public List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    assert distanceHelper != null;
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    List<List<CFAEdge>> paths = new ArrayList<>();
    // clean the paths from useless Statements
    for (int i = 0; i < safePaths.size(); i++) {
      paths.add(distanceHelper.cleanPath(safePaths.get(i)));
    }

    List<CFAEdge> closestSuccessfulExecution =
        comparePaths(ce, paths, counterexample.asStatesList(), safePaths);

    return closestSuccessfulExecution;
  }

  /**
   * @param ce := Counterexample
   * @param sp := Safe paths
   */
  private List<CFAEdge> comparePaths(
      List<CFAEdge> ce,
      List<List<CFAEdge>> sp,
      List<ARGState> ce_states,
      List<ARGPath> pathsStates) {

    // Make sure that the safe path list is not empty
    assert !sp.isEmpty();

    List<Integer> distances = new ArrayList<>();
    // Distance := predicateWeight * predicateDistance + unalignedStatesWeight * unalignedStates
    int predicateWeight = 1;
    int unalignedStatesWeight = 2;

    // "sp" here stands for "Safe Path"
    for (int i = 0; i < sp.size(); i++) {
      // Step 1: CREATE ALIGNMENTS
      List<List<CFAEdge>> alignments = createAlignments(ce, sp.get(i));

      // Step 2: Get Differences between Predicates
      int predicateDistance =
          calculatePredicateDistance(alignments, ce_states, pathsStates.get(i).asStatesList());

      // Step 3: Get Differences between Actions
      List<CFAEdge> better_choice = (sp.get(i).size() > ce.size()) ? sp.get(i) : ce;
      int unalignedStates = getNumberOfUnalignedStates(alignments, better_choice);

      // calculate the distance
      int d = predicateWeight * predicateDistance + unalignedStatesWeight * unalignedStates;
      distances.add(d);
    }

    // clean distance = 0
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) == 0) {
        distances.remove(i);
        sp.remove(i);
      }
    }

    // Make sure that distances is not empty
    assert !distances.isEmpty();

    int index = 0;
    int minimumDistance = distances.get(0);
    for (int i = 0; i < distances.size(); i++) {
      if (distances.get(i) <= minimumDistance) {
        index = i;
        minimumDistance = distances.get(i);
      }
    }

    return sp.get(index);
  }

  /** Calculate the Number of Unaligned States */
  private int getNumberOfUnalignedStates(List<List<CFAEdge>> alignments, List<CFAEdge> safePath) {
    return Math.abs((alignments.get(0).size() - safePath.size()));
  }

  /** Calculates the distance of the predicates */
  private int calculatePredicateDistance(
      List<List<CFAEdge>> alignments, List<ARGState> ce_states, List<ARGState> pathsStates) {
    assert alignments.get(0).size() == alignments.get(1).size();
    int distance = 0;
    List<List<ARGState>> stateAlignments = new ArrayList<>();
    stateAlignments.add(new ArrayList<>());
    stateAlignments.add(new ArrayList<>());

    // First find the ARGStates that are mapped to the equivalent CFANode
    // and put them in a List

    // COMPUTATIONS FOR THE COUNTEREXAMPLE
    for (CFAEdge alignedEdge : alignments.get(0)) {
      for (ARGState ceState : ce_states) {
        if (alignedEdge
            .getPredecessor()
            .equals(
                AbstractStates.extractStateByType(ceState, AbstractStateWithLocation.class)
                    .getLocationNode())) {
          stateAlignments.get(0).add(ceState);
          break;
        }
      }
    }

    // COMPUTATIONS FOR THE SAFE PATH
    for (CFAEdge alignedEdge : alignments.get(1)) {
      for (ARGState pathState : pathsStates) {
        if (alignedEdge
            .getPredecessor()
            .equals(
                AbstractStates.extractStateByType(pathState, AbstractStateWithLocation.class)
                    .getLocationNode())) {
          stateAlignments.get(1).add(pathState);
          break;
        }
      }
    }

    assert stateAlignments.get(0).size() == stateAlignments.get(1).size();
    assert stateAlignments.get(0).size() == alignments.get(0).size();
    assert stateAlignments.get(1).size() == alignments.get(1).size();

    // THE alignments List has only 2 Lists with the same size
    for (int j = 0; j < stateAlignments.get(0).size(); j++) {
      Set<BooleanFormula> predicatesCE =
          distanceHelper.splitPredicates(
              AbstractStates.extractStateByType(
                      stateAlignments.get(0).get(j), PredicateAbstractState.class)
                  .getAbstractionFormula()
                  .asFormula());
      Set<BooleanFormula> predicatesSafePath =
          distanceHelper.splitPredicates(
              AbstractStates.extractStateByType(
                      stateAlignments.get(1).get(j), PredicateAbstractState.class)
                  .getAbstractionFormula()
                  .asFormula());

      for (BooleanFormula predicate : predicatesCE) {
        if (!predicatesSafePath.contains(predicate)) {
          distance++;
        }
      }
      for (BooleanFormula predicate : predicatesSafePath) {
        if (!predicatesCE.contains(predicate)) {
          distance++;
        }
      }
    }
    return distance;
  }

  /** Create Alignments between CE and Safe Path */
  private List<List<CFAEdge>> createAlignments(List<CFAEdge> ce, List<CFAEdge> safePath) {
    List<CFAEdge> ce_1 = new ArrayList<>(ce);
    List<CFAEdge> safePath_1 = new ArrayList<>(safePath);
    List<CFAEdge> ce_2 = new ArrayList<>();
    List<CFAEdge> safePath_2 = new ArrayList<>();

    // MAKING ALIGNMENTS
    for (int i = 0; i < ce_1.size(); i++) {
      for (int j = 0; j < safePath_1.size(); j++) {
        if (ce_1.get(i).getPredecessor().getNodeNumber()
            == safePath_1.get(j).getPredecessor().getNodeNumber()) {
          if (ce_1.get(i).getSuccessor().getNodeNumber()
              != safePath_1.get(j).getSuccessor().getNodeNumber()) {
            ce_2.add(ce_1.get(i));
            safePath_2.add(safePath_1.get(j));
            // remove the aligned Node
            safePath_1.remove(j);
            break;
          }
        }
      }
    }

    List<List<CFAEdge>> result = new ArrayList<>();
    result.add(ce_2);
    result.add(safePath_2);
    return result;
  }
}
