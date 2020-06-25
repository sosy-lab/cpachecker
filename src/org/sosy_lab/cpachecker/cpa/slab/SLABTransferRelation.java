/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.slab;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.SymbolicLocationsUtility;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.java_smt.api.SolverException;

public class SLABTransferRelation implements TransferRelation {

  private TransferRelation transferRelation;
  private EdgeSet allTransitions;
  private SymbolicLocationsUtility symbolicLocationsUtility;

  public SLABTransferRelation(
      TransferRelation pTransferRelation,
      CFA pCfa,
      SymbolicLocationsUtility pSymbolicLocationseUtility) {
    transferRelation = pTransferRelation;
    allTransitions = makeTotalTransitionEdgeSet(pCfa);
    symbolicLocationsUtility = pSymbolicLocationseUtility;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    Collection<? extends AbstractState> successors;
    ARGState element = (ARGState) pState;
    AbstractState wrappedState = element.getWrappedState();
    successors = transferRelation.getAbstractSuccessors(wrappedState, pPrecision);
    Collection<AbstractState> wrappedSuccessors = new ArrayList<>();
    for (AbstractState absElement : successors) {
      boolean isInit;
      boolean isError;
      try {
        isInit = symbolicLocationsUtility.isInit(absElement);
        isError = symbolicLocationsUtility.isError(absElement);
      } catch (SolverException e) {
        throw new CPATransferException("Could not determine successor due to SolverException", e);
      }
      SLARGState successorElem =
          new SLARGState(
              (SLARGState) pState, new EdgeSet(allTransitions), isInit, isError, absElement);
      wrappedSuccessors.add(successorElem);
    }
    ((SLARGState) pState).markExpanded();
    return wrappedSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException();
  }

  private static EdgeSet makeTotalTransitionEdgeSet(CFA pCfa) {
    ImmutableSet.Builder<CFAEdge> edges = new ImmutableSet.Builder<>();
    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge leaving : CFAUtils.leavingEdges(node)) {
        edges.add(leaving);
      }
    }
    return new EdgeSet(edges.build());
  }
}
