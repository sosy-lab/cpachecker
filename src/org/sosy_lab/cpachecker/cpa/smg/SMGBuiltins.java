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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.Sets;
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

public class SMGBuiltins {

  private final SMGTransferRelation smgTransferRelation;

  private final MachineModel machineModel;
  private final LogManager logger;
  private final SMGExportDotOption exportSMGOptions;
  private final SMGOptions options;

  SMGBuiltins(SMGTransferRelation pSmgTransferRelation, SMGOptions pOptions,
      SMGExportDotOption pExportSMGOptions, MachineModel pMachineModel, LogManager pLogger) {
    smgTransferRelation = pSmgTransferRelation;
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

  public final void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
    String name = functionCall.getParameterExpressions().get(0).toASTString();
    if(exportSMGOptions.hasExportPath() && currentState != null) {
      SMGUtils.dumpSMGPlot(logger, currentState, functionCall.toASTString(), exportSMGOptions.getOutputFilePath(name));
    }
  }

  public final SMGAddressValueAndStateList evaluateMemset(CFunctionCallExpression functionCall,
      SMGState pSMGState, CFAEdge cfaEdge) throws CPATransferException {

    //evaluate function: void *memset( void *buffer, int ch, size_t count );

    CExpression bufferExpr;
    CExpression chExpr;
    CExpression countExpr;

    try {
      bufferExpr = functionCall.getParameterExpressions().get(MEMSET_BUFFER_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Memset buffer argument not found.", cfaEdge, functionCall);
    }

    try {
      chExpr = functionCall.getParameterExpressions().get(MEMSET_CHAR_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Memset ch argument not found.", cfaEdge, functionCall);
    }

    try {
      countExpr = functionCall.getParameterExpressions().get(MEMSET_COUNT_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Memset count argument not found.", cfaEdge, functionCall);
    }

    List<SMGAddressValueAndState> result = new ArrayList<>(4);

    SMGAddressValueAndStateList bufferAddressAndStates = evaluateAddress(pSMGState, cfaEdge, bufferExpr);

    for (SMGAddressValueAndState bufferAddressAndState : bufferAddressAndStates.asAddressValueAndStateList()) {
      SMGState currentState = bufferAddressAndState.getSmgState();

      List<SMGExplicitValueAndState> countValueAndStates = evaluateExplicitValue(currentState, cfaEdge, countExpr);

      for (SMGExplicitValueAndState countValueAndState : countValueAndStates) {
        currentState = countValueAndState.getSmgState();

        SMGValueAndStateList chAndStates = evaluateExpressionValue(currentState,
            cfaEdge, chExpr);

        for (SMGValueAndState chAndState : chAndStates.getValueAndStateList()) {
          currentState = chAndState.getSmgState();

          List<SMGExplicitValueAndState> expValueAndStates = evaluateExplicitValue(currentState, cfaEdge, chExpr);

          for (SMGExplicitValueAndState expValueAndState : expValueAndStates) {

            SMGAddressValueAndState memsetResult =
                evaluateMemset(expValueAndState.getSmgState(), cfaEdge, bufferAddressAndState.getObject(),
                    countValueAndState.getObject(), chAndState.getObject(), expValueAndState.getObject());
            result.add(memsetResult);
          }
        }
      }
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  private final SMGAddressValueAndState evaluateMemset(SMGState currentState, CFAEdge cfaEdge, SMGAddressValue bufferAddress, SMGExplicitValue countValue, SMGSymbolicValue ch, SMGExplicitValue expValue)
          throws CPATransferException {

    if (bufferAddress.isUnknown() || countValue.isUnknown()) {
      currentState = currentState.setInvalidWrite();
      currentState.setErrorDescription("Can't evaluate dst or count for memset");
      return SMGAddressValueAndState.of(currentState);
    }

    int count = countValue.getAsInt();

    if (ch.isUnknown()) {
      // If the symbolic value is not known create a new one.
      ch = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
    }

    SMGObject bufferMemory =  bufferAddress.getObject();

    long offset =  bufferAddress.getOffset().getAsLong();

    if (ch.equals(SMGKnownSymValue.ZERO)) {
      // Create one large edge
      currentState = smgTransferRelation.writeValue(currentState, bufferMemory, offset, count * machineModel.getSizeofCharInBits(), ch, cfaEdge);
    } else {
      // We need to create many edges, one for each character written
      // memset() copies ch into the first count characters of buffer
      for (int c = 0; c < count; c++) {
        currentState = smgTransferRelation.writeValue(currentState, bufferMemory, offset + (c  * machineModel.getSizeofCharInBits()),
            CNumericTypes.SIGNED_CHAR, ch, cfaEdge);
      }

      if (!expValue.isUnknown()) {
        currentState.putExplicit((SMGKnownSymValue) ch, (SMGKnownExpValue) expValue);
      }
    }

    return SMGAddressValueAndState.of(currentState, bufferAddress);
  }

  protected SMGValueAndStateList evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge, CExpression rValue)
      throws CPATransferException {

    return smgTransferRelation.expressionEvaluator.evaluateExpressionValue(smgState, cfaEdge, rValue);
  }

  protected List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
      throws CPATransferException {

    return smgTransferRelation.expressionEvaluator.evaluateExplicitValue(pState, pCfaEdge, pRValue);
  }

  protected SMGAddressValueAndStateList evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue) throws CPATransferException {
    return smgTransferRelation.expressionEvaluator.evaluateAddress(pState, pCfaEdge, pRvalue);
  }

  public final SMGAddressValueAndStateList evaluateExternalAllocation(
      CFunctionCallExpression pFunctionCall, SMGState pState) {
    SMGState currentState = pState;

    String functionName = pFunctionCall.getFunctionNameExpression().toASTString();

    List<SMGAddressValueAndState> result = new ArrayList<>();

    // TODO line numbers are not unique when we have multiple input files!
    String allocation_label = functionName + "_ID" + SMGValueFactory.getNewValue() + "_Line:"
        + pFunctionCall.getFileLocation().getStartingLineNumber();
    SMGAddressValue new_address = currentState.addExternalAllocation(allocation_label);

    result.add(SMGAddressValueAndState.of(currentState, new_address));

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }
  /** The method "alloca" (or "__builtin_alloca") allocates memory from the stack.
   * The memory is automatically freed at function-exit.
   */
   // TODO possible property violation "stack-overflow through big allocation" is not handled
  public final SMGAddressValueAndStateList evaluateAlloca(CFunctionCallExpression functionCall,
      SMGState pState, CFAEdge cfaEdge) throws CPATransferException {
    CRightHandSide sizeExpr;
    SMGState currentState = pState;

    try {
      sizeExpr = functionCall.getParameterExpressions().get(MALLOC_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("alloca argument not found.", cfaEdge, functionCall);
    }

    List<SMGExplicitValueAndState> valueAndStates = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

    List<SMGAddressValueAndState> result = new ArrayList<>(valueAndStates.size());

    for (SMGExplicitValueAndState valueAndState : valueAndStates) {
      result.add(evaluateAlloca(valueAndState.getSmgState(), valueAndState.getObject(), cfaEdge, sizeExpr));
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  private SMGAddressValueAndState evaluateAlloca(SMGState currentState, SMGExplicitValue pSizeValue, CFAEdge cfaEdge, CRightHandSide sizeExpr) throws CPATransferException {

    SMGExplicitValue sizeValue = pSizeValue;

    if (sizeValue.isUnknown()) {

      if (options.isGuessSizeOfUnknownMemorySize()) {
        SMGExplicitValueAndState forcedValueAndState =
            smgTransferRelation.expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);
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

          if(smgTransferRelation.kind == SMGTransferRelationKind.REFINEMENT) {
            sizeValue = SMGKnownExpValue.ZERO;
          } else {
            throw new UnrecognizedCCodeException(
                "Not able to compute allocation size", cfaEdge);
          }
        }
      } else {
        if (smgTransferRelation.kind == SMGTransferRelationKind.REFINEMENT) {
          sizeValue = SMGKnownExpValue.ZERO;
        } else {
          throw new UnrecognizedCCodeException(
              "Not able to compute allocation size", cfaEdge);
        }
      }
    }

    // TODO line numbers are not unique when we have multiple input files!
    String allocation_label = "alloc_ID" + SMGValueFactory.getNewValue();
    SMGAddressValue addressValue = currentState.addNewStackAllocation(sizeValue.getAsInt() *
        machineModel.getSizeofCharInBits(), allocation_label);

    smgTransferRelation.possibleMallocFail = true;
    return SMGAddressValueAndState.of(currentState, addressValue);
  }

  private List<SMGExplicitValueAndState> getAllocateFunctionSize(SMGState pState, CFAEdge cfaEdge,
      CFunctionCallExpression functionCall) throws CPATransferException {

    String functionName = functionCall.getFunctionNameExpression().toASTString();

    if (options.getArrayAllocationFunctions().contains(functionName)) {

      List<SMGExplicitValueAndState> result = new ArrayList<>(4);

      List<SMGExplicitValueAndState> numValueAndStates =
          getAllocateFunctionParameter(options.getMemoryArrayAllocationFunctionsNumParameter(),
              functionCall, pState, cfaEdge);

      for (SMGExplicitValueAndState numValueAndState : numValueAndStates) {
        List<SMGExplicitValueAndState> elemSizeValueAndStates =
            getAllocateFunctionParameter(options.getMemoryArrayAllocationFunctionsElemSizeParameter(),
                functionCall, numValueAndState.getSmgState(), cfaEdge);

        for (SMGExplicitValueAndState elemSizeValueAndState : elemSizeValueAndStates) {

          SMGExplicitValue size = numValueAndState.getObject().multiply(elemSizeValueAndState.getObject());
          result.add(SMGExplicitValueAndState.of(elemSizeValueAndState.getSmgState(), size));
        }
      }

      return result;
    } else {
      return getAllocateFunctionParameter(options.getMemoryAllocationFunctionsSizeParameter(),
          functionCall, pState, cfaEdge);
    }
  }

  private List<SMGExplicitValueAndState> getAllocateFunctionParameter(int pParameterNumber, CFunctionCallExpression functionCall,
      SMGState pState, CFAEdge cfaEdge) throws CPATransferException {
    CRightHandSide sizeExpr;
    SMGState currentState = pState;
    String functionName = functionCall.getFunctionNameExpression().toASTString();
    try {
      sizeExpr = functionCall.getParameterExpressions().get(pParameterNumber);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException(functionName + " argument #" + pParameterNumber + " not found.", cfaEdge, functionCall);
    }

    List<SMGExplicitValueAndState> valueAndStates = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

    List<SMGExplicitValueAndState> result = new ArrayList<>(valueAndStates.size());

    for (SMGExplicitValueAndState valueAndState : valueAndStates) {
      SMGExplicitValueAndState resultValueAndState = valueAndState;
      SMGExplicitValue value = valueAndState.getObject();

      if (value.isUnknown()) {

        if (options.isGuessSizeOfUnknownMemorySize()) {
          currentState = valueAndState.getSmgState();
          SMGExplicitValueAndState forcedValueAndState = smgTransferRelation.expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);


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
            if (smgTransferRelation.kind == SMGTransferRelationKind.REFINEMENT) {
              resultValueAndState = SMGExplicitValueAndState.of(currentState ,SMGKnownExpValue.ZERO);
            } else {
              throw new UnrecognizedCCodeException(
                  "Not able to compute allocation size", cfaEdge);
            }
          }
        } else {
          if (smgTransferRelation.kind == SMGTransferRelationKind.REFINEMENT) {
            resultValueAndState = SMGExplicitValueAndState.of(currentState, SMGKnownExpValue.ZERO);
          } else {
            throw new UnrecognizedCCodeException(
                "Not able to compute allocation size", cfaEdge);
          }
        }
      }
      result.add(resultValueAndState);
    }

    return result;
  }

  public SMGAddressValueAndStateList evaluateConfigurableAllocationFunction(
      CFunctionCallExpression functionCall,
      SMGState pState, CFAEdge cfaEdge) throws CPATransferException {
    SMGState currentState = pState;

    String functionName = functionCall.getFunctionNameExpression().toASTString();

    List<SMGExplicitValueAndState> sizeAndStates = getAllocateFunctionSize(currentState, cfaEdge, functionCall);
    List<SMGAddressValueAndState> result = new ArrayList<>(sizeAndStates.size());

    for (SMGExplicitValueAndState sizeAndState : sizeAndStates) {

      int size = sizeAndState.getObject().getAsInt();
      currentState = sizeAndState.getSmgState();

      // TODO line numbers are not unique when we have multiple input files!
      String allocation_label = functionName + "_ID" + SMGValueFactory.getNewValue() + "_Line:"
          + functionCall.getFileLocation().getStartingLineNumber();
      SMGAddressValue new_address = currentState.addNewHeapAllocation(size * machineModel.getSizeofCharInBits(),
          allocation_label);

      if (options.getZeroingMemoryAllocation().contains(functionName)) {
        currentState = smgTransferRelation.writeValue(currentState, new_address.getObject(), 0, AnonymousTypes.createTypeWithLength(size * machineModel.getSizeofCharInBits()),
            SMGKnownSymValue.ZERO, cfaEdge);
      }
      smgTransferRelation.possibleMallocFail = true;
      result.add(SMGAddressValueAndState.of(currentState, new_address));
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  public final List<SMGState> evaluateFree(CFunctionCallExpression pFunctionCall, SMGState pState,
      CFAEdge cfaEdge) throws CPATransferException {
    CExpression pointerExp;

    try {
      pointerExp = pFunctionCall.getParameterExpressions().get(0);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Built-in free(): No parameter passed", cfaEdge, pFunctionCall);
    }

    SMGAddressValueAndStateList addressAndStates = smgTransferRelation.expressionEvaluator.evaluateAddress(pState, cfaEdge, pointerExp);

    List<SMGState> resultStates = new ArrayList<>(addressAndStates.size());

    for (SMGAddressValueAndState addressAndState : addressAndStates.asAddressValueAndStateList()) {
      SMGAddressValue address = addressAndState.getObject();
      SMGState currentState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        logger.log(Level.INFO, "Free on expression ", pointerExp.toASTString(),
            " is invalid, because the target of the address could not be calculated.");
        SMGState invalidFreeState = currentState.setInvalidFree();
        invalidFreeState.setErrorDescription("Free on expression " + pointerExp.toASTString() +
            " is invalid, because the target of the address could not be calculated.");
        resultStates.add(invalidFreeState);
        continue;
      }

      if (address.getAsInt() == 0) {
        logger.log(Level.INFO, pFunctionCall.getFileLocation(), ":",
            "The argument of a free invocation:", cfaEdge.getRawStatement(), "is 0");

      } else {
        currentState = currentState.free(address.getAsInt(), address.getOffset().getAsInt(), address.getObject());
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

  public SMGAddressValueAndStateList evaluateMemcpy(CFunctionCallExpression pFunctionCall,
      SMGState pSmgState, CFAEdge pCfaEdge) throws CPATransferException {

    //evaluate function: void *memcpy(void *str1, const void *str2, size_t n)

    CExpression targetStr1Expr;
    CExpression sourceStr2Expr;
    CExpression sizeExpr;

    try {
      targetStr1Expr = pFunctionCall.getParameterExpressions().get(MEMCPY_TARGET_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Memcpy target argument not found.", pCfaEdge, pFunctionCall);
    }

    try {
      sourceStr2Expr = pFunctionCall.getParameterExpressions().get(MEMCPY_SOURCE_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Memcpy source argument not found.", pCfaEdge, pFunctionCall);
    }

    try {
      sizeExpr = pFunctionCall.getParameterExpressions().get(MEMCPY_SIZE_PARAMETER);
    } catch (IndexOutOfBoundsException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Memcpy count argument not found.", pCfaEdge, pFunctionCall);
    }

    List<SMGAddressValueAndState> result = new ArrayList<>(4);

    SMGAddressValueAndStateList targetStr1AndStates = evaluateAddress(pSmgState, pCfaEdge, targetStr1Expr);

    for (SMGAddressValueAndState targetStr1AndState : targetStr1AndStates.asAddressValueAndStateList()) {
      SMGState currentState = targetStr1AndState.getSmgState();

      SMGAddressValueAndStateList sourceStr2AndStates = evaluateAddress(currentState, pCfaEdge, sourceStr2Expr);

      for (SMGAddressValueAndState sourceStr2AndState : sourceStr2AndStates.asAddressValueAndStateList()) {
        currentState = sourceStr2AndState.getSmgState();

        List<SMGExplicitValueAndState> sizeValueAndStates = evaluateExplicitValue(currentState, pCfaEdge, sizeExpr);

        for (SMGExplicitValueAndState sizeValueAndState : sizeValueAndStates) {
          currentState = sizeValueAndState.getSmgState();
          SMGAddressValue targetObject = targetStr1AndState.getObject();
          SMGAddressValue sourceObject = sourceStr2AndState.getObject();
          SMGExplicitValue explicitSizeValue = sizeValueAndState.getObject();
          if (!targetObject.isUnknown() && !sourceObject.isUnknown()) {
            SMGValueAndStateList sizeSymbolicValueAndStates =
                evaluateExpressionValue(currentState, pCfaEdge, sizeExpr);
            int symbolicValueSize = smgTransferRelation.expressionEvaluator.getBitSizeof(pCfaEdge,
                sizeExpr.getExpressionType(), currentState);
            for (SMGValueAndState sizeSymbolicValueAndState : sizeSymbolicValueAndStates
                .getValueAndStateList()) {
              SMGSymbolicValue symbolicValue = sizeSymbolicValueAndState.getObject();

              int sourceRangeOffset = sourceObject.getOffset().getAsInt() / machineModel.getSizeofCharInBits();
              int sourceSize = sourceObject.getObject().getSize() / machineModel.getSizeofCharInBits();
              int availableSource = sourceSize - sourceRangeOffset;

              int targetRangeOffset = targetObject.getOffset().getAsInt() / machineModel.getSizeofCharInBits();
              int targetSize = targetObject.getObject().getSize() / machineModel.getSizeofCharInBits();
              int availableTarget = targetSize - targetRangeOffset;

              if (explicitSizeValue.isUnknown()) {
                if (!currentState.isObjectExternallyAllocated(sourceObject.getObject())) {
                  currentState.addErrorPredicate(symbolicValue, symbolicValueSize, SMGKnownExpValue
                      .valueOf(availableSource), symbolicValueSize, pCfaEdge);
                }
                if (!currentState.isObjectExternallyAllocated(targetObject.getObject())) {
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

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
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
          currentState = currentState.setInvalidWrite();
          currentState = currentState.setInvalidRead();
          currentState.setErrorDescription("Can't evaluate memcpy dst and src");
        } else if (targetStr1Address.isUnknown()) {
          currentState = currentState.setInvalidWrite();
          currentState.setErrorDescription("Can't evaluate memcpy dst");
        } else {
          currentState = currentState.setInvalidRead();
          currentState.setErrorDescription("Can't evaluate memcpy src");
        }
      }
      if (!sourceStr2Address.isUnknown() && sourceStr2Address.getObject().equals(SMGNullObject.INSTANCE)) {
        currentState = currentState.setInvalidRead();
        currentState.setErrorDescription("Memcpy src is null pointer");
      }
      if (!targetStr1Address.isUnknown() && targetStr1Address.getObject().equals(SMGNullObject.INSTANCE)) {
        currentState = currentState.setInvalidWrite();
        currentState.setErrorDescription("Memcpy to null pointer dst");
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
      currentState = currentState.setInvalidRead();
      currentState.setErrorDescription("Overread on memcpy");
    } else if (targetOffset > target.getSize() - (sizeValue.getAsLong() * machineModel
        .getSizeofCharInBits())) {
      currentState = currentState.setInvalidWrite();
      currentState.setErrorDescription("Overwrite on memcpy");
    } else {
      currentState.copy(source, target, sourceOffset, sourceLastCopyBitOffset, targetOffset);
    }

    return SMGAddressValueAndState.of(currentState, targetStr1Address);
  }
}