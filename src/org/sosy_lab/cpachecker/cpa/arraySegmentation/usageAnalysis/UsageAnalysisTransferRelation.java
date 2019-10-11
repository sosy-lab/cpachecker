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
package org.sosy_lab.cpachecker.cpa.arraySegmentation.usageAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.ArraySegmentationState;
import org.sosy_lab.cpachecker.cpa.arraySegmentation.util.EnhancedCExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class UsageAnalysisTransferRelation extends
    ForwardingTransferRelation<ArraySegmentationState<VariableUsageState>, ArraySegmentationState<VariableUsageState>, Precision> {

  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;
  public static final String PREFIX = "USAGE_ANALYSIS:";
  ExpressionSimplificationVisitor visitor;

  UsageTransformer usageTransformer;

  public UsageAnalysisTransferRelation(
      LogManagerWithoutDuplicates pLogger,
      MachineModel pMachineModel) {
    super();
    logger = pLogger;
    machineModel = pMachineModel;
    visitor = new EnhancedCExpressionSimplificationVisitor(machineModel, logger);
    usageTransformer = new UsageTransformer(machineModel, visitor);

  }

  @Override
  protected ArraySegmentationState<VariableUsageState>
      handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
          throws CPATransferException {

    // Only handle VariableDeclarations with Initialization expression on the RHS
    if (pDecl instanceof CVariableDeclaration
        && ((CVariableDeclaration) pDecl).getInitializer() instanceof CInitializerExpression) {
      CInitializerExpression expr =
          (CInitializerExpression) ((CVariableDeclaration) pDecl).getInitializer();
      Collection<CArraySubscriptExpression> uses = usageTransformer.getUses(expr.getExpression());
      if (!uses.isEmpty()) {
        return usageTransformer.explUse(uses, state.getDeepCopy(), pCfaEdge);
      }
    }
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected @Nullable ArraySegmentationState<VariableUsageState>
      handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
          throws CPATransferException {

    // Check, if any variable is used
    Collection<CArraySubscriptExpression> uses = usageTransformer.getUses(pExpression);
    if (!uses.isEmpty()) {
      return usageTransformer
          .explUse(new ArrayList<>(uses), state.getDeepCopy(), pCfaEdge);
    }
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected ArraySegmentationState<VariableUsageState> handleBlankEdge(BlankEdge pCfaEdge) {
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected ArraySegmentationState<VariableUsageState> handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {

    // Check, if any variable is used as paramter
    Collection<CArraySubscriptExpression> uses = new ArrayList<>();
    pArguments.parallelStream().forEach(a -> uses.addAll(usageTransformer.getUses(a)));
    if (!uses.isEmpty()) {
      return usageTransformer
          .explUse(new ArrayList<>(uses), state.getDeepCopy(), pCfaEdge);
    }
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected ArraySegmentationState<VariableUsageState> handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected ArraySegmentationState<VariableUsageState>
      handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected ArraySegmentationState<VariableUsageState>
      handleReturnStatementEdge(CReturnStatementEdge pCfaEdge) throws CPATransferException {
    if (pCfaEdge.getExpression().isPresent()) {
      // Check, if any variable is used as paramter
      Collection<CArraySubscriptExpression> uses =
          usageTransformer.getUses(pCfaEdge.getExpression().get());
      if (!uses.isEmpty()) {
        return usageTransformer
            .explUse(new ArrayList<>(uses), state.getDeepCopy(), pCfaEdge);
      }
    }
    return state != null ? state.getDeepCopy() : state;
  }

  @Override
  protected ArraySegmentationState<VariableUsageState>
      handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
          throws CPATransferException {

    return usageTransformer.use(pStatement, state.getDeepCopy(), pCfaEdge);

  }


}
