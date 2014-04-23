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
package org.sosy_lab.cpachecker.cpa.smgfork;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
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
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGExpressionEvaluator.AssumeVisitor;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGExpressionEvaluator.LValueAssignmentVisitor;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.ImmutableSet;


@Options(prefix = "cpa.smgfork")
public class SMGTransferRelation implements TransferRelation {

  @Option(name = "exportSMG.file", description = "Filename format for SMG graph dumps")
  @FileOption(Type.OUTPUT_FILE)
  private Path exportSMGFilePattern = Paths.get("smg-%s.dot");

  @Option(description = "with this option enabled, a check for unreachable memory occurs whenever a function returns, and not only at the end of the main function")
  private boolean checkForMemLeaksAtEveryFrameDrop = true;

  @Option(description = "with this option enabled, memory that is not freed before the end of main is reported as memleak even if it is reachable from local variables in main")
  private boolean handleNonFreedMemoryInMainAsMemLeak = false;

  @Option(name = "exportSMGwhen", description = "Describes when SMG graphs should be dumped. One of: {never, leaf, interesting, every}")
  private String exportSMG = "never";

  @Option(name="enableMallocFail", description = "If this Option is enabled, failure of malloc" + "is simulated")
  private boolean enableMallocFailure = true;

  @Option(name="handleUnknownFunctions", description = "Sets how unknown functions are handled. One of: {strict, assume_safe}")
  private String handleUnknownFunctions = "strict";

  final private LogManagerWithoutDuplicates logger;
  final private MachineModel machineModel;

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
   * name for the special variable used as container for return values of functions
   */
  public static final String FUNCTION_RETURN_VAR = "___cpa_temp_result_var_";

  private class SMGBuiltins {

    private static final int MEMSET_BUFFER_PARAMETER = 0;
    private static final int MEMSET_CHAR_PARAMETER = 1;
    private static final int MEMSET_COUNT_PARAMETER = 2;
    private static final int CALLOC_NUM_PARAMETER = 0;
    private static final int CALLOC_SIZE_PARAMETER = 1;
    private static final int MALLOC_PARAMETER = 0;

