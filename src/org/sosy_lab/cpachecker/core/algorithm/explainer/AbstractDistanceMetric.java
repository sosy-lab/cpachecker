// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Abstract Distance Metric
 *
 * @see Explainer
 */
public class AbstractDistanceMetric implements DistanceMetric {

  private final DistanceCalculationHelper distanceHelper;

  public AbstractDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    this.distanceHelper = pDistanceCalculationHelper;
  }

  /** Start Method */
  @Override
  public List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    Preconditions.checkNotNull(distanceHelper);
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    List<List<CFAEdge>> paths = new ArrayList<>();
    // clean the paths from useless Statements

    for (ARGPath p : safePaths) {
      paths.add(distanceHelper.cleanPath(p));
    }

    return comparePaths(ce, paths, counterexample.asStatesList(), safePaths);
  }

  /**
   * @param counterexample := Counterexample
   * @param safePaths := Safe paths
   */
  private List<CFAEdge> comparePaths(
      List<CFAEdge> counterexample,
      List<List<CFAEdge>> safePaths,
      List<ARGState> ceStates,
      List<ARGPath> pathsStates) {

    // Make sure that the safe path list is not empty
    assert !safePaths.isEmpty();

    List<Integer> distances = new ArrayList<>();
    // Distance := predicateWeight * predicateDistance + unalignedStatesWeight * unalignedStates
    int predicateWeight = 1;
    int unalignedStatesWeight = 2;

    for (int i = 0; i < safePaths.size(); i++) {
      // Step 1: CREATE ALIGNMENTS
      Alignment<CFAEdge> alignments = createAlignments(counterexample, safePaths.get(i));

      // Step 2: Get Differences between Predicates
      int predicateDistance =
          calculatePredicateDistance(alignments, ceStates, pathsStates.get(i).asStatesList());

      // Step 3: Get Differences between Actions
      List<CFAEdge> betterChoice =
          (safePaths.get(i).size() > counterexample.size()) ? safePaths.get(i) : counterexample;
      int unalignedStates = getNumberOfUnalignedStates(alignments, betterChoice);

      // calculate the distance
      int d = predicateWeight * predicateDistance + unalignedStatesWeight * unalignedStates;
      distances.add(d);
    }

    // eliminate zero distances
    List<Integer> finalDistances = new ArrayList<>();
    for (int i = 0; i < distances.size(); i++) {
      int distance = distances.get(i);
      if (distance == 0) {
        safePaths.remove(i);
      } else {
        finalDistances.add(distance);
      }
    }
    distances = finalDistances;

    // Make sure that distances is not empty
    if (distances.isEmpty()) {
      return null;
    }

    int minimumDistance = Collections.min(distances);
    int index = distances.indexOf(minimumDistance);

    return safePaths.get(index);
  }

  /**
   * Calculate the Number of Unaligned States. Unaligned states are the Nodes that haven't been
   * considered for comparison, because they didn't fulfill the official criteria of the aligned
   * states.
   */
  private int getNumberOfUnalignedStates(Alignment<CFAEdge> alignments, List<CFAEdge> safePath) {
    return Math.abs((alignments.getCounterexample().size() - safePath.size()));
  }

  /** Calculates the distance of the predicates */
  private int calculatePredicateDistance(
      Alignment<CFAEdge> alignments, List<ARGState> ceStates, List<ARGState> pathsStates) {
    int distance = 0;
    Alignment<ARGState> stateAlignments = new Alignment<>();
    // First find the ARGStates that are mapped to the equivalent CFANode
    // and put them in a List

    for (int i = 0; i < alignments.getCounterexample().size(); i++) {
      ARGState argCeState = null;
      ARGState argSpState = null;
      for (ARGState ceState : ceStates) {
        if (alignments
            .getCounterexampleElement(i)
            .getPredecessor()
            .equals(
                AbstractStates.extractStateByType(ceState, AbstractStateWithLocation.class)
                    .getLocationNode())) {
          argCeState = ceState;
          break;
        }
      }
      for (ARGState pathState : pathsStates) {
        if (alignments
            .getSafePathElement(i)
            .getPredecessor()
            .equals(
                AbstractStates.extractStateByType(pathState, AbstractStateWithLocation.class)
                    .getLocationNode())) {
          argSpState = pathState;
          break;
        }
      }
      stateAlignments.addPair(argCeState, argSpState);
    }

    for (int j = 0; j < stateAlignments.getCounterexample().size(); j++) {
      Set<BooleanFormula> predicatesCE =
          distanceHelper.splitPredicates(
              AbstractStates.extractStateByType(
                      stateAlignments.getCounterexampleElement(j), PredicateAbstractState.class)
                  .getAbstractionFormula()
                  .asFormula());
      Set<BooleanFormula> predicatesSafePath =
          distanceHelper.splitPredicates(
              AbstractStates.extractStateByType(
                      stateAlignments.getSafePathElement(j), PredicateAbstractState.class)
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

  /** Create Alignments between Counterexample and Safe Path */
  private Alignment<CFAEdge> createAlignments(List<CFAEdge> ce, List<CFAEdge> safePath) {
    List<CFAEdge> ceCopy1 = new ArrayList<>(ce);
    List<CFAEdge> safePath1 = new ArrayList<>(safePath);
    Alignment<CFAEdge> alignment = new Alignment<>();

    // MAKING ALIGNMENTS
    for (CFAEdge ceEdge : ceCopy1) {
      for (CFAEdge spEdge : safePath1) {
        if (ceEdge.getPredecessor().getNodeNumber() == spEdge.getPredecessor().getNodeNumber()) {
          if (ceEdge.getSuccessor().getNodeNumber() != spEdge.getSuccessor().getNodeNumber()) {
            // add the two aligned Edges in the Alignment Class
            alignment.addPair(ceEdge, spEdge);
            // remove the aligned Edge
            safePath1.remove(spEdge);
            break;
          }
        }
      }
    }

    return alignment;
  }
}

/**
 * Class Alignment is used for making alignments between two Elements
 *
 * @param <T> T is here either a CFAEdge or a ARGState
 */
class Alignment<T> {

  private List<T> counterexample = new ArrayList<T>();
  private List<T> safePath = new ArrayList<T>();

  public void addPair(T counterexampleElement, T safePathElement) {
    counterexample.add(counterexampleElement);
    safePath.add(safePathElement);
  }

  public T getSafePathElement(int i) {
    return safePath.get(i);
  }

  public T getCounterexampleElement(int i) {
    return counterexample.get(i);
  }

  public List<T> getCounterexample() {
    return counterexample;
  }

  public List<T> getSafePath() {
    return safePath;
  }
}
