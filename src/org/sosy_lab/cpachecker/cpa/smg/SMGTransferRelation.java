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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
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
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisSMGCommunicator;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.Pair;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


@Options(prefix = "cpa.smg")
public class SMGTransferRelation extends SingleEdgeTransferRelation {

  @Option(secure=true, name = "exportSMG.file", description = "Filename format for SMG graph dumps")

  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate exportSMGFilePattern = PathTemplate.ofFormatString("smg/smg-%s.dot");

  @Option(secure=true, description = "with this option enabled, a check for unreachable memory occurs whenever a function returns, and not only at the end of the main function")
  private boolean checkForMemLeaksAtEveryFrameDrop = true;

  @Option(secure=true, description = "with this option enabled, memory that is not freed before the end of main is reported as memleak even if it is reachable from local variables in main")
  private boolean handleNonFreedMemoryInMainAsMemLeak = false;

  @Option(secure=true, toUppercase=true, name = "exportSMGwhen", description = "Describes when SMG graphs should be dumped.")
  private SMGExportLevel exportSMG = SMGExportLevel.NEVER;

  private static enum SMGExportLevel {NEVER, LEAF, INTERESTING, EVERY}

  @Option(secure=true, name="enableMallocFail", description = "If this Option is enabled, failure of malloc" + "is simulated")
  private boolean enableMallocFailure = true;

  @Option(secure=true, toUppercase=true, name="handleUnknownFunctions", description = "Sets how unknown functions are handled.")
  private UnknownFunctionHandling handleUnknownFunctions = UnknownFunctionHandling.STRICT;

  private static enum UnknownFunctionHandling {STRICT, ASSUME_SAFE}

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

