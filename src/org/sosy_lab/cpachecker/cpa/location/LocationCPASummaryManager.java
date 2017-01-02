/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.location;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Operating over summaries for {@link LocationCPA}.
 *
 * <p>Assumes that analysis proceeds forwards.
 */
public class LocationCPASummaryManager implements SummaryManager {

  private final LocationStateFactory locationStateFactory;

  LocationCPASummaryManager(LocationStateFactory pLocationStateFactory) {
    locationStateFactory = pLocationStateFactory;
  }

  @Override
  public List<AbstractState> getAbstractSuccessorsForSummary(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<Summary> pSummary,
      Block pBlock,
      CFANode pCallsite) {

    LocationState lState = (LocationState) pCallState;
    CFANode node = lState.getLocationNode();

    return Collections.singletonList(
        locationStateFactory.getState(node.getLeavingSummaryEdge().getSuccessor()));
  }


  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pCallState, Precision pPrecision, CFANode pCallNode, Block pBlock) {
    return pCallState;
  }

  @Override
  public List<Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pJoinStates,
      List<Precision> pJoinPrecisions,
      CFANode pEntryNode,
      Block pBlock) {


    LocationState joinState = (LocationState) pJoinStates.iterator().next();
    assert pJoinStates.stream()
        .allMatch(s -> AbstractStates.extractLocation(s) == joinState.getLocationNode());

    return Collections.singletonList(
        new LocationSummary(joinState.getLocationNode())
    );
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) {

    // Do not merge states.
    return pSummary2;
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2) {
    return pSummary1.equals(pSummary2);
  }

  @Override
  public boolean isSummaryApplicableAtCallsite(
      Summary pSummary,
      AbstractState pCallsite
  ) throws CPAException, InterruptedException {
    LocationSummary lSummary = (LocationSummary) pSummary;
    LocationState lState = (LocationState) pCallsite;

    return lSummary.getJoinNode() ==
        lState.getLocationNode().getLeavingSummaryEdge().getSuccessor();
  }

  private static class LocationSummary implements Summary {

    private final CFANode joinNode;

    private LocationSummary(CFANode pJoinNode) {
      joinNode = pJoinNode;
    }

    CFANode getJoinNode() {
      return joinNode;
    }

    @Override
    public boolean equals(@Nullable Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      LocationSummary that = (LocationSummary) pO;
      return Objects.equals(joinNode, that.joinNode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(joinNode);
    }

    @Override
    public String toString() {
      return "LocationSummary{joinNode=" + joinNode + ", callNode=" +
          joinNode.getEnteringSummaryEdge().getPredecessor() + '}';
    }
  }
}
