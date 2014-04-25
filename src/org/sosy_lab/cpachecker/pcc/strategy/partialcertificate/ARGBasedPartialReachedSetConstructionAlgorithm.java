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

import java.util.Collection;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class ARGBasedPartialReachedSetConstructionAlgorithm extends
    MonotoneStopARGBasedPartialReachedSetConstructionAlgorithm {

  private final ConfigurableProgramAnalysis cpa;

  public ARGBasedPartialReachedSetConstructionAlgorithm(final ConfigurableProgramAnalysis pCpa,
      final boolean pReturnARGStatesInsteadOfWrappedStates) {
    super(pReturnARGStatesInsteadOfWrappedStates);
    cpa = pCpa;
  }

  @Override
  protected NodeSelectionARGPass getARGPass(Precision pRootPrecision) {
    return new ExtendedNodeSelectionARGPass(pRootPrecision);
  }

  private class ExtendedNodeSelectionARGPass extends NodeSelectionARGPass {

    private Precision precision;

    public ExtendedNodeSelectionARGPass(Precision pRootPrecision) {
      precision = pRootPrecision;
    }

    @Override
    protected boolean isToAdd(final ARGState pNode) {
      boolean isToAdd = super.isToAdd(pNode);
      if (!isToAdd) {
        ARGState graphElem = getNonCoveredElem(pNode);
        for (ARGState parent : pNode.getParents()) {
          if (!isTransferSuccessor(parent, graphElem)) {
            isToAdd = true;
          }
          break;
        }
      }
      return isToAdd;
    }

    private boolean isTransferSuccessor(ARGState pPredecessor, ARGState pSuccessor) {
      CFAEdge edge = pPredecessor.getEdgeToChild(pSuccessor);
      try {
        Collection<? extends AbstractState> successors =
            cpa.getTransferRelation().getAbstractSuccessors(pPredecessor.getWrappedState(), precision, edge);
        if (successors.contains(pSuccessor)) { return true; }
      } catch (CPATransferException | InterruptedException e) {
      }
      return false;
    }

    protected ARGState getNonCoveredElem(ARGState pNode) {
      while (pNode.isCovered()) {
        pNode = pNode.getCoveringState();
      }
      return pNode;
    }
  }

}
