// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.slab;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.SymbolicLocationsUtility;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.java_smt.api.SolverException;

public class SLABTransferRelation implements TransferRelation {

  private final TransferRelation transferRelation;
  private final ImmutableSet<CFAEdge> allTransitions;
  private final SymbolicLocationsUtility symbolicLocationsUtility;

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

  private static ImmutableSet<CFAEdge> makeTotalTransitionEdgeSet(CFA pCfa) {
    return from(pCfa.getAllNodes()).transformAndConcat(CFAUtils::leavingEdges).toSet();
  }
}
