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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;


public class ARGBasedPartialReachedSetConstructionAlgorithm extends
    MonotoneTransferFunctionARGBasedPartialReachedSetConstructionAlgorithm {

  private ConfigurableProgramAnalysis cpa;

  public ARGBasedPartialReachedSetConstructionAlgorithm(final boolean pReturnARGStatesInsteadOfWrappedStates) {
    super(pReturnARGStatesInsteadOfWrappedStates);
  }

  @Override
  protected NodeSelectionARGPass getARGPass(final Precision pRootPrecision, final ARGState pRoot)
      throws InvalidConfigurationException {
    if (!GlobalInfo.getInstance().getCPA().isPresent()) {
      throw new InvalidConfigurationException("No CPA specified.");
    } else {
      ARGCPA cpa = CPAs.retrieveCPA(GlobalInfo.getInstance().getCPA().get(), ARGCPA.class);
      if (cpa == null) { throw new InvalidConfigurationException("Require ARGCPA"); }
      this.cpa = cpa.getWrappedCPAs().get(0);
    }
    return new ExtendedNodeSelectionARGPass(pRootPrecision, pRoot);
  }

  private class ExtendedNodeSelectionARGPass extends NodeSelectionARGPass {

    private final Precision precision;

    public ExtendedNodeSelectionARGPass(final Precision pRootPrecision, final ARGState pRoot) {
      super(pRoot);
      precision = pRootPrecision;
    }

    @Override
    protected boolean isToAdd(final ARGState pNode) {
      boolean isToAdd = super.isToAdd(pNode);
      if (!isToAdd && !pNode.isCovered()) {
        for (ARGState parent : pNode.getParents()) {
          if (!isTransferSuccessor(parent, pNode)) {
            isToAdd = true;
          }
          break;
        }
      }
      return isToAdd;
    }

    private boolean isTransferSuccessor(ARGState pPredecessor, ARGState pChild) {
      CFAEdge edge = pPredecessor.getEdgeToChild(pChild);
      try {
        Collection<AbstractState> successors = new ArrayList<>(
            cpa.getTransferRelation().getAbstractSuccessors(pPredecessor.getWrappedState(), precision, edge));
        // check if child is the successor computed by transfer relation
        if(successors.contains(pChild.getWrappedState())) { return true; }
        // check if check only failed because it is not the same object
        if (!cpa.getStopOperator().stop(pChild.getWrappedState(), successors, precision)){
          return false;
        }
        Collection<AbstractState> childCollection = Collections.singleton(pChild.getWrappedState());
        for (AbstractState state : successors) {
          if (cpa.getStopOperator().stop(state, childCollection, precision)) {
            return true;
          }
        }
      } catch (InterruptedException | CPAException e) {
      }
      return false;
    }

  }

}
