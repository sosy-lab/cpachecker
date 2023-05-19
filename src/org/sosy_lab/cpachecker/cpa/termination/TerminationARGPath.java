// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.termination;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationLoopInformation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Adds intermediate {@link CFAEdge}s created by {@link TerminationTransferRelation} during the
 * analysis to the full path.
 */
public class TerminationARGPath extends ARGPath {

  private final TerminationLoopInformation terminationInformation;

  // Construct full path at most once.
  @Nullable private List<CFAEdge> terminationFullPath = null;

  public TerminationARGPath(
      ARGPath pBasicArgPath, TerminationLoopInformation pTerminationInformation) {
    super(pBasicArgPath);
    terminationInformation = checkNotNull(pTerminationInformation);
  }

  @Override
  public List<CFAEdge> getFullPath() {
    if (terminationFullPath != null) {
      return terminationFullPath;
    }

    ImmutableList.Builder<CFAEdge> fullPathBuilder = ImmutableList.builder();
    PathIterator it = pathIterator();
    Set<CFAEdge> intermediateTermiantionEdges = new HashSet<>();

    while (it.hasNext()) {
      ARGState prev = it.getAbstractState();
      CFAEdge curOutgoingEdge = it.getOutgoingEdge();
      it.advance();
      ARGState succ = it.getAbstractState();

      TerminationState terminationPrev = extractStateByType(prev, TerminationState.class);
      TerminationState terminationSucc = extractStateByType(succ, TerminationState.class);

      // insert transition from loop to stem
      if (terminationPrev.isPartOfStem() && terminationSucc.isPartOfLoop()) {
        CFANode curNode = extractLocation(prev);
        List<CFAEdge> stemToLoopTransition =
            terminationInformation.createStemToLoopTransition(curNode, curNode);
        intermediateTermiantionEdges.addAll(stemToLoopTransition);
        fullPathBuilder.addAll(stemToLoopTransition);
      }

      // compute path between cur and next node
      if (curOutgoingEdge == null) {
        CFANode curNode = extractLocation(prev);
        CFANode nextNode = extractLocation(succ);

        // add negated ranking relation before target state (non-termination label)
        if (AbstractStates.isTargetState(succ)) {
          CFAEdge negatedRankingRelationAssumeEdge =
              terminationInformation.createRankingRelationAssumeEdge(curNode, nextNode, false);

          intermediateTermiantionEdges.add(negatedRankingRelationAssumeEdge);
          fullPathBuilder.add(negatedRankingRelationAssumeEdge);
          nextNode = curNode;
        }

        // we assume a linear chain of edges from 'prev' to 'succ'
        while (!Objects.equals(curNode, nextNode)) {
          FluentIterable<CFAEdge> leavingEdges =
              CFAUtils.leavingEdges(curNode).filter(not(in(intermediateTermiantionEdges)));
          if (!(leavingEdges.size() == 1 && curNode.getLeavingSummaryEdge() == null)) {
            return ImmutableList.of();
          }

          CFAEdge intermediateEdge = leavingEdges.get(0);
          fullPathBuilder.add(intermediateEdge);
          curNode = intermediateEdge.getSuccessor();
        }

        // we have a normal connection without hole in the edges
      } else {
        fullPathBuilder.add(curOutgoingEdge);
      }
    }

    terminationFullPath = fullPathBuilder.build();
    terminationInformation.resetCfa();
    return terminationFullPath;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (!(pOther instanceof TerminationARGPath)) {
      return false;
    }

    return super.equals(pOther);
  }
}
