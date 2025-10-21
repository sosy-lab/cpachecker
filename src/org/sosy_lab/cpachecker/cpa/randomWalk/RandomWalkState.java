// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.randomWalk;

import com.google.common.base.Preconditions;
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
  private final List<CFANode> nodesOnPath;

  private final List<Integer> numberOfSuccessorTaken;

  private @Nullable CFAEdge edgeToTakeNext;

  private @Nullable RandomWalkState lastBranchingPoint;

  private RandomWalkState(
      PathFormula pCurrentPathFormula,
      List<CFANode> pAssumeEdgesTaken,
      List<Integer> pNumberOfSuccessorTaken,
      RandomWalkState pLastBranchingPoint) {
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
        currentPathFormula,
        new ArrayList<>(nodesOnPath),
        new ArrayList<>(numberOfSuccessorTaken),
        lastBranchingPoint);
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
    // exclude lastBranchingPoint from check
    // because by construction all of its information is stored in the other objects
    if (this == pO) {
      return true;
    }

    return pO instanceof RandomWalkState that
        && Objects.equals(currentPathFormula, that.currentPathFormula)
        && nodesOnPath.equals(that.nodesOnPath)
        && numberOfSuccessorTaken.equals(that.numberOfSuccessorTaken)
        && Objects.equals(edgeToTakeNext, that.edgeToTakeNext);
  }

  @Override
  public int hashCode() {
    // exclude lastBranchingPoint from check
    // because by construction all of its information is stored in the other objects
    return Objects.hash(currentPathFormula, nodesOnPath, numberOfSuccessorTaken, edgeToTakeNext);
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
    // compares states based on the taken assume edges
    // as long as callstack is properly handled and
    // the program's control-flow only splits due to AssumeEdges otherwise
    // this should be consistent with equals due to construction of RandomWalkState
    for (int i = 0;
        i < Math.min(this.numberOfSuccessorTaken.size(), o.numberOfSuccessorTaken.size());
        i++) {
      int compare = this.numberOfSuccessorTaken.get(i).compareTo(o.numberOfSuccessorTaken.get(i));
      if (compare != 0) {
        return compare;
      }
    }
    if (this.numberOfSuccessorTaken.size() == o.numberOfSuccessorTaken.size()) {
      if (this.edgeToTakeNext == null) {
        if (o.edgeToTakeNext == null) {
          return 0;
        } else {
          return 1;
        }
      } else {
        if (o.edgeToTakeNext == null) {
          return 1;
        } else {
          if (this.edgeToTakeNext.equals(o.edgeToTakeNext)) {
            return 0;
          } else {
            Preconditions.checkState(
                this.edgeToTakeNext.getPredecessor().equals(o.edgeToTakeNext.getPredecessor()));
            for (CFAEdge succ : edgeToTakeNext.getPredecessor().getAllLeavingEdges()) {
              if (succ.equals(this.edgeToTakeNext)) {
                return -1;
              }
              if (succ.equals(o.edgeToTakeNext)) {
                return 1;
              }
            }
          }
        }
      }
    }
    return Integer.compare(this.numberOfSuccessorTaken.size(), o.numberOfSuccessorTaken.size());
  }

  @Override
  public RandomWalkState join(RandomWalkState other) throws CPAException, InterruptedException {
    if (other.isLessOrEqual(this)) {
      return this;
    } else {
      return other;
    }
  }

  @Override
  public boolean isLessOrEqual(RandomWalkState other) throws CPAException, InterruptedException {
    if (this.numberOfSuccessorTaken.size() == other.numberOfSuccessorTaken.size()) {
      return this.compareTo(other) == 0;
    }
    if (this.numberOfSuccessorTaken.size() < other.numberOfSuccessorTaken.size()) {
      for (int i = 0; i < this.numberOfSuccessorTaken.size(); i++) {
        if (!this.numberOfSuccessorTaken.get(i).equals(other.numberOfSuccessorTaken.get(i))) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }

  public boolean thisEdgeShouldBeTaken(
      AssumeEdge pCfaEdge, int pProbForLeftBranchForLoop, int pProbForLeftBranchForAssign)
      throws CPATransferException {
    CFANode predecessor = pCfaEdge.getPredecessor();
    if (nodesOnPath.size() > 1 && nodesOnPath.get(nodesOnPath.size() - 2).equals(predecessor)) {
      // There is already another edge taken
      return false;
    } else if (!nodesOnPath.isEmpty()
        && this.nodesOnPath.get(nodesOnPath.size() - 1).equals(predecessor)) {
      if (this.edgeToTakeNext == null) {
        // Decide which node to take next
        if (predecessor.getNumLeavingEdges() == 2) {
          if (pCfaEdge.getPredecessor().isLoopStart()) {
            edgeToTakeNext =
                predecessor.getLeavingEdge(
                    ThreadLocalRandom.current().nextInt(100) < pProbForLeftBranchForLoop ? 0 : 1);
          } else {
            edgeToTakeNext =
                predecessor.getLeavingEdge(
                    ThreadLocalRandom.current().nextInt(100) < pProbForLeftBranchForAssign ? 0 : 1);
          }
        } else {

          edgeToTakeNext =
              predecessor.getLeavingEdge(
                  ThreadLocalRandom.current().nextInt(0, predecessor.getNumLeavingEdges()));
        }
      }
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