  @Option(secure = true, name="externalAllocationFunction", description = "Function which indicate on external allocated memory")
  private String externalAllocationFunction = "ext_allocation";

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
        return SMGAddressValueAndState.of(currentState);
      }

      long count = countValue.getAsLong();

      if (ch.isUnknown()) {
        // If the symbolic value is not known create a new one.
        ch = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
      }

      SMGObject bufferMemory =  bufferAddress.getObject();

      int offset =  bufferAddress.getOffset().getAsInt();

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

      return SMGAddressValueAndState.of(currentState, bufferAddress);
    }

    protected SMGValueAndStateList evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge, CExpression rValue)
        throws CPATransferException {

      return expressionEvaluator.evaluateExpressionValue(smgState, cfaEdge, rValue);
    }

    protected List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      return expressionEvaluator.evaluateExplicitValue(pState, pCfaEdge, pRValue);
    }

    protected SMGAddressValueAndStateList evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue) throws CPATransferException {
      return expressionEvaluator.evaluateAddress(pState, pCfaEdge, pRvalue);
    }

    public final SMGAddressValueAndStateList evaluateExternalAllocation(CFunctionCallExpression pFunctionCall,
        SMGState pState, CFAEdge pCFAEdge) throws SMGInconsistentException {
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
        evaluateAlloca(valueAndState.getSmgState(), valueAndState.getObject(), cfaEdge, sizeExpr);
      }

      return SMGAddressValueAndStateList.copyOfAddressValueList(result);
    }

    private SMGAddressValueAndState evaluateAlloca(SMGState currentState, SMGExplicitValue pSizeValue, CFAEdge cfaEdge, CRightHandSide sizeExpr) throws CPATransferException {

      SMGExplicitValue sizeValue = pSizeValue;

      if (sizeValue.isUnknown()) {

        if (guessSizeOfUnknownMemorySize) {
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
      SMGAddressValue addressValue = currentState.addNewStackAllocation(sizeValue.getAsInt(), allocation_label);

      possibleMallocFail = true;
      return SMGAddressValueAndState.of(currentState, addressValue);
    }

    private List<SMGExplicitValueAndState> getAllocateFunctionSize(SMGState pState, CFAEdge cfaEdge,
        CFunctionCallExpression functionCall) throws CPATransferException {

      String functionName = functionCall.getFunctionNameExpression().toASTString();

      if (arrayAllocationFunctions.contains(functionName)) {

        List<SMGExplicitValueAndState> result = new ArrayList<>(4);

        List<SMGExplicitValueAndState> numValueAndStates =
            getAllocateFunctionParameter(memoryArrayAllocationFunctionsNumParameter,
                functionCall, pState, cfaEdge);

        for (SMGExplicitValueAndState numValueAndState : numValueAndStates) {
          List<SMGExplicitValueAndState> elemSizeValueAndStates =
              getAllocateFunctionParameter(memoryArrayAllocationFunctionsElemSizeParameter,
                  functionCall, numValueAndState.getSmgState(), cfaEdge);

          for (SMGExplicitValueAndState elemSizeValueAndState : elemSizeValueAndStates) {

            SMGExplicitValue size = numValueAndState.getObject().multiply(elemSizeValueAndState.getObject());
            result.add(SMGExplicitValueAndState.of(elemSizeValueAndState.getSmgState(), size));
          }
        }

        return result;
      } else {
        return getAllocateFunctionParameter(memoryAllocationFunctionsSizeParameter,
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

          if (guessSizeOfUnknownMemorySize) {
            currentState = valueAndState.getSmgState();
            SMGExplicitValueAndState forcedValueAndState = expressionEvaluator.forceExplicitValue(currentState, cfaEdge, sizeExpr);


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
              throw new UnrecognizedCCodeException(
                  "Not able to compute allocation size", cfaEdge);
            }
          } else {
            throw new UnrecognizedCCodeException(
                "Not able to compute allocation size", cfaEdge);
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
        SMGAddressValue new_address = currentState.addNewHeapAllocation(size, allocation_label);

        if (zeroingMemoryAllocation.contains(functionName)) {
          currentState = writeValue(currentState, new_address.getObject(), 0, AnonymousTypes.createTypeWithLength(size),
              SMGKnownSymValue.ZERO, cfaEdge);
        }
        possibleMallocFail = true;
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

      SMGAddressValueAndStateList addressAndStates = expressionEvaluator.evaluateAddress(pState, cfaEdge, pointerExp);

      List<SMGState> resultStates = new ArrayList<>(addressAndStates.size());

      for (SMGAddressValueAndState addressAndState : addressAndStates.asAddressValueAndStateList()) {
        SMGAddressValue address = addressAndState.getObject();
        SMGState currentState = addressAndState.getSmgState();

        if (address.isUnknown()) {
          logger.log(Level.INFO, "Free on expression " + pointerExp.toASTString() + " is invalid, because the target of the address could not be calculated.");
          SMGState invalidFreeState = currentState.setInvalidFree();
          resultStates.add(invalidFreeState);
          continue;
        }

        if (address.getAsInt() == 0) {
          logger.log(Level.WARNING, pFunctionCall.getFileLocation() + ":",
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
    private boolean isNondetBuiltin(String pFunctionName) {
      return pFunctionName.startsWith(NONDET_PREFIX) || pFunctionName.equals("nondet_int");
    }

    public boolean isConfigurableAllocationFunction(String functionName) {
      return memoryAllocationFunctions.contains(functionName) || arrayAllocationFunctions.contains(functionName);
    }

    public boolean isDeallocationFunction(String functionName) {
      return deallocationFunctions.contains(functionName);
    }

    public boolean isExternalAllocationFunction(String functionName) {
      return externalAllocationFunction.equals(functionName);
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
            result.add(evaluateMemcpy(sizeValueAndState.getSmgState(), targetStr1AndState.getObject(), sourceStr2AndState.getObject(), sizeValueAndState.getObject()));
          }
        }
      }

      return SMGAddressValueAndStateList.copyOfAddressValueList(result);
    }

    private SMGAddressValueAndState evaluateMemcpy(SMGState currentState, SMGAddressValue targetStr1Address,
        SMGAddressValue sourceStr2Address, SMGExplicitValue sizeValue) throws SMGInconsistentException {

      // TODO Unsound because source is not overridden
      if (targetStr1Address.isUnknown() || sourceStr2Address.isUnknown()
          || sizeValue.isUnknown()) { return SMGAddressValueAndState.of(currentState, null); }

      SMGObject source = sourceStr2Address.getObject();
      SMGObject target = targetStr1Address.getObject();

      int sourceRangeOffset = sourceStr2Address.getOffset().getAsInt();
      int sourceRangeSize = sizeValue.getAsInt() + sourceRangeOffset;
      int targetRangeOffset = targetStr1Address.getOffset().getAsInt();

      currentState.copy(source, target, sourceRangeOffset, sourceRangeSize, targetRangeOffset);

      return SMGAddressValueAndState.of(currentState, targetStr1Address);
    }
  }

  final private SMGBuiltins builtins = new SMGBuiltins();

  private void plotWhenConfigured(SMGExportLevel level, String pName, SMGState pState, String pLocation) {
    //TODO: A variation for more pConfigs

    if (level == exportSMG) {
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
          Collection<? extends AbstractState> resultOfOneOp = getAbstractSuccessorsForEdge(next, edge);

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
      results = getAbstractSuccessorsForEdge((SMGState)state, cfaEdge);
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

  public static SMGTransferRelation createTransferRelationForRefinement(Configuration config, LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    SMGTransferRelation result = new SMGTransferRelation(config, pLogger, pMachineModel);
    result.exportSMG = SMGExportLevel.NEVER;
    return result;
  }

  private Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      SMGState state, CFAEdge cfaEdge)
          throws CPATransferException {
    logger.log(Level.FINEST, "SMG GetSuccessor >>");
    logger.log(Level.FINEST, "Edge:", cfaEdge.getEdgeType());
    logger.log(Level.FINEST, "Code:", cfaEdge.getCode());

    List<SMGState> successors;

    SMGState smgState = state;

    setInfo(smgState);

    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      successors = handleDeclaration(smgState, (CDeclarationEdge) cfaEdge);
      break;

    case StatementEdge:
      successors = handleStatement(smgState, (CStatementEdge) cfaEdge);
      plotWhenConfigured(SMGExportLevel.INTERESTING, null, successors, cfaEdge.getDescription());
      break;

      // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      successors = handleAssumption(smgState, assumeEdge.getExpression(),
          cfaEdge, assumeEdge.getTruthAssumption(), true);
      plotWhenConfigured(SMGExportLevel.INTERESTING, null, successors, cfaEdge.getDescription());
      break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdge;
      successors = handleFunctionCall(smgState, functionCallEdge);
      plotWhenConfigured(SMGExportLevel.INTERESTING, null, successors, cfaEdge.getDescription());
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) cfaEdge;
      successors = handleFunctionReturn(smgState, functionReturnEdge);
      if (checkForMemLeaksAtEveryFrameDrop) {
        for (SMGState successor : successors) {
          String name = String.format("%03d-%03d-%03d", successor.getPredecessorId(), successor.getId(), id_counter.getAndIncrement());
          plotWhenConfigured(SMGExportLevel.INTERESTING, "beforePrune" + name, successor, cfaEdge.getDescription());
          successor.pruneUnreachable();
        }
      }
      plotWhenConfigured(SMGExportLevel.INTERESTING, null, successors, cfaEdge.getDescription());
      break;

    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      successors = handleExitFromFunction(smgState, returnEdge);

      // if this is the entry function, there is no FunctionReturnEdge
      // so we have to check for memleaks here
      if (returnEdge.getSuccessor().getNumLeavingEdges() == 0) {
        // Ugly, but I do not know how to do better
        // TODO: Handle leaks at any program exit point (abort, etc.)

        for (SMGState successor : successors) {
          if (handleNonFreedMemoryInMainAsMemLeak) {
            successor.dropStackFrame();
          }
          successor.pruneUnreachable();
        }
      }
      plotWhenConfigured(SMGExportLevel.INTERESTING, null, successors, cfaEdge.getDescription());
      break;

    default:
      successors = ImmutableList.of(smgState);
    }

    for (SMGState smg : successors) {
      plotWhenConfigured(SMGExportLevel.EVERY, null, smg, cfaEdge.getDescription());
    }

    return successors;
  }

  private void plotWhenConfigured(SMGExportLevel pInteresting, String pName, List<SMGState> pStates,
      String pDescription) {
    for (SMGState state : pStates) {
      plotWhenConfigured(pInteresting, pName, state, pDescription);
    }
  }

  private void setInfo(SMGState pOldState) {
    missingInformationList = new ArrayList<>(5);
    oldState = pOldState;
    expressionEvaluator.reset();
  }

  private List<SMGState> handleExitFromFunction(SMGState smgState,
      CReturnStatementEdge returnEdge) throws CPATransferException {

    CExpression returnExp = returnEdge.getExpression().or(CIntegerLiteralExpression.ZERO); // 0 is the default in C

    logger.log(Level.FINEST, "Handling return Statement: ", returnExp);

    CType expType = expressionEvaluator.getRealExpressionType(returnExp);
    SMGObject tmpFieldMemory = smgState.getFunctionReturnObject();
    Optional<CAssignment> returnAssignment = returnEdge.asAssignment();
    if (returnAssignment.isPresent()) {
      expType = returnAssignment.get().getLeftHandSide().getExpressionType();
    }

    if (tmpFieldMemory != null) {
      return handleAssignmentToField(smgState, returnEdge, tmpFieldMemory, 0, expType, returnExp);
    }

    return ImmutableList.of(smgState);
  }

  private List<SMGState> handleFunctionReturn(SMGState smgState,
      CFunctionReturnEdge functionReturnEdge) throws CPATransferException {

    logger.log(Level.FINEST, "Handling function return");

    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall exprOnSummary = summaryEdge.getExpression();

    SMGState newState = new SMGState(smgState);

    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {

      // Assign the return value to the lValue of the functionCallAssignment

      CExpression lValue = ((CFunctionCallAssignmentStatement) exprOnSummary).getLeftHandSide();

      CType rValueType = expressionEvaluator.getRealExpressionType(((CFunctionCallAssignmentStatement) exprOnSummary).getRightHandSide());

      SMGSymbolicValue rValue = getFunctionReturnValue(newState, rValueType, functionReturnEdge);

      SMGAddress address = null;

      // Lvalue is one frame above
      newState.dropStackFrame();
      LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(functionReturnEdge, newState);

      List<SMGAddressAndState> addressAndValues = lValue.accept(visitor);

      List<SMGState> result = new ArrayList<>(addressAndValues.size());

      for (SMGAddressAndState addressAndValue : addressAndValues) {
        address = addressAndValue.getObject();
        newState = addressAndValue.getSmgState();

        if (!address.isUnknown()) {

          if (rValue.isUnknown()) {
            rValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
          }

          SMGObject object = address.getObject();

          int offset = address.getOffset().getAsInt();

          SMGState resultState = assignFieldToState(newState, functionReturnEdge, object, offset, rValue, rValueType);
          result.add(resultState);
        } else {
          //TODO missingInformation, exception
          result.add(newState);
        }
      }

      return result;
    } else {
      newState.dropStackFrame();
      return ImmutableList.of(newState);
    }
  }

  private SMGSymbolicValue getFunctionReturnValue(SMGState smgState, CType type, CFAEdge pCFAEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    SMGObject tmpMemory = smgState.getFunctionReturnObject();

    return expressionEvaluator.readValue(smgState, tmpMemory, SMGKnownExpValue.ZERO, type, pCFAEdge).getObject();
  }

  private List<SMGState> handleFunctionCall(SMGState smgState, CFunctionCallEdge callEdge)
      throws CPATransferException, SMGInconsistentException  {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    logger.log(Level.FINEST, "Handling function call: ", functionEntryNode.getFunctionName());

    SMGState initialNewState = new SMGState(smgState);

    CFunctionDeclaration functionDeclaration = functionEntryNode.getFunctionDefinition();

    List<CParameterDeclaration> paramDecl = functionEntryNode.getFunctionParameters();
    List<? extends CExpression> arguments = callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      //TODO Parameter with varArgs
      assert (paramDecl.size() == arguments.size());
    }

    Map<SMGState, List<Pair<SMGRegion,SMGSymbolicValue>>> valuesMap = new HashMap<>();

    //TODO Refactor, ugly

    List<SMGState> newStates = new ArrayList<>(4);

    newStates.add(initialNewState);

    List<Pair<SMGRegion, SMGSymbolicValue>> initialValuesList = new ArrayList<>(paramDecl.size());
    valuesMap.put(initialNewState, initialValuesList);

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramDecl.size(); i++) {

      CExpression exp = arguments.get(i);

      String varName = paramDecl.get(i).getName();
      CType cParamType = expressionEvaluator.getRealExpressionType(paramDecl.get(i));


      SMGRegion paramObj;
      // If parameter is a array, convert to pointer
      if (cParamType instanceof CArrayType) {
        int size = machineModel.getSizeofPtr();
        paramObj = new SMGRegion(size, varName);
      } else {
        int size = machineModel.getSizeof(cParamType);
        paramObj = new SMGRegion(size, varName);
      }

      List<SMGState> result = new ArrayList<>(4);

      for(SMGState newState : newStates) {
        // We want to write a possible new Address in the new State, but
        // explore the old state for the parameters
        SMGValueAndStateList stateValues = readValueToBeAssiged(newState, callEdge, paramObj, 0, exp);

        for(SMGValueAndState stateValue : stateValues.getValueAndStateList()) {
          SMGState newStateWithReadSymbolicValue = stateValue.getSmgState();
          SMGSymbolicValue value = stateValue.getObject();
          List<SMGState> newStatesWithExpVal = assignExplicitValueToSymbolicValue(newStateWithReadSymbolicValue, callEdge, value, exp);

          for (SMGState newStateWithExpVal : newStatesWithExpVal) {

            if (!valuesMap.containsKey(newStateWithExpVal)) {
              valuesMap.put(newStateWithExpVal, valuesMap.get(initialNewState));
            }

            Pair<SMGRegion, SMGSymbolicValue> lhsValuePair = Pair.of(paramObj, value);
            valuesMap.get(newStateWithExpVal).add(i, lhsValuePair);
            result.add(newStateWithExpVal);
          }
        }
      }

      newStates = result;
    }

    for(SMGState newState : newStates) {
      newState.addStackFrame(functionDeclaration);

      // get value of actual parameter in caller function context
      for (int i = 0; i < paramDecl.size(); i++) {

        CExpression exp = arguments.get(i);

        String varName = paramDecl.get(i).getName();
        CType cParamType = expressionEvaluator.getRealExpressionType(paramDecl.get(i));
        CType rValueType = expressionEvaluator.getRealExpressionType(exp.getExpressionType());
        // if function declaration is in form 'int foo(char b[32])' then omit array length
        if (rValueType instanceof CArrayType) {
          rValueType = new CPointerType(rValueType.isConst(), rValueType.isVolatile(), ((CArrayType)rValueType).getType());
        }

        if (cParamType instanceof CArrayType) {
          cParamType = new CPointerType(cParamType.isConst(), cParamType.isVolatile(), ((CArrayType) cParamType).getType());
        }

        List<Pair<SMGRegion, SMGSymbolicValue>> values = valuesMap.get(newState);
        SMGRegion newObject = values.get(i).getFirst();
        SMGSymbolicValue symbolicValue = values.get(i).getSecond();

        int typeSize = expressionEvaluator.getSizeof(callEdge, cParamType, newState);

        newState.addLocalVariable(typeSize, varName, newObject);

        //TODO (  cast expression)

        //6.5.16.1 right operand is converted to type of assignment expression
        // 6.5.26 The type of an assignment expression is the type the left operand would have after lvalue conversion.
        rValueType = cParamType;

        // We want to write a possible new Address in the new State, but
        // explore the old state for the parameters
        newState = assignFieldToState(newState, callEdge, newObject, 0, symbolicValue, rValueType);
      }
    }

    return newStates;
  }

  private List<SMGState> handleAssumption(SMGState pSmgState, CExpression expression, CFAEdge cfaEdge,
      boolean truthValue, boolean createNewStateIfNecessary) throws CPATransferException {

    SMGState smgState = pSmgState;

    // FIXME Quickfix, simplify expressions for sv-comp, later assumption handling has to be refactored to be able to handle complex expressions
    expression = eliminateOuterEquals(expression);

    // get the value of the expression (either true[-1], false[0], or unknown[null])
    AssumeVisitor visitor = expressionEvaluator.getAssumeVisitor(cfaEdge, smgState);
    SMGValueAndStateList valueAndStates = expression.accept(visitor);

    List<SMGState> result = new ArrayList<>();

    for(SMGValueAndState valueAndState : valueAndStates.getValueAndStateList()) {

      SMGSymbolicValue value = valueAndState.getObject();
      smgState = valueAndState.getSmgState();

      if (!value.isUnknown()) {
        if ((truthValue && value.equals(SMGKnownSymValue.TRUE)) ||
            (!truthValue && value.equals(SMGKnownSymValue.FALSE))) {
          result.add(smgState);
        } else {
          // This signals that there are no new States reachable from this State i. e. the
          // Assumption does not hold.
        }
      } else {
        result.addAll(
            deriveFurtherInformationFromAssumption(smgState, visitor, cfaEdge, truthValue, expression,
                createNewStateIfNecessary));
      }
    }

    return result;
  }

  private List<SMGState> deriveFurtherInformationFromAssumption(SMGState pSmgState, AssumeVisitor visitor,
      CFAEdge cfaEdge, boolean truthValue, CExpression expression, boolean createNewStateIfNecessary) throws CPATransferException {

    SMGState smgState = pSmgState;

    boolean impliesEqOn = visitor.impliesEqOn(truthValue, smgState);
    boolean impliesNeqOn = visitor.impliesNeqOn(truthValue, smgState);

    SMGSymbolicValue val1ImpliesOn;
    SMGSymbolicValue val2ImpliesOn;

    if(impliesEqOn || impliesNeqOn ) {
      val1ImpliesOn = visitor.impliesVal1(smgState);
      val2ImpliesOn = visitor.impliesVal2(smgState);
    } else {
      val1ImpliesOn = SMGUnknownValue.getInstance();
      val2ImpliesOn = SMGUnknownValue.getInstance();
    }

    List<SMGExplicitValueAndState> explicitValueAndStates = expressionEvaluator.evaluateExplicitValue(smgState, cfaEdge, expression);

    List<SMGState> result = new ArrayList<>(explicitValueAndStates.size());

    for (SMGExplicitValueAndState explicitValueAndState : explicitValueAndStates) {

      SMGExplicitValue explicitValue = explicitValueAndState.getObject();
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
        result.add(newState);
      } else if ((truthValue && explicitValue.equals(SMGKnownExpValue.ONE))
          || (!truthValue && explicitValue.equals(SMGKnownExpValue.ZERO))) {
        result.add(smgState);
      } else {
        // This signals that there are no new States reachable from this State i. e. the
        // Assumption does not hold.
      }
    }

    return ImmutableList.copyOf(result);
  }

  private CExpression eliminateOuterEquals(CExpression pExpression) {

    if (!(pExpression instanceof CBinaryExpression)) {
      return pExpression;
    }

    CBinaryExpression binExp = (CBinaryExpression) pExpression;

    CExpression op1 = binExp.getOperand1();
    CExpression op2 = binExp.getOperand2();
    BinaryOperator op = binExp.getOperator();

    if (!(op1 instanceof CBinaryExpression && op2 instanceof CIntegerLiteralExpression && op == BinaryOperator.EQUALS)) {
      return pExpression;
    }

    CBinaryExpression binExpOp1 = (CBinaryExpression) op1;
    CIntegerLiteralExpression IntOp2 = (CIntegerLiteralExpression) op2;

    if(IntOp2.getValue().longValue() != 0) {
      return pExpression;
    }

    switch (binExpOp1.getOperator()) {
    case EQUALS:
      return new CBinaryExpression(binExpOp1.getFileLocation(), binExpOp1.getExpressionType(),
          binExpOp1.getCalculationType(), binExpOp1.getOperand1(), binExpOp1.getOperand2(), BinaryOperator.NOT_EQUALS);
    case NOT_EQUALS:
      return new CBinaryExpression(binExpOp1.getFileLocation(), binExpOp1.getExpressionType(),
          binExpOp1.getCalculationType(), binExpOp1.getOperand1(), binExpOp1.getOperand2(), BinaryOperator.EQUALS);
    default:
      return pExpression;
    }


  }

  private List<SMGState> handleStatement(SMGState pState, CStatementEdge pCfaEdge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling statement");
    List<SMGState> newStates = null;

    CStatement cStmt = pCfaEdge.getStatement();

    if (cStmt instanceof CAssignment) {
      CAssignment cAssignment = (CAssignment) cStmt;
      CExpression lValue = cAssignment.getLeftHandSide();
      CRightHandSide rValue = cAssignment.getRightHandSide();

      newStates = handleAssignment(pState, pCfaEdge, lValue, rValue);
    } else if (cStmt instanceof CFunctionCallStatement) {

      CFunctionCallStatement cFCall = (CFunctionCallStatement) cStmt;
      CFunctionCallExpression cFCExpression = cFCall.getFunctionCallExpression();
      CExpression fileNameExpression = cFCExpression.getFunctionNameExpression();
      boolean isRequiered = false;
      String functionName = fileNameExpression.toASTString();

      if (builtins.isABuiltIn(functionName)) {
        SMGState newState = new SMGState(pState);
        if (builtins.isConfigurableAllocationFunction(functionName)) {
          logger.log(Level.WARNING, pCfaEdge.getFileLocation() + ":",
              "Calling " + functionName + " and not using the result, resulting in memory leak.");
          newStates = builtins.evaluateConfigurableAllocationFunction(cFCExpression, newState, pCfaEdge).asSMGStateList();

          for (SMGState state : newStates) {
            state.setMemLeak();
          }
          isRequiered = true;
        }
        if (builtins.isDeallocationFunction(functionName)) {
          newStates = builtins.evaluateFree(cFCExpression, newState, pCfaEdge);
        }
        if (builtins.isExternalAllocationFunction(functionName)) {
          newStates = builtins.evaluateExternalAllocation(cFCExpression, newState, pCfaEdge).asSMGStateList();
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
          newStates = builtins.evaluateAlloca(cFCExpression, newState, pCfaEdge).asSMGStateList();
          break;
        case "memset":
          SMGAddressValueAndStateList result = builtins.evaluateMemset(cFCExpression, newState, pCfaEdge);
          newStates = result.asSMGStateList();
          break;
        case "memcpy":
          result = builtins.evaluateMemcpy(cFCExpression, newState, pCfaEdge);
          newStates = result.asSMGStateList();
          break;
        case "printf":
          return ImmutableList.of(new SMGState(pState));
        default:
          // nothing to do here
        }

        if (expressionEvaluator.missingExplicitInformation) {
          missingInformationList.add(new MissingInformation(cFCExpression, isRequiered));
          expressionEvaluator.reset();
        }

      } else {
        switch (handleUnknownFunctions) {
        case STRICT:
          throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
        case ASSUME_SAFE:
          return ImmutableList.of(pState);
        default:
          throw new AssertionError("Unhandled enum value in switch: " + handleUnknownFunctions);
        }
      }
    } else {
      newStates = ImmutableList.of(pState);
    }

    return newStates;
  }

  private List<SMGState> handleAssignment(SMGState pState, CFAEdge cfaEdge, CExpression lValue,
      CRightHandSide rValue) throws CPATransferException {

    SMGState state = pState;
    logger.log(Level.FINEST, "Handling assignment:", lValue, "=", rValue);

    List<SMGState> result = new ArrayList<>(4);

    LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(cfaEdge, state);

    List<SMGAddressAndState> addressOfFieldAndStates = lValue.accept(visitor);

    for (SMGAddressAndState addressOfFieldAndState : addressOfFieldAndStates) {
      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      state = addressOfFieldAndState.getSmgState();

      CType fieldType = expressionEvaluator.getRealExpressionType(lValue);

      if (addressOfField.isUnknown()) {
        addMissingInformation(lValue, rValue);
        //TODO: Really? I would say that when we do not know where to write a value, we are in trouble
        /* Maybe defining it as relevant? In some cases, we can get the address through the explicitCPA.
         * In all other cases we could give an Invalid Write*/
        result.add(new SMGState(state));
      } else {
        List<SMGState> newStates =
            handleAssignmentToField(state, cfaEdge, addressOfField.getObject(),
                addressOfField.getOffset().getAsInt(), fieldType, rValue);
        result.addAll(newStates);
      }
    }

    return result;
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
  private SMGValueAndStateList readValueToBeAssiged(SMGState pNewState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CRightHandSide rValue) throws CPATransferException {

    SMGValueAndStateList valueAndStates = expressionEvaluator.evaluateExpressionValue(pNewState, cfaEdge, rValue);

    List<SMGValueAndState> resultValueAndStates = new ArrayList<>(valueAndStates.size());

    for (SMGValueAndState valueAndState : valueAndStates.getValueAndStateList()) {
      SMGSymbolicValue value = valueAndState.getObject();

      if (value.isUnknown()) {

        if (expressionEvaluator.isMissingExplicitInformation()) {
          addMissingInformation(memoryOfField, fieldOffset, rValue, expressionEvaluator.isRequiered());
          expressionEvaluator.reset();
        }

        value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
        valueAndState = SMGValueAndState.of(valueAndState.getSmgState(), value);
      }
      resultValueAndStates.add(valueAndState);
    }
    return SMGValueAndStateList.copyOf(resultValueAndStates);
  }

  // assign value of given expression to State at given location
  private List<SMGState> assignFieldToState(SMGState pNewState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pLFieldType, CRightHandSide rValue)
          throws CPATransferException {

    List<SMGState> result = new ArrayList<>(4);

    CType rValueType = expressionEvaluator.getRealExpressionType(rValue);

    SMGValueAndStateList valueAndStates = readValueToBeAssiged(pNewState, cfaEdge, memoryOfField, fieldOffset, rValue);

    for (SMGValueAndState valueAndState : valueAndStates.getValueAndStateList()) {
      SMGSymbolicValue value = valueAndState.getObject();
      SMGState newState = valueAndState.getSmgState();


      //TODO (  cast expression)

      //6.5.16.1 right operand is converted to type of assignment expression
      // 6.5.26 The type of an assignment expression is the type the left operand would have after lvalue conversion.
      rValueType = pLFieldType;

      List<SMGState> newStates = assignExplicitValueToSymbolicValue(newState, cfaEdge, value, rValue);

      for (SMGState currentNewState : newStates) {
        newState = assignFieldToState(currentNewState, cfaEdge, memoryOfField, fieldOffset, value, rValueType);
        result.add(newState);
      }
    }

    return result;
  }

  // Assign symbolic value to the explicit value calculated from pRvalue
  private List<SMGState> assignExplicitValueToSymbolicValue(SMGState pNewState,
      CFAEdge pCfaEdge, SMGSymbolicValue value, CRightHandSide pRValue)
          throws CPATransferException {

    SMGExpressionEvaluator expEvaluator = new SMGExpressionEvaluator(logger,
        machineModel);

    List<SMGExplicitValueAndState> expValueAndStates = expEvaluator.evaluateExplicitValue(pNewState, pCfaEdge, pRValue);

    for (SMGExplicitValueAndState expValueAndState : expValueAndStates) {
      SMGExplicitValue expValue = expValueAndState.getObject();
      SMGState newState = expValueAndState.getSmgState();

      if (!expValue.isUnknown()) {
        newState.putExplicit((SMGKnownSymValue) value, (SMGKnownExpValue) expValue);
      }
    }

    List<SMGState> result =
        FluentIterable.from(expValueAndStates).transform(new Function<SMGExplicitValueAndState, SMGState>() {

          @Override
          public SMGState apply(SMGExplicitValueAndState valueAndState) {
            return valueAndState.getSmgState();
          }
        }).toList();

    return result;
  }

  private void addMissingInformation(SMGObject pMemoryOfField, int pFieldOffset, CRightHandSide pRValue,
      boolean isRequiered) {

    SMGAddress address = SMGAddress.valueOf(pMemoryOfField, SMGKnownExpValue.valueOf(pFieldOffset));

    missingInformationList.add(
        new MissingInformation(address, pRValue, isRequiered));
  }

  private SMGState assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, SMGSymbolicValue value, CType rValueType)
      throws UnrecognizedCCodeException, SMGInconsistentException {

    //FIXME Does not work with variable array length.
    if (memoryOfField.getSize() < expressionEvaluator.getSizeof(cfaEdge, rValueType, newState)) {
      logger.log(Level.WARNING, cfaEdge.getFileLocation() + ":",
          "Attempting to write " + expressionEvaluator.getSizeof(cfaEdge, rValueType, newState) +
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

      //FIXME Does not work with variable array length.
      int structSize = structOffset + expressionEvaluator.getSizeof(pCfaEdge, pRValueType, pNewState);
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

  //FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject = pFieldOffset < 0
        || pFieldOffset + expressionEvaluator.getSizeof(pEdge, pRValueType, pNewState) > pMemoryOfField.getSize();

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

  private List<SMGState> handleAssignmentToField(SMGState state, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pLFieldType, CRightHandSide rValue)
      throws CPATransferException {

    SMGState newState = new SMGState(state);

    List<SMGState> newStates = assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pLFieldType, rValue);

    // If Assignment contained malloc, handle possible fail with
    // alternate State (don't create state if not enabled)
    if (possibleMallocFail && enableMallocFailure) {
      possibleMallocFail = false;
      SMGState otherState = new SMGState(state);
      CType rValueType = expressionEvaluator.getRealExpressionType(rValue);
      SMGState mallocFailState =
          writeValue(otherState, memoryOfField, fieldOffset, rValueType, SMGKnownSymValue.ZERO, cfaEdge);
      newStates.add(mallocFailState);
    }

    return newStates;
  }

  private List<SMGState> handleVariableDeclaration(SMGState pState, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge) throws CPATransferException {
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
      int typeSize = expressionEvaluator.getSizeof(pEdge, cType, pState);

      if (pVarDecl.isGlobal()) {
        newObject = pState.addGlobalVariable(typeSize, varName);
      } else {
        newObject = pState.addLocalVariable(typeSize, varName);
      }
    }

    List<SMGState> newStates = handleInitializerForDeclaration(pState, newObject, pVarDecl, pEdge);
    return newStates;
  }

  private List<SMGState> handleDeclaration(SMGState smgState, CDeclarationEdge edge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling declaration");

    CDeclaration cDecl = edge.getDeclaration();

    if (!(cDecl instanceof CVariableDeclaration)) {
      return ImmutableList.of(smgState);
    }

    SMGState newState = new SMGState(smgState);

    List<SMGState> newStates = handleVariableDeclaration(newState, (CVariableDeclaration)cDecl, edge);

    return newStates;
  }

  private List<SMGState> handleInitializerForDeclaration(SMGState pState, SMGObject pObject, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge) throws CPATransferException {
    CInitializer newInitializer = pVarDecl.getInitializer();
    CType cType = expressionEvaluator.getRealExpressionType(pVarDecl);

    if (newInitializer != null) {
      logger.log(Level.FINEST, "Handling variable declaration: handling initializer");

      return handleInitializer(pState, pVarDecl, pEdge, pObject, 0, cType, newInitializer);
    } else if (pVarDecl.isGlobal()) {

      // Global variables without initializer are nullified in C
      pState = writeValue(pState, pObject, 0, cType, SMGKnownSymValue.ZERO, pEdge);
    }

    return ImmutableList.of(pState);
  }

  private List<SMGState> handleInitializer(SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
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
      throw new AssertionError("Error in handling initializer, designated Initializer " + pInitializer.toASTString()
          + " should not appear at this point.");

    } else {
      throw new UnrecognizedCCodeException("Did not recognize Initializer", pInitializer);
    }
  }

  private List<SMGState> handleInitializerList(SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
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

    return ImmutableList.of(pNewState);
  }

  private Pair<Integer, Integer> calculateOffsetAndPostionOfFieldFromDesignator(
      int offsetAtStartOfStruct,
      List<CCompositeTypeMemberDeclaration> pMemberTypes,
      CDesignatedInitializer pInitializer,
      CFAEdge pEdge,
      SMGState pNewState,
      CCompositeType pLValueType) throws UnrecognizedCCodeException {

    // TODO More Designators?
    assert pInitializer.getDesignators().size() == 1;

    String fieldDesignator = ((CFieldDesignator) pInitializer.getDesignators().get(0)).getFieldName();

    int offset = offsetAtStartOfStruct;

    for (int listCounter = 0; listCounter < pMemberTypes.size(); listCounter++) {

      CCompositeTypeMemberDeclaration memberDcl = pMemberTypes.get(listCounter);

      if (memberDcl.getName().equals(fieldDesignator)) {
        return Pair.of(offset, listCounter);
      } else {
        if (pLValueType.getKind() == ComplexTypeKind.STRUCT) {
          offset = offset + expressionEvaluator.getSizeof(pEdge, memberDcl.getType(), pNewState);
        }
      }
    }
    throw new UnrecognizedCCodeException("CDesignator field name not in struct.", pInitializer);
  }

  private List<SMGState> handleInitializerList(
      SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CCompositeType pLValueType,
      CInitializerList pNewInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    int listCounter = 0;

    List<CCompositeType.CCompositeTypeMemberDeclaration> memberTypes = pLValueType.getMembers();

    Pair<SMGState, Integer> startOffsetAndState = Pair.of(pNewState, pOffset);

    List<Pair<SMGState, Integer>> offsetAndStates = new ArrayList<>();
    offsetAndStates.add(startOffsetAndState);

    // Move preinitialization of global variable because of unpredictable fields' order within CDesignatedInitializer
    if (pVarDecl.isGlobal()) {

      List<Pair<SMGState, Integer>> result = new ArrayList<>(offsetAndStates.size());

      for (Pair<SMGState, Integer> offsetAndState : offsetAndStates) {

        int offset = offsetAndState.getSecond();
        SMGState newState = offsetAndState.getFirst();

        int sizeOfType = expressionEvaluator.getSizeof(pEdge, pLValueType, pNewState);

        if (offset - pOffset < sizeOfType) {
          newState = writeValue(newState, pNewObject, offset,
              AnonymousTypes.createTypeWithLength(sizeOfType - (offset - pOffset)), SMGKnownSymValue.ZERO, pEdge);
        }

        result.add(Pair.of(newState, offset));
      }

      offsetAndStates = result;
    }


    for (CInitializer initializer : pNewInitializer.getInitializers()) {

      if (initializer instanceof CDesignatedInitializer) {
        Pair<Integer, Integer> offsetAndPosition =
            calculateOffsetAndPostionOfFieldFromDesignator(pOffset, memberTypes,
                (CDesignatedInitializer) initializer, pEdge, pNewState, pLValueType);

        int offset = offsetAndPosition.getFirst();
        listCounter = offsetAndPosition.getSecond();
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();

        List<Pair<SMGState, Integer>> resultOffsetAndStatesDesignated = new ArrayList<>();
        resultOffsetAndStatesDesignated.add(Pair.of(pNewState, offset));

        offsetAndStates = resultOffsetAndStatesDesignated;

      }

      if (listCounter >= memberTypes.size()) {
        throw new UnrecognizedCCodeException(
          "More Initializer in initializer list "
              + pNewInitializer.toASTString()
              + " than fit in type "
              + pLValueType.toASTString(""),
          pEdge); }

      CType memberType = memberTypes.get(listCounter).getType();

      List<Pair<SMGState, Integer>> resultOffsetAndStates = new ArrayList<>();

      for (Pair<SMGState, Integer> offsetAndState : offsetAndStates) {

        int offset = offsetAndState.getSecond();
        SMGState newState = offsetAndState.getFirst();

        List<SMGState> pNewStates =
            handleInitializer(pNewState, pVarDecl, pEdge, pNewObject, offset, memberType, initializer);

        offset = offset + expressionEvaluator.getSizeof(pEdge, memberType, newState);

        List<? extends Pair<SMGState, Integer>> newStatesAndOffset =
            FluentIterable.from(pNewStates).transform(new ListToListOfPairFunction<SMGState, Integer>(offset))
                .toList();

        resultOffsetAndStates.addAll(newStatesAndOffset);
      }

      offsetAndStates = resultOffsetAndStates;
      listCounter++;
    }

    return FluentIterable.from(offsetAndStates).transform(new Function<Pair<SMGState, Integer>, SMGState>() {

      @Override
      public SMGState apply(Pair<SMGState, Integer> pInput) {
        return pInput.getFirst();
      }
    }).toList();
  }

  private static class ListToListOfPairFunction<F, T> implements Function<F, Pair<F, T>> {

    private final T constant;

    public ListToListOfPairFunction(T pConstant) {
      constant = pConstant;
    }

    @Override
    public Pair<F, T> apply(F listElements) {
      return Pair.of(listElements, constant);
    }
  }

  private List<SMGState> handleInitializerList(
      SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CArrayType pLValueType,
      CInitializerList pNewInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    int listCounter = 0;

    CType elementType = pLValueType.getType();

    int sizeOfElementType = expressionEvaluator.getSizeof(pEdge, elementType, pNewState);

    List<SMGState> newStates = new ArrayList<>(4);
    newStates.add(pNewState);

    for (CInitializer initializer : pNewInitializer.getInitializers()) {

      int offset = pOffset + listCounter * sizeOfElementType;

      List<SMGState> result = new ArrayList<>();

      for (SMGState newState : newStates) {
        result.addAll(handleInitializer(newState, pVarDecl, pEdge,
            pNewObject, offset, pLValueType.getType(), initializer));
      }

      newStates = result;
      listCounter++;
    }

    if (pVarDecl.isGlobal()) {

      List<SMGState> result = new ArrayList<>(newStates.size());

      for (SMGState newState : newStates) {
        int sizeOfType = expressionEvaluator.getSizeof(pEdge, pLValueType, pNewState);

        int offset = pOffset + listCounter * sizeOfElementType;
        if (offset - pOffset < sizeOfType) {
          newState = writeValue(newState, pNewObject, offset,
              AnonymousTypes.createTypeWithLength(sizeOfType - (offset - pOffset)), SMGKnownSymValue.ZERO, pEdge);
        }

        result.add(newState);
      }
      newStates = result;
    }

    return ImmutableList.copyOf(newStates);
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

      //FIXME Does not work with variable array length.
      boolean doesNotFitIntoObject = fieldOffset < 0
          || fieldOffset + getSizeof(pEdge, pType, pSmgState) > pObject.getSize();

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

        SMGExplicitValue rValue = evaluateExplicitValueV2(assignableState, edge, exp);

        if (rValue.isUnknown()) {
          // no further information can be inferred
          return;
        }

        SMGSymbolicValue rSymValue = evaluateExpressionValueV2(assignableState, edge, lValue);

        if(rSymValue.isUnknown()) {

          rSymValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());

          SMGExpressionEvaluator.LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

          List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

          if (addressOfFields.size() != 1) {
            return;
          }

          SMGAddress addressOfField = addressOfFields.get(0).getObject();

          if (addressOfField.isUnknown()) {
            return;
          }

          assignableState = writeValue(assignableState, addressOfField.getObject(),
              addressOfField.getOffset().getAsInt(), getRealExpressionType(exp), rSymValue, edge);
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
          break;
        default:
          // TODO alignof is not handled
        }

        return null;
      }

      private void deriveFurtherInformation(CLeftHandSide lValue) throws CPATransferException {

        if (truthValue == true) {
          return; // no further explicit Information can be derived
        }

        SMGExpressionEvaluator.LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

        List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

        if(addressOfFields.size() != 1) {
          return;
        }

        SMGAddress addressOfField = addressOfFields.get(0).getObject();

        if (addressOfField.isUnknown()) {
          return;
        }

        // If this value is known, the assumption can be evaluated, therefore it should be unknown
        assert evaluateExplicitValueV2(assignableState, edge, lValue).isUnknown();

        SMGSymbolicValue value = evaluateExpressionValueV2(assignableState, edge, lValue);

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
      public List<SMGAddressAndState> visit(CIdExpression variableName) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: variable assignment");

        // a = ...
        return super.visit(variableName);
      }

      @Override
      public List<SMGAddressAndState> visit(CPointerExpression pLValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to dereferenced pointer");

        List<SMGAddressAndState> addresses = super.visit(pLValue);

        for (SMGAddressAndState address : addresses) {
          if (address.getObject().isUnknown()) {
            address.getSmgState().setUnknownDereference();
          }
        }
        return addresses;
      }

      @Override
      public List<SMGAddressAndState> visit(CFieldReference lValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to field reference");

        return super.visit(lValue);
      }

      @Override
      public List<SMGAddressAndState> visit(CArraySubscriptExpression lValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to array Cell");

        return super.visit(lValue);
      }
    }

    private class ExpressionValueVisitor extends SMGExpressionEvaluator.ExpressionValueVisitor {

      public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
        super(pEdge, pSmgState);
      }

      @Override
      public SMGValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws CPATransferException {

        CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
        String functionName = fileNameExpression.toASTString();

        //TODO extreme code sharing ...

        // If Calloc and Malloc have not been properly declared,
        // they may be shown to return void
        if (builtins.isABuiltIn(functionName)) {
          if (builtins.isConfigurableAllocationFunction(functionName)) {
            possibleMallocFail = true;
            SMGAddressValueAndStateList configAllocEdge = builtins.evaluateConfigurableAllocationFunction(
                pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return configAllocEdge;
          }
          if (builtins.isExternalAllocationFunction(functionName)) {
            SMGAddressValueAndStateList extAllocEdge = builtins.evaluateExternalAllocation
                (pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return extAllocEdge;
          }
          switch (functionName) {
          case "__VERIFIER_BUILTIN_PLOT":
            builtins.evaluateVBPlot(pIastFunctionCallExpression, getInitialSmgState());
            break;
          case "__builtin_alloca":
            possibleMallocFail = true;
            SMGAddressValueAndStateList allocEdge = builtins.evaluateAlloca(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return allocEdge;
          case "printf":
            return SMGValueAndStateList.of(getInitialSmgState());
          default:
            if (builtins.isNondetBuiltin(functionName)) {
              return SMGValueAndStateList.of(getInitialSmgState());
            } else {
              throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
            }
          }
        } else {
          switch (handleUnknownFunctions) {
          case STRICT:
            throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
          case ASSUME_SAFE:
            return SMGValueAndStateList.of(getInitialSmgState());
          default:
            throw new AssertionError("Unhandled enum value in switch: " + handleUnknownFunctions);
          }
        }

        return SMGValueAndStateList.of(getInitialSmgState());
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
          SMGValueAndStateList symbolicValueAndStates = evaluateExpressionValue(getNewState(),
              getEdge(), exp);

          if(symbolicValueAndStates.size() != 1) {
            throw new SMGInconsistentException("Found abstraction where non should exist,due to the expression " + exp.toASTString() + "already being evaluated once in this transferrelation step.");
          } else {
            symbolicValueAndState = symbolicValueAndStates.getValueAndStateList().get(0);
          }

        } catch (CPATransferException e) {
          UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
              "SMG cannot get symbolic value of : " + exp.toASTString(), exp);
          e2.initCause(e);
          throw e2;
        }

        SMGSymbolicValue value = symbolicValueAndState.getObject();
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
      protected SMGAddressValueAndState createAddressOfFunction(CIdExpression pIdFunctionExpression)
          throws SMGInconsistentException {
        SMGState state = getInitialSmgState();

        CFunctionDeclaration functionDcl = (CFunctionDeclaration) pIdFunctionExpression.getDeclaration();

        SMGObject functionObject =
            state.getObjectForFunction(functionDcl);

        if (functionObject == null) {
          functionObject = state.createObjectForFunction(functionDcl);
        }

        return createAddress(state, functionObject, SMGKnownExpValue.ZERO);
      }

      @Override
      public SMGAddressValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws CPATransferException {
        CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
        String functionName = fileNameExpression.toASTString();

        if (builtins.isABuiltIn(functionName)) {
          if (builtins.isConfigurableAllocationFunction(functionName)) {
            possibleMallocFail = true;
            SMGAddressValueAndStateList configAllocEdge = builtins.evaluateConfigurableAllocationFunction(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return configAllocEdge;
          }
          switch (functionName) {
          case "__builtin_alloca":
            SMGAddressValueAndStateList allocEdge = builtins.evaluateAlloca(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return allocEdge;
          case "memset":
            SMGAddressValueAndStateList memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return memsetTargetEdge;
          case "memcpy":
            SMGAddressValueAndStateList memcpyTargetEdge = builtins.evaluateMemcpy(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
            return memcpyTargetEdge;
          case "printf":
            return SMGAddressValueAndStateList.of(getInitialSmgState());
          default:
            if (builtins.isNondetBuiltin(functionName)) {
              return SMGAddressValueAndStateList.of(getInitialSmgState());
            } else {
              throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
            }
          }
        } else {
          switch (handleUnknownFunctions) {
          case STRICT:
            throw new CPATransferException(
                "Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
          case ASSUME_SAFE:
            return SMGAddressValueAndStateList.of(getInitialSmgState());
          default:
            throw new AssertionError("Unhandled enum value in switch: " + handleUnknownFunctions);
          }
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
    public List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue) throws CPATransferException {

      List<SMGExplicitValueAndState> explicitValues = super.evaluateExplicitValue(pSmgState, pCfaEdge, pRValue);

      for (SMGExplicitValueAndState explicitValue : explicitValues) {
        if (explicitValue.getObject().isUnknown()) {
          missingExplicitInformation = true;
        }
      }

      return explicitValues;
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
      List<SMGState> newElements = handleAssumption(newElement, ((CAssumeEdge)assume).getExpression(), pCfaEdge, assume.getTruthAssumption(), pElement == newElement);

      assert newElements.size() < 2;

      if (newElements.isEmpty()) {
        newElement = null;
        break;
      } else {
        newElement = newElements.get(0);
      }
    }

    if (newElement == null) {
      return Collections.emptyList();
    } else {
      plotWhenConfigured(SMGExportLevel.EVERY, null, newElement, assumeDesc.toString());
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
          (CFunctionCallExpression) rValue, pEdge).getValueAndStateList().get(0).getObject();
    } else {

      String functionName = pEdge.getPredecessor().getFunctionName();

      ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(pExplicitState,
          functionName, oldState, machineModel, logger, pEdge);

      return cc.evaluateSMGExpression(rValue);
    }
  }

  private SMGValueAndStateList resolveFunctionCall(SMGState pSmgState,
      ValueAnalysisState pExplicitState,
      CFunctionCallExpression pIastFunctionCallExpression,
      CFAEdge pEdge) throws CPATransferException {

    SMGExplicitBuiltIns builtins = new SMGExplicitBuiltIns(pExplicitState);

    CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
    String functionName = fileNameExpression.toASTString();

    if (builtins.isABuiltIn(functionName)) {
      if (builtins.isConfigurableAllocationFunction(functionName)) {
        SMGAddressValueAndStateList configAllocEdge = builtins.evaluateConfigurableAllocationFunction(pIastFunctionCallExpression, pSmgState, pEdge);
        return configAllocEdge;
      }
      if (builtins.isDeallocationFunction(functionName)) {
        List<SMGState> newStates = builtins.evaluateFree(pIastFunctionCallExpression, pSmgState, pEdge);
        return SMGValueAndStateList.copyOfUnknownValue(newStates);
      }
      switch (functionName) {
      case "__VERIFIER_BUILTIN_PLOT":
        builtins.evaluateVBPlot(pIastFunctionCallExpression, pSmgState);
        return SMGValueAndStateList.of(pSmgState);
      case "__builtin_alloca":
        SMGAddressValueAndStateList allocEdge = builtins.evaluateAlloca(pIastFunctionCallExpression, pSmgState, pEdge);
        return allocEdge;
      case "memset":
        SMGAddressValueAndStateList memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, pSmgState, pEdge);
        return memsetTargetEdge;
      default:
        // nothing to do here
      }
      throw new AssertionError();
    } else {
      return SMGValueAndStateList.of(pSmgState);
    }
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
    protected SMGAddressValueAndStateList evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue)
        throws CPATransferException {

      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(explicitState, functionName,
          pState, machineModel, logger, pCfaEdge);

      return SMGAddressValueAndStateList.of(SMGAddressValueAndState.of(pState, cc.evaluateSMGAddressExpression(pRvalue)));
    }

    @Override
    protected SMGValueAndStateList evaluateExpressionValue(SMGState pSmgState, CFAEdge pCfaEdge, CExpression pRValue)
        throws CPATransferException {
      return SMGValueAndStateList.of(SMGValueAndState.of(pSmgState, resolveRValue(oldState, pSmgState, explicitState, pRValue, pCfaEdge)));
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
    protected List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      ValueAnalysisSMGCommunicator cc = new ValueAnalysisSMGCommunicator(explicitState, functionName,
          pState, machineModel, logger, pCfaEdge);

      Value valueV = cc.evaluateExpression(pRValue);

      if (!valueV.isExplicitlyKnown() || !valueV.isNumericValue()) {
        return ImmutableList.of(SMGExplicitValueAndState.of(pState));
      } else {
        return ImmutableList.of(SMGExplicitValueAndState.of(pState, SMGKnownExpValue.valueOf(valueV.asNumericValue().longValue())));
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
     * @param pAddedOffset The offset added to this address.
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
}
