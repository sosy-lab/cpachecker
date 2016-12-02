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
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
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
  public AbstractState getAbstractSuccessorsForSummary(
      AbstractState state,
      Precision precision,
      List<Summary> pSummary,
      Block pBlock) throws CPATransferException, InterruptedException {

    LocationSummary lSummary = (LocationSummary) pSummary;

    return locationStateFactory.getState(lSummary.getExitNode());
  }


  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pState, Precision pPrecision, Block pBlock) {
    return pState;
  }

  @Override
  public AbstractState projectToCallsite(Summary pSummary) {
    LocationSummary lSummary = (LocationSummary) pSummary;
    return locationStateFactory.getState(lSummary.getEntryNode());
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    LocationSummary lSummary = (LocationSummary) pSummary;
    return locationStateFactory.getState(lSummary.getExitNode());
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
  }
}
