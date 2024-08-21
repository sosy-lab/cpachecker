// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** The transfer relation of the StatisticsCPA. */
public class StatisticsTransferRelation extends SingleEdgeTransferRelation {

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    StatisticsState state = (StatisticsState) pState;
    CFANode node = state.getLocationNode();
    if (pCfaEdge != null) {
      if (CFAUtils.allLeavingEdges(node).contains(pCfaEdge)) {
        return Collections.singleton(state.nextState(pCfaEdge));
      }
      return ImmutableSet.of();
    }

    ImmutableList.Builder<StatisticsState> allSuccessors =
        ImmutableList.builderWithExpectedSize(node.getNumLeavingEdges());

    for (CFAEdge successor : CFAUtils.leavingEdges(node)) {
      allSuccessors.add(state.nextState(successor));
    }

    return allSuccessors.build();
  }
}
