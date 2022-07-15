// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import com.google.common.collect.ForwardingList;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This Class contains one metric for program executions and an automated Path Generation technique
 * which produces the closest to the error successful execution.
 *
 * <p>1. The Metric depends on the control flow of the execution. It tracks down the control flow
 * branches of the successful executions and it compares the branches of every successful run with
 * the branches of the counterexample trying to find the successful run with the minimum different
 * control-flow branches from the counterexample.
 *
 * <p>2. The automated path generation technique starts from the node where the error is contained
 * and goes backwards the failed program execution, trying to find all the control-flow branches on
 * the program execution. Then, starting from the branch that is closer to the error, it changes its
 * flow and expands its new direction in order to find if it leads to a successful exit of the
 * program
 *
 * <p>Important Information: This class finds all the possible abstract program paths as computed by
 * the used CPA and does not consider the semantics of the program.
 */
public class ControlFlowDistanceMetric implements DistanceMetric {

  private final DistanceCalculationHelper distanceHelper;

  public ControlFlowDistanceMetric(DistanceCalculationHelper pDistanceCalculationHelper) {
    distanceHelper = pDistanceCalculationHelper;
  }

  @Override
  public List<CFAEdge> startDistanceMetric(List<ARGPath> safePaths, ARGPath counterexample) {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> counterexampleBranches = findBranches(ce);
    // compare all the paths with their distance
    return comparePaths(counterexampleBranches, distanceHelper.convertPathsToEdges(safePaths));
  }

  /**
   * Starts the path generator technique using the Control Flow metric
   *
   * @param counterexample the failed program execution
   * @param ceInfo the CounterexampleInfo needed for the ExplainTool
   */
  void generateClosestSuccessfulExecution(ARGPath counterexample, CounterexampleInfo ceInfo) {
    List<CFAEdge> ce = distanceHelper.cleanPath(counterexample);
    // find all Branches in Counterexample
    List<CFAEdge> branchesCe = findBranches(ce);
    // auto path generator
    List<List<CFAEdge>> successfulGeneratedPath =
        pathGenerator(branchesCe, counterexample.asStatesList());

    if (successfulGeneratedPath == null) {
      return;
    }

    List<List<CFAEdge>> replace = new ArrayList<>();
    for (List<CFAEdge> pCFAEdges : successfulGeneratedPath) {
      replace.add(distanceHelper.cleanPath(pCFAEdges));
    }
    successfulGeneratedPath = replace;
    successfulGeneratedPath.removeIf(c -> c.isEmpty());

    if (successfulGeneratedPath.stream().allMatch(c -> c.isEmpty())) {
      return;
    }

    // default location is 0 - the first node
    int locationOfLastChangedNode = 0;

    List<CFAEdge> firstGeneratedPath = successfulGeneratedPath.get(0);
    CFANode firstNodeOfSafePath = firstGeneratedPath.get(0).getPredecessor();

    int spRootNodeNumber = firstNodeOfSafePath.getEnteringEdge(0).getPredecessor().getNodeNumber();

    for (int i = 0; i < ce.size(); i++) {
      if (ce.get(i).getPredecessor().getNodeNumber() == spRootNodeNumber) {
        locationOfLastChangedNode = i;
        break;
      }
    }

    // we hold on to the Edges of the counterexample from the level
    // of the different-evaluated branch
    List<CFAEdge> finalCE = new ArrayList<>();
    for (int i = locationOfLastChangedNode; i < ce.size(); i++) {
      finalCE.add(ce.get(i));
    }

    // check the number of the successfulGeneratedPath
    List<CFAEdge> finalGeneratedPath;
    if (successfulGeneratedPath.size() == 1) {
      finalGeneratedPath = successfulGeneratedPath.get(0);
    } else if (successfulGeneratedPath.size() > 1) {
      finalGeneratedPath = comparePaths(branchesCe, successfulGeneratedPath);
      if (finalGeneratedPath == null) {
        // Case the control flow distance metric couldn't find any differences
        // because the paths are too short
        finalGeneratedPath = successfulGeneratedPath.get(0);
      }
    } else {
      return;
    }

    // Present the differences to the developer
    new ExplainTool().explainDeltas(finalCE, finalGeneratedPath, ceInfo);
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
      distances.add(calculateDistance(ceEvents, events));
    }

    // get rid of useless safe paths with distance = 0
    PathDistanceList<CFAEdge, Event> safePathsDistancesPair =
        new PathDistanceList<>(safePaths, distances);

