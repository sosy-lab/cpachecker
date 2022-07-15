// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.randomWalk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public class RandomWalkState
    implements LatticeAbstractState<RandomWalkState>,
        Targetable,
        Graphable,
        Comparable<RandomWalkState> {

  private @Nullable PathFormula currentPathFormula;
  private List<CFANode> nodesOnPath;

  private List<Integer> numberOfSuccessorTaken;


  private CFAEdge edgeToTakeNext;

  private @Nullable RandomWalkState lastBranchingPoint;

  public RandomWalkState(PathFormula pPathFormula) {
    currentPathFormula = pPathFormula;
    nodesOnPath = new ArrayList<>();
    numberOfSuccessorTaken = new ArrayList<>();
  }

  public RandomWalkState(
      PathFormula pCurrentPathFormula,
      List<CFANode> pAssumeEdgesTaken,
      List<Integer> pNumberOfSuccessorTaken,
      RandomWalkState
      pLastBranchingPoint) {
    currentPathFormula = pCurrentPathFormula;
    nodesOnPath = pAssumeEdgesTaken;
    numberOfSuccessorTaken = pNumberOfSuccessorTaken;
    lastBranchingPoint = pLastBranchingPoint;
  }

  public RandomWalkState() {
    this(null, new ArrayList<>(), new ArrayList<>(), null);
  }

  private RandomWalkState copy() {
    return new RandomWalkState(
        currentPathFormula, new ArrayList<>(nodesOnPath), new ArrayList<>(numberOfSuccessorTaken), lastBranchingPoint);
  }

  public PathFormula getCurrentPathFormula() {
    return currentPathFormula;
  }

  public List<CFANode> getNodesOnPath() {
    return nodesOnPath;
  }

  public RandomWalkState getLastBranchingPoint() {
    return lastBranchingPoint;
  }

  @Override
  public boolean isTarget() {
    return false;
  }

  @Override
  public @NonNull Set<TargetInformation> getTargetInformation() throws IllegalStateException {
    return new HashSet<>();
  }

  @Override
  public String toString() {
    return numberOfSuccessorTaken.toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (!(pO instanceof RandomWalkState)) {
      return false;
    }
    RandomWalkState that = (RandomWalkState) pO;
    return Objects.equals(currentPathFormula, that.currentPathFormula)
        && Objects.equals(nodesOnPath, that.nodesOnPath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentPathFormula, nodesOnPath);
  }

  @Override
  public String toDOTLabel() {
    return this + (this.currentPathFormula == null ? "" : this.currentPathFormula.toString());
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  @Override
  public int compareTo(RandomWalkState o) {
    for (int i = 0;
        i < Math.min(this.numberOfSuccessorTaken.size(), o.numberOfSuccessorTaken.size());
        i++) {
      int compare = this.numberOfSuccessorTaken.get(i).compareTo(o.numberOfSuccessorTaken.get(i));
      if (compare != 0) {
        return compare;
      }
    }
    return this.numberOfSuccessorTaken.size() - o.numberOfSuccessorTaken.size();
  }

  @Override
  public RandomWalkState join(RandomWalkState other) throws CPAException, InterruptedException {
    if (other.isLessOrEqual(this)) {
      return this;
    } else return other;
  }

  @Override
  public boolean isLessOrEqual(RandomWalkState other) throws CPAException, InterruptedException {
    if (this.numberOfSuccessorTaken.size() == other.numberOfSuccessorTaken.size()) {
      return this.compareTo(other) == 0;
    }
    if (this.numberOfSuccessorTaken.size() < other.numberOfSuccessorTaken.size()) {
      for (int i = 0; i < this.numberOfSuccessorTaken.size(); i++) {
        if (this.numberOfSuccessorTaken.get(i) != other.numberOfSuccessorTaken.get(i)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean thisEdgeShouldBeTaken(AssumeEdge pCfaEdge, int pProbForLeftBranchForLoop,
                                       int pProbForLeftBranchForAssign) throws CPATransferException {
    CFANode predecessor = pCfaEdge.getPredecessor();
    if (nodesOnPath.size() > 1 && nodesOnPath.get(nodesOnPath.size() - 2).equals(predecessor)) {
      // There is already another edge taken
      return false;
    } else if (nodesOnPath.size() > 0
        && this.nodesOnPath.get(nodesOnPath.size() - 1).equals(predecessor)) {
      if (this.edgeToTakeNext == null) {
        // Decide which node to take next
        if (predecessor.getNumLeavingEdges() ==2){
          if (pCfaEdge.getPredecessor().isLoopStart()){
            edgeToTakeNext =
                predecessor.getLeavingEdge(
                    ThreadLocalRandom.current().nextInt( 100)< pProbForLeftBranchForLoop? 0 : 1);
          }else {
          edgeToTakeNext =
              predecessor.getLeavingEdge(
                  ThreadLocalRandom.current().nextInt( 100)< pProbForLeftBranchForAssign? 0 : 1);}
        }else{

        edgeToTakeNext =
            predecessor.getLeavingEdge(
                ThreadLocalRandom.current().nextInt(0, predecessor.getNumLeavingEdges()));
      }}
      return pCfaEdge.equals(edgeToTakeNext);
    }
    throw new CPATransferException(
        "The state does not match this edge, as it follows a different path");
  }

  public RandomWalkState takeEdge(CFAEdge pCfaEdge, LogManager pLogger) {
    RandomWalkState newState = this.copy();
    newState.nodesOnPath.add(pCfaEdge.getSuccessor());
    final CFANode predecessor = pCfaEdge.getPredecessor();
    if (pCfaEdge instanceof AssumeEdge) {
      newState.lastBranchingPoint = this;
      for (int i = 0; i < predecessor.getNumLeavingEdges(); i++) {
        if (predecessor.getLeavingEdge(i).equals(pCfaEdge)) {
          newState.numberOfSuccessorTaken.add(i);
          break;
        }
      }
    }
    newState.edgeToTakeNext = null;
    pLogger.log(Level.FINE, "Taking edge " + pCfaEdge);

    return newState;
  }

  public void setPathFormula(PathFormula pPathFormula) {
    this.currentPathFormula = pPathFormula;
  }




}
