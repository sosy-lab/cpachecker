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
package org.sosy_lab.cpachecker.pcc.strategy.partialcertificate;

import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PartialReachedConstructionAlgorithm;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

import java.util.ArrayList;


public class HeuristicPartialReachedSetConstructionAlgorithm implements PartialReachedConstructionAlgorithm {

  @Override
  public AbstractState[] computePartialReachedSet(final UnmodifiableReachedSet pReached) {
    ArrayList<AbstractState> result = new ArrayList<>();
    CFANode node;
    for (AbstractState state : pReached) {
      node = AbstractStates.extractLocation(state);
      if (node == null || node.getNumEnteringEdges() > 1
          || (node.getNumLeavingEdges() > 0 && node.getLeavingEdge(0).getEdgeType() == CFAEdgeType.FunctionCallEdge)) {
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
