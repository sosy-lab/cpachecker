/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.testgen.dummygen;

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;


public class ARGStateDummyCreator {

  private ARGTransferRelation transferRelation;
  private CFA cfa;
//  private final static ImmutableList<PrecisionAdjustment> precs = ImmutableList.of();
//  private ARGPrecisionAdjustment precision;

  public ARGStateDummyCreator(CFA pCfa, Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    cfa = pCfa;
    transferRelation = new ARGTransferRelation(new ExplicitTransferRelation(config, pLogger, pCfa));
//    precision = new ARGPrecisionAdjustment(
//        new ComponentAwareExplicitPrecisionAdjustment(precs, config, pCfa),false);
  }

  /**
   *
   * @param pState the parent state for which to compute the successors
   * @param pNotToChildState a successor of pState that should not be visited.
   * @return
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public ARGState computeOtherSuccessor(ARGState pState, ARGState pNotToChildState) throws CPATransferException, InterruptedException{
    CFAEdge edgeToWrongChild = pState.getEdgeToChild(pNotToChildState);
    CFANode location = AbstractStates.extractLocation(pState);
    if(location.getNumLeavingEdges() > 2) {
      throw new IllegalStateException("Node has more than two leaving edges, which is unsupported.");
    }
    Collection<ARGState> successors = transferRelation.getAbstractSuccessors(pState, SingletonPrecision.getInstance(), null);
    ARGState newState = null;
    for (ARGState argState : successors) {
      CFANode l = AbstractStates.extractLocation(argState);
      if(location.getEdgeTo(l).equals(edgeToWrongChild)) {
        continue;
      } else {
        newState = argState;
      }
    }

    return newState;

  }

}
