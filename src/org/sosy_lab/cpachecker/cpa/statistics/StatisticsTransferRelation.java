/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.statistics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * The transfer relation of the StatisticsCPA.
 */
public class StatisticsTransferRelation implements TransferRelation {


  public StatisticsTransferRelation(Configuration pConfig, LogManager pLogger) {
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
    StatisticsState state = (StatisticsState) pState;
    CFANode node = state.getLocationNode();
    if (pCfaEdge != null){
      if (CFAUtils.allLeavingEdges(node).contains(pCfaEdge)) {
        return Collections.singleton(state.nextState(pCfaEdge));
      }
      return Collections.emptySet();
    }

    List<StatisticsState> allSuccessors = new ArrayList<>(node.getNumLeavingEdges());

    for (CFAEdge successor : CFAUtils.leavingEdges(node)) {
      allSuccessors.add(state.nextState(successor));
    }

    return allSuccessors;

  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

}
