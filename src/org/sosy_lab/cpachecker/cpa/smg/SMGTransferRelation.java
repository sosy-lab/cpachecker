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
package org.sosy_lab.cpachecker.cpa.smg;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.AssumeVisitor;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.LValueAssignmentVisitor;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisSMGCommunicator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.ImmutableSet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@Options(prefix = "cpa.smg")
public class SMGTransferRelation extends SingleEdgeTransferRelation {

  @Option(secure=true, name = "exportSMG.file", description = "Filename format for SMG graph dumps")

  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate exportSMGFilePattern = PathTemplate.ofFormatString("smg-%s.dot");

  @Option(secure=true, description = "with this option enabled, a check for unreachable memory occurs whenever a function returns, and not only at the end of the main function")
  private boolean checkForMemLeaksAtEveryFrameDrop = true;

  @Option(secure=true, description = "with this option enabled, memory that is not freed before the end of main is reported as memleak even if it is reachable from local variables in main")
  private boolean handleNonFreedMemoryInMainAsMemLeak = false;

  @Option(secure=true, name = "exportSMGwhen", description = "Describes when SMG graphs should be dumped. One of: {never, leaf, interesting, every}")
  private String exportSMG = "never";

  @Option(secure=true, name="enableMallocFail", description = "If this Option is enabled, failure of malloc" + "is simulated")
  private boolean enableMallocFailure = true;

  @Option(secure=true, name="handleUnknownFunctions", description = "Sets how unknown functions are handled. One of: {strict, assume_safe}")
  private String handleUnknownFunctions = "strict";

  @Option(secure=true, name="guessSizeOfUnknownMemorySize", description = "Size of memory that cannot be calculated will be guessed.")
  private boolean guessSizeOfUnknownMemorySize = false;

  @Option(secure=true, name="memoryAllocationFunctions", description = "Memory allocation functions")
  private ImmutableSet<String> memoryAllocationFunctions = ImmutableSet.of(
      "malloc");

  @Option(secure=true, name="memoryAllocationFunctionsSizeParameter", description = "Size parameter of memory allocation functions")
  private int memoryAllocationFunctionsSizeParameter = 0;

  @Option(secure=true, name="arrayAllocationFunctions", description = "Array allocation functions")
  private ImmutableSet<String> arrayAllocationFunctions = ImmutableSet.of(
      "calloc");

  @Option(secure=true, name="memoryArrayAllocationFunctionsNumParameter", description = "Position of number of element parameter for array allocation functions")
  private int memoryArrayAllocationFunctionsNumParameter = 0;

  @Option(secure=true, name="memoryArrayAllocationFunctionsElemSizeParameter", description = "Position of element size parameter for array allocation functions")
  private int memoryArrayAllocationFunctionsElemSizeParameter = 1;

  @Option(secure=true, name="zeroingMemoryAllocation", description = "Allocation functions which set memory to zero")
  private ImmutableSet<String> zeroingMemoryAllocation = ImmutableSet.of(
      "calloc");

  @Option(secure=true, name="deallocationFunctions", description = "Deallocation functions")
  private ImmutableSet<String> deallocationFunctions = ImmutableSet.of(
      "free");

  final private LogManagerWithoutDuplicates logger;
  final private MachineModel machineModel;
  private final AtomicInteger id_counter;

  private final SMGRightHandSideEvaluator expressionEvaluator;

  /**
   * Indicates whether the executed statement could result
   * in a failure of the malloc function.
   */
  private boolean possibleMallocFail;

  /**
   * Contains the alternate fail State.
   */
  private SMGState mallocFailState;

  /**
   * This List is used to communicate the missing
   * Information needed from other cpas.
   * (at the moment specifically SMG)
   */
  private List<MissingInformation> missingInformationList;

  /**
   * Save the old State for strengthen.
   */
  private SMGState oldState;

  /**
   * name for the special variable used as container for return values of functions
   */
  public static final String FUNCTION_RETURN_VAR = "___cpa_temp_result_var_";

  private class SMGBuiltins {

    private static final int MEMSET_BUFFER_PARAMETER = 0;
    private static final int MEMSET_CHAR_PARAMETER = 1;
    private static final int MEMSET_COUNT_PARAMETER = 2;
    private static final int MALLOC_PARAMETER = 0;

