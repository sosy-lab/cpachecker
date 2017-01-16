/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.simple.SimpleSummaryManager;

/**
 * New summary management framework for LocationCPA.
 */
public class LocationSimpleSummaryManager implements SimpleSummaryManager {
  private final LocationStateFactory factory;

  public LocationSimpleSummaryManager(LocationStateFactory pStateFactory) {
    factory = pStateFactory;
  }

  @Override
  public List<? extends AbstractState> getEntryStates(
      AbstractState callSite, CFANode callNode, Block calledBlock) {
    assert callNode.getNumLeavingEdges() == 1;
    return Collections.singletonList(
            factory.getState(callNode.getLeavingEdge(0).getSuccessor()));
  }

  @Override
  public List<? extends AbstractState> applyFunctionSummary(
      AbstractState callSite, AbstractState exitState, CFANode callNode, Block calledBlock) {
    return Collections.singletonList(
        factory.getState(callNode.getLeavingSummaryEdge().getSuccessor()));
  }


  @Override
  public boolean isSummaryApplicable(
      AbstractState callSite, AbstractState exitState, CFANode callNode, Block calledBlock) {
    return true;
  }
}
