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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

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
  public AbstractState getAbstractSuccessorForSummary(
      AbstractState state,
      Precision precision,
      List<Summary> pSummary,
      Block pBlock,
      CFANode pCallsite) throws CPATransferException, InterruptedException {

//    Preconditions.checkState(!pSummary.isEmpty());
//    Preconditions.checkArgument(pSummary.stream().allMatch(s -> s.equals(pSummary.get(0))));
//
//    LocationSummary lSummary = (LocationSummary) pSummary.get(0);

    LocationState lState = (LocationState) state;
    CFANode node = lState.getLocationNode();

    return locationStateFactory.getState(node.getLeavingSummaryEdge().getSuccessor());
  }


  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pState, Precision pPrecision, Block pBlock) {
    return pState;
  }

  @Override
  public List<Summary> generateSummaries(
      AbstractState pCallState,
      Precision pEntryPrecision,
      List<? extends AbstractState> pReturnState,
      List<Precision> pReturnPrecision,
      CFANode pEntryNode,
      Block pBlock) {

    assert pEntryNode instanceof FunctionEntryNode;
    FunctionEntryNode eNode = (FunctionEntryNode) pEntryNode;
    return Collections.singletonList(new LocationSummary(eNode));
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) {

    // Do not merge states.
    return pSummary2;
  }

  @Override
  public boolean isDescribedBy(Summary pSummary1, Summary pSummary2)
      throws CPAException, InterruptedException {
    return pSummary1.equals(pSummary2);
  }

  @Override
  public boolean isSummaryApplicableAtCallsite(
      Summary pSummary,
      AbstractState pCallsite
  ) throws CPAException, InterruptedException {
    // todo: any other verdicts?.. todo: compare the actual location.
    // take into the account that the nodes do not match precisely.
    return true;
  }

  private static class LocationSummary implements Summary {

    private final FunctionEntryNode entryNode;

    private LocationSummary(FunctionEntryNode pEntryNode) {
      entryNode = pEntryNode;
    }

    CFANode getEntryNode() {
      return entryNode;
    }

    CFANode getExitNode() {
      return entryNode.getExitNode();
    }

    @Override
    public boolean equals(Object pO) {
      if (this == pO) {
        return true;
      }
      if (pO == null || getClass() != pO.getClass()) {
        return false;
      }
      LocationSummary that = (LocationSummary) pO;
      return Objects.equals(entryNode, that.entryNode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(entryNode);
    }
  }
}