    private final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
        new String[] {
            "__VERIFIER_BUILTIN_PLOT",
            "malloc",
            "free",
            "memset",
            "calloc",
            //TODO: Properly model printf (dereferences and stuff)
            //TODO: General modelling system for functions which do not modify state?
            "printf",
        }));

    private void dumpSMGPlot(String name, SMGState currentState, String location) {
      if (exportSMGFilePattern != null && currentState != null) {
        if (name == null) {
          if (currentState.getPredecessor() == null) {
            name = String.format("initial-%03d", currentState.getId());
          } else {
            name = String.format("%03d-%03d", currentState.getPredecessor().getId(), currentState.getId());
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

    protected Path getOutputFile(Path pExportSMGFilePattern, String pName) {
      return Paths.get(String.format(pExportSMGFilePattern.toAbsolutePath().getPath(), pName));
    }

    protected String getDot(SMGState pCurrentState, String pName, String pLocation) {
      return pCurrentState.toDot(pName, pLocation);
    }

    public final void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
      String name = functionCall.getParameterExpressions().get(0).toASTString();
      dumpSMGPlot(name, currentState, functionCall.toString());
    }

    // TODO: Seems like there is large code sharing with evaluate calloc
    public final SMGEdgePointsTo evaluateMalloc(CFunctionCallExpression functionCall, SMGState currentState, CFAEdge cfaEdge)
        throws CPATransferException {
      CRightHandSide sizeExpr;

      try {
        sizeExpr = functionCall.getParameterExpressions().get(MALLOC_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        logger.logDebugException(e);
        throw new UnrecognizedCCodeException("Malloc argument not found.", cfaEdge, functionCall);
      }

      SMGExplicitValue value = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

      if (value.isUnknown()) {
        //throw new UnrecognizedCCodeException("Not able to compute allocation size", cfaEdge);
        return null;
      }

      // TODO line numbers are not unique when we have multiple input files!
      String allocation_label = "malloc_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(value.getAsInt(), allocation_label);

      possibleMallocFail = true;
      return new_pointer;
    }

    public final SMGEdgePointsTo evaluateMemset(CFunctionCallExpression functionCall,
        SMGState currentState, CFAEdge cfaEdge) throws CPATransferException {

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

      SMGAddressValue bufferAddress = evaluateAddress(currentState, cfaEdge, bufferExpr);

      SMGExplicitValue countValue = evaluateExplicitValue(currentState, cfaEdge, countExpr);

      if (bufferAddress.isUnknown() || countValue.isUnknown()) {
        return null;
      }

      SMGEdgePointsTo pointer = currentState.getPointerFromValue(bufferAddress.getAsInt());

      long count = countValue.getAsLong();

      SMGObject bufferMemory =  bufferAddress.getObject();

      int offset =  bufferAddress.getOffset().getAsInt();

      //TODO write explicit Value into smg
      SMGSymbolicValue ch = evaluateExpressionValue(currentState, cfaEdge, chExpr);

      if (ch.isUnknown()) {
        throw new UnrecognizedCCodeException("Can't simulate memset", cfaEdge, functionCall);
      }

      SMGExpressionEvaluator expEvaluator = new SMGExpressionEvaluator(logger, machineModel);

      SMGExplicitValue expValue = expEvaluator.evaluateExplicitValue(currentState, cfaEdge, chExpr);

      if (ch.equals(SMGKnownSymValue.ZERO)) {
        // Create one large edge
        writeValue(currentState, bufferMemory, offset, count, ch, cfaEdge);
      } else {
        // We need to create many edges, one for each character written
        // memset() copies ch into the first count characters of buffer
        for (int c = 0; c < count; c++) {
          writeValue(currentState, bufferMemory, offset + c, AnonymousTypes.dummyChar, ch, cfaEdge);
        }

        if (!expValue.isUnknown()) {
          currentState.putExplicit((SMGKnownSymValue) ch, (SMGKnownExpValue) expValue);
        }
      }

      return pointer;
    }

    protected SMGSymbolicValue evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge, CExpression rValue)
        throws CPATransferException {

      return expressionEvaluator.evaluateExpressionValue(smgState, cfaEdge, rValue);
    }

    protected SMGExplicitValue evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      return expressionEvaluator.evaluateExplicitValue(pState, pCfaEdge, pRValue);
    }

    protected SMGAddressValue evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue) throws CPATransferException {
      return expressionEvaluator.evaluateAddress(pState, pCfaEdge, pRvalue);
    }

    public final SMGEdgePointsTo evaluateCalloc(CFunctionCallExpression functionCall,
        SMGState currentState, CFAEdge cfaEdge) throws CPATransferException {

      CExpression numExpr;
      CExpression sizeExpr;

      try {
        numExpr = functionCall.getParameterExpressions().get(CALLOC_NUM_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        logger.logDebugException(e);
        throw new UnrecognizedCCodeException("Calloc num argument not found.", cfaEdge, functionCall);
      }

      try {
        sizeExpr = functionCall.getParameterExpressions().get(CALLOC_SIZE_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        logger.logDebugException(e);
        throw new UnrecognizedCCodeException("Calloc size argument not found.", cfaEdge, functionCall);
      }

      SMGExplicitValue numValue = expressionEvaluator.evaluateExplicitValue(currentState, cfaEdge, numExpr);
      SMGExplicitValue sizeValue = expressionEvaluator.evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

      if (numValue.isUnknown() || sizeValue.isUnknown()) {
        //throw new UnrecognizedCCodeException(
          //"Not able to compute allocation size", cfaEdge);
        return null;
      }

      int num = numValue.getAsInt();
      int size = sizeValue.getAsInt();

      // TODO line numbers are not unique when we have multiple input files!
      String allocation_label = "Calloc_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(num * size, allocation_label);

      currentState.writeValue(new_pointer.getObject(), 0, AnonymousTypes.createTypeWithLength(size), SMGKnownSymValue.ZERO);

      possibleMallocFail = true;
      return new_pointer;
    }

    public final void evaluateFree(CFunctionCallExpression pFunctionCall, SMGState currentState,
        CFAEdge cfaEdge) throws CPATransferException {
      CExpression pointerExp;

      try {
        pointerExp = pFunctionCall.getParameterExpressions().get(0);
      } catch (IndexOutOfBoundsException e) {
        logger.logDebugException(e);
        throw new UnrecognizedCCodeException("Built-in free(): No parameter passed", cfaEdge, pFunctionCall);
      }

      SMGAddressValue address = expressionEvaluator.evaluateAddress(currentState, cfaEdge, pointerExp);

      if (address.isUnknown()) {
        currentState.setInvalidFree();
        return;
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
        currentState.free(pointer.getValue(), pointer.getOffset(), pointer.getObject());
      }
    }

    public final boolean isABuiltIn(String functionName) {
      return (BUILTINS.contains(functionName) || isNondetBuiltin(functionName));
    }

    private static final String NONDET_PREFIX = "__VERIFIER_nondet_";
    private boolean isNondetBuiltin(String pFunctionName) {
      return pFunctionName.startsWith(NONDET_PREFIX) || pFunctionName.equals("nondet_int");
    }
  }

  final private SMGBuiltins builtins = new SMGBuiltins();

  private void plotWhenConfigured(String pConfig, String pName, SMGState pState, String pLocation ) {
    //TODO: A variation for more pConfigs

    if (pConfig.equals(exportSMG)){
      builtins.dumpSMGPlot(pName, pState, pLocation);
    }
  }

  public SMGTransferRelation(Configuration config, LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    config.inject(this);
    logger = new LogManagerWithoutDuplicates(pLogger);
    machineModel = pMachineModel;
    expressionEvaluator = new SMGRightHandSideEvaluator(logger, machineModel);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState state, Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    logger.log(Level.FINEST, "SMG GetSuccessor >>");
    logger.log(Level.FINEST, "Edge:", cfaEdge.getEdgeType());
    logger.log(Level.FINEST, "Code:", cfaEdge.getCode());

    SMGState successor;

    SMGState smgState = (SMGState) state;

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
          cfaEdge, assumeEdge.getTruthAssumption());
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
      successor.dropStackFrame();
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
      successor.setPredecessor(smgState);
      mallocFailState.setPredecessor(smgState);
      result = ImmutableSet.of(successor, mallocFailState);
      mallocFailState = null;
    } else {
      successor.setPredecessor(smgState);
      result = Collections.singleton(successor);
    }

    for (SMGState smg : result) {
      plotWhenConfigured("every", null, smg, cfaEdge.getDescription());
    }

    return result;
  }

  private SMGState handleExitFromFunction(SMGState smgState,
      CReturnStatementEdge returnEdge) throws CPATransferException {

    CExpression returnExp = returnEdge.getExpression();

    if (returnExp == null) {
      returnExp = CNumericTypes.ZERO; // this is the default in C
    }

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

    // We create a temporary State to get the LValue of the Stack_Frame above
    // the current one
    //TODO A method in the SMG to get Variables from a Stack above the current Stack.
    SMGState tmpState = new SMGState(smgState);

    tmpState.dropStackFrame();
    tmpState.pruneUnreachable(); // TODO necessary?

    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {

      // Assign the return value to the lValue of the functionCallAssignment

      CExpression lValue = ((CFunctionCallAssignmentStatement) exprOnSummary).getLeftHandSide();

      CType lValueType = expressionEvaluator.getRealExpressionType(lValue);

      CType rValueType = expressionEvaluator.getRealExpressionType(((CFunctionCallAssignmentStatement) exprOnSummary).getRightHandSide());

      SMGSymbolicValue rValue = getFunctionReturnValue(newState, rValueType, functionReturnEdge);

      SMGAddress address = null;

      LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(functionReturnEdge, tmpState);

      address = lValue.accept(visitor);

      if (!address.isUnknown()) {

        if (rValue.isUnknown()) {
          rValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
        }

        SMGObject object = address.getObject();

        int offset = address.getOffset().getAsInt();

        assignFieldToState(newState, functionReturnEdge, object, offset, lValueType, rValue, rValueType);
      } else {
        //TODO missingInformation, exception
      }
    }

    return newState;
  }

  private SMGSymbolicValue getFunctionReturnValue(SMGState smgState, CType type, CFAEdge pCFAEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    SMGObject tmpMemory = smgState.getFunctionReturnObject();

    return expressionEvaluator.readValue(smgState, tmpMemory, SMGKnownExpValue.ZERO, type, pCFAEdge);
  }

  private SMGState handleFunctionCall(SMGState smgState, CFunctionCallEdge callEdge)
      throws CPATransferException, SMGInconsistentException  {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    logger.log(Level.FINEST, "Handling function call: ", functionEntryNode.getFunctionName());

    SMGState newState = new SMGState(smgState);

    CFunctionDeclaration functionDeclaration = functionEntryNode.getFunctionDefinition();
    newState.addStackFrame(functionDeclaration);

    List<CParameterDeclaration> paramDecl = functionEntryNode.getFunctionParameters();
    List<? extends CExpression> arguments = callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      //TODO Parameter with varArgs
      assert (paramDecl.size() == arguments.size());
    }

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramDecl.size(); i++) {

      CExpression exp = arguments.get(i);

      String varName = paramDecl.get(i).getName();
      CType cType = expressionEvaluator.getRealExpressionType(paramDecl.get(i));

      SMGObject newObject = newState.addLocalVariable(cType, varName);

      // We want to write a possible new Address in the new State, but
      // explore the old state for the parameters
      assignFieldToState(newState, smgState, callEdge, newObject, 0, cType, exp);
    }

    return newState;
  }

  private SMGState handleAssumption(SMGState smgState, CExpression expression, CFAEdge cfaEdge,
      boolean truthValue) throws CPATransferException {

    // get the value of the expression (either true[-1], false[0], or unknown[null])
    AssumeVisitor visitor = expressionEvaluator.getAssumeVisitor(cfaEdge, smgState);
    SMGSymbolicValue value = expression.accept(visitor);

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

    SMGExplicitValue explicitValue = expressionEvaluator.evaluateExplicitValue(smgState, cfaEdge, expression);

    if (explicitValue.isUnknown()) {
      SMGState newState = new SMGState(smgState);
      /*
      Changing the state here breaks strengthen of ExplicitCPA
      which acceses newState instead of oldState.
      if (visitor.impliesEqOn(truthValue)) {
        newState.identifyEqualValues(visitor.knownVal1, visitor.knownVal2);
      } else if (visitor.impliesNeqOn(truthValue)) {
        newState.identifyNonEqualValues(visitor.knownVal1, visitor.knownVal2);
      }
      */

      expressionEvaluator.deriveFurtherInformation(newState, truthValue, cfaEdge, expression);
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
      String functionName = fileNameExpression.toASTString();

      if (builtins.isABuiltIn(functionName)) {
        newState = new SMGState(pState);
        switch (functionName) {
        case "__VERIFIER_BUILTIN_PLOT":
          builtins.evaluateVBPlot(cFCExpression, newState);
          break;
        case "free":
          builtins.evaluateFree(cFCExpression, newState, pCfaEdge);
          break;
        case "malloc":
          logger.log(Level.WARNING, pCfaEdge.getFileLocation() + ":",
              "Calling malloc and not using the result, resulting in memory leak.");
          newState.setMemLeak();
          break;
        case "calloc":
          logger.log(Level.WARNING, pCfaEdge.getFileLocation() + ":",
              "Calling calloc and not using the result, resulting in memory leak.");
          newState.setMemLeak();
          break;
        case "memset":
          builtins.evaluateMemset(cFCExpression, newState, pCfaEdge);
          break;
        case "printf":
          return new SMGState(pState);
        }

      } else {
        switch (handleUnknownFunctions) {
        case "strict":
          throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
        case "assume_safe":
          return new SMGState(pState);
        }
        throw new AssertionError();
      }
    } else {
      newState = new SMGState(pState);
    }

    return newState;
  }

  private SMGState handleAssignment(SMGState state, CFAEdge cfaEdge, CExpression lValue,
      CRightHandSide rValue) throws CPATransferException {

    SMGState newState;
    logger.log(Level.FINEST, "Handling assignment:", lValue, "=", rValue);

    LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(cfaEdge, state);

    SMGAddress addressOfField = lValue.accept(visitor);

    CType fieldType = expressionEvaluator.getRealExpressionType(lValue);

    if (addressOfField.isUnknown()) {
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

  private void assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    assignFieldToState(newState, newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, rValue);
  }

  private void assignFieldToState(SMGState newState, SMGState readState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    CType rValueType = expressionEvaluator.getRealExpressionType(rValue);

    //TODO also evaluate explicit value and assign to symbolic value
    SMGSymbolicValue value = expressionEvaluator.evaluateExpressionValue(readState, cfaEdge, rValue);

    if (value.isUnknown()) {

      if (expressionEvaluator.isMissingExplicitInformation()) {
        expressionEvaluator.reset();
      }

      value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
    }

    SMGExpressionEvaluator expEvaluator = new SMGExpressionEvaluator(logger, machineModel);

    SMGExplicitValue expValue = expEvaluator.evaluateExplicitValue(newState, cfaEdge, rValue);

    assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, value, rValueType);

    if (!expValue.isUnknown()) {
      newState.putExplicit((SMGKnownSymValue) value, (SMGKnownExpValue) expValue);
    }
  }

  private void assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, SMGSymbolicValue value, CType rValueType)
      throws UnrecognizedCCodeException, SMGInconsistentException {

    if (memoryOfField.getSize() < expressionEvaluator.getSizeof(cfaEdge, rValueType)) {
      logger.log(Level.WARNING, cfaEdge.getFileLocation() + ":",
          "Attempting to write " + expressionEvaluator.getSizeof(cfaEdge, rValueType) +
          " bytes into a field with size " + memoryOfField.getSize() + "bytes:",
          cfaEdge.getRawStatement());
    }

    if (expressionEvaluator.isStructOrUnionType(rValueType)) {
      assignStruct(newState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    } else {
      writeValue(newState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    }
  }

  private void assignStruct(SMGState pNewState, SMGObject pMemoryOfField,
      int pFieldOffset, CType pRValueType, SMGSymbolicValue pValue,
      CFAEdge pCfaEdge) throws SMGInconsistentException,
      UnrecognizedCCodeException {

    if (pValue instanceof SMGKnownAddVal) {
      SMGKnownAddVal structAddress = (SMGKnownAddVal) pValue;

      SMGObject source = structAddress.getObject();
      int structOffset = structAddress.getOffset().getAsInt();
      int structSize = structOffset + expressionEvaluator.getSizeof(pCfaEdge, pRValueType);
      pNewState.copy(source, pMemoryOfField,
          structOffset, structSize, pFieldOffset);
    }
  }
  private void writeValue(SMGState pNewState, SMGObject pMemoryOfField, int pFieldOffset, long pSizeType,
      SMGSymbolicValue pValue, CFAEdge pEdge) throws UnrecognizedCCodeException, SMGInconsistentException {
    writeValue(pNewState, pMemoryOfField, pFieldOffset, AnonymousTypes.createTypeWithLength(pSizeType), pValue, pEdge);
  }

  private void writeValue(SMGState pNewState, SMGObject pMemoryOfField, int pFieldOffset, CType pRValueType,
      SMGSymbolicValue pValue, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    boolean doesNotFitIntoObject = pFieldOffset < 0
        || pFieldOffset + expressionEvaluator.getSizeof(pEdge, pRValueType) > pMemoryOfField.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, pEdge.getFileLocation() + ":",
          "Field " + "(" + pFieldOffset + ", " + pRValueType.toASTString("") + ")" +
          " does not fit object " + pMemoryOfField.toString() + ".");

      pNewState.setInvalidWrite();
      return;
    }

    if (pValue.isUnknown() || pNewState == null) {
      return;
    }

    pNewState.writeValue(pMemoryOfField, pFieldOffset, pRValueType, pValue);
  }

  private SMGState handleAssignmentToField(SMGState state, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    SMGState newState = new SMGState(state);

    assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, rValue);

    // If Assignment contained malloc, handle possible fail with
    // alternate State
    if (possibleMallocFail) {
      possibleMallocFail = false;
      SMGState otherState = new SMGState(state);
      CType rValueType = expressionEvaluator.getRealExpressionType(rValue);
      writeValue(otherState, memoryOfField, fieldOffset, rValueType, SMGKnownSymValue.ZERO, cfaEdge);
      mallocFailState = otherState;
    }

    return newState;
  }

  private SMGState handleVariableDeclaration(SMGState pState, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge) throws CPATransferException {
    logger.log(Level.FINEST, "Handling variable declaration:", pVarDecl);

    String varName = pVarDecl.getName();
    CType cType = expressionEvaluator.getRealExpressionType(pVarDecl);

    SMGObject newObject;

    if (pVarDecl.isGlobal()) {
      newObject = pState.addGlobalVariable(cType, varName);
    } else {
      newObject = pState.getObjectForVisibleVariable(varName);

      /*
       *  The variable is not null if we seen the declaration already, for example in loops. Invalid
       *  occurrences (variable really declared twice) should be caught for us by the parser. If we
       *  already processed the declaration, we do nothing.
       */
      if (newObject == null) {
        newObject = pState.addLocalVariable(cType, varName);
      }
    }

    pState = handleInitializerForDeclaration(pState, newObject, pVarDecl, pEdge);
    return pState;
  }

  private SMGState handleDeclaration(SMGState smgState, CDeclarationEdge edge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling declaration");

    SMGState newState = new SMGState(smgState);
    CDeclaration cDecl = edge.getDeclaration();

    if (cDecl instanceof CVariableDeclaration) {
      newState = handleVariableDeclaration(newState, (CVariableDeclaration)cDecl, edge);
    }
    //TODO: Handle other declarations?
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
      pState.writeValue(pObject, 0, cType, SMGKnownSymValue.ZERO);
    }

    return pState;
  }

  private SMGState handleInitializer(SMGState pNewState, CVariableDeclaration pVarDecl, CFAEdge pEdge,
      SMGObject pNewObject, int pOffset, CType pLValueType, CInitializer pInitializer)
      throws UnrecognizedCCodeException, CPATransferException {

    if (pInitializer instanceof CInitializerExpression) {
      return handleAssignmentToField(pNewState, pEdge, pNewObject,
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

    if(pVarDecl.isGlobal()) {
      int sizeOfType = expressionEvaluator.getSizeof(pEdge, pLValueType);

      if(offset < sizeOfType ) {
        pNewState.writeValue(pNewObject, offset, AnonymousTypes.createTypeWithLength(sizeOfType), SMGKnownSymValue.ZERO);
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
      if (offset < sizeOfType) {
        pNewState.writeValue(pNewObject, offset, AnonymousTypes.createTypeWithLength(sizeOfType-offset), SMGKnownSymValue.ZERO);
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

    public SMGRightHandSideEvaluator(LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel) {
      super(pLogger, pMachineModel);
    }

    public void deriveFurtherInformation(SMGState pNewState, boolean pTruthValue, CFAEdge pCfaEdge, CExpression rValue)
        throws CPATransferException {
      rValue.accept(new AssigningValueVisitor(pNewState, pTruthValue, pCfaEdge));
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

        if(operand1 instanceof CLeftHandSide) {
          deriveFurtherInformation((CLeftHandSide) operand1, operand2, op);
        }

        if(operand2 instanceof CLeftHandSide) {
          deriveFurtherInformation((CLeftHandSide) operand2, operand1, op);
        }

        return null;
      }

      private void deriveFurtherInformation(CLeftHandSide lValue, CExpression exp, BinaryOperator op) throws CPATransferException {

        SMGExplicitValue rValue = evaluateExplicitValue(assignableState, edge, exp);

        if(rValue.isUnknown()) {
          // no further information can be inferred
          return;
        }

        SMGSymbolicValue rSymValue = evaluateExpressionValue(assignableState, edge, exp);

        if(rSymValue.isUnknown()) {
          return;
        }

        SMGExpressionEvaluator.LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

        SMGAddress addressOfField = lValue.accept(visitor);

        if(addressOfField.isUnknown()) {
          return;
        }

        if (truthValue) {
          if (op == BinaryOperator.EQUALS) {
            assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
          }
        } else {
          if(op == BinaryOperator.NOT_EQUALS) {
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

        if(truthValue == true) {
          return; // no further explicit Information can be derived
        }

        SMGExpressionEvaluator.LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

        SMGAddress addressOfField = lValue.accept(visitor);

        if(addressOfField.isUnknown()) {
          return;
        }

        // If this value is known, the assumption can be evaluated, therefore it should be unknown
        assert evaluateExplicitValue(assignableState, edge, lValue).isUnknown();

        SMGSymbolicValue value = evaluateExpressionValue(assignableState, edge, lValue);

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
      public SMGAddress visit(CIdExpression variableName) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: variable assignment");

        // a = ...
        return super.visit(variableName);
      }

      @Override
      public SMGAddress visit(CPointerExpression pLValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to dereferenced pointer");

        SMGAddress address = super.visit(pLValue);

        if (address.isUnknown()) {
          getSmgState().setUnknownDereference();
        }

        return address;
      }

      @Override
      public SMGAddress visit(CFieldReference lValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to field reference");

        return super.visit(lValue);
      }

      @Override
      public SMGAddress visit(CArraySubscriptExpression lValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to array Cell");

        return super.visit(lValue);
      }
    }

    private class ExpressionValueVisitor extends SMGExpressionEvaluator.ExpressionValueVisitor {

      public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
        super(pEdge, pSmgState);
      }

      @Override
      public SMGSymbolicValue visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws CPATransferException {

        CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
        String functionName = fileNameExpression.toASTString();

        // If Calloc and Malloc have not been properly declared,
        // they may be shown to return void
        if (builtins.isABuiltIn(functionName)) {
          switch (functionName) {
          case "__VERIFIER_BUILTIN_PLOT":
            builtins.evaluateVBPlot(pIastFunctionCallExpression, getSmgState());
            break;
          case "malloc":
            possibleMallocFail = true;
            SMGEdgePointsTo mallocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, getSmgState(), getCfaEdge());
            return createAddress(mallocEdge);
          case "calloc":
            possibleMallocFail = true;
            SMGEdgePointsTo callocEdge = builtins.evaluateCalloc(pIastFunctionCallExpression, getSmgState(), getCfaEdge());
            return createAddress(callocEdge);
          case "printf":
            return SMGUnknownValue.getInstance();
          default:
            if (builtins.isNondetBuiltin(functionName)) {
              return SMGUnknownValue.getInstance();
            } else {
              throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
            }
          }
        } else {
          switch (handleUnknownFunctions) {
          case "strict":
            throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
          case "assume_safe":
            return SMGUnknownValue.getInstance();
          }
          throw new AssertionError();
        }

        return SMGUnknownValue.getInstance();
      }
    }

    private class PointerAddressVisitor extends SMGExpressionEvaluator.PointerVisitor {

   public PointerAddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
   }

    @Override
      public SMGAddressValue visit(CFunctionCallExpression pIastFunctionCallExpression)
          throws CPATransferException {
        CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
        String functionName = fileNameExpression.toASTString();

        if (builtins.isABuiltIn(functionName)) {
          switch (functionName) {
          case "malloc":
            possibleMallocFail = true;
            SMGEdgePointsTo mallocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, getSmgState(), getCfaEdge());
            return createAddress(mallocEdge);
          case "calloc":
            possibleMallocFail = true;
            SMGEdgePointsTo callocEdge = builtins.evaluateCalloc(pIastFunctionCallExpression, getSmgState(), getCfaEdge());
            return createAddress(callocEdge);
          case "memset":
            SMGEdgePointsTo memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, getSmgState(), getCfaEdge());
            return createAddress(memsetTargetEdge);
          case "printf":
            return SMGUnknownValue.getInstance();
          default:
            if (builtins.isNondetBuiltin(functionName)) {
              return SMGUnknownValue.getInstance();
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
            return SMGUnknownValue.getInstance();
          }
          throw new AssertionError();
        }
      }
    }

    @Override
    protected org.sosy_lab.cpachecker.cpa.smgfork.SMGExpressionEvaluator.PointerVisitor getPointerVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      return new PointerAddressVisitor(pCfaEdge, pNewState);
    }

    @Override
    protected org.sosy_lab.cpachecker.cpa.smgfork.SMGExpressionEvaluator.ExpressionValueVisitor getExpressionValueVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      return new ExpressionValueVisitor(pCfaEdge, pNewState);
    }

    @Override
    public org.sosy_lab.cpachecker.cpa.smgfork.SMGExpressionEvaluator.LValueAssignmentVisitor getLValueAssignmentVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      return new LValueAssignmentVisitor(pCfaEdge, pNewState);
    }

    @Override
    public SMGExplicitValue evaluateExplicitValue(SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {

      SMGExplicitValue explicitValue = super.evaluateExplicitValue(pSmgState, pCfaEdge, pRValue);
      if (explicitValue.isUnknown()) {
        missingExplicitInformation = true;
      }
      return explicitValue;
    }

    public boolean isMissingExplicitInformation() {
      return missingExplicitInformation;
    }

    @Override
    protected SMGSymbolicValue handleUnknownDereference(SMGState pSmgState, CFAEdge pEdge) {
      pSmgState.setUnknownDereference();
      return super.handleUnknownDereference(pSmgState, pEdge);
    }

    public void reset() {
      missingExplicitInformation= false;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements,
      CFAEdge cfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> retVal = null;

    for (AbstractState ae : elements) {
      if (ae instanceof AutomatonState) {
        strengthen((AutomatonState) ae, (SMGState) element, cfaEdge);
      }
    }

    possibleMallocFail = false;
    return retVal;
  }

  private Collection<? extends AbstractState> strengthen(AutomatonState pAutomatonState, SMGState pElement,
      CFAEdge pCfaEdge) throws CPATransferException {

    List<AssumeEdge> assumptions = pAutomatonState.getAsAssumeEdges(null, pCfaEdge.getPredecessor().getFunctionName());

    SMGState newElement = new SMGState(pElement);

    for (AssumeEdge assume : assumptions) {
      if (!(assume instanceof CAssumeEdge)) {
        continue;
      }
      newElement = handleAssumption(newElement, ((CAssumeEdge)assume).getExpression(), pCfaEdge, assume.getTruthAssumption());
      if (newElement == null) {
        break;
      }
    }

    if (newElement == null) {
      return Collections.emptyList();
    } else {
      return Collections.singleton(newElement);
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
      checkNotNull(pValue);
      value = BigInteger.valueOf(pValue);
    }

    private SMGKnownValue(int pValue) {
      checkNotNull(pValue);
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

    public static final SMGKnownSymValue valueOf(int pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownSymValue(BigInteger.valueOf(pValue));
      }
    }

    public static final SMGKnownSymValue valueOf(long pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownSymValue(BigInteger.valueOf(pValue));
      }
    }

    public static final SMGKnownSymValue valueOf(BigInteger pValue) {

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

    public static final SMGKnownExpValue valueOf(int pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownExpValue(BigInteger.valueOf(pValue));
      }
    }

    public static final SMGKnownExpValue valueOf(long pValue) {

      if (pValue == 0) {
        return ZERO;
      } else if (pValue == 1) {
        return ONE;
      } else {
        return new SMGKnownExpValue(BigInteger.valueOf(pValue));
      }
    }

    public static final SMGKnownExpValue valueOf(BigInteger pValue) {

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

      public static final SMGKnownAddress valueOf(SMGObject object, SMGKnownExpValue offset) {
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
        new SMGAddress(null, SMGUnknownValue.getInstance());

    private SMGAddress(SMGObject pObject, SMGExplicitValue pOffset) {
      checkNotNull(pOffset);
      object = pObject;
      offset = pOffset;
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

    public static final SMGAddress valueOf(SMGObject object, SMGExplicitValue offset) {
      return new SMGAddress(object, offset);
    }

    @Override
    public final String toString() {

      if(isUnknown()) {
        return "Unkown";
      }

      return "Object: " + object.toString() + " Offset: " + offset.toString();
    }

    public static SMGAddress valueOf(SMGObject pObj, int pOffset) {
      return new SMGAddress(pObj, SMGKnownExpValue.valueOf(pOffset));
    }
  }
}
