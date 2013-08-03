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
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
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

  private /*static*/ final class SMGBuiltins {

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
        File outputFile = new File(String.format(exportSMGFilePattern.getAbsolutePath(), name));
        try {
          Files.writeFile(outputFile, currentState.toDot(name, location));
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write SMG " + name + " to file");
        }
      }
    }

    public void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
      String name = functionCall.getParameterExpressions().get(0).toASTString();
      this.dumpSMGPlot(name, currentState, functionCall.toString());
    }

    // TODO: Seems like there is large code sharing with evaluate calloc
    public SMGEdgePointsTo evaluateMalloc(CFunctionCallExpression functionCall, SMGState currentState, CFAEdge cfaEdge)
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
        throw new UnrecognizedCCodeException("Not able to compute allocation size", cfaEdge);
      }

      String allocation_label = "malloc_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(value.getAsInt(), allocation_label);

      possibleMallocFail = true;
      return new_pointer;
    }

    public SMGEdgePointsTo evaluateMemset(CFunctionCallExpression functionCall,
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
        writeValue(currentState, bufferMemory, offset + c, charType, ch);
      }

      return pointer;
    }

    public SMGEdgePointsTo evaluateCalloc(CFunctionCallExpression functionCall,
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
        throw new UnrecognizedCCodeException(
          "Not able to compute allocation size", cfaEdge);
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

    public void evaluateFree(CFunctionCallExpression pFunctionCall, SMGState currentState,
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

    public boolean isABuiltIn(String functionName) {
      return BUILTINS.contains(functionName);
    }
  }

  final private SMGBuiltins builtins = new SMGBuiltins();

  public SMGTransferRelation(Configuration config, LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    machineModel = pMachineModel;
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

  private SMGState handleExitFromFunction(SMGState smgState,
      CReturnStatementEdge returnEdge) throws CPATransferException {

    CExpression returnExp = returnEdge.getExpression();

    if (returnExp == null) {
      returnExp = CNumericTypes.ZERO; // this is the default in C
    }

    logger.log(Level.FINEST, "Handling return Statement: ",
        returnExp.toASTString());

    CType expType = getRealExpressionType(returnExp);
    SMGObject tmpFieldMemory = smgState.getFunctionReturnObject();

    return handleAssignmentToField(smgState, returnEdge, tmpFieldMemory, 0, expType, returnExp);
  }

  private int getSizeof(CFAEdge edge,  CType pType) throws UnrecognizedCCodeException {

    try {
     return machineModel.getSizeof(pType);
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
        throw new UnrecognizedCCodeException( "Could not resolve type.", edge);
    }
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

      CType lValueType = getRealExpressionType(lValue);

      CType rValueType = getRealExpressionType(((CFunctionCallAssignmentStatement) exprOnSummary).getRightHandSide());

      SMGSymbolicValue rValue = getFunctionReturnValue(newState, rValueType);

      SMGAddress address = null;

      LValueAssignmentVisitor visitor =
          new LValueAssignmentVisitor(functionReturnEdge, tmpState);

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

  private SMGSymbolicValue getFunctionReturnValue(SMGState smgState, CType type) throws SMGInconsistentException {

    SMGObject tmpMemory = smgState.getFunctionReturnObject();

    return readValue(smgState, tmpMemory, SMGKnownExpValue.ZERO, type);
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
      CType cType = getRealExpressionType(paramDecl.get(i));

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
    SMGSymbolicValue value = evaluateAssumptionValue(smgState, cfaEdge, expression);

    if (value.isUnknown()) {
      //TODO derive further information of value (in essence, implement smg.replace(symbValue, symbValue for y == x
      // and addNeq for y != x))

      //TODO Refactor
      SMGExplicitValue explicitValue = evaluateExplicitValue(smgState, cfaEdge, expression);

      if (explicitValue.isUnknown()) {
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
          logger.log(Level.WARNING, "Calling malloc and not using the result, resulting in memory leak at line "
              + pCfaEdge.getLineNumber());
          newState.setMemLeak();
          break;
        case "calloc":
          logger.log(Level.WARNING, "Calling calloc and not using the result, resulting in memory leak at line "
              + pCfaEdge.getLineNumber());
          newState.setMemLeak();
          break;
        case "memset":
          builtins.evaluateMemset(cFCExpression, newState, pCfaEdge);
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

    LValueAssignmentVisitor visitor = new LValueAssignmentVisitor(cfaEdge, state);

    SMGAddress addressOfField = lValue.accept(visitor);

    CType fieldType = getRealExpressionType(lValue);

    if (addressOfField.isUnknown()) {
      return new SMGState(state);
    }

    newState =
        handleAssignmentToField(state, cfaEdge, addressOfField.getObject(),
            addressOfField.getOffset().getAsInt(), fieldType, rValue);

    return newState;
  }

  private /*static*/ class LValueAssignmentVisitor extends DefaultCExpressionVisitor<SMGAddress, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    public LValueAssignmentVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGAddress visitDefault(CExpression pExp) throws CPATransferException {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public SMGAddress visit(CIdExpression variableName) throws CPATransferException {

      // a = ...
      return handleVariableAssignment(variableName);
    }

    private SMGAddress handleVariableAssignment(CIdExpression variableName) throws CPATransferException {
      logger.log(Level.FINEST, ">>> Handling statement: variable assignment");

      SMGObject object = smgState.getObjectForVisibleVariable(variableName.getName());

      return new SMGAddress(object, SMGKnownExpValue.ZERO);
    }

    @Override
    public SMGAddress visit(CUnaryExpression lValue) throws CPATransferException {

      throw new UnrecognizedCCodeException(lValue.toASTString() + " is not an lValue", cfaEdge, lValue);
    }

    @Override
    public SMGAddress visit(CPointerExpression lValue) throws CPATransferException {
      // handle Pointer assignment
      logger.log(Level.FINEST, ">>> Handling statement: assignment to dereferenced pointer");

      CExpression addressExpression = lValue.getOperand();

      SMGAddressValue addressValue = evaluateAddress(smgState, cfaEdge, addressExpression);

      if (addressValue.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      return addressValue.getAddress();
      }

    @Override
    public SMGAddress visit(CFieldReference lValue) throws CPATransferException {
      // a->b = ...
      return handleAssignmentToFieldReference(lValue);
    }

    private SMGAddress handleAssignmentToFieldReference(CFieldReference fieldReference)
        throws CPATransferException {
      logger.log(Level.FINEST, ">>> Handling statement: assignment to field reference");

      CType ownerType = getRealExpressionType(fieldReference.getFieldOwner());

      SMGField field = getField(cfaEdge, ownerType, fieldReference.getFieldName());

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, fieldReference);

      if (addressOfField.isUnknown() || field.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      return addressOfField.add(field.getOffset());
    }

    @Override
    public SMGAddress visit(CArraySubscriptExpression lValue) throws CPATransferException {

      // a[i] = ...
      return handleArrayAssignment(lValue);
    }

    private SMGAddress handleArrayAssignment(CArraySubscriptExpression lValue) throws CPATransferException {
      logger.log(Level.FINEST, ">>> Handling statement: assignment to array Cell");

      SMGAddress memoryAndOffset = evaluateArraySubscriptExpression(smgState, cfaEdge, lValue);

      return memoryAndOffset;
    }
  }

  private SMGAddress evaluateArraySubscriptExpression(SMGState smgState, CFAEdge cfaEdge,
      CArraySubscriptExpression exp) throws CPATransferException {

    SMGAddress arrayMemoryAndOffset =
        evaluateArrayExpression(smgState, cfaEdge, exp.getArrayExpression());

    if (arrayMemoryAndOffset.isUnknown()) {
      return arrayMemoryAndOffset;
    }

    SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, exp.getSubscriptExpression());

    if (subscriptValue.isUnknown()) {
      return SMGAddress.UNKNOWN;
    }

    SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, exp.getExpressionType()));

    SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

    return arrayMemoryAndOffset.add(subscriptOffset);
  }

  private SMGAddress evaluateArrayExpression(SMGState smgState, CFAEdge cfaEdge,
      CExpression arrayExpression) throws CPATransferException {

    CType arrayExpressionType = getRealExpressionType(arrayExpression);

    if (arrayExpressionType instanceof CPointerType) {

      SMGAddressValue address = evaluateAddress(smgState, cfaEdge, arrayExpression);

      return address.getAddress();

    } else if (arrayExpressionType instanceof CArrayType) {

      ArrayMemoryVisitor visitor = new ArrayMemoryVisitor(cfaEdge, smgState);

      return arrayExpression.accept(visitor);
    } else {
      return SMGAddress.UNKNOWN;
    }
  }

  private SMGAddress getAddressOfField(SMGState smgState, CFAEdge cfaEdge, CFieldReference fieldReference)
      throws CPATransferException {

    //TODO Refactor

    CExpression fieldOwner = fieldReference.getFieldOwner();

    CType ownerType = getRealExpressionType(fieldOwner);

    SMGAddress fieldAddress;

    if (fieldOwner instanceof CIdExpression) {
      // a.b

      CIdExpression idExpOwner = (CIdExpression) fieldOwner;
      SMGObject memoryOfFieldOwner = smgState.getObjectForVisibleVariable(idExpOwner.getName());

      if (fieldReference.isPointerDereference()) {

        SMGSymbolicValue address = readValue(smgState, memoryOfFieldOwner, SMGKnownExpValue.ZERO, ownerType);

        SMGAddressValue addressValue = getAddressFromSymbolicValue(smgState, address);

        fieldAddress = addressValue.getAddress();

      } else {
        fieldAddress = new SMGAddress(memoryOfFieldOwner, SMGKnownExpValue.ZERO);
      }

    } else if (fieldOwner instanceof CFieldReference) {
      // (a.b).c

     CFieldReference ownerFieldReference = (CFieldReference) fieldOwner;

     SMGAddress addressOfFieldOwner = getAddressOfField(smgState, cfaEdge, ownerFieldReference);

     if (addressOfFieldOwner.isUnknown()) {
      return SMGAddress.UNKNOWN;
     }

     // type of a of (a.b).c
     CType typeOfFieldOwnerOwner = getRealExpressionType(ownerFieldReference.getFieldOwner());

     String fieldName = ownerFieldReference.getFieldName();

     SMGField field = getField(cfaEdge, typeOfFieldOwnerOwner, fieldName);

     if (field.isUnknown()) {
       return SMGAddress.UNKNOWN;
     }

     SMGExplicitValue fieldOffset = addressOfFieldOwner.add(field.getOffset()).getOffset();

     SMGObject fieldObject = addressOfFieldOwner.getObject();


      if (fieldReference.isPointerDereference()) {

        SMGSymbolicValue address = readValue(smgState, fieldObject, fieldOffset, field.getType());

        SMGAddressValue addressValue = getAddressFromSymbolicValue(smgState, address);

        fieldAddress = addressValue.getAddress();

      } else {
        fieldAddress = new SMGAddress(fieldObject, fieldOffset);
      }

    } else if (fieldOwner instanceof CPointerExpression) {
      // (*a).b
      SMGAddressValue address = evaluateAddress(smgState, cfaEdge, ((CPointerExpression) fieldOwner).getOperand());

      SMGAddress fieldOwnerAddress = address.getAddress();

      if (fieldReference.isPointerDereference()) {
        SMGSymbolicValue address2 = readValue(smgState, fieldOwnerAddress.getObject(),
            fieldOwnerAddress.getOffset(), ownerType);

        SMGAddressValue address2Value = getAddressFromSymbolicValue(smgState, address2);

        fieldAddress = address2Value.getAddress();
      } else {
        fieldAddress = fieldOwnerAddress;
      }

    } else if (fieldOwner instanceof CArraySubscriptExpression) {
      // (a[]).b
      fieldAddress = SMGAddress.UNKNOWN;

    } else {
      fieldAddress = SMGAddress.UNKNOWN;
    }

    return fieldAddress;
  }

  private SMGSymbolicValue readValue(SMGState pSmgState, SMGObject pObject,
      SMGExplicitValue pOffset, CType pType) throws SMGInconsistentException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGUnknownValue.getInstance();
    }

    Integer value = pSmgState.readValue(pObject, pOffset.getAsInt(), pType);

    if (value == null) {
      return SMGUnknownValue.getInstance();
    }

    return SMGKnownSymValue.valueOf(value);
  }

  private SMGField getField(CFAEdge edge, CType ownerType, String fieldName) throws UnrecognizedCCodeException {

    if (ownerType instanceof CElaboratedType) {
      return getField(edge, ((CElaboratedType) ownerType).getRealType(), fieldName);
    } else if (ownerType instanceof CCompositeType) {
      return getField(edge, (CCompositeType)ownerType, fieldName);
    } else if (ownerType instanceof CPointerType) {

      CType type = ((CPointerType) ownerType).getType();

      type = getRealExpressionType(type);

      return getField(edge, type, fieldName);
    }

    throw new AssertionError();
  }

  private SMGField getField(CFAEdge pEdge, CCompositeType ownerType, String fieldName) throws UnrecognizedCCodeException {

    List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

    int offset = 0;

    for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
      String memberName = typeMember.getName();
      if (memberName.equals(fieldName)) {

      return new SMGField(SMGKnownExpValue.valueOf(offset),
          getRealExpressionType(typeMember.getType())); }

      if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {
        offset = offset + getSizeof(pEdge, getRealExpressionType(typeMember.getType()));
      }
    }

    return new SMGField(SMGUnknownValue.getInstance(), ownerType);
  }

  private void assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    assignFieldToState(newState, newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, rValue);
  }

  private void assignFieldToState(SMGState newState, SMGState readState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, CRightHandSide rValue)
      throws CPATransferException {

    CType rValueType = getRealExpressionType(rValue);

    SMGSymbolicValue value = evaluateExpressionValue(readState, cfaEdge, rValue);

    if (value.isUnknown()) {

      value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());

      if (newValueIsNeqZero(newState, cfaEdge, rValue)) {
        newState.setUnequal(value.getAsInt(), 0);
      }
    }

    assignFieldToState(newState, cfaEdge, memoryOfField, fieldOffset, pFieldType, value, rValueType);
  }

  private void assignFieldToState(SMGState newState, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType pFieldType, SMGSymbolicValue value, CType rValueType)
      throws UnrecognizedCCodeException, SMGInconsistentException {

    if (memoryOfField.getSizeInBytes() < getSizeof(cfaEdge, rValueType)) {
      logger.log(Level.WARNING, "Attempting to write " + getSizeof(cfaEdge, rValueType) +
          " bytes into a field with size " + memoryOfField.getSizeInBytes() + "bytes.\n" +
          "Line " + cfaEdge.getLineNumber() + ": " + cfaEdge.getRawStatement());
    }

    if (isStructOrUnionType(rValueType)) {

      if (value instanceof SMGKnownAddVal) {

        SMGObject object = ((SMGKnownAddVal) value).getObject();
        copy(newState, memoryOfField, object);
      }
    } else {
      writeValue(newState, memoryOfField, fieldOffset, rValueType, value);
    }
  }

  private void writeValue(SMGState pNewState, SMGObject pMemoryOfField, int pFieldOffset, CType pRValueType,
      SMGSymbolicValue pValue) throws SMGInconsistentException {

    if (pValue.isUnknown() || pNewState == null) {
      return;
    }

    pNewState.writeValue(pMemoryOfField, pFieldOffset, pRValueType, pValue);
  }

  private void copy(SMGState pNewState, SMGObject pMemoryOfField, SMGObject pObject) {


  }

  boolean isStructOrUnionType(CType rValueType) {

    if (rValueType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    if (rValueType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return false;
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
      CType rValueType = getRealExpressionType(rValue);
      writeValue(otherState, memoryOfField, fieldOffset, rValueType, SMGKnownSymValue.ZERO);
      mallocFailState = otherState;
    }

    return newState;
  }

  private SMGExplicitValue evaluateExplicitValue(SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(smgState, cfaEdge);
    SMGExplicitValue value = rValue.accept(visitor);
    return value;
  }


  private boolean newValueIsNeqZero(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws UnrecognizedCCodeException {

    return rValue.accept(new IsNotZeroVisitor(newState, cfaEdge));
  }

  private SMGSymbolicValue evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || expressionType instanceof CArrayType
        || isStructOrUnionType(expressionType)) {

      return evaluateAddress(smgState, cfaEdge, rValue);
    } else {
      return evaluateNonAddressValue(smgState, cfaEdge, rValue);
    }
  }

  private SMGSymbolicValue evaluateNonAddressValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(cfaEdge, newState);

    SMGSymbolicValue symbolicValue = rValue.accept(visitor);

    return symbolicValue;
  }

  private SMGSymbolicValue evaluateAssumptionValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = new AssumeVisitor(cfaEdge, newState);
    return rValue.accept(visitor);
  }

  private SMGAddressValue evaluateAddress(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    PointerAddressVisitor visitor = new PointerAddressVisitor(cfaEdge, newState);
    SMGSymbolicValue address = rValue.accept(visitor);

    return getAddressFromSymbolicValue(newState, address);
  }

  private SMGState handleDeclaration(SMGState smgState, CDeclarationEdge edge) throws CPATransferException {
    logger.log(Level.FINEST, ">>> Handling declaration");
    SMGState newState = new SMGState(smgState);

    CDeclaration cDecl = edge.getDeclaration();

    if (cDecl instanceof CVariableDeclaration) {
      CVariableDeclaration cVarDecl = (CVariableDeclaration) cDecl;
      logger.log(Level.FINEST, "Handling variable declaration:", cVarDecl.toASTString());
      String varName = cVarDecl.getName();
      CType cType = getRealExpressionType(cVarDecl);

      SMGObject newObject;

      CInitializer newInitializer = cVarDecl.getInitializer();

      if (cVarDecl.isGlobal()) {
        newObject = smgState.createObject(getSizeof(edge, cType), varName);
        logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to global objects");
        newState.addGlobalObject(newObject);

        if (newInitializer == null) {
          // global variables without initializer are set to 0 in C
          writeValue(newState, newObject, 0, cType, SMGKnownSymValue.ZERO);
        }

      } else {

        newObject = newState.addLocalVariable(cType, varName);
        logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to current stack");

        //Check whether variable was already declared, for example in loops
        // TODO explicitly check this
        newObject = newState.getObjectForVisibleVariable(newObject.getLabel());
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

  private CType getRealExpressionType(CSimpleDeclaration decl) {
    return getRealExpressionType(decl.getType());
  }

  private CType getRealExpressionType(CType type) {

    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }

    return type;
  }

  private CType getRealExpressionType(CRightHandSide exp) {
    return getRealExpressionType(exp.getExpressionType());
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
                getRealExpressionType(expr),
                riteOperand,
                riteAddend,
                newOperation);

            CBinaryExpression assume = new CBinaryExpression(expression.getFileLocation(),
                getRealExpressionType(binaryExpression),
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

  private /*static*/ class PointerAddressVisitor extends ExpressionValueVisitor
      implements CRightHandSideVisitor<SMGSymbolicValue, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;


    public PointerAddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
      cfaEdge = super.getCfaEdge();
      smgState = super.getSmgState();
    }

    @Override
    public SMGAddressValue visit(CIntegerLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CCharLiteralExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CFloatLiteralExpression pExp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(pExp));
    }

    @Override
    public SMGAddressValue visit(CIdExpression exp) throws CPATransferException {

      CType c = getRealExpressionType(exp);

      if (c instanceof CArrayType) {
        // a == &a[0];
        return createAddressOfVariable(exp);
      } else if (isStructOrUnionType(c)) {
        // We use this temporary address to copy the values of the struct or union
        return createAddressOfVariable(exp);
      }

      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CUnaryExpression unaryExpression) throws  CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        return handleAmper(unaryOperand);

      case SIZEOF:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + unaryOperand.toASTString()
            + " as pointer type", cfaEdge, unaryExpression);

      case MINUS:
      case NOT:
      case TILDE:
      default:
        // Can't evaluate these Addresses
        return SMGUnknownValue.getInstance();
      }
    }

    private SMGAddressValue handleAmper(CExpression lValue) throws CPATransferException {
      if (lValue instanceof CIdExpression) {
        // &a
        return createAddressOfVariable((CIdExpression) lValue);
      } else if (lValue instanceof CPointerExpression) {
        // &(*(a))

        return  getAddressFromSymbolicValue( smgState ,
            ((CPointerExpression) lValue).getOperand().accept(this));

      } else if (lValue instanceof CFieldReference) {
        // &(a.b)
        return createAddressOfField((CFieldReference) lValue);
      } else if (lValue instanceof CArraySubscriptExpression) {
        // &a[b]
        return createAddressOfArraySubscript((CArraySubscriptExpression) lValue);
      } else {
        return SMGUnknownValue.getInstance();
      }
    }

    private SMGAddressValue createAddressOfArraySubscript(CArraySubscriptExpression lValue)
        throws CPATransferException {

      CExpression arrayExpression = lValue.getArrayExpression();

      SMGAddress arrayAddress = evaluateArrayExpression(smgState, cfaEdge, arrayExpression);

      if (arrayAddress.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      CExpression subscriptExpr = lValue.getSubscriptExpression();

      SMGExplicitValue subscriptValue = evaluateExplicitValue(smgState, cfaEdge, subscriptExpr);

      if (subscriptValue.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGExplicitValue arrayOffset = arrayAddress.getOffset();

      int typeSize = getSizeof(cfaEdge, getRealExpressionType(lValue));

      SMGExplicitValue sizeOfType = SMGKnownExpValue.valueOf(typeSize);

      SMGExplicitValue offset =  arrayOffset.add(subscriptValue).multiply(sizeOfType);

      return createAddress(smgState, arrayAddress.getObject(), offset);
    }

    private SMGAddressValue createAddressOfField(CFieldReference lValue) throws CPATransferException {

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, lValue);
      SMGField field = getField(cfaEdge, getRealExpressionType(lValue.getFieldOwner()), lValue.getFieldName());

      if (field.isUnknown() || addressOfField.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGAddress address = addressOfField.add(field.getOffset());

      return createAddress(smgState, address.getObject(), address.getOffset());
    }

    private SMGAddressValue createAddressOfVariable(CIdExpression idExpression) throws SMGInconsistentException {

      SMGObject variableObject = smgState.getObjectForVisibleVariable(idExpression.getName());

      if (variableObject == null) {
        return SMGUnknownValue.getInstance();
      } else {
        return createAddress(smgState, variableObject, SMGKnownExpValue.ZERO);
      }
    }

    @Override
    public SMGAddressValue visit(CPointerExpression pointerExpression) throws  CPATransferException {

      CExpression operand = pointerExpression.getOperand();
      CType operandType = getRealExpressionType(operand);
      CType expType = getRealExpressionType(pointerExpression);

      if (operandType instanceof CPointerType) {

        SMGSymbolicValue address = dereferencePointer(operand, expType);
        return getAddressFromSymbolicValue(smgState, address);

      } else if (operandType instanceof CArrayType) {

        SMGSymbolicValue address = dereferenceArray(operand, expType);
        return getAddressFromSymbolicValue(smgState, address);

      } else {
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + operand.toASTString()
            + " as pointer type", cfaEdge, pointerExpression);
      }
    }

    @Override
    public SMGAddressValue visit(CBinaryExpression binaryExp) throws CPATransferException {

      BinaryOperator binaryOperator = binaryExp.getOperator();
      CExpression lVarInBinaryExp = binaryExp.getOperand1();
      CExpression rVarInBinaryExp = binaryExp.getOperand2();
      CType lVarInBinaryExpType = getRealExpressionType(lVarInBinaryExp);
      CType rVarInBinaryExpType = getRealExpressionType(rVarInBinaryExp);

      boolean lVarIsAddress = lVarInBinaryExpType instanceof CPointerType;
      boolean rVarIsAddress = rVarInBinaryExpType instanceof CPointerType;

      CExpression address = null;
      CExpression pointerOffset = null;
      CType addressType = null;

      if (lVarIsAddress == rVarIsAddress) {
        return SMGUnknownValue.getInstance(); // If both or neither are Addresses,
        //  we can't evaluate the address this pointer stores.
      } else if (lVarIsAddress) {
        address = lVarInBinaryExp;
        pointerOffset = rVarInBinaryExp;
        addressType = lVarInBinaryExpType;
      } else if (rVarIsAddress) {
        address = rVarInBinaryExp;
        pointerOffset = lVarInBinaryExp;
        addressType = rVarInBinaryExpType;
      } else {
        // TODO throw Exception, no Pointer
        return SMGUnknownValue.getInstance();
      }

      switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        SMGSymbolicValue addressVal = address.accept(this);

        if (!(addressVal instanceof SMGAddressValue)) {
          return SMGUnknownValue.getInstance();
        }

        SMGAddressValue addressValue = (SMGAddressValue) addressVal;

        ExplicitValueVisitor v = new ExplicitValueVisitor(smgState, cfaEdge);

        SMGExplicitValue offsetValue = pointerOffset.accept(v);

        if (addressValue.isUnknown() || offsetValue.isUnknown()) {
          return addressValue;
        }

        SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, addressType));

        SMGExplicitValue pointerOffsetValue = offsetValue.multiply(typeSize);

        SMGObject target = addressValue.getObject();

        SMGExplicitValue addressOffset = addressValue.getOffset();

        switch (binaryOperator) {
        case PLUS:
          return createAddress(smgState, target, addressOffset.add(pointerOffsetValue));
        case MINUS:
          if (lVarIsAddress) {
            return createAddress(smgState, target, addressOffset.subtract(pointerOffsetValue));
          } else {
            return createAddress(smgState, target, pointerOffsetValue.subtract(addressOffset));
          }
        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + binaryExp + " as pointer type",
            cfaEdge, binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      default:
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGAddressValue visit(CArraySubscriptExpression exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
    }

    @Override
    public SMGAddressValue visit(CFieldReference exp) throws CPATransferException {
      return getAddressFromSymbolicValue(smgState, super.visit(exp));
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
          return createAddress(mallocEdge);
        case "calloc":
          possibleMallocFail = true;
          SMGEdgePointsTo callocEdge = builtins.evaluateCalloc(pIastFunctionCallExpression, smgState, cfaEdge);
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

  private SMGAddressValue createAddress(SMGEdgePointsTo pEdge) {
    return SMGKnownAddVal.valueOf(pEdge.getValue(), pEdge.getObject(), pEdge.getOffset());
  }

  private SMGAddressValue getAddressFromSymbolicValue(SMGState pSmgState,
      SMGSymbolicValue pAddressValue) throws SMGInconsistentException {

    if (pAddressValue instanceof SMGAddressValue) {
      return (SMGAddressValue) pAddressValue;
    }

    if (pAddressValue.isUnknown()) {
      return SMGUnknownValue.getInstance();
    }

    //TODO isPointer(symbolicValue)
    SMGEdgePointsTo edge = pSmgState.getPointerFromValue(pAddressValue.getAsInt());

    return createAddress(edge);
  }

  private SMGAddressValue createAddress(SMGState pSmgState, SMGObject pTarget, SMGExplicitValue pOffset) throws SMGInconsistentException {

    SMGAddressValue addressValue = getAddress(pSmgState, pTarget, pOffset);

    if (addressValue.isUnknown()) {

      SMGKnownSymValue value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
      addressValue = SMGKnownAddVal.valueOf(pTarget, (SMGKnownExpValue)pOffset, value);
    }

    return addressValue;
  }

  private SMGAddressValue getAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    if (pTarget == null || pOffset.isUnknown()) {
      return SMGUnknownValue.getInstance();
    }

    Integer address = pSmgState.getAddress(pTarget, pOffset.getAsInt());

    if (address == null) {
      return SMGUnknownValue.getInstance();
    }

    return createAddress(pSmgState.getPointerFromValue(address));
  }

  private class ArrayMemoryVisitor extends DefaultCExpressionVisitor<SMGAddress, CPATransferException>
      implements CRightHandSideVisitor<SMGAddress, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    public ArrayMemoryVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGAddress visitDefault(CExpression exp) {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public SMGAddress visit(CIdExpression idExpression) throws UnrecognizedCCodeException {
      return new SMGAddress(smgState.getObjectForVisibleVariable(idExpression.getName()),
          SMGKnownExpValue.ZERO);
    }

    @Override
    public SMGAddress visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();

      switch (unaryOperator) {

      case SIZEOF:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + unaryExpression
            + " as array type", cfaEdge, unaryExpression);
      case MINUS:
      case NOT:
      case TILDE:
      case AMPER:
      default:
        // Can't evaluate these ArrayExpressions
        return SMGAddress.UNKNOWN;
      }
    }

    @Override
    public SMGAddress visit(CPointerExpression pointerExpression) throws CPATransferException {

      CExpression operand = pointerExpression.getOperand();
      CType operandType = getRealExpressionType(operand);

      boolean operandIsPointer = operandType instanceof CPointerType;


      if (operandIsPointer) {

        SMGAddressValue addressValue = evaluateAddress(smgState, cfaEdge, operand);

        if (addressValue.isUnknown()) {
          return SMGAddress.UNKNOWN;
        }

        return addressValue.getAddress();

      } else {
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + pointerExpression
            + " as array type", cfaEdge, pointerExpression);
      }
    }

    @Override
    public SMGAddress visit(CBinaryExpression binaryExp) throws CPATransferException {

      BinaryOperator binaryOperator = binaryExp.getOperator();
      CExpression lVarInBinaryExp = binaryExp.getOperand1();
      CExpression rVarInBinaryExp = binaryExp.getOperand2();
      CType lVarInBinaryExpType = getRealExpressionType(lVarInBinaryExp);
      CType rVarInBinaryExpType = getRealExpressionType(rVarInBinaryExp);

      boolean lVarIsAddress = lVarInBinaryExpType instanceof CArrayType;
      boolean rVarIsAddress = rVarInBinaryExpType instanceof CArrayType;

      CExpression address = null;
      CExpression arrayOffset = null;
      CType addressType = null;

      if (lVarIsAddress == rVarIsAddress) {
        return SMGAddress.UNKNOWN; // If both or neither are Addresses,
        //  we can't evaluate the address this pointer stores.
      } else if (lVarIsAddress) {
        address = lVarInBinaryExp;
        arrayOffset = rVarInBinaryExp;
        addressType = lVarInBinaryExpType;
      } else if (rVarIsAddress) {
        address = rVarInBinaryExp;
        arrayOffset = lVarInBinaryExp;
        addressType = rVarInBinaryExpType;
      } else {
        // TODO throw Exception, no Pointer
        return SMGAddress.UNKNOWN;
      }

      switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        SMGAddress addressVal = address.accept(this);

        if (addressVal.isUnknown()) {
          return addressVal;
        }

        ExplicitValueVisitor v = new ExplicitValueVisitor(smgState, cfaEdge);

        SMGExplicitValue offsetValue = arrayOffset.accept(v);

        if (offsetValue.isUnknown()) {
          return SMGAddress.UNKNOWN;
        }

        SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, addressType));

        SMGExplicitValue arrayOffsetValue = offsetValue.multiply(typeSize);

        SMGObject target = addressVal.getObject();

        SMGExplicitValue addressOffset = addressVal.getOffset();

        switch (binaryOperator) {
        case PLUS:
          return SMGAddress.valueOf(target, addressOffset.add(addressOffset));
        case MINUS:
          if (lVarIsAddress) {
            return SMGAddress.valueOf(target, addressOffset.subtract(arrayOffsetValue));
          } else {
            return SMGAddress.valueOf(target, arrayOffsetValue.subtract(addressOffset));
          }
        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + binaryExp + " as pointer type", cfaEdge, binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      default:
        return SMGAddress.UNKNOWN;
      }
    }

    @Override
    public SMGAddress visit(CArraySubscriptExpression exp) throws CPATransferException {

      SMGAddress arrayAddress = exp.getArrayExpression().accept(this);

      if (arrayAddress.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      ExplicitValueVisitor v = new ExplicitValueVisitor(smgState, cfaEdge);

      SMGExplicitValue offsetVal = exp.getSubscriptExpression().accept(v);

      if (offsetVal.isUnknown()) {
        return SMGAddress.UNKNOWN;
      }

      SMGExplicitValue offsetValue = offsetVal;

      CType arrayType = getRealExpressionType(exp.getArrayExpression());

      SMGKnownExpValue typeSize = SMGKnownExpValue.valueOf(getSizeof(cfaEdge, arrayType));

      SMGExplicitValue arrayOffsetValue = offsetValue.multiply(typeSize);

      return arrayAddress.add(arrayOffsetValue);
    }

    @Override
    public SMGAddress visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return SMGAddress.UNKNOWN;
    }

    @Override
    public SMGAddress visit(CCastExpression cast) throws CPATransferException {
      return cast.getOperand().accept(this);
    }

    @Override
    public SMGAddress visit(CFieldReference fieldReference) throws CPATransferException {

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, fieldReference);
      SMGField field = getField(cfaEdge, getRealExpressionType(fieldReference.getFieldOwner()), fieldReference.getFieldName());

      return addressOfField.add(field.getOffset());
    }
  }

  private class AssumeVisitor extends ExpressionValueVisitor {

    private final SMGState smgState;

    public AssumeVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
      smgState = getSmgState();
    }

    @Override
    public SMGSymbolicValue visit(CBinaryExpression exp) throws CPATransferException {

      BinaryOperator binaryOperator = exp.getOperator();

      switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:

        CExpression lVarInBinaryExp = exp.getOperand1();
        CExpression rVarInBinaryExp = exp.getOperand2();

        SMGSymbolicValue lVal = evaluateExpressionValue(smgState, getCfaEdge(), lVarInBinaryExp);
        if (lVal.isUnknown()) { return SMGUnknownValue.getInstance(); }

        SMGSymbolicValue rVal = evaluateExpressionValue(smgState, getCfaEdge(), rVarInBinaryExp);
        if (rVal.isUnknown()) { return SMGUnknownValue.getInstance(); }

        boolean isZero;
        boolean isOne;

        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = lVal.equals(rVal);
          isOne = smgState.isUnequal(lVal.getAsInt(), rVal.getAsInt());
          break;
        case EQUALS:
          isOne = lVal.equals(rVal);
          isZero = smgState.isUnequal(lVal.getAsInt(), rVal.getAsInt());
          break;
        case LESS_EQUAL:
        case GREATER_EQUAL:
          isOne = lVal.equals(rVal);
          isZero = false;
          if (isOne) {
            break;
          }

          //$FALL-THROUGH$
        case GREATER_THAN:
        case LESS_THAN:

          SMGAddressValue rAddress = getAddressFromSymbolicValue(getSmgState(), rVal);

          if (rAddress.isUnknown()) {
            return SMGUnknownValue.getInstance();
          }

          SMGAddressValue lAddress = getAddressFromSymbolicValue(getSmgState(), rVal);

          if (lAddress.isUnknown()) {
            return SMGUnknownValue.getInstance();
          }

          SMGObject lObject = lAddress.getObject();
          SMGObject rObject = rAddress.getObject();

          if (!lObject.equals(rObject)) {
            return SMGUnknownValue.getInstance();
          }

          long rOffset = rAddress.getOffset().getAsLong();
          long lOffset = lAddress.getOffset().getAsLong();

          // We already checked equality
          switch (binaryOperator) {
          case LESS_THAN:
          case LESS_EQUAL:
            isOne = lOffset < rOffset;
            isZero = !isOne;
            break;
          case GREATER_EQUAL:
          case GREATER_THAN:
            isOne = lOffset > rOffset;
            isZero = !isOne;
            break;
          default:
            throw new AssertionError();
          }
          break;
        default:
          throw new AssertionError();
        }

        if (isZero) {
          // return 0 if the expression does not hold
          return SMGKnownSymValue.FALSE;
        } else if (isOne) {
          // return a symbolic Value representing 1 if the expression does hold
          return SMGKnownSymValue.TRUE;
        } else {
          // otherwise return UNKNOWN
          return SMGUnknownValue.getInstance();
        }

      default:
        return super.visit(exp);
      }
    }
  }

  private /*static*/ class ExpressionValueVisitor extends DefaultCExpressionVisitor<SMGSymbolicValue, CPATransferException>
      implements CRightHandSideVisitor<SMGSymbolicValue, CPATransferException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGSymbolicValue visitDefault(CExpression pExp) {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CArraySubscriptExpression exp) throws CPATransferException {

      SMGAddress address = evaluateArraySubscriptExpression(smgState, cfaEdge, exp);

      if (address.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGSymbolicValue value = readValue(smgState, address.getObject(), address.getOffset(), getRealExpressionType(exp));

      return value;
    }

    @Override
    public SMGSymbolicValue visit(CIntegerLiteralExpression exp) throws CPATransferException {

      BigInteger value = exp.getValue();

      boolean isZero = value.equals(BigInteger.ZERO);

      return (isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance());
    }

    @Override
    public SMGSymbolicValue visit(CCharLiteralExpression exp) throws CPATransferException {

      char value = exp.getCharacter();

      return (value == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CFieldReference fieldReference) throws CPATransferException {

      SMGAddress addressOfField = getAddressOfField(smgState, cfaEdge, fieldReference);

      SMGField field = getField(cfaEdge, getRealExpressionType(fieldReference.getFieldOwner()), fieldReference.getFieldName());

      if (addressOfField.isUnknown() || field.isUnknown()) {
        return SMGUnknownValue.getInstance();
      }

      SMGExplicitValue addressOffset =  addressOfField.getOffset();
      SMGExplicitValue fieldOffset =    field.getOffset();

      SMGExplicitValue offset = addressOffset.add(fieldOffset);

      return readValue(smgState, addressOfField.getObject(), offset, field.getType());
    }

    @Override
    public SMGSymbolicValue visit(CFloatLiteralExpression exp) throws CPATransferException {

      boolean isZero = exp.getValue().equals(BigDecimal.ZERO);

      return isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CIdExpression idExpression) throws CPATransferException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {

        long enumValue = ((CEnumerator) decl).getValue();

        return enumValue == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

      } else if (decl instanceof CVariableDeclaration
          || decl instanceof CParameterDeclaration) {

        SMGObject variableObject = smgState.getObjectForVisibleVariable(idExpression.getName());

        return readValue(smgState, variableObject, SMGKnownExpValue.ZERO, getRealExpressionType(idExpression));
      }

      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CUnaryExpression unaryExpression) throws CPATransferException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        throw new UnrecognizedCCodeException("Can't use & of expression " + unaryOperand.toASTString(), cfaEdge,
            unaryExpression);

      case MINUS:
        SMGSymbolicValue value = unaryOperand.accept(this);
        return value.equals(SMGKnownSymValue.ZERO) ? value : SMGUnknownValue.getInstance();

      case SIZEOF:
        int size = getSizeof(cfaEdge, getRealExpressionType(unaryOperand));
        return (size == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

      case NOT:
        return handleNot(unaryOperand);

      case TILDE:

      default:
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGSymbolicValue visit(CPointerExpression pointerExpression) throws CPATransferException {

      CExpression operand = pointerExpression.getOperand();
      CType operandType = getRealExpressionType(operand);
      CType expType = getRealExpressionType(pointerExpression);


      if (operandType instanceof CPointerType) {
        return dereferencePointer(operand, expType);
      } else if (operandType instanceof CArrayType) {
        return dereferenceArray(operand, expType);
      } else {
        throw new UnrecognizedCCodeException(cfaEdge, pointerExpression);
      }
    }

    private SMGSymbolicValue handleNot(CExpression pUnaryOperand) throws CPATransferException {
      CType unaryOperandType = getRealExpressionType(pUnaryOperand);

      SMGSymbolicValue value;

      if (unaryOperandType instanceof CPointerType || unaryOperandType instanceof CArrayType) {
        value = evaluateAddress(smgState, cfaEdge, pUnaryOperand);
      } else {
        value = pUnaryOperand.accept(this);
      }

      if (value.equals(SMGKnownSymValue.ZERO)) {
        return SMGKnownSymValue.ZERO;
      } else if (isUnequal(smgState, value, SMGKnownSymValue.ZERO)) {
        return SMGKnownSymValue.ZERO;
      } else {
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGSymbolicValue visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        return getSizeof(cfaEdge, type) == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      default:
        return SMGUnknownValue.getInstance();
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public SMGSymbolicValue visit(CBinaryExpression exp) throws CPATransferException {

      BinaryOperator binaryOperator = exp.getOperator();
      CExpression lVarInBinaryExp = exp.getOperand1();
      CExpression rVarInBinaryExp = exp.getOperand2();

      switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {
        SMGSymbolicValue lVal = lVarInBinaryExp.accept(this);
        if (lVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        SMGSymbolicValue rVal = rVarInBinaryExp.accept(this);
        if (rVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        boolean isZero;

        switch (binaryOperator) {
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          isZero = lVal.equals(SMGKnownSymValue.ZERO) && lVal.equals(SMGKnownSymValue.ZERO);
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        case MINUS:
        case MODULO:
          isZero = (lVal.equals(rVal));
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal.equals(SMGKnownSymValue.ZERO)) { return SMGUnknownValue.getInstance(); }

          isZero = lVal.equals(SMGKnownSymValue.ZERO);
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        case MULTIPLY:
        case BINARY_AND:
          isZero = lVal.equals(SMGKnownSymValue.ZERO)
              || rVal.equals(SMGKnownSymValue.ZERO);
          return (isZero) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        SMGSymbolicValue lVal = lVarInBinaryExp.accept(this);
        if (lVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        SMGSymbolicValue rVal = rVarInBinaryExp.accept(this);
        if (rVal.equals(SMGUnknownValue.getInstance())) { return SMGUnknownValue.getInstance(); }

        boolean isZero;
        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = (lVal.equals(rVal));
          break;
        case EQUALS:
          isZero = isUnequal(smgState, lVal, rVal);
          break;
        case GREATER_THAN:
        case GREATER_EQUAL:
        case LESS_THAN:
        case LESS_EQUAL:
          isZero = false;
          break;

        default:
          throw new AssertionError();
        }

        if (isZero) {
          return SMGKnownSymValue.ZERO;
        } else {
          return SMGUnknownValue.getInstance();
        }
      }

      default:
        return SMGUnknownValue.getInstance();
      }
    }

    private boolean isUnequal(SMGState pSmgState, SMGSymbolicValue pLVal, SMGSymbolicValue pRVal) {

      if (pLVal.isUnknown() || pRVal.isUnknown()) {
        return false;
      }

      return pSmgState.isUnequal(pLVal.getAsInt(), pRVal.getAsInt());
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
          SMGEdgePointsTo mallocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge);
          return createAddress(mallocEdge);
        case "calloc":
          possibleMallocFail = true;
          SMGEdgePointsTo callocEdge = builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge);
          return createAddress(callocEdge);
        }
      } else {
        return SMGUnknownValue.getInstance();
      }

      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGSymbolicValue visit(CCastExpression cast) throws CPATransferException {
      return cast.getOperand().accept(this);
    }

    protected SMGSymbolicValue dereferenceArray(CRightHandSide exp, CType derefType) throws CPATransferException {

      ArrayMemoryVisitor v = new ArrayMemoryVisitor(cfaEdge, smgState);

      SMGAddress address = exp.accept(v);

      if (address.isUnknown()) {
        // We can't resolve the field to dereference, therefore
        // we must assume, that it is invalid
        smgState.setUnkownDereference();
        return SMGUnknownValue.getInstance();
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(smgState, address.getObject(), address.getOffset());
      } else {
        return readValue(smgState, address.getObject(), address.getOffset(), derefType);
      }
    }

    protected SMGSymbolicValue dereferencePointer(CRightHandSide exp, CType derefType)
        throws CPATransferException {

      SMGAddressValue address = evaluateAddress(smgState, cfaEdge, exp);

      if (address.isUnknown()) {
        // We can't resolve the field to dereference , therefore
        // we must assume, that it is invalid
        smgState.setUnkownDereference();
        return SMGUnknownValue.getInstance();
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        return createAddress(smgState, address.getObject(), address.getOffset());
      } else {
        return readValue(smgState, address.getObject(), address.getOffset(), derefType);
      }
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }

  private class ExplicitValueVisitor extends DefaultCExpressionVisitor<SMGExplicitValue, CPATransferException>
      implements CRightHandSideVisitor<SMGExplicitValue, CPATransferException> {

    @SuppressWarnings("unused")
    private final SMGState smgState;
    private final CFAEdge cfaEdge;

    public ExplicitValueVisitor(SMGState pSmgState, CFAEdge pCfaEdge) {
      smgState = pSmgState;
      cfaEdge = pCfaEdge;
    }

    @Override
    protected SMGExplicitValue visitDefault(CExpression pExp) {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      return SMGKnownExpValue.valueOf(exp.asLong());
    }

    @Override
    public SMGExplicitValue visit(CBinaryExpression pE) throws CPATransferException {
      BinaryOperator binaryOperator = pE.getOperator();
      CExpression lVarInBinaryExp = pE.getOperand1();
      CExpression rVarInBinaryExp = pE.getOperand2();

      switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {
        SMGExplicitValue lValue = lVarInBinaryExp.accept(this);

        if (lValue.isUnknown()) {
          return SMGUnknownValue.getInstance();
        }

        SMGExplicitValue rValue = rVarInBinaryExp.accept(this);
        if (rValue.isUnknown()) {
          return SMGUnknownValue.getInstance();
        }

        switch (binaryOperator) {
        case PLUS:
          return lValue.add(rValue);

        case MINUS:
          return lValue.subtract(rValue);

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rValue.equals(SMGKnownExpValue.ZERO)) {
            return SMGUnknownValue.getInstance();
          }

          return lValue.divide(rValue);

        case MULTIPLY:
          return lValue.multiply(rValue);

        case SHIFT_LEFT:
          return lValue.shiftLeft(rValue);

        case BINARY_AND:
          return lValue.and(rValue);

        case BINARY_OR:
          return lValue.or(rValue);

        case BINARY_XOR:
          return lValue.xor(rValue);

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        SMGExplicitValue lValue = lVarInBinaryExp.accept(this);
        if (lValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

        SMGExplicitValue rValue = rVarInBinaryExp.accept(this);
        if (rValue.isUnknown()) { return SMGUnknownValue.getInstance(); }

        long rVal = rValue.getAsLong();
        long lVal = lValue.getAsLong();

        boolean result;
        switch (binaryOperator) {
        case EQUALS:
          result = lVal == rVal;
          break;
        case NOT_EQUALS:
          result = lVal != rVal;
          break;
        case GREATER_THAN:
          result = lVal > rVal;
          break;
        case GREATER_EQUAL:
          result = lVal >= rVal;
          break;
        case LESS_THAN:
          result = lVal < rVal;
          break;
        case LESS_EQUAL:
          result = lVal <= rVal;
          break;

        default:
          throw new AssertionError();
        }

        // return 1 if expression holds, 0 otherwise
        return (result ? SMGKnownExpValue.ONE : SMGKnownExpValue.ZERO);
      }

      case MODULO:
      case SHIFT_RIGHT:
      default:
        // TODO check which cases can be handled
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGExplicitValue visit(CIdExpression idExpression) throws UnrecognizedCCodeException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {
        return SMGKnownExpValue.valueOf(((CEnumerator) decl).getValue());
      }

      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CUnaryExpression unaryExpression) throws CPATransferException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();


      SMGExplicitValue value = null;

      switch (unaryOperator) {
      case MINUS:
        value = unaryOperand.accept(this);
        return (value.isUnknown()) ? SMGUnknownValue.getInstance() : value.negate();

      case NOT:
        value = unaryOperand.accept(this);

        if (value.isUnknown()) {
          return SMGUnknownValue.getInstance();
        } else {
          return (value.equals(SMGKnownExpValue.ZERO)) ? SMGKnownExpValue.ONE : SMGKnownExpValue.ZERO;
        }

      case AMPER:
        // valid expression, but we don't have explicit values for addresses.
        return SMGUnknownValue.getInstance();

      case SIZEOF:

        int size = getSizeof(cfaEdge, getRealExpressionType(unaryOperand));
        return SMGKnownExpValue.valueOf(size);
      case TILDE:
      default:
        // TODO handle unimplemented operators
        return SMGUnknownValue.getInstance();
      }
    }

    @Override
    public SMGExplicitValue visit(CPointerExpression pointerExpression) throws CPATransferException {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {
      // TODO Check if correct
      return SMGKnownExpValue.valueOf(exp.getValue());
    }

    @Override
    public SMGExplicitValue visit(CFieldReference exp) throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }

    @Override
    public SMGExplicitValue visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        return SMGKnownExpValue.valueOf(getSizeof(cfaEdge, type));
      default:
        return SMGUnknownValue.getInstance();
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public SMGExplicitValue visit(CCastExpression pE) throws CPATransferException {
      return pE.getOperand().accept(this);
    }

    @Override
    public SMGExplicitValue visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return SMGUnknownValue.getInstance();
    }
  }

  @Override
  public Collection<SMGState> strengthen(AbstractState element, List<AbstractState> elements,
      CFAEdge cfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  private String getScopedVariableName(String variableName, String functionName, boolean global) {

    if (global) { return variableName; }

    return functionName + "::" + variableName;
  }

  @SuppressWarnings("unused")
  private Collection<SMGState> strengthen(SMGState smgState, ExplicitState explState, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    Collection<SMGState> sharpenedStates = null;
/*
    if (searchForExplicitValue && !symbolicValue.isUnknown() && lParam != null) {
      sharpenedStates = searchForExplicitValue(smgState, explState, cfaEdge, symbolicValue, lParam);
    }

    searchForExplicitValue = false;
    symbolicValue = SMGUnknownValue.getInstance();
    lParam = null;
*/
    return sharpenedStates;
  }

  @SuppressWarnings("unused")
  private Collection<SMGState> searchForExplicitValue(SMGState smgState, ExplicitState explState, CFAEdge cfaEdge,
      SMGSymbolicValue symbolicValue, String lParam) throws UnrecognizedCCodeException {

    @SuppressWarnings("unused")
    SMGState sharpenedState = new SMGState(smgState);

    if (symbolicValue.isUnknown()) {
      return null;
    }

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // added to avoid compile error
    boolean lParamIsGlobal = false;
    String scopedVariableName = getScopedVariableName(lParam, functionName, lParamIsGlobal);

    @SuppressWarnings("unused")
    Long explicitValue = null;

    if (explState.contains(scopedVariableName)) {
      explicitValue = explState.getValueFor(scopedVariableName);
    }

    return Collections.singleton(sharpenedState);
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
  }
}