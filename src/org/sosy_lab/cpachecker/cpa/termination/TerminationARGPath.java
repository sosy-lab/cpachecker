/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.termination;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationLoopInformation;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Adds intermediate {@link CFAEdge}s created by {@link TerminationTransferRelation}
 * during the analysis to the full path.
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
    Set<CFAEdge> intermediateTermiantionEdges = Sets.newHashSet();

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
        while (curNode != nextNode) {
          FluentIterable<CFAEdge> leavingEdges =
              CFAUtils.leavingEdges(curNode).filter(not(in(intermediateTermiantionEdges)));
          if (!(leavingEdges.size() == 1 && curNode.getLeavingSummaryEdge() == null)) {
            return Collections.emptyList();
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
