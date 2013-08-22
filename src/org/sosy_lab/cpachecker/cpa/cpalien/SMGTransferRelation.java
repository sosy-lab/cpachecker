/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGExpressionEvaluator.LValueAssignmentVisitor;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.SMGExplicitCommunicator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.ImmutableSet;


@Options(prefix = "cpa.cpalien")
public class SMGTransferRelation implements TransferRelation {

  @Option(name = "exportSMG.file", description = "Filename format for SMG graph dumps")
  @FileOption(Type.OUTPUT_FILE)
  private File exportSMGFilePattern = new File("smg-%s.dot");

  @Option(name = "exportSMGwhen", description = "Describes when SMG graphs should be dumped. One of: {never, leaf, every}")
  private String exportSMG = "never";

  @Option(name="enableMallocFail", description = "If this Option is enabled, failure of malloc" + "is simulated")
  private boolean enableMallocFailure = true;

  final private LogManager logger;
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
    private static final int CALLOC_NUM_PARAMETER = 0;
    private static final int CALLOC_SIZE_PARAMETER = 1;
    private static final int MALLOC_PARAMETER = 0;

    private final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
        new String[] {
            "__VERIFIER_BUILTIN_PLOT",
            "malloc",
            "free",
            "memset",
            "calloc"
        }));

    public void dumpSMGPlot(String name, SMGState currentState, String location)
    {
      if (exportSMGFilePattern != null) {
        if (name == null) {
          if (currentState.getPredecessor() == null) {
            name = String.format("initial-%03d", currentState.getId());
          } else {
            name = String.format("%03d-%03d", currentState.getPredecessor().getId(), currentState.getId());
          }
        }
        name = name.replace("\"", "");
        File outputFile = getOutputFile(exportSMGFilePattern, name);
        try {
          String dot = getDot(currentState, name, location);
          Files.writeFile(outputFile, dot);
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write SMG " + name + " to file");
        }
      }
    }

    protected File getOutputFile(File pExportSMGFilePattern, String pName) {
      return new File(String.format(pExportSMGFilePattern.getAbsolutePath(), pName));
    }

    protected String getDot(SMGState pCurrentState, String pName, String pLocation) {
      return pCurrentState.toDot(pName, pLocation);
    }

    public final void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
      String name = functionCall.getParameterExpressions().get(0).toASTString();
      this.dumpSMGPlot(name, currentState, functionCall.toString());
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

      SMGSymbolicValue ch = evaluateExpressionValue(currentState, cfaEdge, chExpr);

      if (ch.isUnknown()) {
        throw new UnrecognizedCCodeException("Can't simulate memset", cfaEdge, functionCall);
      }

      //TODO Mock Type char
      CType charType = new CSimpleType(false, false, CBasicType.CHAR, false, false, false, false, false, false, false);

      //TODO effective memset
      //  memset() copies ch into the first count characters of buffer
      for (int c = 0; c < count; c++) {
        writeValue(currentState, bufferMemory, offset + c, charType, ch, cfaEdge);
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

      SMGExplicitValue numValue = evaluateExplicitValue(currentState, cfaEdge, numExpr);
      SMGExplicitValue sizeValue = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

      if (numValue.isUnknown() || sizeValue.isUnknown()) {
        //throw new UnrecognizedCCodeException(
          //"Not able to compute allocation size", cfaEdge);
        return null;
      }

      int num = numValue.getAsInt();
      int size = sizeValue.getAsInt();

      String allocation_label = "Calloc_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(num * size, allocation_label);


      //TODO Create mock types
      CSimpleType charType = new CSimpleType(false, false, CBasicType.CHAR,
          false, false, false, false, false, false, false);
      CType newType = new CArrayType(false, false, charType,
          new CIntegerLiteralExpression(null, null, BigInteger.valueOf(size)));

      currentState.writeValue(new_pointer.getObject(), 0, newType, 0);

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
        throw new UnrecognizedCCodeException("Bulit in function free has no parameter", cfaEdge, pFunctionCall);
      }

      SMGAddressValue address = evaluateAddress(currentState, cfaEdge, pointerExp);

      if (address.isUnknown()) {
        currentState.setInvalidFree();
        return;
      }

      SMGEdgePointsTo pointer = currentState.getPointerFromValue(address.getAsInt());

      if (address.getAsInt() == 0) {
        logger.log(Level.WARNING, "The argument of a free invocation: "
            + cfaEdge.getRawStatement() + ", in Line "
            + pFunctionCall.getFileLocation().getStartingLineNumber() + " is 0");

      } else {
        currentState.free(pointer.getValue(), pointer.getOffset(), pointer.getObject());
      }
    }

    public final boolean isABuiltIn(String functionName) {
      return BUILTINS.contains(functionName);
    }
  }

  final private SMGBuiltins builtins = new SMGBuiltins();

  public SMGTransferRelation(Configuration config, LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    machineModel = pMachineModel;
    expressionEvaluator = new SMGRightHandSideEvaluator(logger, machineModel);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState state, Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    logger.log(Level.FINEST, "SMG GetSuccessor >>");
    logger.log(Level.FINEST, "Edge:", cfaEdge.getEdgeType());
    logger.log(Level.FINEST, "Code:", cfaEdge.getCode());

    SMGState smgState = (SMGState) state;

    setInfo(smgState);

    SMGState successor;



    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      successor = handleDeclaration(smgState, (CDeclarationEdge) cfaEdge);
      break;

    case StatementEdge:
      successor = handleStatement(smgState, (CStatementEdge) cfaEdge);
      break;

      // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      successor = handleAssumption(smgState, assumeEdge.getExpression(),
          cfaEdge, assumeEdge.getTruthAssumption());
      break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(smgState, functionCallEdge);
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(smgState, functionReturnEdge);
      successor.dropStackFrame(functionReturnEdge.getPredecessor().getFunctionName());
      break;

    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      successor = handleExitFromFunction(smgState, returnEdge);
      String funcName = returnEdge.getPredecessor().getFunctionName();
      if (funcName.equals("main")) {
        // Ugly, but I do not know how to do better
        // TODO: Handle leaks at any program exit point (abort, etc.)
        successor.dropStackFrame(funcName);
      }
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

    if ( this.exportSMG.equals("never")) {
      return result;
    }
    else if ( this.exportSMG.equals("every")) {
      for (SMGState smg : result) {
        builtins.dumpSMGPlot(null, smg, cfaEdge.getDescription());
      }
    }
    return result;
  }

  private void setInfo(SMGState pOldState) {
    missingInformationList = new ArrayList<>(5);
    oldState = new SMGState(pOldState);
    expressionEvaluator.reset();
  }

  private SMGState handleExitFromFunction(SMGState smgState,
      CReturnStatementEdge returnEdge) throws CPATransferException {

    CExpression returnExp = returnEdge.getExpression();

    if (returnExp == null) {
      returnExp = CNumericTypes.ZERO; // this is the default in C
    }

    logger.log(Level.FINEST, "Handling return Statement: ",
        returnExp.toASTString());

    CType expType = expressionEvaluator.getRealExpressionType(returnExp);
    SMGObject tmpFieldMemory = smgState.getFunctionReturnObject();

    return handleAssignmentToField(smgState, returnEdge, tmpFieldMemory, 0, expType, returnExp);
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

    tmpState.dropStackFrame(functionReturnEdge.getPredecessor().getFunctionName());

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
      }
    }

    return newState;
  }

  private SMGSymbolicValue getFunctionReturnValue(SMGState smgState, CType type, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    SMGObject tmpMemory = smgState.getFunctionReturnObject();

    return expressionEvaluator.readValue(smgState, tmpMemory, SMGKnownExpValue.ZERO, type, pEdge);
  }

  private SMGState handleFunctionCall(SMGState smgState, CFunctionCallEdge callEdge)
      throws CPATransferException, SMGInconsistentException  {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    logger.log(Level.FINEST, "Handling function call: " + functionEntryNode.getFunctionName());

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

    // convert an expression like [a + 753 != 951] to [a != 951 - 753]
    expression = optimizeAssumeForEvaluation(expression);

    // get the value of the expression (either true[-1], false[0], or unknown[null])
    SMGSymbolicValue value = expressionEvaluator.evaluateAssumptionValue(smgState, cfaEdge, expression);

    if (value.isUnknown()) {
      //TODO derive further information of value (in essence, implement smg.replace(symbValue, symbValue for y == x
      // and addNeq for y != x))

      //TODO Refactor
      SMGExplicitValue explicitValue = expressionEvaluator.evaluateExplicitValue(smgState, cfaEdge, expression);

      if (explicitValue.isUnknown()) {

        if (expressionEvaluator.isMissingExplicitInformation()) {
          missingInformationList.add(new MissingInformation(truthValue, expression));
          expressionEvaluator.reset();
        }

        return smgState;
      } else if ((truthValue && explicitValue.equals(SMGKnownExpValue.ONE))
          || (!truthValue && explicitValue.equals(SMGKnownExpValue.ZERO))) {
        return smgState;
      } else {
        // This signals that there are no new States reachable from this State i. e. the
        // Assumption does not hold.
        return null;
      }
    } else if ((truthValue && value.equals(SMGKnownSymValue.TRUE))
        || (!truthValue && value.equals(SMGKnownSymValue.FALSE))) {
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
        switch (functionName) {
        case "__VERIFIER_BUILTIN_PLOT":
          builtins.evaluateVBPlot(cFCExpression, newState);
          expressionEvaluator.reset();
          missingInformationList.add(new MissingInformation(cFCExpression, false));
          break;
        case "free":
          builtins.evaluateFree(cFCExpression, newState, pCfaEdge);
          break;
        case "malloc":
          logger.log(Level.WARNING, "Calling malloc and not using the result, resulting in memory leak at line "
              + pCfaEdge.getLineNumber());
          newState.setMemLeak();
          isRequiered = true;
          break;
        case "calloc":
          logger.log(Level.WARNING, "Calling calloc and not using the result, resulting in memory leak at line "
              + pCfaEdge.getLineNumber());
          newState.setMemLeak();
          isRequiered = true;
          break;
        case "memset":
          builtins.evaluateMemset(cFCExpression, newState, pCfaEdge);
        }

        if(expressionEvaluator.missingExplicitInformation) {
          missingInformationList.add(new MissingInformation(cFCExpression, isRequiered));
          expressionEvaluator.reset();
        }

      } else {
        logger.log(Level.FINEST, ">>> Handling statement: non-builtin function call");
        newState = new SMGState(pState);
      }
    } else {
      newState = new SMGState(pState);
    }

    return newState;
  }

  private SMGState handleAssignment(SMGState state, CFAEdge cfaEdge, CExpression lValue,
      CRightHandSide rValue) throws CPATransferException {

    SMGState newState;
    logger.log(Level.FINEST, "Handling assignment:", lValue.toASTString(), "=", rValue.toASTString());

    LValueAssignmentVisitor visitor = expressionEvaluator.getLValueAssignmentVisitor(cfaEdge, state);

    SMGAddress addressOfField = lValue.accept(visitor);

    CType fieldType = expressionEvaluator.getRealExpressionType(lValue);

    if (addressOfField.isUnknown()) {
      addMissingInformation(lValue, rValue);
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

  private void assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    assignFieldToState(newState, newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, rValue);
  }

  private void assignFieldToState(SMGState newState, SMGState readState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    CType rValueType = expressionEvaluator.getRealExpressionType(rValue);

    SMGSymbolicValue value = expressionEvaluator.evaluateExpressionValue(readState, cfaEdge, rValue);

    if (value.isUnknown()) {

      if (expressionEvaluator.isMissingExplicitInformation()) {
        addMissingInformation(memoryOfField, fieldOffset, rValue, expressionEvaluator.isRequiered());
        expressionEvaluator.reset();
      }

      value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());

      if (newValueIsNeqZero(newState, cfaEdge, rValue)) {
        newState.setUnequal(value.getAsInt(), 0);
      }
    }

    assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, value, rValueType);
  }

  private void addMissingInformation(SMGObject pMemoryOfField, int pFieldOffset, CRightHandSide pRValue,
      boolean isRequiered) {

    SMGAddress address = SMGAddress.valueOf(pMemoryOfField, SMGKnownExpValue.valueOf(pFieldOffset));

    missingInformationList.add(
        new MissingInformation(address, pRValue, isRequiered));
  }

  private void assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, SMGSymbolicValue value, CType rValueType)
      throws UnrecognizedCCodeException, SMGInconsistentException {

    if (memoryOfField.getSizeInBytes() < expressionEvaluator.getSizeof(cfaEdge, rValueType)) {
      logger.log(Level.WARNING, "Attempting to write " + expressionEvaluator.getSizeof(cfaEdge, rValueType) +
          " bytes into a field with size " + memoryOfField.getSizeInBytes() + "bytes.\n" +
          "Line " + cfaEdge.getLineNumber() + ": " + cfaEdge.getRawStatement());
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

  private void writeValue(SMGState pNewState, SMGObject pMemoryOfField, int pFieldOffset, CType pRValueType,
      SMGSymbolicValue pValue, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    if (pValue.isUnknown() || pNewState == null) {
      return;
    }

    boolean doesNotFitIntoObject = pFieldOffset < 0
        || pFieldOffset + expressionEvaluator.getSizeof(pEdge, pRValueType) > pMemoryOfField.getSizeInBytes();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, "Field " + "(" + pFieldOffset + ", " + pRValueType.toASTString("") + ")" +
          " does not fit object " + pMemoryOfField.toString() + ".\n Line: " + pEdge.getLineNumber());
      pNewState.setInvalidWrite();
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

  private boolean newValueIsNeqZero(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws UnrecognizedCCodeException {

    return rValue.accept(new IsNotZeroVisitor(newState, cfaEdge));
  }

  private SMGState handleDeclaration(SMGState smgState, CDeclarationEdge edge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling declaration");
    SMGState newState = new SMGState(smgState);

    CDeclaration cDecl = edge.getDeclaration();

    if (cDecl instanceof CVariableDeclaration) {
      CVariableDeclaration cVarDecl = (CVariableDeclaration) cDecl;
      logger.log(Level.FINEST, "Handling variable declaration:", cVarDecl.toASTString());
      String varName = cVarDecl.getName();
      CType cType = expressionEvaluator.getRealExpressionType(cVarDecl);

      SMGObject newObject;

      CInitializer newInitializer = cVarDecl.getInitializer();

      if (cVarDecl.isGlobal()) {
        newObject = smgState.createObject(expressionEvaluator.getSizeof(edge, cType), varName);
        logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to global objects");
        newState.addGlobalObject(newObject);

        if (newInitializer == null) {
          // global variables without initializer are set to 0 in C
          writeValue(newState, newObject, 0, cType, SMGKnownSymValue.ZERO, edge);
        }

      } else {

        //Check whether variable was already declared, for example in loops
        // TODO explicitly check this
        newObject = newState.getObjectForVisibleVariable(varName);

        if(newObject == null) {

          newObject = newState.addLocalVariable(cType, varName);
          logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to current stack");
        }
      }

      if (newInitializer != null) {
        logger.log(Level.FINEST, "Handling variable declaration: handling initializer");

        if (newInitializer instanceof CInitializerExpression) {
          newState = handleAssignmentToField(newState, edge, newObject, 0, cType,
              ((CInitializerExpression) newInitializer).getExpression());
          /*
          lParam = cVarDecl.toASTString();
          lParamIsGlobal = cVarDecl.isGlobal();
          */
        }
        //TODO handle other Cases
      }
    }
    return newState;
  }

  /**
   * This method converts an expression like [a + 753 != 951] to [a != 951 - 753], to be able to derive addition information easier with the current expression evaluation visitor.
   *
   * @param expression the expression to generalize
   * @return the generalized expression
   */
  private CExpression optimizeAssumeForEvaluation(CExpression expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression) expression;

      BinaryOperator operator = binaryExpression.getOperator();
      CExpression leftOperand = binaryExpression.getOperand1();
      CExpression riteOperand = binaryExpression.getOperand2();


      if (operator == BinaryOperator.EQUALS || operator == BinaryOperator.NOT_EQUALS) {
        if (leftOperand instanceof CBinaryExpression && riteOperand instanceof CLiteralExpression) {
          CBinaryExpression expr = (CBinaryExpression) leftOperand;

          BinaryOperator operation = expr.getOperator();
          CExpression leftAddend = expr.getOperand1();
          CExpression riteAddend = expr.getOperand2();

          // [(a + 753) != 951] => [a != 951 + 753]

          if (riteAddend instanceof CLiteralExpression
              && (operation == BinaryOperator.PLUS || operation == BinaryOperator.MINUS)) {
            BinaryOperator newOperation =
                (operation == BinaryOperator.PLUS) ? BinaryOperator.MINUS : BinaryOperator.PLUS;

            CBinaryExpression sum = new CBinaryExpression(expr.getFileLocation(),
                expressionEvaluator.getRealExpressionType(expr),
                riteOperand,
                riteAddend,
                newOperation);

            CBinaryExpression assume = new CBinaryExpression(expression.getFileLocation(),
                expressionEvaluator.getRealExpressionType(binaryExpression),
                leftAddend,
                sum,
                operator);
            return assume;
          }
        }
      }
    }
    return expression;
  }

  private class IsNotZeroVisitor extends DefaultCExpressionVisitor<Boolean, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Boolean, UnrecognizedCCodeException> {

    //TODO Refactor, this visitor should not be neccessary

    @SuppressWarnings("unused")
    private final CFAEdge cfaEdge;
    @SuppressWarnings("unused")
    private final SMGState smgState;

    public IsNotZeroVisitor(SMGState smgState, CFAEdge cfaEdge) {
      this.cfaEdge = cfaEdge;
      this.smgState = smgState;
    }

    @Override
    public Boolean visit(CFunctionCallExpression exp) throws UnrecognizedCCodeException {
      return false;
    }

    @Override
    protected Boolean visitDefault(CExpression exp) throws UnrecognizedCCodeException {
      return false;
    }

    @Override
    public Boolean visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      return !exp.getValue().equals(BigInteger.ZERO);
    }

    @Override
    public Boolean visit(CCastExpression exp) throws UnrecognizedCCodeException {
      return exp.getOperand().accept(this);
    }

    @Override
    public Boolean visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {
      return !(exp.getCharacter() == 0);
    }

    @Override
    public Boolean visit(CFloatLiteralExpression exp) throws UnrecognizedCCodeException {
      return !exp.getValue().equals(BigDecimal.ZERO);
    }
  }

  private class SMGRightHandSideEvaluator extends SMGExpressionEvaluator {

    private boolean missingExplicitInformation;
    private boolean isRequiered;

    public SMGRightHandSideEvaluator(LogManager pLogger, MachineModel pMachineModel) {
      super(pLogger, pMachineModel);
    }

    private class LValueAssignmentVisitor extends SMGExpressionEvaluator.LValueAssignmentVisitor {

      public LValueAssignmentVisitor(CFAEdge pEdge, SMGState pSmgState) {
        super(pEdge, pSmgState);
      }

      @Override
      public SMGAddress visit(CIdExpression pVariableName) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: variable assignment");

        return super.visit(pVariableName);
      }

      @Override
      public SMGAddress visit(CPointerExpression pLValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to dereferenced pointer");

        return super.visit(pLValue);
      }

      @Override
      public SMGAddress visit(CFieldReference pLValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to field reference");

        return super.visit(pLValue);
      }

      @Override
      public SMGAddress visit(CArraySubscriptExpression pLValue) throws CPATransferException {
        logger.log(Level.FINEST, ">>> Handling statement: assignment to array Cell");

        return super.visit(pLValue);
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
            builtins.evaluateVBPlot(pIastFunctionCallExpression, smgState);
            break;
          case "malloc":
            possibleMallocFail = true;
            isRequiered = true;
            SMGEdgePointsTo mallocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge);
            return createAddress(mallocEdge);
          case "calloc":
            possibleMallocFail = true;
            isRequiered = true;
            SMGEdgePointsTo callocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge);
            return createAddress(callocEdge);
          }
        } else {
          return SMGUnknownValue.getInstance();
        }

        return SMGUnknownValue.getInstance();
      }
    }

    private class PointerAddressVisitor extends SMGExpressionEvaluator.PointerAddressVisitor {

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
            SMGEdgePointsTo mallocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge);
            isRequiered = true;
            return createAddress(mallocEdge);
          case "calloc":
            possibleMallocFail = true;
            SMGEdgePointsTo callocEdge = builtins.evaluateCalloc(pIastFunctionCallExpression, smgState, cfaEdge);
            isRequiered = true;
            return createAddress(callocEdge);
          case "memset":
            SMGEdgePointsTo memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, smgState, cfaEdge);
            return createAddress(memsetTargetEdge);
          }
          throw new AssertionError();
        } else {
          return SMGUnknownValue.getInstance();
        }
      }
    }

    @Override
    protected org.sosy_lab.cpachecker.cpa.cpalien.SMGExpressionEvaluator.PointerAddressVisitor getPointerAddressVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      // TODO Auto-generated method stub
      return new PointerAddressVisitor(pCfaEdge, pNewState);
    }

    @Override
    protected org.sosy_lab.cpachecker.cpa.cpalien.SMGExpressionEvaluator.ExpressionValueVisitor getExpressionValueVisitor(
        CFAEdge pCfaEdge, SMGState pNewState) {
      // TODO Auto-generated method stub
      return new ExpressionValueVisitor(pCfaEdge, pNewState);
    }

    @Override
    public org.sosy_lab.cpachecker.cpa.cpalien.SMGExpressionEvaluator.LValueAssignmentVisitor getLValueAssignmentVisitor(
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

    public boolean isRequiered() {
      return isRequiered;
    }

    public void reset() {
      isRequiered = false;
      missingExplicitInformation= false;
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements,
      CFAEdge cfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    Collection<? extends AbstractState> retVal = null;

    for (AbstractState ae : elements) {
      if(ae instanceof ExplicitState) {
        retVal = strengthen((ExplicitState) ae, (SMGState)element, cfaEdge);
      }
    }

    //TODO More common handling of missing information (erase missing Information if other cpas solved it).
    missingInformationList.clear();
    possibleMallocFail = false;
    hasChanged = false;
    oldState = null;
    return retVal;
  }

  private boolean hasChanged;

  private Collection<? extends AbstractState> strengthen(ExplicitState explicitState, SMGState pSMGState, CFAEdge cfaEdge) throws CPATransferException {

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
      ExplicitState explicitState, MissingInformation pMissingInformation, CFAEdge edge) throws CPATransferException {

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
      writeValue(pSmgState, memoryLocation.getObject(), memoryLocation.getOffset().getAsInt(),
          expressionEvaluator.getRealExpressionType(rValue), symbolicValue, edge);

    }

    return pSmgState;
  }

  private SMGSymbolicValue resolveRValue(SMGState oldState, SMGState newSmgState, ExplicitState pExplicitState,
      CRightHandSide rValue, CFAEdge pEdge) throws CPATransferException {

    //TODO Refactor ...
    if (rValue instanceof CFunctionCallExpression) {
      return resolveFunctionCall(newSmgState, pExplicitState,
          (CFunctionCallExpression) rValue, pEdge);
    } else {

      SMGExplicitCommunicator cc = new SMGExplicitCommunicator();

      String functionName = pEdge.getPredecessor().getFunctionName();

      return cc.evaluateSMGExpression(pExplicitState, functionName, oldState, machineModel, logger, pEdge,
          rValue);
    }
  }

  private SMGSymbolicValue resolveFunctionCall(SMGState pSmgState,
      ExplicitState pExplicitState,
      CFunctionCallExpression pIastFunctionCallExpression,
      CFAEdge pEdge) throws CPATransferException {

    SMGExplicitBuiltIns builtins = new SMGExplicitBuiltIns(pExplicitState);

    CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
    String functionName = fileNameExpression.toASTString();

    if (builtins.isABuiltIn(functionName)) {
      switch (functionName) {
      case "__VERIFIER_BUILTIN_PLOT":
        builtins.evaluateVBPlot(pIastFunctionCallExpression, pSmgState);
        return SMGUnknownValue.getInstance();
      case "malloc":
        SMGEdgePointsTo mallocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, pSmgState, pEdge);
        return createAddress(mallocEdge);
      case "calloc":
        SMGEdgePointsTo callocEdge = builtins.evaluateCalloc(pIastFunctionCallExpression, pSmgState, pEdge);
        return createAddress(callocEdge);
      case "memset":
        SMGEdgePointsTo memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, pSmgState, pEdge);
        return createAddress(memsetTargetEdge);
      case "free":
        builtins.evaluateFree(pIastFunctionCallExpression, pSmgState, pEdge);
        return SMGUnknownValue.getInstance();
      }
      throw new AssertionError();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  private SMGSymbolicValue createAddress(SMGEdgePointsTo pMallocEdge) {
    return expressionEvaluator.createAddress(pMallocEdge);
  }

  private SMGAddress resolveMemoryLocation(SMGState pSmgState, ExplicitState pExplicitState,
      CExpression lValue, CFAEdge edge) throws UnrecognizedCCodeException {
    SMGExplicitCommunicator cc = new SMGExplicitCommunicator();

    String functionName = edge.getPredecessor().getFunctionName();

    return cc.evaluateSMGLeftHandSide(pExplicitState, functionName, pSmgState, machineModel, logger, edge, lValue);
  }

  @SuppressWarnings("unused")
  private SMGState resolvingAssumption(SMGState pSmgState, ExplicitState pExplicitState,
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

  private Long resolveAssumptionValue(SMGState pSmgState, ExplicitState pExplicitState,
      CRightHandSide rValue, CFAEdge edge) throws UnrecognizedCCodeException {
    SMGExplicitCommunicator cc = new SMGExplicitCommunicator();

    String functionName = edge.getPredecessor().getFunctionName();

    return cc.evaluateExpression(pExplicitState, functionName, pSmgState, machineModel, logger, edge, rValue);
  }

  @SuppressWarnings("unused")
  private void checkForMissingRequiredInformation(CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    for (MissingInformation missingInformation : missingInformationList) {
      if (missingInformation.isRequieredInformation()) { throw new UnrecognizedCCodeException(
          "Not able to compute allocation size", cfaEdge); }
    }
  }

  private class SMGExplicitBuiltIns extends SMGBuiltins {

    private final ExplicitState explicitState;

    public SMGExplicitBuiltIns(ExplicitState pExplicitState) {
      explicitState = pExplicitState;
    }

    @Override
    protected SMGAddressValue evaluateAddress(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRvalue)
        throws CPATransferException {
      SMGExplicitCommunicator cc = new SMGExplicitCommunicator();

      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      return cc.evaluateSMGAddressExpression(explicitState, functionName, pState, machineModel, logger, pCfaEdge, pRvalue);
     }

    @Override
    protected SMGSymbolicValue evaluateExpressionValue(SMGState pSmgState, CFAEdge pCfaEdge, CExpression pRValue)
        throws CPATransferException {
      return resolveRValue(oldState, pSmgState, explicitState, pRValue, pCfaEdge);
    }

    @Override
    protected String getDot(SMGState pCurrentState, String pName, String pLocation) {
      return pCurrentState.toDot(pName, pLocation, explicitState);
    }

    @Override
    protected File getOutputFile(File pExportSMGFilePattern, String pName) {
      return new File(String.format(exportSMGFilePattern.getAbsolutePath(), "Explicit_" + pName));
    }

    @Override
    protected SMGExplicitValue evaluateExplicitValue(SMGState pState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {
      SMGExplicitCommunicator cc = new SMGExplicitCommunicator();

      String functionName = pCfaEdge.getPredecessor().getFunctionName();

      Long value = cc.evaluateExpression(explicitState, functionName, pState, machineModel, logger, pCfaEdge, pRValue);

      if (value == null) {
        return SMGUnknownValue.getInstance();
      } else {
        return SMGKnownExpValue.valueOf(value);
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
        IARightHandSide pMissingCExpressionInformation) {

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
      return "Object: " + object.toString() + " Offset: " + offset.toString();
    }

    public static SMGAddress valueOf(SMGObject pObj, int pOffset) {
      return new SMGAddress(pObj, SMGKnownExpValue.valueOf(pOffset));
    }
  }
}