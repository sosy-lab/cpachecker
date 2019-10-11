/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.arraySegmentation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.extenedArraySegmentationDomain.CExtendedArraySegmentationTransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.extenedArraySegmentationDomain.ExtendedArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ExtendedLocationArrayContentTransferRelation<T extends ExtendedCompletLatticeAbstractState<T>>
    extends
    ForwardingTransferRelation<ExtendedLocationArrayContentState<T>, ExtendedLocationArrayContentState<T>, Precision> {

  private final LogManagerWithoutDuplicates logger;
  private final CExtendedArraySegmentationTransferRelation<T> transferForExtendedSegmentations;
  private LocationStateFactory locFactory;

  public ExtendedLocationArrayContentTransferRelation(
      LogManagerWithoutDuplicates pLogger,
      LocationStateFactory pLocFactory,
      CExtendedArraySegmentationTransferRelation<T> pInnerTransferRelation) {
    super();
    logger = pLogger;
    transferForExtendedSegmentations = pInnerTransferRelation;
    this.locFactory = pLocFactory;
  }

  @Override
  protected ExtendedLocationArrayContentState<T>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
          throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);
  }

  @Override
  protected ExtendedLocationArrayContentState<T> handleBlankEdge(BlankEdge pCfaEdge) {
    try {
      return delegateEdgeHandling(pCfaEdge);
    } catch (CPATransferException e) {
      // TODO Enhance error Handling
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected ExtendedLocationArrayContentState<T> handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);
  }

  @Override
  protected ExtendedLocationArrayContentState<T> handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);
  }

  @Override
  protected ExtendedLocationArrayContentState<T>
      handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);
  }

  @Override
  protected ExtendedLocationArrayContentState<T>
      handleReturnStatementEdge(CReturnStatementEdge pCfaEdge) throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);
  }

  @Override
  protected ExtendedLocationArrayContentState<T>
      handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
          throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);
  }

  @Override
  protected ExtendedLocationArrayContentState<T>
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {
    return delegateEdgeHandling(pCfaEdge);

  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (pState instanceof ExtendedLocationArrayContentState) {
      ExtendedLocationArrayContentState<T> cluAnalysisState =
          (ExtendedLocationArrayContentState<T>) pState;
      List<ExtendedArraySegmentationState<T>> strengthened =

          new ArrayList<>(
              transferForExtendedSegmentations.strengthen(
                  cluAnalysisState.getArraySegmentation(),
                  pOtherStates,
                  pCfaEdge,
                  pPrecision));
      if (strengthened.size() > 0
          && !strengthened.get(0)
              .equals(((ExtendedLocationArrayContentState) pState).getArraySegmentation())) {
        return Collections.singleton(
            new ExtendedLocationArrayContentState<>(
                cluAnalysisState.getLocation(),
                strengthened.get(0),
                logger));
      }
      return Collections.singleton(pState);
    }
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }

  /**
   * Applies the transfer functions of the included analysis to a copy of the current state
   *
   * @param pCfaEdge the current edge
   * @return the element obtained by the transfer functions
   * @throws CPATransferException if any transfer function throws one or more than one result is
   *         returned
   */
  private ExtendedLocationArrayContentState<T>
      delegateEdgeHandling(AbstractCFAEdge pCfaEdge)
      throws CPATransferException {
    if (super.state == null) {
      return state;
    }
    // Clone the state
    Collection<ExtendedArraySegmentationState<T>> arraySegmentation =
        transferForExtendedSegmentations.getAbstractSuccessorsForEdge(
            new ExtendedArraySegmentationState<>(state.getArraySegmentation()),
            getPrecision(),
            pCfaEdge);
    // Check if a single result is returned
    if (arraySegmentation.size() != 1) {
      throw new CPATransferException(
          "The UsageAnalysis transfer function could not determine a single sucessor, hence computation is abported");
    }
    List<ExtendedArraySegmentationState<T>> transformedSeg =
        new ArrayList<>(arraySegmentation);
    // Determine the correct successor of the the current location

    return new ExtendedLocationArrayContentState<>(
        locFactory.getState(pCfaEdge.getSuccessor()),
        transformedSeg.get(0),
        this.logger);
  }
}
