// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class HeuristicPartialReachedSetConstructionAlgorithm
    implements PartialReachedConstructionAlgorithm {

  @Override
  public AbstractState[] computePartialReachedSet(
      final UnmodifiableReachedSet pReached, final ConfigurableProgramAnalysis pCpa) {
    List<AbstractState> result = new ArrayList<>();
    CFANode node;
    for (AbstractState state : pReached) {
      node = AbstractStates.extractLocation(state);
      if (node == null
          || node.getNumEnteringEdges() > 1
          || (node.getNumLeavingEdges() > 0
              && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.FunctionCallEdge)) {
        result.add(state);
      }
    }
    if (!result.contains(pReached.getFirstState())) {
      result.add(pReached.getFirstState());
    }
    AbstractState[] arrayRep = new AbstractState[result.size()];
    result.toArray(arrayRep);
    return arrayRep;
  }
}
