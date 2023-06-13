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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Abstract Distance Metric This class contains a metric for program executions. The metric consists
 * of two weighted sub-distance functions: 1. predicate distance 2. number of unaligned states and
 * the final distance is "d(a,b) = (predicateWeight * predicateDistance) + (unalignedStatesWeight *
 * unalignedStates)"
 *
 * @see Explainer
 */
public class AbstractDistanceMetric implements DistanceMetric {

  private final DistanceCalculationHelper distanceHelper;

  public AbstractDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    distanceHelper = pDistanceCalculationHelper;
  }

  /** Start the metric */
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
   * @param counterexample the failed program execution
   * @param safePaths all the successful program executions found
   * @param ceStates the ARGStates of the counterexample
   * @param pathsStates the ARGStates of the successful executions
   * @return the safe program path closest to the counterexample, or <code>null</code> if no safe
   *     path exists.
   */
  private List<CFAEdge> comparePaths(
      List<CFAEdge> counterexample,
      List<List<CFAEdge>> safePaths,
      List<ARGState> ceStates,
      List<ARGPath> pathsStates) {

    // Make sure that the safe path list is not empty
    if (safePaths.isEmpty()) {
      return null;
    }
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
      int unalignedStates =
          getNumberOfUnalignedStates(alignments.getCounterexample(), betterChoice);

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
   * considered for comparison, because they didn't fulfill the criteria for the alignment
   *
   * @param alignedEdges a list with edges that are aligned
   * @param safePath the successful program execution that is being currently compared with the
   *     counterexample
   * @return the number of edges that are not aligned with another edge
   */
  private int getNumberOfUnalignedStates(List<CFAEdge> alignedEdges, List<CFAEdge> safePath) {
    return Math.abs((alignedEdges.size() - safePath.size()));
  }

  /**
   * Calculates the predicate distance between two executions, i.e. how many predicates from the
   * first program execution are not contained in the second execution and vice versa
   *
   * @param alignments the aligned edges between the two executions that are being compared
   * @param ceStates the ARGStates that are contained in the counterexample
   * @param pathsStates the ARGStates that are contained in the successful executions
   * @return how many predicates are different in these two program executions
   */
  private int calculatePredicateDistance(
      Alignment<CFAEdge> alignments, List<ARGState> ceStates, List<ARGState> pathsStates) {
    int distance = 0;
    Alignment<ARGState> stateAlignments = new Alignment<>();
    // First find the ARGStates that are mapped to the equivalent CFANode
    // and put them in a List
    for (int i = 0; i < alignments.getCounterexample().size(); i++) {
      CFANode safePathCFANode = alignments.getSafePathElement(i).getPredecessor();
      CFANode ceCFANode = alignments.getCounterexampleElement(i).getPredecessor();
      ARGState argCeState = null;
      ARGState argSpState = null;
      for (ARGState ceState : ceStates) {
        CFANode equivalentCFANode =
            AbstractStates.extractStateByType(ceState, AbstractStateWithLocation.class)
                .getLocationNode();
        if (ceCFANode.equals(equivalentCFANode)) {
          argCeState = ceState;
          break;
        }
      }
      for (ARGState pathState : pathsStates) {
        CFANode equivalentCFANode =
            AbstractStates.extractStateByType(pathState, AbstractStateWithLocation.class)
                .getLocationNode();
        if (safePathCFANode.equals(equivalentCFANode)) {
          argSpState = pathState;
          break;
        }
      }
      stateAlignments.addPair(argCeState, argSpState);
    }

    for (Pair<ARGState, ARGState> cePathStatePair : stateAlignments) {

      ARGState counterexampleState = cePathStatePair.getFirst();
      ARGState safePathState = cePathStatePair.getSecond();

      Set<BooleanFormula> predicatesCE =
          distanceHelper.splitPredicates(
              AbstractStates.extractStateByType(counterexampleState, PredicateAbstractState.class)
                  .getAbstractionFormula()
                  .asFormula());
      Set<BooleanFormula> predicatesSafePath =
          distanceHelper.splitPredicates(
              AbstractStates.extractStateByType(safePathState, PredicateAbstractState.class)
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

  /**
   * Create Alignments between Counterexample and Safe Path
   *
   * @param ce the counterexample
   * @param safePath the successful program execution
   * @return an Alignment with the aligned edges of the counterexample and the safe path
   */
  private Alignment<CFAEdge> createAlignments(List<CFAEdge> ce, List<CFAEdge> safePath) {
    Alignment<CFAEdge> alignment = new Alignment<>();
    Set<CFAEdge> alreadyAligned = new HashSet<>();

    for (CFAEdge ceEdge : ce) {
      for (CFAEdge spEdge : safePath) {
        if (ceEdge.getPredecessor().getNodeNumber() == spEdge.getPredecessor().getNodeNumber()) {
          if (ceEdge.getSuccessor().getNodeNumber() != spEdge.getSuccessor().getNodeNumber()) {
            if (!alreadyAligned.contains(spEdge)) {
              // add the two aligned Edges in the Alignments Class
              alignment.addPair(ceEdge, spEdge);
              // insert the safePath edge to the list of already aligned edges
              alreadyAligned.add(spEdge);
              break;
            }
          }
        }
      }
    }
    return alignment;
  }
}
