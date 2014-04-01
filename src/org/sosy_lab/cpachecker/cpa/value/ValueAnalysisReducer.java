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
package org.sosy_lab.cpachecker.cpa.value;

import com.google.common.base.Preconditions;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.util.ArrayList;
import java.util.List;

import static  org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation.FUNCTION_RETURN_VAR;

public class ValueAnalysisReducer implements Reducer {

  final MachineModel machineModel;
  final LogManagerWithoutDuplicates logger;

  ValueAnalysisReducer(MachineModel pMachineModel, LogManager pLogger) {
    this.machineModel = pMachineModel;
    this.logger = new LogManagerWithoutDuplicates(pLogger);
  }

  private boolean occursInBlock(Block pBlock, String pVar) {
    // TODO could be more efficient (avoid linear runtime)
    for (ReferencedVariable referencedVar : pBlock.getReferencedVariables()) {
      if (referencedVar.getName().equals(pVar)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AbstractState getVariableReducedState(AbstractState pExpandedState, Block pContext, CFANode pCallNode) {
    ValueAnalysisState expandedState = (ValueAnalysisState)pExpandedState;

    ValueAnalysisState clonedElement = expandedState.clone();
    for (String trackedVar : expandedState.getTrackedVariableNames()) {
      if (!occursInBlock(pContext, trackedVar)) {
        clonedElement.forget(trackedVar);
      }
    }

    return clonedElement;
  }

  @Override
  public AbstractState getVariableExpandedState(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    ValueAnalysisState rootState = (ValueAnalysisState)pRootState;
    ValueAnalysisState reducedState = (ValueAnalysisState)pReducedState;

    ValueAnalysisState diffElement = rootState.clone();
    for (String trackedVar : reducedState.getTrackedVariableNames()) {
      diffElement.forget(trackedVar);
    }
    //TODO: following is needed with threshold != inf
  /*  for (String trackedVar : diffElement.getTrackedVariableNames()) {
      if (occursInBlock(pReducedContext, trackedVar)) {
        diffElement.deleteValue(trackedVar);
      }
    }*/
    for (String trackedVar : reducedState.getTrackedVariableNames()) {
      Value value = reducedState.getValueFor(trackedVar);
      if (!value.isUnknown()) {
        diffElement.assignConstant(trackedVar, reducedState.getValueFor(trackedVar));
      } else {
        diffElement.forget(trackedVar);
      }
    }
    // set difference to avoid null pointer exception due to precision adaption of omniscient composite precision adjustment
    // to avoid that due to precision adaption in BAM ART which is not yet propagated tracked variable information is deleted
    diffElement.addToDelta(diffElement);

    return diffElement;
  }

  @Override
  public Precision getVariableReducedPrecision(Precision pPrecision, Block pContext) {
    ValueAnalysisPrecision precision = (ValueAnalysisPrecision)pPrecision;

    // TODO: anything meaningful we can do here?

    return precision;
  }

  @Override
  public Precision getVariableExpandedPrecision(Precision pRootPrecision, Block pRootContext,
      Precision pReducedPrecision) {
    //ValueAnalysisPrecision rootPrecision = (ValueAnalysisPrecision)pRootPrecision;
    ValueAnalysisPrecision reducedPrecision = (ValueAnalysisPrecision)pReducedPrecision;

    // TODO: anything meaningful we can do here?

    return reducedPrecision;
  }

  @Override
  public Object getHashCodeForState(AbstractState pElementKey, Precision pPrecisionKey) {
    ValueAnalysisState elementKey = (ValueAnalysisState)pElementKey;
    ValueAnalysisPrecision precisionKey = (ValueAnalysisPrecision)pPrecisionKey;

    return Pair.of(elementKey.getConstantsMap(), precisionKey);
  }

  @Override
  public int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  @Override
  public AbstractState getVariableReducedStateForProofChecking(AbstractState pExpandedState, Block pContext,
      CFANode pCallNode) {
    return getVariableReducedState(pExpandedState, pContext, pCallNode);
  }

  @Override
  public AbstractState getVariableExpandedStateForProofChecking(AbstractState pRootState, Block pReducedContext,
      AbstractState pReducedState) {
    return getVariableExpandedState(pRootState, pReducedContext, pReducedState);
  }


  @Override
  public AbstractState getReducedStateAfterFunctionCall(
          AbstractState previousState, Block context, FunctionCallEdge pEdge)
          throws UnrecognizedCCodeException {

    Preconditions.checkArgument(pEdge instanceof CFunctionCallEdge, "only C supported");
    CFunctionCallEdge edge = (CFunctionCallEdge)pEdge;

    ValueAnalysisState state = (ValueAnalysisState)previousState;
    String functionName = edge.getPredecessor().getFunctionName();
    String calledFunctionName = edge.getSuccessor().getFunctionName();

    List<CExpression> arguments = edge.getArguments();
    List<CParameterDeclaration> parameters = edge.getSuccessor().getFunctionParameters();
    ValueAnalysisState newElement = state.clone();

    if (!edge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (parameters.size() == arguments.size());
    }

    // visitor for getting the values of the actual parameters in caller function context
    final ExpressionValueVisitor visitor = new ExpressionValueVisitor(state, functionName, machineModel, logger);

    // get value of actual parameters in caller function context,
    // 2 Steps: first calculate all values, then assign them. so we avoid problems with equal names.
    final List<Value> paramValues = new ArrayList<>(parameters.size());
    for (int i = 0; i < parameters.size(); i++) {
      paramValues.add(visitor.evaluate(arguments.get(i), parameters.get(i).getType()));
    }

    for (int i = 0; i < parameters.size(); i++) {
      String paramName = parameters.get(i).getName();
      Value value = paramValues.get(i);
      ValueAnalysisState.MemoryLocation formalParamName =
              ValueAnalysisState.MemoryLocation.valueOf(calledFunctionName, paramName, 0);

      if (value.isUnknown()) {
        newElement.forget(formalParamName);
      } else {
        // this will override equal named variables, if necessary
        newElement.assignConstant(formalParamName, value);
      }
    }

    // delete all information from outside the function.
    // this is save, because we only remove names, that are not inside the function.
    // all params are inside the function.
    return getVariableReducedState(newElement, context, edge.getSuccessor());
  }

  @Override
  // 3 Steps:
  // - first get value,
  // - then forget everything from the inner function ant expand the state,
  // - at last assign return-value.
  // This should avoid conflicts for equal named variables.
  public AbstractState getExpandedStateAfterFunctionReturn(
          AbstractState rootState, Block reducedContext, AbstractState reducedState, FunctionReturnEdge pEdge)
          throws UnrecognizedCodeException {

    Preconditions.checkArgument(pEdge instanceof FunctionReturnEdge, "only C supported");
    CFunctionReturnEdge edge = (CFunctionReturnEdge)pEdge;

    ValueAnalysisState newState  = ((ValueAnalysisState)reducedState).clone();
    CFunctionCall exprOnSummary = edge.getSummaryEdge().getExpression();
    String functionName = edge.getPredecessor().getFunctionName();
    String callerFunctionName = edge.getSuccessor().getFunctionName();

    ValueAnalysisState.MemoryLocation assignedVarName = null;
    Value value = null;

    // STEP 1
    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp = ((CFunctionCallAssignmentStatement)exprOnSummary);
      CExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable
      ValueAnalysisState.MemoryLocation returnVarName =
              ValueAnalysisState.MemoryLocation.valueOf(functionName, FUNCTION_RETURN_VAR, 0);
      ExpressionValueVisitor v = new ExpressionValueVisitor(newState, callerFunctionName,
                      machineModel, logger);
      assignedVarName = v.evaluateMemoryLocation(op1);
      if (newState.contains(returnVarName)) {
        value = newState.getValueFor(returnVarName);
      }
    }

    // STEP 2
    newState.dropFrame(functionName);
    newState = (ValueAnalysisState)getVariableExpandedState(rootState, reducedContext, newState);

    // STEP 3
    if (assignedVarName != null) { // CFunctionCallAssignmentStatement -> assignment
      if (value == null) { // value exists
        newState.forget(assignedVarName);
      } else {
        newState.assignConstant(assignedVarName, value);
      }
    }

    return newState;
  }
}
