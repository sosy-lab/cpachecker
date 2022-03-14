// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGRightHandSideEvaluator;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
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
  private ImmutableList<Pattern> safeUnknownFunctionCompiledPatterns;

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
  private static final int STRCMP_FIRST_PARAMETER = 0;
  private static final int STRCMP_SECOND_PARAMETER = 1;

  private final Set<String> BUILTINS =
      Sets.newHashSet(
          "__VERIFIER_BUILTIN_PLOT",
          "memcpy",
          "memset",
          "__builtin_alloca",
          // TODO: Properly model printf (dereferences and stuff)
          // TODO: General modelling system for functions which do not modify state?
          "printf",
          "strcmp",
          "realloc");

  private void evaluateVBPlot(
      CFunctionCallExpression functionCall, UnmodifiableSMGState currentState) {
    String name = functionCall.getParameterExpressions().get(0).toASTString();
    if (exportSMGOptions.hasExportPath() && currentState != null) {
      SMGUtils.dumpSMGPlot(
          logger,
          currentState,
          functionCall.toASTString(),
          exportSMGOptions.getOutputFilePath(name));
    }
  }

  private List<SMGAddressValueAndState> evaluateMemset(
      CFunctionCallExpression functionCall, SMGState pSMGState, CFAEdge cfaEdge)
      throws CPATransferException {

    // evaluate function: void *memset( void *buffer, int ch, size_t count );

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
      SMGValue ch,
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

    SMGObject bufferMemory = bufferAddress.getObject();

    long offset = bufferAddress.getOffset().getAsLong();

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

  protected List<SMGExplicitValueAndState> evaluateExplicitValue(
      SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue) throws CPATransferException {
    return expressionEvaluator.evaluateExplicitValue(pState, pCfaEdge, pRValue);
  }

  protected List<SMGAddressValueAndState> evaluateAddress(
      SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue) throws CPATransferException {
    return expressionEvaluator.evaluateAddress(pState, pCfaEdge, pRvalue);
  }

  private List<SMGAddressValueAndState> evaluateExternalAllocation(
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
  private List<SMGAddressValueAndState> evaluateAlloca(
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

        // Sanity check

        List<SMGExplicitValueAndState> valueAndStates =
            evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

        if (valueAndStates.size() != 1) {
          throw new SMGInconsistentException(
              "Found abstraction where non should exist,due to the expression "
                  + sizeExpr.toASTString()
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

          SMGExplicitValue size =
              numValueAndState.getObject().multiply(elemSizeValueAndState.getObject());
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

          // Sanity check

          currentState = forcedValueAndState.getSmgState();
          List<SMGExplicitValueAndState> forcedvalueAndStates =
              evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

          if (forcedvalueAndStates.size() != 1) {
            throw new SMGInconsistentException(
                "Found abstraction where non should exist,due to the expression "
                    + sizeExpr.toASTString()
                    + "already being evaluated once in this transferrelation step.");
          }

          resultValueAndState = forcedvalueAndStates.get(0);

          value = resultValueAndState.getObject();

          if (value.isUnknown()) {
            if (kind == SMGTransferRelationKind.REFINEMENT) {
              resultValueAndState =
                  SMGExplicitValueAndState.of(currentState, SMGZeroValue.INSTANCE);
            } else {
              throw new UnsupportedCodeException("Not able to compute allocation size", cfaEdge);
            }
          }
        } else {
          if (kind == SMGTransferRelationKind.REFINEMENT) {
            resultValueAndState = SMGExplicitValueAndState.of(currentState, SMGZeroValue.INSTANCE);
          } else {
            throw new UnsupportedCodeException("Not able to compute allocation size", cfaEdge);
          }
        }
      }
      result.add(resultValueAndState);
    }

    return result;
  }

  List<SMGAddressValueAndState> evaluateConfigurableAllocationFunction(
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

  public final List<SMGState> evaluateFree(
      CFunctionCallExpression pFunctionCall, SMGState pState, CFAEdge cfaEdge)
      throws CPATransferException {
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
        logger.log(
            Level.INFO,
            "Free on expression ",
            pointerExp.toASTString(),
            " is invalid, because the target of the address could not be calculated.");
        SMGState invalidFreeState =
            currentState
                .withInvalidFree()
                .withErrorDescription(
                    "Free on expression "
                        + pointerExp.toASTString()
                        + " is invalid, because the target of the address could not be"
                        + " calculated.");
        resultStates.add(invalidFreeState);
        continue;
      }

      if (address.isZero()) {
        logger.log(
            Level.INFO,
            pFunctionCall.getFileLocation(),
            ":",
            "The argument of a free invocation:",
            cfaEdge.getRawStatement(),
            "is 0");

      } else {
        currentState = currentState.free(address.getOffset().getAsInt(), address.getObject());
      }

      resultStates.add(currentState);
    }

    return resultStates;
  }

  boolean isABuiltIn(String functionName) {
    return (BUILTINS.contains(functionName)
        || isNondetBuiltin(functionName)
        || isConfigurableAllocationFunction(functionName)
        || isDeallocationFunction(functionName)
        || isExternalAllocationFunction(functionName));
  }

  private static final String NONDET_PREFIX = "__VERIFIER_nondet_";

  private boolean isNondetBuiltin(String pFunctionName) {
    return pFunctionName.startsWith(NONDET_PREFIX) || pFunctionName.equals("nondet_int");
  }

  boolean isConfigurableAllocationFunction(String functionName) {
    return options.getMemoryAllocationFunctions().contains(functionName)
        || options.getArrayAllocationFunctions().contains(functionName);
  }

  boolean isDeallocationFunction(String functionName) {
    return options.getDeallocationFunctions().contains(functionName);
  }

  private boolean isExternalAllocationFunction(String functionName) {
    return options.getExternalAllocationFunction().contains(functionName);
  }

  private List<SMGAddressValueAndState> evaluateMemcpy(
      CFunctionCallExpression pFunctionCall, SMGState pSmgState, CFAEdge pCfaEdge)
      throws CPATransferException {

    // evaluate function: void *memcpy(void *str1, const void *str2, size_t n)

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
            CType expressionType = sizeExpr.getExpressionType();
            SMGType symbolicValueSMGType =
                SMGType.constructSMGType(
                    expressionType, currentState, pCfaEdge, expressionEvaluator);
            for (SMGValueAndState sizeSymbolicValueAndState :
                evaluateExpressionValue(currentState, pCfaEdge, sizeExpr)) {
              SMGValue symbolicValue = sizeSymbolicValueAndState.getObject();

              long sourceRangeOffset =
                  sourceObject.getOffset().getAsLong() / machineModel.getSizeofCharInBits();
              long sourceSize =
                  sourceObject.getObject().getSize() / machineModel.getSizeofCharInBits();
              long availableSource = sourceSize - sourceRangeOffset;

              long targetRangeOffset =
                  targetObject.getOffset().getAsLong() / machineModel.getSizeofCharInBits();
              long targetSize =
                  targetObject.getObject().getSize() / machineModel.getSizeofCharInBits();
              long availableTarget = targetSize - targetRangeOffset;

              if (explicitSizeValue.isUnknown() && !symbolicValue.isUnknown()) {
                if (!currentState.getHeap().isObjectExternallyAllocated(sourceObject.getObject())) {
                  currentState.addErrorPredicate(
                      symbolicValue,
                      symbolicValueSMGType,
                      SMGKnownExpValue.valueOf(availableSource),
                      pCfaEdge);
                }
                if (!currentState.getHeap().isObjectExternallyAllocated(targetObject.getObject())) {
                  currentState.addErrorPredicate(
                      symbolicValue,
                      symbolicValueSMGType,
                      SMGKnownExpValue.valueOf(availableTarget),
                      pCfaEdge);
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

  private SMGAddressValueAndState evaluateMemcpy(
      SMGState currentState,
      SMGAddressValue targetStr1Address,
      SMGAddressValue sourceStr2Address,
      SMGExplicitValue sizeValue)
      throws SMGInconsistentException {

    /*If target is unknown, clear all values, because we don't know where memcpy was used,
     *  and mark invalid write.
     * If source is unknown, just clear all values of target, and mark invalid read.
     * If size is unknown, clear all values of target, and mark invalid write and read.*/
    if (targetStr1Address.isUnknown() || sourceStr2Address.isUnknown() || sizeValue.isUnknown()) {

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
      if (!sourceStr2Address.isUnknown()
          && sourceStr2Address.getObject().equals(SMGNullObject.INSTANCE)) {
        currentState =
            currentState.withInvalidRead().withErrorDescription("Memcpy src is null pointer");
      }
      if (!targetStr1Address.isUnknown()
          && targetStr1Address.getObject().equals(SMGNullObject.INSTANCE)) {
        currentState =
            currentState.withInvalidWrite().withErrorDescription("Memcpy to null pointer dst");
      }

      if (targetStr1Address.isUnknown()) {
        currentState.unknownWrite();
      } else {
        // TODO More precise clear of values
        currentState.writeUnknownValueInUnknownField(targetStr1Address.getAddress().getObject());
      }

      return SMGAddressValueAndState.of(currentState);
    }

    SMGObject source = sourceStr2Address.getObject();
    SMGObject target = targetStr1Address.getObject();

    long sourceOffset = sourceStr2Address.getOffset().getAsLong();
    long sourceLastCopyBitOffset =
        sizeValue.getAsLong() * machineModel.getSizeofCharInBits() + sourceOffset;
    long targetOffset = targetStr1Address.getOffset().getAsLong();

    if (sourceLastCopyBitOffset > source.getSize()) {
      currentState = currentState.withInvalidRead().withErrorDescription("Overread on memcpy");
    } else if (targetOffset
        > target.getSize() - (sizeValue.getAsLong() * machineModel.getSizeofCharInBits())) {
      currentState = currentState.withInvalidWrite().withErrorDescription("Overwrite on memcpy");
    } else {
      currentState.copy(source, target, sourceOffset, sourceLastCopyBitOffset, targetOffset);
    }

    return SMGAddressValueAndState.of(currentState, targetStr1Address);
  }

  List<? extends SMGValueAndState> handleBuiltinFunctionCall(
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

      case "strcmp":
        return evaluateStrcmp(cFCExpression, newState, pCfaEdge);

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

  /**
   * returns the result of the comparison of the String arguments of the functioncall.
   *
   * @param pFunctionCall contains the parameters for String comparison
   * @param pState the original state (please do not change this state)
   * @param pCfaEdge part of the CFA, mostly for logging and debugging.
   */
  private List<SMGValueAndState> evaluateStrcmp(
      CFunctionCallExpression pFunctionCall, SMGState pState, CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pFunctionCall.getParameterExpressions().size() != 2) {
      throw new UnrecognizedCodeException("Strcmp needs exact 2 arguments", pCfaEdge);
    }

    // extract parameters and the corresponding SMG nodes.
    CExpression firstArgumentExpr =
        pFunctionCall.getParameterExpressions().get(STRCMP_FIRST_PARAMETER);
    CExpression secondArgumentExpr =
        pFunctionCall.getParameterExpressions().get(STRCMP_SECOND_PARAMETER);

    List<SMGValueAndState> result = new ArrayList<>();

    for (SMGAddressValueAndState firstValueAndState :
        evaluateAddress(pState, pCfaEdge, firstArgumentExpr)) {
      for (SMGAddressValueAndState secondValueAndState :
          evaluateAddress(firstValueAndState.getSmgState(), pCfaEdge, secondArgumentExpr)) {
        // iterate over all chars and compare them
        result.add(
            evaluateStrcmp(
                firstValueAndState.getObject(),
                secondValueAndState.getObject(),
                secondValueAndState.getSmgState()));
      }
    }
    return result;
  }

  /**
   * Evaluates 'int strcmp(const char *s1, const char *s2)' for two value and state arguments (c99
   * 7.21.4.2)
   *
   * @param firstSymbolic address of first String
   * @param secondSymbolic address of second String
   * @param pState - current programm state
   * @return SMGUnknownValue if compare fails, SMGZeroValue if equals or
   *     SMGKnownSymValue.valueOf(diff) with difference of the first not equal chars (diff = c1 -
   *     c2)
   */
  private SMGValueAndState evaluateStrcmp(
      SMGAddressValue firstSymbolic, SMGAddressValue secondSymbolic, SMGState pState)
      throws SMGInconsistentException {
    // resolve addresses and perform initial null and unknown check
    if (!allValuesAreDefined(firstSymbolic, secondSymbolic)) {
      return SMGValueAndState.of(pState, SMGUnknownValue.INSTANCE);
    }

    // if equal addresses return zero
    if (firstSymbolic.equals(secondSymbolic)) {
      return SMGValueAndState.of(pState, SMGZeroValue.INSTANCE);
    }

    // get corresponding SMGRegions
    SMGObject firstRegion = firstSymbolic.getObject();
    SMGObject secondRegion = secondSymbolic.getObject();
    if (firstRegion == null || secondRegion == null) {
      return SMGValueAndState.of(pState, SMGUnknownValue.INSTANCE);
    }

    // iterate over both regions as long as chars at a given positions are equal
    int comp = 0;
    int offset = 0;
    SMGState state = pState;

    while (comp == 0) {
      // read symbolic values
      // TODO handle changed state after READ
      SMGValueAndState symFirstValueAndState =
          state.readValue(firstRegion, offset, machineModel.getSizeofCharInBits());
      // TODO handle changed state after READ
      SMGValueAndState symSecondValueAndState =
          symFirstValueAndState
              .getSmgState()
              .readValue(secondRegion, offset, machineModel.getSizeofCharInBits());
      SMGValue symFirstValue = symFirstValueAndState.getObject();
      SMGValue symSecondValue = symSecondValueAndState.getObject();
      state = symSecondValueAndState.getSmgState();
      if (!allValuesAreDefined(symFirstValue, symSecondValue)) {
        return SMGValueAndState.of(pState, SMGUnknownValue.INSTANCE);
      }

      // read explicit values
      // explicit values are necessary to calculate difference between char ascii codes
      SMGExplicitValue expFirstValue = state.getExplicit(symFirstValue);
      SMGExplicitValue expSecondValue = state.getExplicit(symSecondValue);

      if (!allValuesAreDefined(expFirstValue, expSecondValue)) {
        // in case evaluation for explicit values compare symbolic values
        // TODO does this happen?
        if (symFirstValue.equals(symSecondValue)) {
          comp = 0;
        } else {
          return SMGValueAndState.of(pState, SMGUnknownValue.INSTANCE);
        }
      } else {
        // calculate ascii character difference
        comp = expFirstValue.subtract(expSecondValue).getAsInt();
        // if c1='\0' exit loop
        if (expFirstValue.equals(SMGZeroValue.INSTANCE)) {
          break;
        }
      }
      offset += machineModel.getSizeofCharInBits();
    }

    if (comp == 0) {
      return SMGValueAndState.of(state, SMGZeroValue.INSTANCE);
    }

    // create new state with explicit difference assigned to new symbolic value
    // TODO this loooks strange, better return the explicit value directly.
    // it seems that the current interface does not allow this.
    SMGKnownSymbolicValue symbolicResult = SMGKnownSymValue.of();
    SMGState resultState = state.copyOf();
    resultState.putExplicit(symbolicResult, SMGKnownExpValue.valueOf(comp));
    return SMGValueAndState.of(resultState, symbolicResult);
  }

  private boolean allValuesAreDefined(SMGValue... values) {
    for (SMGValue value : values) {
      if (value == null || value.isUnknown()) {
        return false;
      }
    }
    return true;
  }

  List<SMGAddressValueAndState> handleUnknownFunction(
      CFAEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String calledFunctionName,
      SMGState pState)
      throws CPATransferException, AssertionError {
    switch (options.getHandleUnknownFunctions()) {
      case STRICT:
        if (!isSafeFunction(calledFunctionName)) {
          throw new CPATransferException(
              String.format(
                  "Unknown function '%s' may be unsafe. See the cpa.smg.handleUnknownFunctions or"
                      + " cpa.smg.safeUnknownFunctionsPatterns",
                  calledFunctionName));
        }
        // $FALL-THROUGH$ // for safe functions
      case ASSUME_SAFE:
        return ImmutableList.of(SMGAddressValueAndState.of(pState));
      case ASSUME_EXTERNAL_ALLOCATED:
        return expressionEvaluator.handleSafeExternFunction(cFCExpression, pState, pCfaEdge);
      default:
        throw new AssertionError(
            "Unhandled enum value in switch: " + options.getHandleUnknownFunctions());
    }
  }

  private boolean isSafeFunction(String calledFunctionName) {
    if (safeUnknownFunctionCompiledPatterns == null) {
      List<Pattern> list = new ArrayList<>();
      for (String safeUnknownFunctionPattern : options.getSafeUnknownFunctionsPatterns()) {
        list.add(Pattern.compile(safeUnknownFunctionPattern));
      }
      safeUnknownFunctionCompiledPatterns = ImmutableList.copyOf(list);
    }
    for (Pattern safeUnknownFunctionPattern : safeUnknownFunctionCompiledPatterns) {
      if (safeUnknownFunctionPattern.matcher(calledFunctionName).matches()) {
        return true;
      }
    }
    return false;
  }

  public List<? extends SMGValueAndState> handleFunctioncall(
      CFunctionCallExpression pFunctionCall,
      String functionName,
      SMGState pSmgState,
      CFAEdge pCfaEdge,
      SMGTransferRelationKind pKind)
      throws CPATransferException, AssertionError {
    if (isABuiltIn(functionName)) {
      if (isConfigurableAllocationFunction(functionName)) {
        return evaluateConfigurableAllocationFunction(pFunctionCall, pSmgState, pCfaEdge, pKind);
      } else if (functionName.equals("realloc")) {
        return evaluateRealloc();
      } else {
        return handleBuiltinFunctionCall(pCfaEdge, pFunctionCall, functionName, pSmgState, pKind);
      }
    } else {
      return handleUnknownFunction(pCfaEdge, pFunctionCall, functionName, pSmgState);
    }
  }

  private List<SMGAddressValueAndState> evaluateRealloc(
      //      CFunctionCallExpression functionCall,
      //      SMGState pState,
      //      CFAEdge cfaEdge,
      //      SMGTransferRelationKind kind)
      ) throws CPATransferException {
    //      List<SMGAddressValueAndState> result = new ArrayList<>();
    //      evaluateAlloca();
    //      evaluateMemcpy();
    //      evaluateFree();
    throw new CPATransferException("Unhandled realloc function");
    //    return result;
  }
}
