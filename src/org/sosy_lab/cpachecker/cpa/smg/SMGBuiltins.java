/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGRightHandSideEvaluator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class SMGBuiltins {

  private final SMGRightHandSideEvaluator expressionEvaluator;

  private final MachineModel machineModel;
  private final LogManager logger;
  private final SMGExportDotOption exportSMGOptions;
  private final SMGOptions options;

  public SMGBuiltins(
      SMGRightHandSideEvaluator pExpressionEvaluator,
      SMGOptions pOptions,
      SMGExportDotOption pExportSMGOptions,
      MachineModel pMachineModel,
      LogManager pLogger) {
    expressionEvaluator = pExpressionEvaluator;
    machineModel = pMachineModel;
    logger = pLogger;
    exportSMGOptions = pExportSMGOptions;
    options = pOptions;
  }

  private static final int MEMSET_BUFFER_PARAMETER = 0;
  private static final int MEMSET_CHAR_PARAMETER = 1;
  private static final int MEMSET_COUNT_PARAMETER = 2;
  private static final int MEMCPY_TARGET_PARAMETER = 0;
  private static final int MEMCPY_SOURCE_PARAMETER = 1;
  private static final int MEMCPY_SIZE_PARAMETER = 2;
  private static final int MALLOC_PARAMETER = 0;

  private final Set<String> BUILTINS = Sets.newHashSet(
          "__VERIFIER_BUILTIN_PLOT",
          "memcpy",
          "memset",
          "__builtin_alloca",
          //TODO: Properly model printf (dereferences and stuff)
          //TODO: General modelling system for functions which do not modify state?
          "printf"
      );

  public final void evaluateVBPlot(
      CFunctionCallExpression functionCall, UnmodifiableSMGState currentState) {
    String name = functionCall.getParameterExpressions().get(0).toASTString();
    if(exportSMGOptions.hasExportPath() && currentState != null) {
      SMGUtils.dumpSMGPlot(logger, currentState, functionCall.toASTString(), exportSMGOptions.getOutputFilePath(name));
    }
  }

  public final List<SMGAddressValueAndState> evaluateMemset(
      CFunctionCallExpression functionCall, SMGState pSMGState, CFAEdge cfaEdge)
      throws CPATransferException {

    //evaluate function: void *memset( void *buffer, int ch, size_t count );

    CExpression bufferExpr;
    CExpression chExpr;
    CExpression countExpr;

    try {
      bufferExpr = functionCall.getParameterExpressions().get(MEMSET_BUFFER_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          "Memset buffer argument not found.", cfaEdge, functionCall);
    }

    try {
      chExpr = functionCall.getParameterExpressions().get(MEMSET_CHAR_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException("Memset ch argument not found.", cfaEdge, functionCall);
    }

    try {
      countExpr = functionCall.getParameterExpressions().get(MEMSET_COUNT_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          "Memset count argument not found.", cfaEdge, functionCall);
    }

    List<SMGAddressValueAndState> result = new ArrayList<>(4);

    for (SMGAddressValueAndState bufferAddressAndState :
        evaluateAddress(pSMGState, cfaEdge, bufferExpr)) {

      for (SMGExplicitValueAndState countValueAndState :
          evaluateExplicitValue(bufferAddressAndState.getSmgState(), cfaEdge, countExpr)) {

        for (SMGValueAndState chAndState :
            evaluateExpressionValue(countValueAndState.getSmgState(), cfaEdge, chExpr)) {

          for (SMGExplicitValueAndState expValueAndState :
              evaluateExplicitValue(chAndState.getSmgState(), cfaEdge, chExpr)) {

            result.add(
                evaluateMemset(
                    expValueAndState.getSmgState(),
                    cfaEdge,
                    bufferAddressAndState.getObject(),
                    countValueAndState.getObject(),
                    chAndState.getObject(),
                    expValueAndState.getObject()));
          }
        }
      }
    }

    return result;
  }

  private SMGAddressValueAndState evaluateMemset(
      SMGState currentState,
      CFAEdge cfaEdge,
      SMGAddressValue bufferAddress,
      SMGExplicitValue countValue,
      SMGSymbolicValue ch,
      SMGExplicitValue expValue)
      throws CPATransferException {

    if (bufferAddress.isUnknown() || countValue.isUnknown()) {
      currentState =
          currentState
              .withInvalidWrite()
              .withErrorDescription("Can't evaluate dst or count for memset");
      return SMGAddressValueAndState.of(currentState);
    }

    int count = countValue.getAsInt();

    if (ch.isUnknown()) {
      // If the symbolic value is not known create a new one.
      ch = SMGKnownSymValue.of();
    }

    SMGObject bufferMemory =  bufferAddress.getObject();

    long offset =  bufferAddress.getOffset().getAsLong();

    if (ch.equals(SMGZeroValue.INSTANCE)) {
      // Create one large edge
      currentState =
          expressionEvaluator.writeValue(
              currentState,
              bufferMemory,
              offset,
              TypeUtils.createTypeWithLength(count * machineModel.getSizeofCharInBits()),
              ch,
              cfaEdge);
    } else {
      // We need to create many edges, one for each character written
      // memset() copies ch into the first count characters of buffer
      for (int c = 0; c < count; c++) {
        currentState =
            expressionEvaluator.writeValue(
                currentState,
                bufferMemory,
                offset + (c * machineModel.getSizeofCharInBits()),
                CNumericTypes.SIGNED_CHAR,
                ch,
                cfaEdge);
      }

      if (!expValue.isUnknown()) {
        currentState.putExplicit((SMGKnownSymbolicValue) ch, (SMGKnownExpValue) expValue);
      }
    }

    return SMGAddressValueAndState.of(currentState, bufferAddress);
  }

  protected List<? extends SMGValueAndState> evaluateExpressionValue(
      SMGState smgState, CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    return expressionEvaluator.evaluateExpressionValue(smgState, cfaEdge, rValue);
  }

  protected List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
      throws CPATransferException {
    return expressionEvaluator.evaluateExplicitValue(pState, pCfaEdge, pRValue);
  }

  protected List<SMGAddressValueAndState> evaluateAddress(
      SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue) throws CPATransferException {
    return expressionEvaluator.evaluateAddress(pState, pCfaEdge, pRvalue);
  }

  public final List<SMGAddressValueAndState> evaluateExternalAllocation(
      CFunctionCallExpression pFunctionCall, SMGState pState) throws SMGInconsistentException {

    String functionName = pFunctionCall.getFunctionNameExpression().toASTString();

    List<SMGAddressValueAndState> result = new ArrayList<>();

    // TODO line numbers are not unique when we have multiple input files!
    String allocation_label =
        functionName
            + "_ID"
            + SMGCPA.getNewValue()
            + "_Line:"
            + pFunctionCall.getFileLocation().getStartingLineNumber();

    result.add(SMGAddressValueAndState.of(pState, pState.addExternalAllocation(allocation_label)));

    return result;
  }
  /**
   * The method "alloca" (or "__builtin_alloca") allocates memory from the stack. The memory is
   * automatically freed at function-exit.
   */
  // TODO possible property violation "stack-overflow through big allocation" is not handled
  public final List<SMGAddressValueAndState> evaluateAlloca(
      CFunctionCallExpression functionCall,
      SMGState pState,
      CFAEdge cfaEdge,
      SMGTransferRelationKind kind)
      throws CPATransferException {
    CRightHandSide sizeExpr;

    try {
      sizeExpr = functionCall.getParameterExpressions().get(MALLOC_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException("alloca argument not found.", cfaEdge, functionCall);
    }

    List<SMGAddressValueAndState> result = new ArrayList<>();

    for (SMGExplicitValueAndState valueAndState :
        evaluateExplicitValue(pState, cfaEdge, sizeExpr)) {
      result.addAll(
          evaluateAlloca(
              valueAndState.getSmgState(), valueAndState.getObject(), cfaEdge, sizeExpr, kind));
    }

    return result;
  }

  private List<SMGAddressValueAndState> evaluateAlloca(
      SMGState currentState,
      SMGExplicitValue pSizeValue,
      CFAEdge cfaEdge,
      CRightHandSide sizeExpr,
      SMGTransferRelationKind kind)
      throws CPATransferException {

    SMGExplicitValue sizeValue = pSizeValue;

    if (sizeValue.isUnknown()) {

      if (options.isGuessSizeOfUnknownMemorySize()) {
        SMGExplicitValueAndState forcedValueAndState =
            expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);
        currentState = forcedValueAndState.getSmgState();

        //Sanity check

        List<SMGExplicitValueAndState> valueAndStates = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

        if (valueAndStates.size() != 1) {
          throw new SMGInconsistentException(
            "Found abstraction where non should exist,due to the expression " + sizeExpr.toASTString()
                + "already being evaluated once in this transferrelation step.");
         }

        SMGExplicitValueAndState valueAndState = valueAndStates.get(0);

        sizeValue = valueAndState.getObject();
        currentState = valueAndState.getSmgState();

        if (sizeValue.isUnknown()) {

          if (kind == SMGTransferRelationKind.REFINEMENT) {
            sizeValue = SMGZeroValue.INSTANCE;
          } else {
            throw new UnsupportedCodeException("Not able to compute allocation size", cfaEdge);
          }
        }
      } else {
        if (kind == SMGTransferRelationKind.REFINEMENT) {
          sizeValue = SMGZeroValue.INSTANCE;
        } else {
          throw new UnsupportedCodeException("Not able to compute allocation size", cfaEdge);
        }
      }
    }

    // TODO line numbers are not unique when we have multiple input files!
    String allocation_label = "alloc_ID" + SMGCPA.getNewValue();
    SMGState state = currentState.copyOf();
    SMGEdgePointsTo addressValue =
        state.addNewStackAllocation(
            sizeValue.getAsInt() * machineModel.getSizeofCharInBits(), allocation_label);

    List<SMGAddressValueAndState> result = new ArrayList<>(2);
    result.add(SMGAddressValueAndState.of(state, addressValue));

    // If malloc can fail, handle fail with alternative state
    if (options.isEnableMallocFailure()) {
      result.add(SMGAddressValueAndState.of(currentState.copyOf(), SMGZeroValue.INSTANCE));
    }

    return result;
  }

  private List<SMGExplicitValueAndState> getAllocateFunctionSize(
      SMGState pState,
      CFAEdge cfaEdge,
      CFunctionCallExpression functionCall,
      SMGTransferRelationKind kind)
      throws CPATransferException {

    String functionName = functionCall.getFunctionNameExpression().toASTString();

    if (options.getArrayAllocationFunctions().contains(functionName)) {

      List<SMGExplicitValueAndState> result = new ArrayList<>();
      for (SMGExplicitValueAndState numValueAndState :
          getAllocateFunctionParameter(
              options.getMemoryArrayAllocationFunctionsNumParameter(),
              functionCall,
              pState,
              cfaEdge,
              kind)) {
        for (SMGExplicitValueAndState elemSizeValueAndState :
            getAllocateFunctionParameter(
                options.getMemoryArrayAllocationFunctionsElemSizeParameter(),
                functionCall,
                numValueAndState.getSmgState(),
                cfaEdge,
                kind)) {

          SMGExplicitValue size = numValueAndState.getObject().multiply(elemSizeValueAndState.getObject());
          result.add(SMGExplicitValueAndState.of(elemSizeValueAndState.getSmgState(), size));
        }
      }

      return result;
    } else {
      return getAllocateFunctionParameter(
          options.getMemoryAllocationFunctionsSizeParameter(), functionCall, pState, cfaEdge, kind);
    }
  }

  private List<SMGExplicitValueAndState> getAllocateFunctionParameter(
      int pParameterNumber,
      CFunctionCallExpression functionCall,
      SMGState pState,
      CFAEdge cfaEdge,
      SMGTransferRelationKind kind)
      throws CPATransferException {
    CRightHandSide sizeExpr;
    SMGState currentState = pState;
    String functionName = functionCall.getFunctionNameExpression().toASTString();
    try {
      sizeExpr = functionCall.getParameterExpressions().get(pParameterNumber);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          functionName + " argument #" + pParameterNumber + " not found.", cfaEdge, functionCall);
    }

    List<SMGExplicitValueAndState> result = new ArrayList<>();
    for (SMGExplicitValueAndState valueAndState :
        evaluateExplicitValue(currentState, cfaEdge, sizeExpr)) {
      SMGExplicitValueAndState resultValueAndState = valueAndState;
      SMGExplicitValue value = valueAndState.getObject();

      if (value.isUnknown()) {

        if (options.isGuessSizeOfUnknownMemorySize()) {
          currentState = valueAndState.getSmgState();
          SMGExplicitValueAndState forcedValueAndState =
              expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);

          //Sanity check

          currentState = forcedValueAndState.getSmgState();
          List<SMGExplicitValueAndState> forcedvalueAndStates = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

          if (forcedvalueAndStates.size() != 1) { throw new SMGInconsistentException(
              "Found abstraction where non should exist,due to the expression " + sizeExpr.toASTString()
                  + "already being evaluated once in this transferrelation step.");
          }

          resultValueAndState = forcedvalueAndStates.get(0);

          value = resultValueAndState.getObject();

          if(value.isUnknown()) {
            if (kind == SMGTransferRelationKind.REFINEMENT) {
              resultValueAndState =
                  SMGExplicitValueAndState.of(currentState, SMGZeroValue.INSTANCE);
            } else {
              throw new UnsupportedCodeException("Not able to compute allocation size", cfaEdge);
            }
          }
        } else {
          if (kind == SMGTransferRelationKind.REFINEMENT) {
            resultValueAndState =
                SMGExplicitValueAndState.of(currentState, SMGZeroValue.INSTANCE);
          } else {
            throw new UnsupportedCodeException("Not able to compute allocation size", cfaEdge);
          }
        }
      }
      result.add(resultValueAndState);
    }

    return result;
  }

  public List<SMGAddressValueAndState> evaluateConfigurableAllocationFunction(
      CFunctionCallExpression functionCall,
      SMGState pState,
      CFAEdge cfaEdge,
      SMGTransferRelationKind kind)
      throws CPATransferException {

    String functionName = functionCall.getFunctionNameExpression().toASTString();
    List<SMGAddressValueAndState> result = new ArrayList<>();
    for (SMGExplicitValueAndState sizeAndState :
        getAllocateFunctionSize(pState, cfaEdge, functionCall, kind)) {

      int size = sizeAndState.getObject().getAsInt();
      SMGState currentState = sizeAndState.getSmgState();

      // TODO line numbers are not unique when we have multiple input files!
      String allocation_label =
          functionName
              + "_ID"
              + SMGCPA.getNewValue()
              + "_Line:"
              + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_address =
          currentState.addNewHeapAllocation(
              size * machineModel.getSizeofCharInBits(), allocation_label);

      SMGState newState = currentState;
      if (options.getZeroingMemoryAllocation().contains(functionName)) {
        newState =
            expressionEvaluator.writeValue(
                currentState,
                new_address.getObject(),
                0,
                TypeUtils.createTypeWithLength(size * machineModel.getSizeofCharInBits()),
                SMGZeroValue.INSTANCE,
                cfaEdge);
      }
      result.add(SMGAddressValueAndState.of(newState, new_address));

      // If malloc can fail, handle fail with alternative state
      if (options.isEnableMallocFailure()) {
        result.add(SMGAddressValueAndState.of(currentState.copyOf(), SMGZeroValue.INSTANCE));
      }
    }

    return result;
  }

  public final List<SMGState> evaluateFree(CFunctionCallExpression pFunctionCall, SMGState pState,
      CFAEdge cfaEdge) throws CPATransferException {
    CExpression pointerExp;

    try {
      pointerExp = pFunctionCall.getParameterExpressions().get(0);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          "Built-in free(): No parameter passed", cfaEdge, pFunctionCall);
    }

    List<SMGState> resultStates = new ArrayList<>();
    for (SMGAddressValueAndState addressAndState :
        expressionEvaluator.evaluateAddress(pState, cfaEdge, pointerExp)) {
      SMGAddressValue address = addressAndState.getObject();
      SMGState currentState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        logger.log(Level.INFO, "Free on expression ", pointerExp.toASTString(),
            " is invalid, because the target of the address could not be calculated.");
        SMGState invalidFreeState =
            currentState
                .withInvalidFree()
                .withErrorDescription(
                    "Free on expression "
                        + pointerExp.toASTString()
                        + " is invalid, because the target of the address could not be calculated.");
        resultStates.add(invalidFreeState);
        continue;
      }

      if (address.isZero()) {
        logger.log(Level.INFO, pFunctionCall.getFileLocation(), ":",
            "The argument of a free invocation:", cfaEdge.getRawStatement(), "is 0");

      } else {
        currentState = currentState.free(address.getOffset().getAsInt(), address.getObject());
      }

      resultStates.add(currentState);
    }

    return resultStates;
  }

  public final boolean isABuiltIn(String functionName) {
    return (BUILTINS.contains(functionName) || isNondetBuiltin(functionName) ||
        isConfigurableAllocationFunction(functionName) || isDeallocationFunction(functionName) ||
        isExternalAllocationFunction(functionName));
  }

  private static final String NONDET_PREFIX = "__VERIFIER_nondet_";
  public boolean isNondetBuiltin(String pFunctionName) {
    return pFunctionName.startsWith(NONDET_PREFIX) || pFunctionName.equals("nondet_int");
  }

  public boolean isConfigurableAllocationFunction(String functionName) {
    return options.getMemoryAllocationFunctions().contains(functionName)
        || options.getArrayAllocationFunctions().contains(functionName);
  }

  public boolean isDeallocationFunction(String functionName) {
    return options.getDeallocationFunctions().contains(functionName);
  }

  public boolean isExternalAllocationFunction(String functionName) {
    return options.getExternalAllocationFunction().contains(functionName);
  }

  public List<SMGAddressValueAndState> evaluateMemcpy(
      CFunctionCallExpression pFunctionCall, SMGState pSmgState, CFAEdge pCfaEdge)
      throws CPATransferException {

    //evaluate function: void *memcpy(void *str1, const void *str2, size_t n)

    CExpression targetStr1Expr;
    CExpression sourceStr2Expr;
    CExpression sizeExpr;

    try {
      targetStr1Expr = pFunctionCall.getParameterExpressions().get(MEMCPY_TARGET_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          "Memcpy target argument not found.", pCfaEdge, pFunctionCall);
    }

    try {
      sourceStr2Expr = pFunctionCall.getParameterExpressions().get(MEMCPY_SOURCE_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          "Memcpy source argument not found.", pCfaEdge, pFunctionCall);
    }

    try {
      sizeExpr = pFunctionCall.getParameterExpressions().get(MEMCPY_SIZE_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException(
          "Memcpy count argument not found.", pCfaEdge, pFunctionCall);
    }

    List<SMGAddressValueAndState> result = new ArrayList<>(4);

    for (SMGAddressValueAndState targetStr1AndState :
        evaluateAddress(pSmgState, pCfaEdge, targetStr1Expr)) {

      for (SMGAddressValueAndState sourceStr2AndState :
          evaluateAddress(targetStr1AndState.getSmgState(), pCfaEdge, sourceStr2Expr)) {

        for (SMGExplicitValueAndState sizeValueAndState :
            evaluateExplicitValue(sourceStr2AndState.getSmgState(), pCfaEdge, sizeExpr)) {

          SMGState currentState = sizeValueAndState.getSmgState();
          SMGAddressValue targetObject = targetStr1AndState.getObject();
          SMGAddressValue sourceObject = sourceStr2AndState.getObject();
          SMGExplicitValue explicitSizeValue = sizeValueAndState.getObject();

          if (!targetObject.isUnknown() && !sourceObject.isUnknown()) {
            int symbolicValueSize =
                expressionEvaluator.getBitSizeof(
                    pCfaEdge, sizeExpr.getExpressionType(), currentState);
            for (SMGValueAndState sizeSymbolicValueAndState :
                evaluateExpressionValue(currentState, pCfaEdge, sizeExpr)) {
              SMGSymbolicValue symbolicValue = sizeSymbolicValueAndState.getObject();

              int sourceRangeOffset = sourceObject.getOffset().getAsInt() / machineModel.getSizeofCharInBits();
              int sourceSize = sourceObject.getObject().getSize() / machineModel.getSizeofCharInBits();
              int availableSource = sourceSize - sourceRangeOffset;

              int targetRangeOffset = targetObject.getOffset().getAsInt() / machineModel.getSizeofCharInBits();
              int targetSize = targetObject.getObject().getSize() / machineModel.getSizeofCharInBits();
              int availableTarget = targetSize - targetRangeOffset;

              if (explicitSizeValue.isUnknown() && !symbolicValue.isUnknown()) {
                if (!currentState.getHeap().isObjectExternallyAllocated(sourceObject.getObject())) {
                  currentState.addErrorPredicate(symbolicValue, symbolicValueSize, SMGKnownExpValue
                      .valueOf(availableSource), symbolicValueSize, pCfaEdge);
                }
                if (!currentState.getHeap().isObjectExternallyAllocated(targetObject.getObject())) {
                  currentState.addErrorPredicate(symbolicValue, symbolicValueSize, SMGKnownExpValue
                      .valueOf(availableTarget), symbolicValueSize, pCfaEdge);
                }
              }
            }
          }
          result.add(evaluateMemcpy(currentState, targetObject, sourceObject, explicitSizeValue));
        }
      }
    }

    return result;
  }

  private SMGAddressValueAndState evaluateMemcpy(SMGState currentState, SMGAddressValue targetStr1Address,
      SMGAddressValue sourceStr2Address, SMGExplicitValue sizeValue) throws SMGInconsistentException {

    /*If target is unknown, clear all values, because we don't know where memcpy was used,
     *  and mark invalid write.
     * If source is unknown, just clear all values of target, and mark invalid read.
     * If size is unknown, clear all values of target, and mark invalid write and read.*/
    if (targetStr1Address.isUnknown() || sourceStr2Address.isUnknown()
        || sizeValue.isUnknown()) {

      if (!currentState.isTrackPredicatesEnabled()) {
        if (sizeValue.isUnknown()) {
          currentState =
              currentState
                  .withInvalidWrite()
                  .withInvalidRead()
                  .withErrorDescription("Can't evaluate memcpy dst and src");
        } else if (targetStr1Address.isUnknown()) {
          currentState =
              currentState.withInvalidWrite().withErrorDescription("Can't evaluate memcpy dst");
        } else {
          currentState =
              currentState.withInvalidRead().withErrorDescription("Can't evaluate memcpy src");
        }
      }
      if (!sourceStr2Address.isUnknown() && sourceStr2Address.getObject().equals(SMGNullObject.INSTANCE)) {
        currentState =
            currentState.withInvalidRead().withErrorDescription("Memcpy src is null pointer");
      }
      if (!targetStr1Address.isUnknown() && targetStr1Address.getObject().equals(SMGNullObject.INSTANCE)) {
        currentState =
            currentState.withInvalidWrite().withErrorDescription("Memcpy to null pointer dst");
      }

      if (targetStr1Address.isUnknown()) {
        currentState.unknownWrite();
      } else {
        //TODO More precise clear of values
        currentState.writeUnknownValueInUnknownField(targetStr1Address.getAddress().getObject());
      }

      return SMGAddressValueAndState.of(currentState);
    }

    SMGObject source = sourceStr2Address.getObject();
    SMGObject target = targetStr1Address.getObject();

    long sourceOffset = sourceStr2Address.getOffset().getAsLong();
    long sourceLastCopyBitOffset = sizeValue.getAsLong() * machineModel.getSizeofCharInBits() +
        sourceOffset;
    long targetOffset = targetStr1Address.getOffset().getAsLong();

    if (sourceLastCopyBitOffset > source.getSize()) {
      currentState = currentState.withInvalidRead().withErrorDescription("Overread on memcpy");
    } else if (targetOffset > target.getSize() - (sizeValue.getAsLong() * machineModel
        .getSizeofCharInBits())) {
      currentState = currentState.withInvalidWrite().withErrorDescription("Overwrite on memcpy");
    } else {
      currentState.copy(source, target, sourceOffset, sourceLastCopyBitOffset, targetOffset);
    }

    return SMGAddressValueAndState.of(currentState, targetStr1Address);
  }

  public List<SMGAddressValueAndState> handleBuiltinFunctionCall(
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String calledFunctionName,
      SMGState newState,
      SMGTransferRelationKind kind)
      throws CPATransferException {

    if (isExternalAllocationFunction(calledFunctionName)) {
      return evaluateExternalAllocation(cFCExpression, newState);
    }

    switch (calledFunctionName) {
      case "__builtin_alloca":
        return evaluateAlloca(cFCExpression, newState, pCfaEdge, kind);

      case "memset":
        return evaluateMemset(cFCExpression, newState, pCfaEdge);

      case "memcpy":
        return evaluateMemcpy(cFCExpression, newState, pCfaEdge);

      case "__VERIFIER_BUILTIN_PLOT":
        evaluateVBPlot(cFCExpression, newState);
        // $FALL-THROUGH$
      case "printf":
        return ImmutableList.of(SMGAddressValueAndState.of(newState));

      default:
        if (isNondetBuiltin(calledFunctionName)) {
          return Collections.singletonList(SMGAddressValueAndState.of(newState));
        } else {
          throw new AssertionError(
              "Unexpected function handled as a builtin: " + calledFunctionName);
        }
    }
  }

  public List<SMGAddressValueAndState> handleUnknownFunction(
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String calledFunctionName,
      SMGState pState)
      throws CPATransferException, AssertionError {
    switch (options.getHandleUnknownFunctions()) {
      case STRICT:
        throw new CPATransferException(
            "Unknown function '"
                + calledFunctionName
                + "' may be unsafe. See the cpa.smg.handleUnknownFunctions option.");
      case ASSUME_SAFE:
        return ImmutableList.of(SMGAddressValueAndState.of(pState));
      case ASSUME_EXTERNAL_ALLOCATED:
        return expressionEvaluator.handleSafeExternFuction(cFCExpression, pState, pCfaEdge);
      default:
        throw new AssertionError(
            "Unhandled enum value in switch: " + options.getHandleUnknownFunctions());
    }
  }
}