    if (safePathsDistancesPair.isEmpty()) {
      return null;
    }
    return findClosestSuccessfulRun(safePathsDistancesPair);
  }

  /**
   * Finds the closest successful execution to the counterexample
   *
   * @param pDistancePairs the list of path-distance pairs
   * @return the closest successful run
   */
  private List<CFAEdge> findClosestSuccessfulRun(PathDistanceList<CFAEdge, Event> pDistancePairs) {
    PathDistancePair<CFAEdge, Event> closest =
        Collections.min(
            pDistancePairs,
            new Comparator<PathDistancePair<CFAEdge, Event>>() {
              @Override
              public int compare(
                  PathDistancePair<CFAEdge, Event> a, PathDistancePair<CFAEdge, Event> b) {
                int aSum =
                    a.getDistances().stream()
                        .map(e -> e.getDistanceFromTheEnd())
                        .reduce(0, Integer::sum);
                int bSum =
                    b.getDistances().stream()
                        .map(e -> e.getDistanceFromTheEnd())
                        .reduce(0, Integer::sum);
                return Integer.compare(aSum, bSum);
              }
            });

    return closest.getPath();
  }

  /**
   * Find the distance between all safe paths and the counterexample
   *
   * @param pCEvents the events of the counterexample
   * @param pSafeEvents the events of all the safe paths
   * @return the Distance := List of events that are aligned but have a different outcome
   */
  private List<Event> calculateDistance(List<Event> pCEvents, List<Event> pSafeEvents) {
    Alignment<Event> alignments = createAlignments(pCEvents, pSafeEvents);

    List<Event> deltas = new ArrayList<>();
    // Find Differences - Distance Calculation in the form of a List of Events
    for (int i = 0; i < alignments.getCounterexample().size(); i++) {
      if (alignments.getCounterexampleElement(i).getLine()
          == alignments.getSafePathElement(i).getLine()) {
        if (!alignments
            .getCounterexampleElement(i)
            .getStatement()
            .equals(alignments.getSafePathElement(i).getStatement())) {
          deltas.add(alignments.getCounterexampleElement(i));
          break;
        }
      }
    }
    return deltas;
  }

  /**
   * Create alignments between the control-flow branches of the two executions
   *
   * @param pCEvents control-flow branches of the counterexample
   * @param pSafeEvents control-flow branches of the successful execution
   * @return the aligned Events
   */
  private Alignment<Event> createAlignments(List<Event> pCEvents, List<Event> pSafeEvents) {
    Set<Event> alreadyAligned = new HashSet<>();
    Alignment<Event> alignments = new Alignment<>();

    for (Event pCEvent : pCEvents) {
      for (Event pSafeEvent : pSafeEvents) {
        if (pCEvent.getNode().getNodeNumber() == pSafeEvent.getNode().getNodeNumber()) {
          if (!alreadyAligned.contains(pSafeEvent)) {
            alignments.addPair(pCEvent, pSafeEvent);
            // insert the safePath edge to the list of already aligned events
            alreadyAligned.add(pSafeEvent);
            break;
          }
        }
      }
    }
    return alignments;
  }

  /**
   * Finds the control flow branches of the path
   *
   * @param pCe list of CFAEdges that belong to the counterexample
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
   * @param pARGStates the ARGStates
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
        // in case of number of leaving edges >= 3
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

  /**
   * Searches in the List of states of the safe path to find the ARGState that is equivalent to the
   * predecessor Node of the CFAEdge
   *
   * @param pCFAEdge the CFAEdge containing the predecessor that we want to find its paired ARGState
   * @param states the original ARGStates of the successful execution
   * @return the equivalent ARGState of the predecessor CFANode
   */
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

  /**
   * This method builds all possible paths that start from a given a root (here the root is
   * lastBranchAsState)
   *
   * @param lastBranch the control-flow branch that has to be expanded and search its children if
   *     they lead to possible successful executions
   * @param lastBranchAsState the equivalent ARGState of the last branch
   * @return a list with all produced executions starting from the lastBranch
   */
  private List<List<CFAEdge>> buildNewPath(CFAEdge lastBranch, ARGState lastBranchAsState) {
    assert lastBranch.getEdgeType().equals(CFAEdgeType.AssumeEdge);
    // In Case that the last branch has more than one feasible safe paths
    // then this technique finds all of them and returns them in the form
    // of List<List<CFAEdge>>
    List<ARGPath> paths =
        distanceHelper.generateAllSuccessfulExecutions(null, lastBranchAsState, false);
    List<List<CFAEdge>> filteredPaths = new ArrayList<>();

    for (ARGPath path : paths) {
      if (!path.getLastState().isTarget()) {
        filteredPaths.add(path.getFullPath());
      }
    }
    return filteredPaths;
  }
}

class PathDistancePair<T, Y> {

  private final Pair<List<T>, List<Y>> pair;

  public PathDistancePair(List<T> pExecution, List<Y> pDistance) {
    pair = Pair.of(pExecution, pDistance);
  }

  public List<T> getPath() {
    return pair.getFirst();
  }

  public List<Y> getDistances() {
    return pair.getSecond();
  }
}

/**
 * This class is responsible for storing program executions and their distances
 *
 * @param <T> Could be CFAEdge or ARGState or CFANode
 * @param <Y> The distance: Integer or Event
 * @see PathDistancePair
 */
class PathDistanceList<T, Y> extends ForwardingList<PathDistancePair<T, Y>> {

  private final ImmutableList<PathDistancePair<T, Y>> delegate;

  PathDistanceList(List<List<T>> programExecution, List<List<Y>> listOfDistances) {
    assert programExecution.size() == listOfDistances.size();
    delegate =
        Streams.zip(programExecution.stream(), listOfDistances.stream(), PathDistancePair::new)
            .filter(p -> p.getDistances().isEmpty()) // get rid of safe paths with distance = 0
            .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected List<PathDistancePair<T, Y>> delegate() {
    return delegate;
  }
}
