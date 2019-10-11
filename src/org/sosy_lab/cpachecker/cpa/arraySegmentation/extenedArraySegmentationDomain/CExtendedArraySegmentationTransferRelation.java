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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.extenedArraySegmentationDomain;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ErrorSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedCompletLatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ExtendedLocationArrayContentCPA;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.UnreachableSegmentation;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.EnhancedCExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.VariableCollector;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.transfer.CSegmentationTransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class CExtendedArraySegmentationTransferRelation<T extends ExtendedCompletLatticeAbstractState<T>>
    extends
    ForwardingTransferRelation<ExtendedArraySegmentationState<T>, ExtendedArraySegmentationState<T>, Precision> {

  private static final String NAME_OF_MAIN = "main";
  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;
  public final String PREFIX;
  private EnhancedCExpressionSimplificationVisitor visitor;
  private VariableCollector collector;


  CSegmentationTransferRelation<T> transferRelationForSegmentation;
  private CExtendedUpdateTransformer<T> extendedUpdateTransformer;

  /**
   *
   * @param transferRelationForInnerDomain needs to return a single element!
   * @param pLogger for logging
   * @param pMachineModel of the machine used
   * @param typeOfAnalysis string for logging
   */
  public CExtendedArraySegmentationTransferRelation(
      TransferRelation transferRelationForInnerDomain,
      LogManagerWithoutDuplicates pLogger,
      MachineModel pMachineModel,
      String typeOfAnalysis) {
    super();
    logger = pLogger;
    machineModel = pMachineModel;
    visitor = new EnhancedCExpressionSimplificationVisitor(machineModel, logger);
    PREFIX = typeOfAnalysis + "_ANALYSIS:";

    collector = new VariableCollector(machineModel, logger);
    CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(machineModel, logger);
    extendedUpdateTransformer =
        new CExtendedUpdateTransformer<>(
            logger,
            visitor,
            builder,
            new CSplitTransformer<T>(visitor, builder, logger));
    transferRelationForSegmentation =
        new CSegmentationTransferRelation<>(
            transferRelationForInnerDomain,
            pLogger,
            pMachineModel,
            typeOfAnalysis);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected ExtendedArraySegmentationState<T>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
          throws CPATransferException {
    return applyTransferFunctionsAndLog(pCfaEdge);
  }

  @Override
  protected @Nullable ExtendedArraySegmentationState<T>
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {
    String inputArgumentsAsString = computeInnputString(pCfaEdge);
    if (super.state == null) {
      logger.log(
          ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
          PREFIX + " " + inputArgumentsAsString + ")= NULL");
      logger.flush();
      return state;
    }

    if (state.getSegmentations().size() == 1) {

      CAssumeEdge updatedEdge =
        updateEdge(
            pCfaEdge,
            pExpression,
            pTruthAssumption,
            state.getSegmentations().get(0).getCallStack());
      // Check, if a corner-case applies and the state can be returned directly:
      ArraySegmentationState<T> segmentation = state.getSegmentations().get(0);
      if (isCornerCase(segmentation)) {
        return new ExtendedArraySegmentationState<>(Lists.newArrayList(segmentation), logger);
      }

      // Apply the inner transfer function
      Optional<ArraySegmentationState<T>> resState =
          transferRelationForSegmentation
              .applyInnerTransferRelation(updatedEdge, segmentation.getDeepCopy());
      if (!resState.isPresent()) {
        return null;
      }

      // Case 3: Update(e,d) and Case 4 Split
      if (updatedEdge.getExpression() instanceof CBinaryExpression) {
        return extendedUpdateTransformer.updateWithSplit(
            (CBinaryExpression) updatedEdge.getExpression(),
            pTruthAssumption,
            resState.get(),
            updatedEdge);
      }
    }
    return applyTransferFunctionsAndLog(pCfaEdge);
  }

  @Override
  protected ExtendedArraySegmentationState<T> handleBlankEdge(BlankEdge pCfaEdge) {
    try {
      return applyTransferFunctionsAndLog(pCfaEdge);
    } catch (CPATransferException e) {
      logger.log(
          ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
          Throwables.getStackTraceAsString(e));
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  protected ExtendedArraySegmentationState<T> handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    return applyTransferFunctionsAndLog(pCfaEdge);
  }

  @Override
  protected ExtendedArraySegmentationState<T> handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    return applyTransferFunctionsAndLog(pCfaEdge);

  }

  @Override
  protected ExtendedArraySegmentationState<T>
      handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    return applyTransferFunctionsAndLog(pCfaEdge);
  }

  @Override
  protected ExtendedArraySegmentationState<T>
      handleReturnStatementEdge(CReturnStatementEdge pCfaEdge) throws CPATransferException {
    return applyTransferFunctionsAndLog(pCfaEdge);
  }

  @Override
  protected ExtendedArraySegmentationState<T>
      handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
          throws CPATransferException {
    return applyTransferFunctionsAndLog(pCfaEdge);
  }

  /**
   * If parameter pOTherStates contains an FormulaState, the segmentation may be updated
   */
  @Override
  public Collection<ExtendedArraySegmentationState<T>> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      @Nullable CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    if (pState instanceof ExtendedArraySegmentationState) {
      @SuppressWarnings("unchecked")
      ExtendedArraySegmentationState<T> s = (ExtendedArraySegmentationState<T>) pState;
      List<ArraySegmentationState<T>> strengthendSegmentations = new ArrayList<>();
      for (ArraySegmentationState<T> segmentation : s.getSegmentations()) {
        List<ArraySegmentationState<T>> res =
            new ArrayList<>(
                transferRelationForSegmentation
                    .strengthen(segmentation, pOtherStates, pCfaEdge, pPrecision));
        if (res.isEmpty()) {
          return Collections.emptyList();
        } else {
          // Assuming that it is a singelton
          strengthendSegmentations.add(res.get(0));
        }

      }
      return Collections
          .singleton(new ExtendedArraySegmentationState<>(strengthendSegmentations, logger));
    }
    logger.log(
        ExtendedLocationArrayContentCPA.GENERAL_LOG_LEVEL,
        "The strengthening failed, hence abort the analysis");
    return Collections.emptyList();
  }

  public boolean isCornerCase(ArraySegmentationState<T> s) {
    return s instanceof ErrorSegmentation || s instanceof UnreachableSegmentation;
  }

  private ExtendedArraySegmentationState<T> applyTransferFunctionsAndLog(AbstractCFAEdge pCfaEdge)
      throws CPATransferException {
    if (super.state == null) {
      // logger.log(Level.FINE, PREFIX + " " + inputArgumentsAsString + ")= NULL");
      // logger.flush();
      return state;
    }
    List<ArraySegmentationState<T>> transformedStates = new ArrayList<>();
    for (int i = 0; i < state.getSegmentations().size(); i++) {
      ArraySegmentationState<T> segmentation = state.getSegmentations().get(i);

      // Check, if a corner-case applies and the state can be returned directly:
      if (isCornerCase(segmentation)) {
        transformedStates.add(segmentation);
        continue;
      }
      // THe inner transfer function is applied on a lower level
      transformedStates.add(
          new ArrayList<>(
              transferRelationForSegmentation
                  .getAbstractSuccessorsForEdge(segmentation, precision, pCfaEdge)).get(0));
    }
    return new ExtendedArraySegmentationState<>(transformedStates, logger);
  }



  private String computeInnputString(AbstractCFAEdge pCfaEdge) {
    return pCfaEdge.getSuccessor().getNodeNumber()
        + " Compute PHI("
        + pCfaEdge.getRawStatement()
        + this.state;
  }

  private CAssumeEdge updateEdge(
      CAssumeEdge pCfaEdge,
      CExpression pExpression,
      boolean pTruthAssumption,
      CallstackState callStack)
      throws UnrecognizedCodeException {
    // Check, if the state is not the main function (hence called) and replace the function
    // parameter by the actual called parameters and if the callstate analysis is executed (the
    // initial state has no predecessor. If we apply the callstack analysis, than the current
    // callstack has at least two elements (size >1)
    CAssumeEdge updatedEdge = pCfaEdge;
    CExpression replacedExpr = pExpression;
    if (!pCfaEdge.getPredecessor().getFunctionName().equalsIgnoreCase(NAME_OF_MAIN)
        && callStack.getDepth() > 1) {
      CFunctionCallExpression summaryOfCall =
          (CFunctionCallExpression) callStack.getCallNode()
              .getLeavingSummaryEdge()
              .getExpression()
              .getFunctionCallExpression();
      // Check, if the assumption expression contains any parameters. If so, replace them
      Collection<CIdExpression> varsInExpr = collector.collectVariables(pExpression);
      List<CParameterDeclaration> params = summaryOfCall.getDeclaration().getParameters();
      Map<CIdExpression, CExpression> replacements = new HashMap<>();
      for (int i = 0; i < params.size(); i++) {
        for (CIdExpression v : varsInExpr) {
          if (params.get(i).getName().equals(v.getName())) {
            // Replace the parameter by the called value
            replacements.put(v, summaryOfCall.getParameterExpressions().get(i));
          }
        }
      }

      // Next, replace the variables by the expressions in pExpression
      replacedExpr = collector.replaceVarsInExpr(pExpression, replacements);
      updatedEdge =
          new CAssumeEdge(
              replacedExpr.toASTString(),
              pCfaEdge.getFileLocation(),
              pCfaEdge.getPredecessor(),
              pCfaEdge.getSuccessor(),
              replacedExpr,
              pTruthAssumption);
    }
    return updatedEdge;
  }
}