    private final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
        new String[] {
            "__VERIFIER_BUILTIN_PLOT",
            "memset",
            "__builtin_alloca",
            //TODO: Properly model printf (dereferences and stuff)
            //TODO: General modelling system for functions which do not modify state?
            "printf",
        }));

    private void dumpSMGPlot(String name, SMGState currentState, String location) {
      if (exportSMGFilePattern != null && currentState != null) {
        if (name == null) {
          if (currentState.getPredecessorId() == 0) {
            name = String.format("initial-%03d", currentState.getId());
          } else {
            name = String.format("%03d-%03d-%03d", currentState.getPredecessorId(), currentState.getId(), id_counter.getAndIncrement());
          }
        }
        name = name.replace("\"", "");
        Path outputFile = getOutputFile(exportSMGFilePattern, name);
        try {
          String dot = getDot(currentState, name, location);
          Files.writeFile(outputFile, dot);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write SMG " + name + " to file");
        }
      }
    }

    protected Path getOutputFile(PathTemplate pExportSMGFilePattern, String pName) {
      return pExportSMGFilePattern.getPath(pName);
    }

    protected String getDot(SMGState pCurrentState, String pName, String pLocation) {
      return pCurrentState.toDot(pName, pLocation);
    }

    public final void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
      String name = functionCall.getParameterExpressions().get(0).toASTString();
      dumpSMGPlot(name, currentState, functionCall.toString());
    }

    public final SMGEdgePointsToAndState evaluateMemset(CFunctionCallExpression functionCall,
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

      SMGAddressValueAndState bufferAddressAndState = evaluateAddress(pSMGState, cfaEdge, bufferExpr);
      SMGAddressValue bufferAddress = bufferAddressAndState.getValue();
      SMGState currentState = bufferAddressAndState.getSmgState();

      SMGExplicitValueAndState countValueAndState = evaluateExplicitValue(currentState, cfaEdge, countExpr);
      SMGExplicitValue countValue = countValueAndState.getValue();
      currentState = bufferAddressAndState.getSmgState();

      if (bufferAddress.isUnknown() || countValue.isUnknown()) {
        return SMGEdgePointsToAndState.of(currentState, null);
      }

      // avoid to call getPointerFromValue for memset(&param, val, size) while pointer is not registered as Edge
      if (!currentState.isPointer(bufferAddress.getAsInt())) {
        currentState.addPointsToEdge(bufferAddress.getObject(), bufferAddress.getOffset().getAsInt(), bufferAddress.getValue().intValue());
      }
      SMGEdgePointsTo pointer = currentState.getPointerFromValue(bufferAddress.getAsInt());

      long count = countValue.getAsLong();

      SMGObject bufferMemory =  bufferAddress.getObject();

      int offset =  bufferAddress.getOffset().getAsInt();

      SMGSymbolicValue ch;

        // TODO write explicit Value into smg
        SMGValueAndState chAndState = evaluateExpressionValue(currentState,
            cfaEdge, chExpr);
        ch = chAndState.getValue();
        currentState = chAndState.getSmgState();

      if (ch.isUnknown()) {
        // If the symbolic value is not known create a new one.
        ch = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
      }

      SMGExpressionEvaluator expEvaluator = new SMGExpressionEvaluator(logger, machineModel);

      SMGExplicitValueAndState expValueAndState = expEvaluator.evaluateExplicitValue(currentState, cfaEdge, chExpr);
      SMGExplicitValue expValue = expValueAndState.getValue();
      currentState = expValueAndState.getSmgState();

      if (ch.equals(SMGKnownSymValue.ZERO)) {
        // Create one large edge
        currentState = writeValue(currentState, bufferMemory, offset, count, ch, cfaEdge);
      } else {
        // We need to create many edges, one for each character written
        // memset() copies ch into the first count characters of buffer
        for (int c = 0; c < count; c++) {
          currentState = writeValue(currentState, bufferMemory, offset + c, CNumericTypes.SIGNED_CHAR, ch, cfaEdge);
        }

        if (!expValue.isUnknown()) {
          currentState.putExplicit((SMGKnownSymValue) ch, (SMGKnownExpValue) expValue);
        }
      }

      return SMGEdgePointsToAndState.of(currentState, pointer);
    }

    protected SMGValueAndState evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge, CExpression rValue)
        throws CPATransferException {

      return expressionEvaluator.evaluateExpressionValue(smgState, cfaEdge, rValue);
    }

    protected SMGExplicitValueAndState evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      return expressionEvaluator.evaluateExplicitValue(pState, pCfaEdge, pRValue);
    }

    protected SMGAddressValueAndState evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue) throws CPATransferException {
      return expressionEvaluator.evaluateAddress(pState, pCfaEdge, pRvalue);
    }

    public final SMGEdgePointsToAndState evaluateAlloc(CFunctionCallExpression functionCall,
        SMGState pState, CFAEdge cfaEdge) throws CPATransferException {
      CRightHandSide sizeExpr;
      SMGState currentState = pState;

      try {
        sizeExpr = functionCall.getParameterExpressions().get(MALLOC_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        logger.logDebugException(e);
        throw new UnrecognizedCCodeException("alloca argument not found.", cfaEdge, functionCall);
      }

      SMGExplicitValueAndState valueAndState = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);
      SMGExplicitValue value = valueAndState.getValue();
      currentState = valueAndState.getSmgState();

      if (value.isUnknown()) {

        if (guessSizeOfUnknownMemorySize) {
          SMGExplicitValueAndState forcedValueAndState = expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);
          currentState = forcedValueAndState.getSmgState();

          //Sanity check

          valueAndState = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);
          value = valueAndState.getValue();
          currentState = valueAndState.getSmgState();

          if(value.isUnknown()) {
            throw new UnrecognizedCCodeException(
                "Not able to compute allocation size", cfaEdge);
          }
        } else {
          throw new UnrecognizedCCodeException(
              "Not able to compute allocation size", cfaEdge);
        }
      }

      // TODO line numbers are not unique when we have multiple input files!
      String allocation_label = "alloc_ID" + SMGValueFactory.getNewValue();
      SMGEdgePointsTo new_pointer = currentState.addNewAllocAllocation(value.getAsInt(), allocation_label);

      possibleMallocFail = true;
      return SMGEdgePointsToAndState.of(currentState, new_pointer);
    }


    private SMGExplicitValue getAllocateFunctionParameter(int pParameterNumber, CFunctionCallExpression functionCall,
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

      SMGExplicitValueAndState valueAndState = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);
      SMGExplicitValue value = valueAndState.getValue();
      currentState = valueAndState.getSmgState();

      if (value.isUnknown()) {

        if (guessSizeOfUnknownMemorySize) {
          SMGExplicitValueAndState forcedValueAndState = expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);
          currentState = forcedValueAndState.getSmgState();

          //Sanity check

          valueAndState = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);
          value = valueAndState.getValue();
          currentState = valueAndState.getSmgState();

          if(value.isUnknown()) {
            throw new UnrecognizedCCodeException(
                "Not able to compute allocation size", cfaEdge);
          }
        } else {
          throw new UnrecognizedCCodeException(
              "Not able to compute allocation size", cfaEdge);
        }
      }
      return value;
    }

    public SMGEdgePointsToAndState evaluateConfigurableAllocationFunction(
        CFunctionCallExpression functionCall,
        SMGState pState, CFAEdge cfaEdge) throws CPATransferException {
      int size;
      SMGState currentState = pState;

      String functionName = functionCall.getFunctionNameExpression().toASTString();

      if (arrayAllocationFunctions.contains(functionName)) {
        SMGExplicitValue numValue = getAllocateFunctionParameter(memoryArrayAllocationFunctionsNumParameter,
            functionCall, pState, cfaEdge);
        SMGExplicitValue elemSizeValue = getAllocateFunctionParameter(memoryArrayAllocationFunctionsElemSizeParameter,
            functionCall, pState, cfaEdge);
        size = numValue.getAsInt() * elemSizeValue.getAsInt();
      } else {
        SMGExplicitValue sizeValue = getAllocateFunctionParameter(memoryAllocationFunctionsSizeParameter,
            functionCall, pState, cfaEdge);
        size = sizeValue.getAsInt();
      }

      // TODO line numbers are not unique when we have multiple input files!
      String allocation_label = functionName + "_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(size, allocation_label);

      if (zeroingMemoryAllocation.contains(functionName)) {
        currentState = writeValue(currentState, new_pointer.getObject(), 0, AnonymousTypes.createTypeWithLength(size), SMGKnownSymValue.ZERO, cfaEdge);
      }
      possibleMallocFail = true;
      return SMGEdgePointsToAndState.of(currentState, new_pointer);

    }

    public final SMGState evaluateFree(CFunctionCallExpression pFunctionCall, SMGState pState,
        CFAEdge cfaEdge) throws CPATransferException {
      CExpression pointerExp;

      try {
        pointerExp = pFunctionCall.getParameterExpressions().get(0);
      } catch (IndexOutOfBoundsException e) {
        logger.logDebugException(e);
        throw new UnrecognizedCCodeException("Built-in free(): No parameter passed", cfaEdge, pFunctionCall);
      }

      SMGAddressValueAndState addressAndState = expressionEvaluator.evaluateAddress(pState, cfaEdge, pointerExp);
      SMGAddressValue address = addressAndState.getValue();
      SMGState currentState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        return currentState.setInvalidFree();
      }

      SMGEdgePointsTo pointer;

      if (currentState.isPointer(address.getAsInt())) {
        pointer = currentState.getPointerFromValue(address.getAsInt());
      } else {
        pointer = new SMGEdgePointsTo(address.getAsInt(), address.getObject(), address.getOffset().getAsInt());
      }

      if (address.getAsInt() == 0) {
        logger.log(Level.WARNING, pFunctionCall.getFileLocation() + ":",
            "The argument of a free invocation:", cfaEdge.getRawStatement(), "is 0");

      } else {
        currentState = currentState.free(pointer.getValue(), pointer.getOffset(), pointer.getObject());
      }

      return currentState;
    }

    public final boolean isABuiltIn(String functionName) {
      return (BUILTINS.contains(functionName) || isNondetBuiltin(functionName) ||
          isConfigurableAllocationFunction(functionName) || isDeallocationFunction(functionName));
    }

    private static final String NONDET_PREFIX = "__VERIFIER_nondet_";
    private boolean isNondetBuiltin(String pFunctionName) {
      return pFunctionName.startsWith(NONDET_PREFIX) || pFunctionName.equals("nondet_int");
    }

    public boolean isConfigurableAllocationFunction(String functionName) {
      return memoryAllocationFunctions.contains(functionName) || arrayAllocationFunctions.contains(functionName);
    }

    public boolean isDeallocationFunction(String functionName) {
      return deallocationFunctions.contains(functionName);
    }
  }

  final private SMGBuiltins builtins = new SMGBuiltins();

  private void plotWhenConfigured(String pConfig, String pName, SMGState pState, String pLocation) {
    //TODO: A variation for more pConfigs

    if (pConfig.equals(exportSMG)) {
      builtins.dumpSMGPlot(pName, pState, pLocation);
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
          throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> results;

    if(cfaEdge instanceof MultiEdge) {

      MultiEdge multiEdge = (MultiEdge) cfaEdge;

      Queue<SMGState> processQueue = new ArrayDeque<>();
      Queue<SMGState> resultQueue = new ArrayDeque<>();
      processQueue.add((SMGState) state);

      for(CFAEdge edge : multiEdge) {

        while(!processQueue.isEmpty()) {
          SMGState next = processQueue.poll();
          Collection<? extends AbstractState> resultOfOneOp = getAbstractSuccessorsForEdge(next, precision, edge);

          for(AbstractState result : resultOfOneOp) {
            resultQueue.add((SMGState) result);
          }
        }

        while(!resultQueue.isEmpty()) {
          processQueue.add(resultQueue.poll());
        }
      }

      results = ImmutableSet.copyOf(processQueue);
    } else {
      results = getAbstractSuccessorsForEdge((SMGState)state, precision, cfaEdge);
    }

    return results;
  }

  public SMGTransferRelation(Configuration config, LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    config.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    expressionEvaluator = new SMGRightHandSideEvaluator(logger, machineModel);
    id_counter = new AtomicInteger(0);
  }

  private Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      SMGState state, Precision precision, CFAEdge cfaEdge)
          throws CPATransferException {
    logger.log(Level.FINEST, "SMG GetSuccessor >>");
    logger.log(Level.FINEST, "Edge:", cfaEdge.getEdgeType());
    logger.log(Level.FINEST, "Code:", cfaEdge.getCode());

    SMGState successor;

    SMGState smgState = state;

    setInfo(smgState);

    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      successor = handleDeclaration(smgState, (CDeclarationEdge) cfaEdge);
      break;

    case StatementEdge:
      successor = handleStatement(smgState, (CStatementEdge) cfaEdge);
      plotWhenConfigured("interesting", null, successor, cfaEdge.getDescription());
      break;

      // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      successor = handleAssumption(smgState, assumeEdge.getExpression(),
          cfaEdge, assumeEdge.getTruthAssumption(), true);
      plotWhenConfigured("interesting", null, successor, cfaEdge.getDescription());
      break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(smgState, functionCallEdge);
      plotWhenConfigured("interesting", null, successor, cfaEdge.getDescription());
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(smgState, functionReturnEdge);
      if (checkForMemLeaksAtEveryFrameDrop) {
        successor.pruneUnreachable();
      }
      plotWhenConfigured("interesting", null, successor, cfaEdge.getDescription());
      break;

    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      successor = handleExitFromFunction(smgState, returnEdge);

      // if this is the entry function, there is no FunctionReturnEdge
      // so we have to check for memleaks here
      if (returnEdge.getSuccessor().getNumLeavingEdges() == 0) {
        // Ugly, but I do not know how to do better
        // TODO: Handle leaks at any program exit point (abort, etc.)

        if (handleNonFreedMemoryInMainAsMemLeak) {
          successor.dropStackFrame();
        }
        successor.pruneUnreachable();
      }
      plotWhenConfigured("interesting", null, successor, cfaEdge.getDescription());
      break;

    default:
      successor = smgState;
    }

    Collection<SMGState> result;

    if (successor == null) {
      result = Collections.emptySet();
    } else if (mallocFailState != null && enableMallocFailure) {
      // Return a successor for malloc succeeding, and one for malloc failing.
      result = ImmutableSet.of(successor, mallocFailState);
      mallocFailState = null;
    } else {
      result = Collections.singleton(successor);
    }

    for (SMGState smg : result) {
      plotWhenConfigured("every", null, smg, cfaEdge.getDescription());
    }

    return result;
  }

  private void setInfo(SMGState pOldState) {
    missingInformationList = new ArrayList<>(5);
    oldState = pOldState;
    expressionEvaluator.reset();
  }

  private SMGState handleExitFromFunction(SMGState smgState,
      CReturnStatementEdge returnEdge) throws CPATransferException {

    CExpression returnExp = returnEdge.getExpression().or(CIntegerLiteralExpression.ZERO); // 0 is the default in C

    logger.log(Level.FINEST, "Handling return Statement: ", returnExp);

    CType expType = expressionEvaluator.getRealExpressionType(returnExp);
    SMGObject tmpFieldMemory = smgState.getFunctionReturnObject();

    if (tmpFieldMemory != null) {
      return handleAssignmentToField(smgState, returnEdge, tmpFieldMemory, 0, expType, returnExp);
    }

    return smgState;
  }

  private SMGState handleFunctionReturn(SMGState smgState,
      CFunctionReturnEdge functionReturnEdge) throws CPATransferException {

    logger.log(Level.FINEST, "Handling function return");

    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall exprOnSummary = summaryEdge.getExpression();

    SMGState newState = new SMGState(smgState);

    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {

      // Assign the return value to the lValue of the functionCallAssignment

      CExpression lValue = ((CFunctionCallAssignmentStatement) exprOnSummary).getLeftHandSide();

      CType lValueType = expressionEvaluator.getRealExpressionType(lValue);

      CType rValueType = expressionEvaluator.getRealExpressionType(((CFunctionCallAssignmentStatement) exprOnSummary).getRightHandSide());

      SMGSymbolicValue rValue = getFunctionReturnValue(newState, rValueType, functionReturnEdge);

      SMGAddress address = null;

      // Lvalue is one frame above
      newState.dropStackFrame();
      LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(functionReturnEdge, newState);

      SMGAddressAndState addressAndValue = lValue.accept(visitor);
      address = addressAndValue.getAddress();
      newState = addressAndValue.getSmgState();

      if (!address.isUnknown()) {

        if (rValue.isUnknown()) {
          rValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
        }

        SMGObject object = address.getObject();

        int offset = address.getOffset().getAsInt();

        return assignFieldToState(newState, functionReturnEdge, object, offset, lValueType, rValue, rValueType);
      } else {
        //TODO missingInformation, exception
        return newState;
      }
    } else {
      newState.dropStackFrame();
      return newState;
    }
  }

  private SMGSymbolicValue getFunctionReturnValue(SMGState smgState, CType type, CFAEdge pCFAEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    SMGObject tmpMemory = smgState.getFunctionReturnObject();

    return expressionEvaluator.readValue(smgState, tmpMemory, SMGKnownExpValue.ZERO, type, pCFAEdge).getValue();
  }

  private SMGState handleFunctionCall(SMGState smgState, CFunctionCallEdge callEdge)
      throws CPATransferException, SMGInconsistentException  {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    logger.log(Level.FINEST, "Handling function call: ", functionEntryNode.getFunctionName());

    SMGState newState = new SMGState(smgState);

    CFunctionDeclaration functionDeclaration = functionEntryNode.getFunctionDefinition();

    List<CParameterDeclaration> paramDecl = functionEntryNode.getFunctionParameters();
    List<? extends CExpression> arguments = callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      //TODO Parameter with varArgs
      assert (paramDecl.size() == arguments.size());
    }

    List<Pair<SMGRegion,SMGSymbolicValue>> values = new ArrayList<>(paramDecl.size());

    //TODO Refactor, ugly

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramDecl.size(); i++) {

      CExpression exp = arguments.get(i);

      String varName = paramDecl.get(i).getName();
      CType cType = expressionEvaluator.getRealExpressionType(paramDecl.get(i));


      SMGRegion paramObj;
      // If parameter is a array, convert to pointer
      if (cType instanceof CArrayType) {
        int size = machineModel.getSizeofPtr();
        paramObj = new SMGRegion(size, varName);
      } else {
        int size = machineModel.getSizeof(cType);
        paramObj = new SMGRegion(size, varName);
      }

      // We want to write a possible new Address in the new State, but
      // explore the old state for the parameters
      SMGValueAndState stateValue = readValueToBeAssiged(newState, callEdge, paramObj, 0, exp);
      newState = stateValue.getSmgState();
      SMGSymbolicValue value = stateValue.getValue();
      newState = assignExplicitValueToSymbolicValue(newState, callEdge, value, exp);

      values.add(i, Pair.of(paramObj, value));
    }

    newState.addStackFrame(functionDeclaration);

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramDecl.size(); i++) {

      CExpression exp = arguments.get(i);

      String varName = paramDecl.get(i).getName();
      CType cType = expressionEvaluator.getRealExpressionType(paramDecl.get(i));
      CType rValueType = expressionEvaluator.getRealExpressionType(exp.getExpressionType());
      // if function declaration is in form 'int foo(char b[32])' then omit array length
      if (rValueType instanceof CArrayType) {
        rValueType = new CArrayType(rValueType.isConst(), rValueType.isVolatile(), ((CArrayType)rValueType).getType(), null);
      }


      SMGRegion newObject = values.get(i).getFirst();
      SMGSymbolicValue symbolicValue = values.get(i).getSecond();

      newState.addLocalVariable(cType, varName,newObject);

      // We want to write a possible new Address in the new State, but
      // explore the old state for the parameters
      newState = assignFieldToState(newState, callEdge, newObject, 0, cType, symbolicValue, rValueType);
    }

    return newState;
  }

  private SMGState handleAssumption(SMGState pSmgState, CExpression expression, CFAEdge cfaEdge,
      boolean truthValue, boolean createNewStateIfNecessary) throws CPATransferException {

    SMGState smgState = pSmgState;

    // get the value of the expression (either true[-1], false[0], or unknown[null])
    AssumeVisitor visitor = expressionEvaluator.getAssumeVisitor(cfaEdge, smgState);
    SMGValueAndState valueAndState = expression.accept(visitor);
    SMGSymbolicValue value = valueAndState.getValue();
    smgState = valueAndState.getSmgState();

    if (! value.isUnknown()) {
      if ((truthValue && value.equals(SMGKnownSymValue.TRUE)) ||
          (!truthValue && value.equals(SMGKnownSymValue.FALSE))) {
        return smgState;
      } else {
        // This signals that there are no new States reachable from this State i. e. the
        // Assumption does not hold.
        return null;
      }
    }

    boolean impliesEqOn = visitor.impliesEqOn(truthValue);
    boolean impliesNeqOn = visitor.impliesNeqOn(truthValue);

    SMGSymbolicValue val1ImpliesOn;
    SMGSymbolicValue val2ImpliesOn;

    if(impliesEqOn || impliesNeqOn ) {
      val1ImpliesOn = visitor.impliesVal1();
      val2ImpliesOn = visitor.impliesVal2();
    } else {
      val1ImpliesOn = SMGUnknownValue.getInstance();
      val2ImpliesOn = SMGUnknownValue.getInstance();
    }

    SMGExplicitValueAndState explicitValueAndState = expressionEvaluator.evaluateExplicitValue(smgState, cfaEdge, expression);
    SMGExplicitValue explicitValue = explicitValueAndState.getValue();
    smgState = explicitValueAndState.getSmgState();

    if (expressionEvaluator.isMissingExplicitInformation()) {
      missingInformationList
          .add(new MissingInformation(truthValue, expression));
      expressionEvaluator.reset();
    }

    if (explicitValue.isUnknown()) {

      SMGState newState;

      if (createNewStateIfNecessary) {
        newState = new SMGState(smgState);
      } else {
        // Don't continuously create new states when strengthening.
        newState = smgState;
      }

      if (!val1ImpliesOn.isUnknown() && !val2ImpliesOn.isUnknown()) {
        if (impliesEqOn) {
          newState.identifyEqualValues((SMGKnownSymValue) val1ImpliesOn, (SMGKnownSymValue) val2ImpliesOn);
        } else if (impliesNeqOn) {
          newState.identifyNonEqualValues((SMGKnownSymValue) val1ImpliesOn, (SMGKnownSymValue) val2ImpliesOn);
        }
      }

      newState = expressionEvaluator.deriveFurtherInformation(newState, truthValue, cfaEdge, expression);
      return newState;
    } else if ((truthValue && explicitValue.equals(SMGKnownExpValue.ONE))
        || (!truthValue && explicitValue.equals(SMGKnownExpValue.ZERO))) {
      return smgState;
    } else {
      // This signals that there are no new States reachable from this State i. e. the
      // Assumption does not hold.
      return null;
    }
  }

  private SMGState handleStatement(SMGState pState, CStatementEdge pCfaEdge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling statement");
    SMGState newState;

    CStatement cStmt = pCfaEdge.getStatement();

    if (cStmt instanceof CAssignment) {
      CAssignment cAssignment = (CAssignment) cStmt;
      CExpression lValue = cAssignment.getLeftHandSide();
      CRightHandSide rValue = cAssignment.getRightHandSide();

      newState = handleAssignment(pState, pCfaEdge, lValue, rValue);
    } else if (cStmt instanceof CFunctionCallStatement) {

      CFunctionCallStatement cFCall = (CFunctionCallStatement) cStmt;
      CFunctionCallExpression cFCExpression = cFCall.getFunctionCallExpression();
      CExpression fileNameExpression = cFCExpression.getFunctionNameExpression();
      boolean isRequiered = false;
      String functionName = fileNameExpression.toASTString();

      if (builtins.isABuiltIn(functionName)) {
        newState = new SMGState(pState);
        if (builtins.isConfigurableAllocationFunction(functionName)) {
          logger.log(Level.WARNING, pCfaEdge.getFileLocation() + ":",
              "Calling " + functionName + " and not using the result, resulting in memory leak.");
          newState = builtins.evaluateConfigurableAllocationFunction(cFCExpression, newState, pCfaEdge).getSmgState();
          newState.setMemLeak();
          isRequiered = true;
        }
        if (builtins.isDeallocationFunction(functionName)) {
          newState = builtins.evaluateFree(cFCExpression, newState, pCfaEdge);
        }
        switch (functionName) {
        case "__VERIFIER_BUILTIN_PLOT":
          builtins.evaluateVBPlot(cFCExpression, newState);
          expressionEvaluator.reset();
          missingInformationList.add(new MissingInformation(cFCExpression, false));
          break;
        case "__builtin_alloca":
          logger.log(Level.WARNING, pCfaEdge.getFileLocation() + ":",
              "Calling alloc and not using the result.");
          newState = builtins.evaluateAlloc(cFCExpression, newState, pCfaEdge).getSmgState();
          break;
        case "memset":
          SMGEdgePointsToAndState result = builtins.evaluateMemset(cFCExpression, newState, pCfaEdge);
          newState = result.getSmgState();
          break;
        case "printf":
          return new SMGState(pState);
        }

        if (expressionEvaluator.missingExplicitInformation) {
          missingInformationList.add(new MissingInformation(cFCExpression, isRequiered));
          expressionEvaluator.reset();
        }

      } else {
        switch (handleUnknownFunctions) {
        case "strict":
          throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
        case "assume_safe":
          return pState;
        }
        throw new AssertionError();
      }
    } else {
      newState = pState;
    }

    return newState;
  }

  private SMGState handleAssignment(SMGState pState, CFAEdge cfaEdge, CExpression lValue,
      CRightHandSide rValue) throws CPATransferException {

    SMGState newState;
    SMGState state = pState;
    logger.log(Level.FINEST, "Handling assignment:", lValue, "=", rValue);

    LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(cfaEdge, state);

    SMGAddressAndState addressOfFieldAndState = lValue.accept(visitor);
    SMGAddress addressOfField = addressOfFieldAndState.getAddress();
    state = addressOfFieldAndState.getSmgState();

    CType fieldType = expressionEvaluator.getRealExpressionType(lValue);

    if (addressOfField.isUnknown()) {
      addMissingInformation(lValue, rValue);
      //TODO: Really? I would say that when we do not know where to write a value, we are in trouble
      /* Maybe defining it as relevant? In some cases, we can get the address through the explicitCPA.
       * In all other cases we could give an Invalid Write*/
      return new SMGState(state);
    }

    newState =
        handleAssignmentToField(state, cfaEdge, addressOfField.getObject(),
            addressOfField.getOffset().getAsInt(), fieldType, rValue);

    return newState;
  }

  private void addMissingInformation(CExpression pLValue, CRightHandSide pRValue) {
    missingInformationList.add(new MissingInformation(pLValue, pRValue, false));
  }

  /*
   * Creates value to be assigned to given field, by either reading it from the state,
   * or creating it, if an unknown value is returned, and marking it in missing Information.
   * Note that this read may modify the state.
   *
   */
  private SMGValueAndState readValueToBeAssiged(SMGState pNewState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CRightHandSide rValue) throws CPATransferException {

    SMGValueAndState valueAndState = expressionEvaluator.evaluateExpressionValue(pNewState, cfaEdge, rValue);
    SMGSymbolicValue value = valueAndState.getValue();

    if (value.isUnknown()) {

      if (expressionEvaluator.isMissingExplicitInformation()) {
        addMissingInformation(memoryOfField, fieldOffset, rValue, expressionEvaluator.isRequiered());
        expressionEvaluator.reset();
      }

      value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
      valueAndState = SMGValueAndState.of(valueAndState.getSmgState(), value);
    }

    return valueAndState;
  }

  // assign value of given expression to State at given location
  private SMGState assignFieldToState(SMGState pNewState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    CType rValueType = expressionEvaluator.getRealExpressionType(rValue);

    SMGValueAndState valueAndState = readValueToBeAssiged(pNewState, cfaEdge, memoryOfField, fieldOffset, rValue);
    SMGSymbolicValue value = valueAndState.getValue();
    SMGState newState = valueAndState.getSmgState();


    //TODO (  cast expression)

    //6.5.16.1 right operand is converted to type of assignment expression
    // 6.5.26 The type of an assignment expression is the type the left operand would have after lvalue conversion.
    rValueType = pFieldType;

    newState = assignExplicitValueToSymbolicValue(newState, cfaEdge, value, rValue);

    newState = assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, value, rValueType);

    return newState;
  }

  // Assign symbolic value to the explicit value calculated from pRvalue
  private SMGState assignExplicitValueToSymbolicValue(SMGState pNewState,
      CFAEdge pCfaEdge, SMGSymbolicValue value, CRightHandSide pRValue)
      throws CPATransferException {

    SMGExpressionEvaluator expEvaluator = new SMGExpressionEvaluator(logger,
        machineModel);

    SMGExplicitValueAndState expValueAndState = expEvaluator.evaluateExplicitValue(pNewState, pCfaEdge, pRValue);
    SMGExplicitValue expValue = expValueAndState.getValue();
    SMGState newState = expValueAndState.getSmgState();

    if (!expValue.isUnknown()) {
      newState.putExplicit((SMGKnownSymValue) value, (SMGKnownExpValue) expValue);
    }

    return newState;
  }

  private void addMissingInformation(SMGObject pMemoryOfField, int pFieldOffset, CRightHandSide pRValue,
      boolean isRequiered) {

    SMGAddress address = SMGAddress.valueOf(pMemoryOfField, SMGKnownExpValue.valueOf(pFieldOffset));

    missingInformationList.add(
        new MissingInformation(address, pRValue, isRequiered));
  }

  private SMGState assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, SMGSymbolicValue value, CType rValueType)
      throws UnrecognizedCCodeException, SMGInconsistentException {

    if (memoryOfField.getSize() < expressionEvaluator.getSizeof(cfaEdge, rValueType)) {
      logger.log(Level.WARNING, cfaEdge.getFileLocation() + ":",
          "Attempting to write " + expressionEvaluator.getSizeof(cfaEdge, rValueType) +
          " bytes into a field with size " + memoryOfField.getSize() + "bytes:",
          cfaEdge.getRawStatement());
    }

    if (expressionEvaluator.isStructOrUnionType(rValueType)) {
      return assignStruct(newState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    } else {
      return writeValue(newState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    }
  }

  private SMGState assignStruct(SMGState pNewState, SMGObject pMemoryOfField,
      int pFieldOffset, CType pRValueType, SMGSymbolicValue pValue,
      CFAEdge pCfaEdge) throws SMGInconsistentException,
      UnrecognizedCCodeException {

    if (pValue instanceof SMGKnownAddVal) {
      SMGKnownAddVal structAddress = (SMGKnownAddVal) pValue;

      SMGObject source = structAddress.getObject();
      int structOffset = structAddress.getOffset().getAsInt();
      int structSize = structOffset + expressionEvaluator.getSizeof(pCfaEdge, pRValueType);
      return pNewState.copy(source, pMemoryOfField,
          structOffset, structSize, pFieldOffset);
    }

    return pNewState;
  }

  private SMGState writeValue(SMGState pNewState, SMGObject pMemoryOfField, int pFieldOffset, long pSizeType,
      SMGSymbolicValue pValue, CFAEdge pEdge) throws UnrecognizedCCodeException, SMGInconsistentException {
    return writeValue(pNewState, pMemoryOfField, pFieldOffset, AnonymousTypes.createTypeWithLength(pSizeType), pValue, pEdge);
  }

  private SMGState writeValue(SMGState pNewState, SMGObject pMemoryOfField, int pFieldOffset, CType pRValueType,
      SMGSymbolicValue pValue, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    boolean doesNotFitIntoObject = pFieldOffset < 0
        || pFieldOffset + expressionEvaluator.getSizeof(pEdge, pRValueType) > pMemoryOfField.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, pEdge.getFileLocation() + ":",
          "Field " + "(" + pFieldOffset + ", " + pRValueType.toASTString("") + ")" +
          " does not fit object " + pMemoryOfField.toString() + ".");

      return pNewState.setInvalidWrite();
    }

    if (pValue.isUnknown()) {
      return pNewState;
    }

    return pNewState.writeValue(pMemoryOfField, pFieldOffset, pRValueType, pValue).getState();
  }

  private SMGState handleAssignmentToField(SMGState state, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    SMGState newState = new SMGState(state);

    newState = assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, rValue);

    // If Assignment contained malloc, handle possible fail with
    // alternate State (don't create state if not enabled)
    if (possibleMallocFail && enableMallocFailure) {
      possibleMallocFail = false;
      SMGState otherState = new SMGState(state);
      CType rValueType = expressionEvaluator.getRealExpressionType(rValue);
      mallocFailState = writeValue(otherState, memoryOfField, fieldOffset, rValueType, SMGKnownSymValue.ZERO, cfaEdge);
    }

    return newState;
  }

  private SMGState handleVariableDeclaration(SMGState pState, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge) throws CPATransferException {
    logger.log(Level.FINEST, "Handling variable declaration:", pVarDecl);

    String varName = pVarDecl.getName();
    CType cType = expressionEvaluator.getRealExpressionType(pVarDecl);

    SMGObject newObject;

    newObject = pState.getObjectForVisibleVariable(varName);
      /*
       *  The variable is not null if we seen the declaration already, for example in loops. Invalid
       *  occurrences (variable really declared twice) should be caught for us by the parser. If we
       *  already processed the declaration, we do nothing.
       */
    if (newObject == null) {
      if (pVarDecl.isGlobal()) {
        newObject = pState.addGlobalVariable(cType, varName);
      } else {
        newObject = pState.addLocalVariable(cType, varName);
      }
    }

    pState = handleInitializerForDeclaration(pState, newObject, pVarDecl, pEdge);
    return pState;
  }

  private SMGState handleDeclaration(SMGState smgState, CDeclarationEdge edge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling declaration");

    CDeclaration cDecl = edge.getDeclaration();

    if (!(cDecl instanceof CVariableDeclaration)) {
      return smgState;
    }

    SMGState newState = new SMGState(smgState);

    newState = handleVariableDeclaration(newState, (CVariableDeclaration)cDecl, edge);

    return newState;
  }

  private SMGState handleInitializerForDeclaration(SMGState pState, SMGObject pObject, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge) throws CPATransferException {
    CInitializer newInitializer = pVarDecl.getInitializer();
    CType cType = expressionEvaluator.getRealExpressionType(pVarDecl);

    if (newInitializer != null) {
      logger.log(Level.FINEST, "Handling variable declaration: handling initializer");

      return handleInitializer(pState, pVarDecl, pEdge, pObject, 0, cType, newInitializer);
    } else if (pVarDecl.isGlobal()) {

      // Global variables without initializer are nullified in C
      pState = writeValue(pState, pObject, 0, cType, SMGKnownSymValue.ZERO, pEdge);
    }

    return pState;
  }

  private SMGState handleInitializer(SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CType pLValueType, CInitializer pInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    if (pInitializer instanceof CInitializerExpression) {
       return assignFieldToState(pNewState, pEdge, pNewObject,
          pOffset, pLValueType,
          ((CInitializerExpression) pInitializer).getExpression());

    } else if (pInitializer instanceof CInitializerList) {

      return handleInitializerList(pNewState, pVarDecl, pEdge,
          pNewObject, pOffset, pLValueType, ((CInitializerList) pInitializer));
    } else if (pInitializer instanceof CDesignatedInitializer) {
      // TODO handle CDesignatedInitializer
      return pNewState;

    } else {
      throw new UnrecognizedCCodeException("Did not recognize Initializer", pInitializer);
    }
  }

  private SMGState handleInitializerList(SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CType pLValueType, CInitializerList pNewInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    CType realCType = pLValueType.getCanonicalType();

    if (realCType instanceof CArrayType) {

      CArrayType arrayType = (CArrayType) realCType;
      return handleInitializerList(pNewState, pVarDecl, pEdge,
          pNewObject, pOffset, arrayType, pNewInitializer);
    } else if (realCType instanceof CCompositeType) {

      CCompositeType structType = (CCompositeType) realCType;
      return handleInitializerList(pNewState, pVarDecl, pEdge,
          pNewObject, pOffset, structType, pNewInitializer);
    }

    // Type cannot be resolved
    logger.log(Level.WARNING, "Type " + realCType.toASTString("")
        + "cannot be resolved sufficiently to handle initializer "
        + pNewInitializer.toASTString());

    return pNewState;
  }

  private SMGState handleInitializerList(
      SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CCompositeType pLValueType,
      CInitializerList pNewInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    int listCounter = 0;

    List<CCompositeType.CCompositeTypeMemberDeclaration> memberTypes = pLValueType.getMembers();

    int offset = pOffset;

    for (CInitializer initializer : pNewInitializer.getInitializers()) {

      if (listCounter >= memberTypes.size()) {
        throw new UnrecognizedCCodeException(
            "More Initializer in initializer list "
                + pNewInitializer.toASTString()
                + " than fit in type "
                + pLValueType.toASTString(""), pEdge);
      }

      CType memberType = memberTypes.get(listCounter).getType();

      pNewState = handleInitializer(pNewState, pVarDecl, pEdge, pNewObject, offset, memberType, initializer);

      offset = offset + expressionEvaluator.getSizeof(pEdge, memberType);

      listCounter++;
    }

    if (pVarDecl.isGlobal()) {
      int sizeOfType = expressionEvaluator.getSizeof(pEdge, pLValueType);

      if (offset - pOffset < sizeOfType ) {
        pNewState = writeValue(pNewState, pNewObject, offset, AnonymousTypes.createTypeWithLength(sizeOfType - (offset - pOffset)), SMGKnownSymValue.ZERO, pEdge);
      }
    }

    return pNewState;
  }

  private SMGState handleInitializerList(
      SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CArrayType pLValueType,
      CInitializerList pNewInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    int listCounter = 0;

    CType elementType = pLValueType.getType();

    int sizeOfElementType = expressionEvaluator.getSizeof(pEdge, elementType);

    for (CInitializer initializer : pNewInitializer.getInitializers()) {

      int offset = pOffset + listCounter * sizeOfElementType;

      pNewState = handleInitializer(pNewState, pVarDecl, pEdge,
          pNewObject, offset, pLValueType.getType(), initializer);

      listCounter++;
    }

    if (pVarDecl.isGlobal()) {

      int sizeOfType = expressionEvaluator.getSizeof(pEdge, pLValueType);

      int offset = pOffset + listCounter * sizeOfElementType;
      if (offset - pOffset < sizeOfType) {
        pNewState = writeValue(pNewState, pNewObject, offset, AnonymousTypes.createTypeWithLength(sizeOfType - (offset - pOffset)), SMGKnownSymValue.ZERO, pEdge);
      }
    }

    return pNewState;
  }

  /**
   * The class {@link SMGExpressionEvaluator} is meant to evaluate
   * a expression using an arbitrary SMGState. Thats why it does not
   * permit semantic changes of the state it uses. This class implements
   * additionally the changes that occur while calculating the next smgState
   * in the Transfer Relation. These mainly include changes when evaluating
   * functions. They also contain code that should only be executed during
   * the calculation of the next SMG State, e.g. logging.
   */
  private class SMGRightHandSideEvaluator extends SMGExpressionEvaluator {

    private boolean missingExplicitInformation;
    private boolean isRequiered;

    public SMGRightHandSideEvaluator(LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel) {
      super(pLogger, pMachineModel);
    }

    public SMGExplicitValueAndState forceExplicitValue(SMGState smgState,
        CFAEdge pCfaEdge, CRightHandSide rVal)
        throws UnrecognizedCCodeException {

      ForceExplicitValueVisitor v = new ForceExplicitValueVisitor(smgState,
          null, machineModel, logger, pCfaEdge);

      Value val = rVal.accept(v);

      if (val.isUnknown()) {
        return SMGExplicitValueAndState.of(v.getNewState());
      }

      return SMGExplicitValueAndState.of(v.getNewState(),
          SMGKnownExpValue.valueOf(val.asNumericValue().longValue()));
    }

    public SMGState deriveFurtherInformation(SMGState pNewState, boolean pTruthValue, CFAEdge pCfaEdge, CExpression rValue)
        throws CPATransferException {
      AssigningValueVisitor v = new AssigningValueVisitor(pNewState, pTruthValue, pCfaEdge);

      rValue.accept(v);
      return v.getAssignedState();
    }

    @Override
    public SMGValueAndState readValue(SMGState pSmgState, SMGObject pObject,
        SMGExplicitValue pOffset, CType pType, CFAEdge pEdge)
        throws SMGInconsistentException, UnrecognizedCCodeException {

      if (pOffset.isUnknown() || pObject == null) {
        return SMGValueAndState.of(pSmgState);
      }

      int fieldOffset = pOffset.getAsInt();

      boolean doesNotFitIntoObject = fieldOffset < 0
          || fieldOffset + getSizeof(pEdge, pType) > pObject.getSize();

      if (doesNotFitIntoObject) {
        // Field does not fit size of declared Memory
        logger.log(Level.WARNING, pEdge.getFileLocation() + ":", "Field " + "("
            + fieldOffset + ", " + pType.toASTString("") + ")"
            + " does not fit object " + pObject.toString() + ".");

        return SMGValueAndState.of(pSmgState.setInvalidRead());
      }

      return pSmgState.forceReadValue(pObject, fieldOffset, pType);
    }

    /**
     * Visitor that derives further information from an assume edge
     */
    private class AssigningValueVisitor extends DefaultCExpressionVisitor<Void, CPATransferException> {

      private SMGState assignableState;
      private boolean truthValue = false;
      private CFAEdge edge;

      public AssigningValueVisitor(SMGState pSMGState, boolean pTruthvalue, CFAEdge pEdge) {
        assignableState = pSMGState;
        truthValue = pTruthvalue;
        edge = pEdge;
      }

      public SMGState getAssignedState() {
        return assignableState;
      }

      @Override
      protected Void visitDefault(CExpression pExp) throws CPATransferException {
        return null;
      }

      @Override
      public Void visit(CPointerExpression pointerExpression) throws CPATransferException {
        deriveFurtherInformation(pointerExpression);
        return null;
      }

      @Override
      public Void visit(CIdExpression pExp) throws CPATransferException {
        deriveFurtherInformation(pExp);
        return null;
      }

      @Override
      public Void visit(CArraySubscriptExpression pExp) throws CPATransferException {
        deriveFurtherInformation(pExp);
        return null;
      }

      @Override
      public Void visit(CFieldReference pExp) throws CPATransferException {
        deriveFurtherInformation(pExp);
        return null;
      }

      @Override
      public Void visit(CCastExpression pE) throws CPATransferException {
        // TODO cast reinterpretations
        return pE.getOperand().accept(this);
      }

      @Override
      public Void visit(CCharLiteralExpression pE) throws CPATransferException {

        assert false;
        return null;
      }

      @Override
      public Void visit(CFloatLiteralExpression pE) throws CPATransferException {

        assert false;
        return null;
      }

      @Override
      public Void visit(CIntegerLiteralExpression pE) throws CPATransferException {

        assert false;
        return null;
      }


      @Override
      public Void visit(CBinaryExpression binExp) throws CPATransferException {
        //TODO More precise

        CExpression operand1 = unwrap(binExp.getOperand1());
        CExpression operand2 = unwrap(binExp.getOperand2());
        BinaryOperator op = binExp.getOperator();

        if (operand1 instanceof CLeftHandSide) {
          deriveFurtherInformation((CLeftHandSide) operand1, operand2, op);
        }

        if (operand2 instanceof CLeftHandSide) {
          deriveFurtherInformation((CLeftHandSide) operand2, operand1, op);
        }

        return null;
      }

      private void deriveFurtherInformation(CLeftHandSide lValue, CExpression exp, BinaryOperator op) throws CPATransferException {

        SMGExplicitValue rValue = evaluateExplicitValue(assignableState, edge, exp).getValue();

        if (rValue.isUnknown()) {
          // no further information can be inferred
          return;
        }

        SMGSymbolicValue rSymValue = evaluateExpressionValue(assignableState, edge, lValue).getValue();

        if(rSymValue.isUnknown()) {

          rSymValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());

          SMGExpressionEvaluator.LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

          SMGAddress addressOfField = lValue.accept(visitor).getAddress();

          if(addressOfField.isUnknown()) {
            return;
          }

          assignableState = writeValue(assignableState, addressOfField.getObject(), addressOfField.getOffset().getAsInt(), getRealExpressionType(exp), rSymValue, edge);
        }

        if (truthValue) {
          if (op == BinaryOperator.EQUALS) {
            assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
          }
        } else {
          if (op == BinaryOperator.NOT_EQUALS) {
            assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
            //TODO more precise
          }
        }
      }

      @Override
      public Void visit(CUnaryExpression pE) throws CPATransferException {

        UnaryOperator op = pE.getOperator();

        CExpression operand = pE.getOperand();

        switch (op) {
        case AMPER:
          assert false : "In this case, the assume should be able to be calculated";
          return null;
        case MINUS:
        case TILDE:
          // don't change the truth value
          return operand.accept(this);
        case SIZEOF:
          assert false : "At the moment, this cae should be able to be calculated";

        }

        return null;
      }

      private void deriveFurtherInformation(CLeftHandSide lValue) throws CPATransferException {

        if (truthValue == true) {
          return; // no further explicit Information can be derived
        }

        SMGExpressionEvaluator.LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

        SMGAddress addressOfField = lValue.accept(visitor).getAddress();

        if (addressOfField.isUnknown()) {
          return;
        }

        // If this value is known, the assumption can be evaluated, therefore it should be unknown
        assert evaluateExplicitValue(assignableState, edge, lValue).getValue().isUnknown();

        SMGSymbolicValue value = evaluateExpressionValue(assignableState, edge, lValue).getValue();

        // This symbolic value should have been added when evaluating the assume
        assert !value.isUnknown();

        assignableState.putExplicit((SMGKnownSymValue)value, SMGKnownExpValue.ZERO);

      }

      private CExpression unwrap(CExpression expression) {
        // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

        if (expression instanceof CCastExpression) {
          CCastExpression exp = (CCastExpression) expression;
          expression = exp.getOperand();

          expression = unwrap(expression);
        }

        return expression;
      }
    }

    private class LValueAssignmentVisitor extends SMGExpressionEvaluator.LValueAssignmentVisitor {

      public LValueAssignmentVisitor(CFAEdge pEdge, SMGState pSmgState) {
        super(pEdge, pSmgState);
      }

      @Override
      public SMGAddressAndState visit(CIdExpression variableName) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: variable assignment");

        // a = ...
        return super.visit(variableName);
      }

      @Override
      public SMGAddressAndState visit(CPointerExpression pLValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to dereferenced pointer");

        SMGAddressAndState address = super.visit(pLValue);

        if (address.getAddress().isUnknown()) {
          return SMGAddressAndState.of(address.getSmgState().setUnknownDereference(), address.getAddress());
        }

        return address;
      }

      @Override
      public SMGAddressAndState visit(CFieldReference lValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to field reference");

        return super.visit(lValue);
      }

      @Override
      public SMGAddressAndState visit(CArraySubscriptExpression lValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to array Cell");

        return super.visit(lValue);
      }
    }

    private class ExpressionValueVisitor extends SMGExpressionEvaluator.ExpressionValueVisitor {

      public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
        super(pEdge, pSmgState);
      }

      @Override
      public SMGValueAndState visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws CPATransferException {

        CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
        String functionName = fileNameExpression.toASTString();

        //TODO extreme code sharing ...

        // If Calloc and Malloc have not been properly declared,
        // they may be shown to return void
        if (builtins.isABuiltIn(functionName)) {
          if (builtins.isConfigurableAllocationFunction(functionName)) {
            possibleMallocFail = true;
            SMGEdgePointsToAndState configAllocEdge = builtins.evaluateConfigurableAllocationFunction(
                pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return createAddress(configAllocEdge);
          }
          switch (functionName) {
          case "__VERIFIER_BUILTIN_PLOT":
            builtins.evaluateVBPlot(pIastFunctionCallExpression, getInitialSmgState());
            break;
          case "__builtin_alloca":
            possibleMallocFail = true;
            SMGEdgePointsToAndState allocEdge = builtins.evaluateAlloc(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return createAddress(allocEdge);
          case "printf":
            return SMGValueAndState.of(getInitialSmgState());
          default:
            if (builtins.isNondetBuiltin(functionName)) {
              return SMGValueAndState.of(getInitialSmgState());
            } else {
              throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
            }
          }
        } else {
          switch (handleUnknownFunctions) {
          case "strict":
            throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
          case "assume_safe":
            return SMGValueAndState.of(getInitialSmgState());
          }
          throw new AssertionError();
        }

        return SMGValueAndState.of(getInitialSmgState());
      }
    }

    private class ForceExplicitValueVisitor extends
        SMGExpressionEvaluator.ExplicitValueVisitor {

      private final SMGKnownExpValue GUESS = SMGKnownExpValue.valueOf(2);

      public ForceExplicitValueVisitor(SMGState pSmgState, String pFunctionName, MachineModel pMachineModel,
          LogManagerWithoutDuplicates pLogger, CFAEdge pEdge) {
        super(pSmgState, pFunctionName, pMachineModel, pLogger, pEdge);
      }

      @Override
      protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
          throws UnrecognizedCCodeException {
        Value result = super.evaluateCArraySubscriptExpression(pLValue);

        if (result.isUnknown()) {
          return guessLHS(pLValue);
        } else {
          return result;
        }
      }

      @Override
      protected Value evaluateCIdExpression(CIdExpression pCIdExpression)
          throws UnrecognizedCCodeException {

        Value result = super.evaluateCIdExpression(pCIdExpression);

        if (result.isUnknown()) {
          return guessLHS(pCIdExpression);
        } else {
          return result;
        }
      }

      private Value guessLHS(CLeftHandSide exp)
          throws UnrecognizedCCodeException {

        SMGValueAndState symbolicValueAndState;

        try {
          symbolicValueAndState = evaluateExpressionValue(getNewState(),
              getEdge(), exp);
        } catch (CPATransferException e) {
          UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
              "SMG cannot get symbolic value of : " + exp.toASTString(), exp);
          e2.initCause(e);
          throw e2;
        }

        SMGSymbolicValue value = symbolicValueAndState.getValue();
        setSmgState(symbolicValueAndState.getSmgState());

        if (value.isUnknown()) {
          return UnknownValue.getInstance();
        }

        getNewState().putExplicit((SMGKnownSymValue) value, GUESS);

        return new NumericValue(GUESS.getValue());
      }

      @Override
      protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
        Value result = super.evaluateCFieldReference(pLValue);

        if (result.isUnknown()) {
          return guessLHS(pLValue);
        } else {
          return result;
        }
      }

      @Override
      protected Value evaluateCPointerExpression(CPointerExpression pCPointerExpression)
          throws UnrecognizedCCodeException {
        Value result = super.evaluateCPointerExpression(pCPointerExpression);

        if (result.isUnknown()) {
          return guessLHS(pCPointerExpression);
        } else {
          return result;
        }
      }
    }

    private class PointerAddressVisitor extends SMGExpressionEvaluator.PointerVisitor {

      public PointerAddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
        super(pEdge, pSmgState);
      }

      @Override
      public SMGAddressValueAndState visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws CPATransferException {
        CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
        String functionName = fileNameExpression.toASTString();

        if (builtins.isABuiltIn(functionName)) {
          if (builtins.isConfigurableAllocationFunction(functionName)) {
            possibleMallocFail = true;
            SMGEdgePointsToAndState configAllocEdge = builtins.evaluateConfigurableAllocationFunction(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return createAddress(configAllocEdge);
          }
          switch (functionName) {
          case "__builtin_alloca":
            SMGEdgePointsToAndState allocEdge = builtins.evaluateAlloc(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return createAddress(allocEdge);
          case "memset":
            SMGEdgePointsToAndState memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return createAddress(memsetTargetEdge);
          case "printf":
            return SMGAddressValueAndState.of(getInitialSmgState());
          default:
            if (builtins.isNondetBuiltin(functionName)) {
              return SMGAddressValueAndState.of(getInitialSmgState());
            } else {
              throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
            }
          }
        } else {
          switch (handleUnknownFunctions) {
          case "strict":
            throw new CPATransferException(
                "Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
          case "assume_safe":
            return SMGAddressValueAndState.of(getInitialSmgState());
          }
          throw new AssertionError();
        }
      }
    }

    @Override
    protected org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.PointerVisitor getPointerVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      return new PointerAddressVisitor(pCfaEdge, pNewState);
    }

    @Override
    protected org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.ExpressionValueVisitor getExpressionValueVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      return new ExpressionValueVisitor(pCfaEdge, pNewState);
    }

    @Override
    public org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.LValueAssignmentVisitor getLValueAssignmentVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      return new LValueAssignmentVisitor(pCfaEdge, pNewState);
    }

    @Override
    public SMGExplicitValueAndState evaluateExplicitValue(SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      SMGExplicitValueAndState explicitValue = super.evaluateExplicitValue(pSmgState, pCfaEdge, pRValue);
      if (explicitValue.getValue().isUnknown()) {
        missingExplicitInformation = true;
      }
      return explicitValue;
    }

    public boolean isMissingExplicitInformation() {
      return missingExplicitInformation;
    }

    public boolean isRequiered() {
      return isRequiered;
    }

    @Override
    protected SMGValueAndState handleUnknownDereference(SMGState pSmgState,
        CFAEdge pEdge) {

      SMGState newState = pSmgState.setUnknownDereference();
      return super.handleUnknownDereference(newState, pEdge);
    }

    public void reset() {
      isRequiered = false;
      missingExplicitInformation= false;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements,
      CFAEdge cfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    ArrayList<SMGState> toStrengthen = new ArrayList<>();
    ArrayList<SMGState> result = new ArrayList<>();
    toStrengthen.add((SMGState) element);
    result.add((SMGState) element);

    for (AbstractState ae : elements) {
      if (ae instanceof AutomatonState) {
        // New result
        result.clear();
        for (SMGState state : toStrengthen) {
          Collection<SMGState> ret = strengthen((AutomatonState) ae, state, cfaEdge);
          if (ret == null) {
            result.add(state);
          } else {
            result.addAll(ret);
          }
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      }
    }

    missingInformationList.clear();
    possibleMallocFail = false;
    hasChanged = false;
    oldState = null;
    return result;
  }

  private Collection<SMGState> strengthen(AutomatonState pAutomatonState, SMGState pElement,
      CFAEdge pCfaEdge) throws CPATransferException {

    List<AssumeEdge> assumptions = pAutomatonState.getAsAssumeEdges(pCfaEdge.getPredecessor().getFunctionName());

    if(assumptions.isEmpty()) {
      return Collections.singleton(pElement);
    }

    StringBuilder assumeDesc = new StringBuilder();

    SMGState newElement = pElement;

    for (AssumeEdge assume : assumptions) {
      if (!(assume instanceof CAssumeEdge)) {
        continue;
      }

      assumeDesc.append(assume.getDescription());

      // only create new SMGState if necessary
      newElement = handleAssumption(newElement, ((CAssumeEdge)assume).getExpression(), pCfaEdge, assume.getTruthAssumption(), pElement == newElement);

      if (newElement == null) {
        break;
      }
    }

    if (newElement == null) {
      return Collections.emptyList();
    } else {
      plotWhenConfigured("every", null, newElement, assumeDesc.toString());
      return Collections.singleton(newElement);
    }
  }

  private boolean hasChanged;

  @SuppressWarnings("unused")
  @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
  private Collection<? extends AbstractState> strengthen(ValueAnalysisState explicitState, SMGState pSMGState, CFAEdge cfaEdge) throws CPATransferException {

    SMGState newElement = new SMGState(pSMGState);

    for (MissingInformation missingInformation : missingInformationList) {
      if (missingInformation.isMissingAssumption()) {
       // newElement = resolvingAssumption(newElement, explicitState, missingInformation, cfaEdge);
      } else if (missingInformation.isMissingAssignment()) {
        if (isRelevant(missingInformation)) {
          newElement = resolvingAssignment(newElement, explicitState, missingInformation, cfaEdge);
        }
      } else if (missingInformation.isFunctionCall()) {
        resolveRValue(pSMGState, newElement, explicitState, missingInformation.getMissingCExpressionInformation(), cfaEdge);
      }
    }


    return hasChanged ? Collections.singleton(newElement) : null;
  }

  private boolean isRelevant(MissingInformation missingInformation) {

    CRightHandSide value;

    if (missingInformation.hasUnknownMemoryLocation()) {
      value = missingInformation.getMissingCLeftMemoryLocation();
    } else if (missingInformation.hasUnknownValue()) {
      value = missingInformation.getMissingCExpressionInformation();
    } else {
      return false;
    }

    CType type = expressionEvaluator.getRealExpressionType(value);
    boolean result = type instanceof CPointerType;
    return result;
  }

  private SMGState resolvingAssignment(SMGState pSmgState,
      ValueAnalysisState explicitState, MissingInformation pMissingInformation, CFAEdge edge) throws CPATransferException {

    SMGAddress memoryLocation = null;

    if (pMissingInformation.hasKnownMemoryLocation()) {
      memoryLocation = pMissingInformation.getcLeftMemoryLocation();
    } else if (pMissingInformation.hasUnknownMemoryLocation()) {
      memoryLocation = resolveMemoryLocation(oldState, explicitState,
          pMissingInformation.getMissingCLeftMemoryLocation(), edge);
    }

    if (memoryLocation == null || memoryLocation.isUnknown()) {
      // Always return the new Element
      // if you want to interrupt the calculation
      // in case it was changed before

      if (pMissingInformation.isRequieredInformation()) {
        throw new UnrecognizedCCodeException("Not able to compute allocation size", edge);
      }

      return pSmgState;
    }

    SMGSymbolicValue symbolicValue = null;

    if (pMissingInformation.hasUnknownValue()) {

      CRightHandSide rValue = pMissingInformation.getMissingCExpressionInformation();

      symbolicValue = resolveRValue(oldState,pSmgState, explicitState,
          pMissingInformation.getMissingCExpressionInformation(), edge);

      if (symbolicValue == null || symbolicValue.isUnknown()) {
        // Always return the new Element
        // if you want to interrupt the calculation
        // in case it was changed before

        if (pMissingInformation.isRequieredInformation()) {
          throw new UnrecognizedCCodeException("Not able to compute allocation size", edge);
        }
        return pSmgState;
      }

      hasChanged = true;
      pSmgState = writeValue(pSmgState, memoryLocation.getObject(), memoryLocation.getOffset().getAsInt(),
          expressionEvaluator.getRealExpressionType(rValue), symbolicValue, edge);

    }

    return pSmgState;
  }

  private SMGSymbolicValue resolveRValue(SMGState oldState, SMGState newSmgState,
      ValueAnalysisState pExplicitState, CRightHandSide rValue, CFAEdge pEdge)
      throws CPATransferException {

    //TODO Refactor ...
    if (rValue instanceof CFunctionCallExpression) {
      return resolveFunctionCall(newSmgState, pExplicitState,
          (CFunctionCallExpression) rValue, pEdge).getValue();
    } else {

      String functionName = pEdge.getPredecessor().getFunctionName();

      ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(pExplicitState,
          functionName, oldState, machineModel, logger, pEdge);

      return cc.evaluateSMGExpression(rValue);
    }
  }

  private SMGValueAndState resolveFunctionCall(SMGState pSmgState,
      ValueAnalysisState pExplicitState,
      CFunctionCallExpression pIastFunctionCallExpression,
      CFAEdge pEdge) throws CPATransferException {

    SMGExplicitBuiltIns builtins = new SMGExplicitBuiltIns(pExplicitState);

    CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
    String functionName = fileNameExpression.toASTString();

    if (builtins.isABuiltIn(functionName)) {
      if (builtins.isConfigurableAllocationFunction(functionName)) {
        SMGEdgePointsToAndState configAllocEdge = builtins.evaluateConfigurableAllocationFunction(pIastFunctionCallExpression, pSmgState, pEdge);
        return createAddress(configAllocEdge);
      }
      if (builtins.isDeallocationFunction(functionName)) {
        SMGState newState = builtins.evaluateFree(pIastFunctionCallExpression, pSmgState, pEdge);
        return SMGValueAndState.of(newState);
      }
      switch (functionName) {
      case "__VERIFIER_BUILTIN_PLOT":
        builtins.evaluateVBPlot(pIastFunctionCallExpression, pSmgState);
        return SMGValueAndState.of(pSmgState);
      case "__builtin_alloca":
        SMGEdgePointsToAndState allocEdge = builtins.evaluateAlloc(pIastFunctionCallExpression, pSmgState, pEdge);
        return createAddress(allocEdge);
      case "memset":
        SMGEdgePointsToAndState memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, pSmgState, pEdge);
        return createAddress(memsetTargetEdge);
      }
      throw new AssertionError();
    } else {
      return SMGValueAndState.of(pSmgState);
    }
  }

  private SMGAddressValueAndState createAddress(SMGEdgePointsToAndState pMallocEdge) {
    return expressionEvaluator.createAddress(pMallocEdge);
  }

  private SMGAddress resolveMemoryLocation(SMGState pSmgState, ValueAnalysisState pExplicitState,
      CExpression lValue, CFAEdge edge) throws UnrecognizedCCodeException {

    String functionName = edge.getPredecessor().getFunctionName();

    ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(pExplicitState, functionName,
        pSmgState, machineModel, logger, edge);

    return cc.evaluateSMGLeftHandSide(lValue);
  }

  @SuppressWarnings("unused")
  private SMGState resolvingAssumption(SMGState pSmgState, ValueAnalysisState pExplicitState,
      MissingInformation pMissingInformation, CFAEdge edge) throws UnrecognizedCCodeException {

    long truthValue = pMissingInformation.getTruthAssumption() ? 1 : 0;

    Long value =
        resolveAssumptionValue(oldState,
            pExplicitState,
            pMissingInformation.getMissingCExpressionInformation(),
            edge);

    if (value != null && value != truthValue) {
      return null;
    } else {
      hasChanged = true;
      return pSmgState;
    }
  }

  private Long resolveAssumptionValue(SMGState pSmgState, ValueAnalysisState pExplicitState,
      CRightHandSide rValue, CFAEdge edge) throws UnrecognizedCCodeException {

    String functionName = edge.getPredecessor().getFunctionName();

    ValueAnalysisSMGCommunicator cc =
        new ValueAnalysisSMGCommunicator(pExplicitState, functionName,
            pSmgState, machineModel, logger, edge);

    Value value = cc.evaluateExpression(rValue);

    if (value.isExplicitlyKnown() && value.isNumericValue()) {
      return value.asNumericValue().longValue();
    }

    return null;
  }

  @SuppressWarnings("unused")
  private void checkForMissingRequiredInformation(CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    for (MissingInformation missingInformation : missingInformationList) {
      if (missingInformation.isRequieredInformation()) { throw new UnrecognizedCCodeException(
          "Not able to compute allocation size", cfaEdge); }
    }
  }

  private class SMGExplicitBuiltIns extends SMGBuiltins {

    private final ValueAnalysisState explicitState;

    public SMGExplicitBuiltIns(ValueAnalysisState pExplicitState) {
      explicitState = pExplicitState;
    }

    @Override
    protected SMGAddressValueAndState evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue)
        throws CPATransferException {

      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(explicitState, functionName,
          pState, machineModel, logger, pCfaEdge);

      return SMGAddressValueAndState.of(pState, cc.evaluateSMGAddressExpression(pRvalue));
    }

    @Override
    protected SMGValueAndState evaluateExpressionValue(SMGState pSmgState, CFAEdge pCfaEdge, CExpression pRValue)
        throws CPATransferException {
      return SMGValueAndState.of(pSmgState, resolveRValue(oldState, pSmgState, explicitState, pRValue, pCfaEdge));
    }

    @Override
    protected String getDot(SMGState pCurrentState, String pName, String pLocation) {
      return pCurrentState.toDot(pName, pLocation, explicitState);
    }

    @Override
    protected Path getOutputFile(PathTemplate pExportSMGFilePattern, String pName) {
      return exportSMGFilePattern.getPath("Explicit_" + pName);
    }

    @Override
    protected SMGExplicitValueAndState evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(explicitState, functionName,
          pState, machineModel, logger, pCfaEdge);

      Value valueV = cc.evaluateExpression(pRValue);

      if (!valueV.isExplicitlyKnown() || !valueV.isNumericValue()) {
        return SMGExplicitValueAndState.of(pState);
      } else {
        return SMGExplicitValueAndState.of(pState, SMGKnownExpValue.valueOf(valueV.asNumericValue().longValue()));
      }
    }
  }

  private static class MissingInformation {

    /**
     * This field stores the Expression of the Memory Location that
     * could not be evaluated.
     */
    private final CExpression missingCLeftMemoryLocation;

    /**
     *  This expression stores the Memory Location
     *  to be assigned.
     */
    private final SMGAddress cLeftMemoryLocation;

    /**
     * Expression could not be evaluated due to missing information.
     */
    private final CRightHandSide missingCExpressionInformation;

    /**
     * Expression could not be evaluated due to missing information.
     */
    private final SMGSymbolicValue cExpressionValue;

    /**
     * The truth Assumption made in this assume edge.
     */
    private final Boolean truthAssumption;

    /**
     * If this missing Information can't be evaluated, stop analysis
     */
    private final boolean requieredInformation;

    public MissingInformation(CExpression pMissingCLeftMemoryLocation,
        CRightHandSide pMissingCExpressionInformation, boolean pRequieredInformation) {
      missingCExpressionInformation = pMissingCExpressionInformation;
      missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = null;
      requieredInformation = pRequieredInformation;
    }

    //TODO Better checks...don't be lazy, just because class
    // will likely change.

    @SuppressWarnings("unused")
    public boolean hasUnknownValue() {
      return missingCExpressionInformation != null;
    }

    @SuppressWarnings("unused")
    public boolean hasKnownValue() {
      return cExpressionValue != null;
    }

    @SuppressWarnings("unused")
    public boolean hasUnknownMemoryLocation() {
      return missingCLeftMemoryLocation != null;
    }

    public boolean isFunctionCall() {
      return missingCLeftMemoryLocation == null && cLeftMemoryLocation == null
          && missingCExpressionInformation instanceof CFunctionCallExpression;
    }

    @SuppressWarnings("unused")
    public boolean hasKnownMemoryLocation() {
      return cLeftMemoryLocation != null;
    }

    public boolean isMissingAssignment() {
      // TODO Better Name for this method.
      // Checks if a variable needs to be assigned a value,
      // but to evaluate the MemoryLocation, or the value,
      // we lack information.

      return (missingCExpressionInformation != null
          || missingCLeftMemoryLocation != null)
          && truthAssumption == null &&
          (missingCLeftMemoryLocation != null
          || cLeftMemoryLocation != null);
    }

    public boolean isMissingAssumption() {
      return truthAssumption != null && missingCExpressionInformation != null;
  }

    @SuppressWarnings("unused")
    public MissingInformation(CExpression pMissingCLeftMemoryLocation,
        SMGSymbolicValue pCExpressionValue, boolean pRequieredInformation) {
      missingCExpressionInformation = null;
      missingCLeftMemoryLocation = pMissingCLeftMemoryLocation;
      cExpressionValue = pCExpressionValue;
      cLeftMemoryLocation = null;
      truthAssumption = null;
      requieredInformation = pRequieredInformation;
    }

    public MissingInformation(SMGAddress pCLeftMemoryLocation,
        CRightHandSide pMissingCExpressionInformation, boolean pRequieredInformation) {
      missingCExpressionInformation = pMissingCExpressionInformation;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = pCLeftMemoryLocation;
      truthAssumption = null;
      requieredInformation = pRequieredInformation;
    }

    public MissingInformation(boolean pTruthAssumption,
        ARightHandSide pMissingCExpressionInformation) {

      missingCExpressionInformation = (CExpression) pMissingCExpressionInformation;
      missingCLeftMemoryLocation = null;
      cExpressionValue = null;
      cLeftMemoryLocation = null;
      truthAssumption = pTruthAssumption;
      requieredInformation = false;
    }

    public MissingInformation(CFunctionCallExpression pCFCExpression, boolean pIsRequiered) {
      missingCExpressionInformation = pCFCExpression;
      requieredInformation = pIsRequiered;
      cExpressionValue = null;
      truthAssumption = null;
      missingCLeftMemoryLocation = null;
      cLeftMemoryLocation = null;
    }

    @SuppressWarnings("unused")
    public SMGSymbolicValue getcExpressionValue() {
      checkNotNull(cExpressionValue);
      return cExpressionValue;
    }

    @SuppressWarnings("unused")
    public SMGAddress getcLeftMemoryLocation() {
      checkNotNull(cLeftMemoryLocation);
      return cLeftMemoryLocation;
    }

    @SuppressWarnings("unused")
    public CRightHandSide getMissingCExpressionInformation() {
      checkNotNull(missingCExpressionInformation);
      return missingCExpressionInformation;
    }

    public CExpression getMissingCLeftMemoryLocation() {
      checkNotNull(missingCLeftMemoryLocation);
      return missingCLeftMemoryLocation;
    }

    @SuppressWarnings("unused")
    public Boolean getTruthAssumption() {
      checkNotNull(truthAssumption);
      return truthAssumption;
    }

    public boolean isRequieredInformation() {
      return requieredInformation;
    }
  }





  public interface SMGSymbolicValue extends SMGValue {

  }

  public interface SMGValue {

    public boolean isUnknown();

    public BigInteger getValue();

    public int getAsInt();

    public long getAsLong();
  }

  public interface SMGAddressValue extends SMGSymbolicValue {

    @Override
    public boolean isUnknown();

    public SMGAddress getAddress();

    public SMGExplicitValue getOffset();

    public SMGObject getObject();

  }

  public interface SMGExplicitValue  extends SMGValue {

    public SMGExplicitValue negate();

    public SMGExplicitValue xor(SMGExplicitValue pRVal);

    public SMGExplicitValue or(SMGExplicitValue pRVal);

    public SMGExplicitValue and(SMGExplicitValue pRVal);

    public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal);

    public SMGExplicitValue multiply(SMGExplicitValue pRVal);

    public SMGExplicitValue divide(SMGExplicitValue pRVal);

    public SMGExplicitValue subtract(SMGExplicitValue pRVal);

    public SMGExplicitValue add(SMGExplicitValue pRVal);

  }

  public static abstract class SMGKnownValue {

    /**
     * A symbolic value representing an explicit value.
     */
    private final BigInteger value;

    private SMGKnownValue(BigInteger pValue) {
      checkNotNull(pValue);
      value = pValue;
    }

    private SMGKnownValue(long pValue) {
      value = BigInteger.valueOf(pValue);
    }

    private SMGKnownValue(int pValue) {
      value = BigInteger.valueOf(pValue);
    }

    @Override
    public boolean equals(Object pObj) {

      if (this == pObj) {
        return true;
      }

      if (!(pObj instanceof SMGKnownValue)) {
        return false;
      }

      SMGKnownValue otherValue = (SMGKnownValue) pObj;

      return value.equals(otherValue.value);
    }

    @Override
    public int hashCode() {

      int result = 5;

      int c = value.hashCode();

      return result * 31 + c;
    }

    public final BigInteger getValue() {
      return value;
    }

    public final int getAsInt() {
      return value.intValue();
    }

    public final long getAsLong() {
      return value.longValue();
    }

    @Override
    public String toString() {
      return value.toString();
    }

    public boolean isUnknown() {
      return false;
    }
  }

  public static class SMGKnownSymValue  extends SMGKnownValue implements SMGSymbolicValue {

    public static final SMGKnownSymValue ZERO = new SMGKnownSymValue(BigInteger.ZERO);

    public static final SMGKnownSymValue ONE = new SMGKnownSymValue(BigInteger.ONE);

    public static final SMGKnownSymValue TRUE = new SMGKnownSymValue(BigInteger.valueOf(-1));

    public static final SMGKnownSymValue FALSE = ZERO;

    private SMGKnownSymValue(BigInteger pValue) {
      super(pValue);
    }

    public static SMGKnownSymValue valueOf(int pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownSymValue(BigInteger.valueOf(pValue));
      }
    }

    public static SMGKnownSymValue valueOf(long pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownSymValue(BigInteger.valueOf(pValue));
      }
    }

    public static SMGKnownSymValue valueOf(BigInteger pValue) {

      checkNotNull(pValue);

      if (pValue.equals(BigInteger.ZERO)) {
        return ZERO;
      } else if (pValue.equals(BigInteger.ONE)) {
        return ONE;
      } else {
        return new SMGKnownSymValue(pValue);
      }
    }

    @Override
    public final boolean equals(Object pObj) {

      if (!(pObj instanceof SMGKnownSymValue)) {
        return false;
      }

      return super.equals(pObj);
    }

    @Override
    public final int hashCode() {
      int result = 17;

      result = 31 * result + super.hashCode();

      return result;
    }
  }

  public static final class SMGKnownExpValue extends SMGKnownValue implements SMGExplicitValue {

    public static final SMGKnownExpValue ONE = new SMGKnownExpValue(BigInteger.ONE);

    public static final SMGKnownExpValue ZERO = new SMGKnownExpValue(BigInteger.ZERO);

    private SMGKnownExpValue(BigInteger pValue) {
      super(pValue);
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof SMGKnownExpValue)) {
        return false;
      }

      return super.equals(pObj);
    }

    @Override
    public int hashCode() {

      int result = 5;

      result = 31 * result + super.hashCode();

      return result;
    }

    @Override
    public SMGExplicitValue negate() {
      return valueOf(getValue().negate());
    }

    @Override
    public SMGExplicitValue xor(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().xor(pRVal.getValue()));
    }

    @Override
    public SMGExplicitValue or(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().or(pRVal.getValue()));
    }

    @Override
    public SMGExplicitValue and(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().and(pRVal.getValue()));
    }

    @Override
    public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().shiftLeft(pRVal.getAsInt()));
    }

    @Override
    public SMGExplicitValue multiply(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().multiply(pRVal.getValue()));
    }

    @Override
    public SMGExplicitValue divide(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().divide(pRVal.getValue()));
    }

    @Override
    public SMGExplicitValue subtract(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().subtract(pRVal.getValue()));
    }

    @Override
    public SMGExplicitValue add(SMGExplicitValue pRVal) {

      if (pRVal.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      return valueOf(getValue().add(pRVal.getValue()));
    }

    public static SMGKnownExpValue valueOf(int pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownExpValue(BigInteger.valueOf(pValue));
      }
    }

    public static SMGKnownExpValue valueOf(long pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownExpValue(BigInteger.valueOf(pValue));
      }
    }

    public static SMGKnownExpValue valueOf(BigInteger pValue) {

      checkNotNull(pValue);

      if (pValue.equals(BigInteger.ZERO)) {
        return ZERO;
      } else if (pValue.equals(BigInteger.ONE)) {
        return ONE;
      } else {
        return new SMGKnownExpValue(pValue);
      }
    }
  }


  /**
   * Class representing values which can't be resolved.
   */
  public static final class SMGUnknownValue implements SMGSymbolicValue, SMGExplicitValue, SMGAddressValue
  {

    private static final SMGUnknownValue instance = new SMGUnknownValue();

    @Override
    public String toString() {
      return "UNKNOWN";
    }

    public static SMGUnknownValue getInstance() {
      return instance;
    }

    @Override
    public boolean isUnknown() {
      return true;
    }

    @Override
    public SMGAddress getAddress() {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public BigInteger getValue() {
      throw new  IllegalStateException("Can't get Value of an Unknown Value.");
    }

    @Override
    public int getAsInt() {
      throw new  IllegalStateException("Can't get Value of an Unknown Value.");
    }

    @Override
    public long getAsLong() {
      throw new  IllegalStateException("Can't get Value of an Unknown Value.");
    }

    @Override
    public SMGExplicitValue negate() {
      return instance;
    }

    @Override
    public SMGExplicitValue xor(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue or(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue and(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue shiftLeft(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue multiply(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue divide(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue subtract(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue add(SMGExplicitValue pRVal) {
      return instance;
    }

    @Override
    public SMGExplicitValue getOffset() {
      return instance;
    }

    @Override
    public SMGObject getObject() {
      return null;
    }
  }

  /**
   * A class to represent a field. This class is mainly used
   * to store field Information.
   */
  public static final class SMGField {

    private static final SMGField UNKNOWN = new SMGField(SMGUnknownValue.getInstance(), new CProblemType("unknown"));

    /**
     * the offset of this field relative to the memory
     * this field belongs to.
     */
    private final SMGExplicitValue offset;

    /**
     * The type of this field, it determines its size
     * and the way information stored in this field is read.
     */
    private final CType type;

    public SMGField(SMGExplicitValue pOffset, CType pType) {
      checkNotNull(pOffset);
      checkNotNull(pType);
      offset = pOffset;
      type = pType;
    }

    public SMGExplicitValue getOffset() {
      return offset;
    }

    public CType getType() {
      return type;
    }

    public boolean isUnknown() {
      return offset.isUnknown() || type instanceof CProblemType;
    }

    @Override
    public String toString() {
      return "offset: " + offset + "Type:" + type.toASTString("");
    }

    public static SMGField getUnknownInstance() {
      return UNKNOWN;
    }
  }

  /**
   * A class to represent a value which points to an address. This class is mainly used
   * to store value information.
   */
  public static final class SMGKnownAddVal extends SMGKnownSymValue implements SMGAddressValue {

    /**
     * The address this value represents.
     */
    private final SMGKnownAddress address;

    private SMGKnownAddVal(BigInteger pValue, SMGKnownAddress pAddress) {
      super(pValue);
      checkNotNull(pAddress);
      address = pAddress;
    }

    public static SMGKnownAddVal valueOf(SMGObject pObject, SMGKnownExpValue pOffset, SMGKnownSymValue pAddress) {
      return new SMGKnownAddVal(pAddress.getValue(), SMGKnownAddress.valueOf(pObject, pOffset));
    }

    @Override
    public SMGKnownAddress getAddress() {
      return address;
    }

    public static SMGKnownAddVal valueOf(BigInteger pValue, SMGKnownAddress pAddress) {
      return new SMGKnownAddVal(pValue, pAddress);
    }

    public static SMGKnownAddVal valueOf(SMGKnownSymValue pValue, SMGKnownAddress pAddress) {
      return new SMGKnownAddVal(pValue.getValue(), pAddress);
    }

    public static SMGKnownAddVal valueOf(int pValue, SMGKnownAddress pAddress) {
      return new SMGKnownAddVal(BigInteger.valueOf(pValue), pAddress);
    }

    public static SMGKnownAddVal valueOf(long pValue, SMGKnownAddress pAddress) {
      return new SMGKnownAddVal(BigInteger.valueOf(pValue), pAddress);
    }

    public static SMGKnownAddVal valueOf(int pValue, SMGObject object, int offset) {
      return new SMGKnownAddVal(BigInteger.valueOf(pValue), SMGKnownAddress.valueOf(object, offset));
    }

    @Override
    public String toString() {
      return "Value: " + super.toString() + " " + address.toString();
    }

    @Override
    public SMGKnownExpValue getOffset() {
      return address.getOffset();
    }

    @Override
    public SMGObject getObject() {
      return address.getObject();
    }

    /**
     * A class to represent an Address. This class is mainly used
     * to store Address Information.
     */
    private static class SMGKnownAddress extends SMGAddress {

      private SMGKnownAddress(SMGObject pObject, SMGKnownExpValue pOffset) {
        super(pObject, pOffset);
      }

      public static SMGKnownAddress valueOf(SMGObject pObject, int pOffset) {
        return new SMGKnownAddress(pObject, SMGKnownExpValue.valueOf(pOffset));
      }

      public static SMGKnownAddress valueOf(SMGObject object, SMGKnownExpValue offset) {
        return new SMGKnownAddress(object, offset);
      }

      @Override
      public SMGKnownExpValue getOffset() {
        return (SMGKnownExpValue) super.getOffset();
      }

      @Override
      public SMGObject getObject() {
        return super.getObject();
      }
    }
  }

  /**
   * A class to represent an Address. This class is mainly used
   * to store Address Information.
   */
  public static class SMGAddress  {

    public static final SMGAddress UNKNOWN =
        new SMGAddress();

    private SMGAddress(SMGObject pObject, SMGExplicitValue pOffset) {
      checkNotNull(pOffset);
      object = pObject;
      offset = pOffset;
    }

    private SMGAddress() {
      object = null;
      offset = SMGUnknownValue.getInstance();
    }

    /**
     * The SMGObject representing the Memory this address belongs to.
     */
    private final SMGObject object;

    /**
     * The offset relative to the beginning of object in byte.
     */
    private final SMGExplicitValue offset;

    public final boolean isUnknown() {
      return object == null || offset.isUnknown();
    }

    /**
     * Return an address with (offset + pAddedOffset).
     *
     * @param offset The offset added to this address.
     */
    public final SMGAddress add(SMGExplicitValue pAddedOffset) {

      if (object == null || offset.isUnknown() || pAddedOffset.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      return valueOf(object, offset.add(pAddedOffset));
    }

    public SMGExplicitValue getOffset() {
      return offset;
    }

    public SMGObject getObject() {
      return object;
    }

    public static SMGAddress valueOf(SMGObject object, SMGExplicitValue offset) {
      return new SMGAddress(object, offset);
    }

    @Override
    public final String toString() {

      if (isUnknown()) {
        return "Unkown";
      }

      return "Object: " + object.toString() + " Offset: " + offset.toString();
    }

    public static SMGAddress valueOf(SMGObject pObj, int pOffset) {
      return new SMGAddress(pObj, SMGKnownExpValue.valueOf(pOffset));
    }

    public static SMGAddress getUnknownInstance() {
      return UNKNOWN;
    }
  }

  public static class SMGEdgePointsToAndState {
    private final SMGState smgState;
    private final SMGEdgePointsTo value;

    private SMGEdgePointsToAndState(SMGState pState, SMGEdgePointsTo pValue) {
      smgState = pState;
      value = pValue;
    }

    public SMGEdgePointsToAndState(SMGState pState) {
      smgState = pState;
      value = null;
    }

    public SMGEdgePointsTo getValue() {
      return value;
    }

    public static SMGValueAndState of(SMGState pState) {
      return new SMGValueAndState(pState);
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public static SMGEdgePointsToAndState of(SMGState pState,
        SMGEdgePointsTo pValue) {
      return new SMGEdgePointsToAndState(pState, pValue);
    }
  }
}
