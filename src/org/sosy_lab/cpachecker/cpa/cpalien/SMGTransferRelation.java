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
import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
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
   * Determines, whether to search for a explicit Value for a symbolic one.
   */
  private boolean searchForExplicitValue = false;

  /**
   * Search for the explicit Value of this symbolic Value
   */
  private Integer symbolicValue = null;

  /**
   * Contains the LValue String to be read from an explicit State
   */
  private String lParam = null;

  /**
   * Determines whether the lValue to be read vom explicit State  is global.
   */
  private boolean lParamIsGlobal = false;


  private final class SMGBuiltins {

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
          if (currentState.getPredecessor() == null){
            name = String.format("initial-%03d", currentState.getId());
          } else {
            name = String.format("%03d-%03d", currentState.getPredecessor().getId(), currentState.getId());
          }
        }
        File outputFile = new File(String.format(exportSMGFilePattern.getAbsolutePath(), name));
        try {
          Files.writeFile(outputFile, currentState.toDot(name, location));
        } catch (IOException e) {
          logger.logUserException(Level.WARNING, e, "Could not write SMG " + name + " to file");
        }
      }
    }

    public void evaluateVBPlot(CFunctionCallExpression functionCall, SMGState currentState) {
      String name = ((CStringLiteralExpression) functionCall.getParameterExpressions().get(0)).getContentString();
      this.dumpSMGPlot(name, currentState, functionCall.toString());
    }

    public Address evaluateMalloc(CFunctionCallExpression functionCall, SMGState currentState, CFAEdge cfaEdge)
        throws UnrecognizedCCodeException {
      CRightHandSide sizeExpr;

      try {
       sizeExpr = functionCall.getParameterExpressions().get(MALLOC_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Malloc argument not found." , cfaEdge, functionCall);
      }

      Integer value = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

      if (value == null) {
        throw new UnrecognizedCCodeException("Not able to compute allocation size", cfaEdge);
      }

      String allocation_label = "malloc_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(value.intValue(), allocation_label);

      possibleMallocFail = true;
      return new Address(new_pointer.getValue(), new_pointer.getObject(), 0);
    }

    public Address evaluateMemset(CFunctionCallExpression functionCall,
        SMGState currentState, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

      //evaluate function: void *memset( void *buffer, int ch, size_t count );

      CExpression bufferExpr;
      CExpression chExpr;
      CExpression countExpr;

      try {
        bufferExpr = functionCall.getParameterExpressions().get(MEMSET_BUFFER_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Memset buffer argument not found.", cfaEdge, functionCall);
      }

      try {
        chExpr = functionCall.getParameterExpressions().get(MEMSET_CHAR_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Memset ch argument not found.", cfaEdge, functionCall);
      }

      try {
        countExpr = functionCall.getParameterExpressions().get(MEMSET_COUNT_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Memset count argument not found.", cfaEdge, functionCall);
      }

      Address bufferAddress = evaluateAddress(currentState, cfaEdge, bufferExpr);

      Integer count = evaluateExplicitValue(currentState, cfaEdge, countExpr);

      if (bufferAddress == null || count == null) {
        return null;
      }

      Integer bufferOffset = bufferAddress.getOffset();
      SMGObject memory = bufferAddress.getObject();

      //TODO create Valid unsigned Char type mock Type?
      CType type = new CSimpleType(false, false, CBasicType.CHAR, false, false, false, true, false, false, false);

      //TODO More effective memset evaluation
      for (int c = 0; c < count; c++) {
        handleAssignmentToField(currentState, cfaEdge, memory, bufferOffset, type, chExpr);
      }

      return bufferAddress;
    }

    public Address evaluateCalloc(CFunctionCallExpression functionCall,
        SMGState currentState, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

      CExpression numExpr;
      CExpression sizeExpr;

      try {
        numExpr = functionCall.getParameterExpressions().get(CALLOC_NUM_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Calloc num argument not found.", cfaEdge, functionCall);
      }

      try {
        sizeExpr = functionCall.getParameterExpressions().get(CALLOC_SIZE_PARAMETER);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Calloc size argument not found.", cfaEdge, functionCall);
      }

      Integer num = evaluateExplicitValue(currentState, cfaEdge, numExpr);
      Integer size = evaluateExplicitValue(currentState, cfaEdge, sizeExpr);

      if (num == null || size == null) {
        return null;
      }

      String allocation_label = "Calloc_ID" + SMGValueFactory.getNewValue() + "_Line:" + functionCall.getFileLocation().getStartingLineNumber();
      SMGEdgePointsTo new_pointer = currentState.addNewHeapAllocation(num.intValue() * size.intValue(), allocation_label);

      possibleMallocFail = true;
      return new Address(new_pointer.getValue(), new_pointer.getObject(), 0);
    }

    public void evaluateFree(CFunctionCallExpression pFunctionCall, SMGState currentState,
        CFAEdge cfaEdge) throws UnrecognizedCCodeException {
      CExpression pointerExp;

      try {
        pointerExp = pFunctionCall.getParameterExpressions().get(0);
      } catch (IndexOutOfBoundsException e) {
        throw new UnrecognizedCCodeException("Bulit in function free has no parameter", cfaEdge, pFunctionCall);
      }

      Address address = evaluateAddress(currentState, cfaEdge, pointerExp);

      if (address == null) {
        currentState.setInvalidFree();
      } else if (address.getValue() == 0) {
        logger.log(Level.WARNING, "The argument of a free invocation: "
            + cfaEdge.getRawStatement() + ", in Line "
            + pFunctionCall.getFileLocation().getStartingLineNumber() + " is 0");

      } else {
        currentState.free(address.getValue(), address.getOffset(), address.getObject());
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
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      successor = handleExitFromFunction(smgState, returnEdge);
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
      CReturnStatementEdge returnEdge) throws UnrecognizedCCodeException {

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

  private SMGState handleFunctionReturn(SMGState smgState,
      CFunctionReturnEdge functionReturnEdge) throws UnrecognizedCCodeException {

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

      CType rValueType = getRealExpressionType(((CFunctionCallAssignmentStatement) exprOnSummary).getRightHandSide());

      Integer rValue = getFunctionReturnValue(newState, rValueType);

      SMGObject memoryOfField = null;
      Integer offset = null;

      LValueAssignmentVisitor visitor =
          new LValueAssignmentVisitor(functionReturnEdge, tmpState);

      memoryOfField = lValue.accept(visitor);
      offset = visitor.offset;

      if (memoryOfField != null && offset != null && rValueType != null && rValue != null) {
        // Assignment of explicit Value to symbolic Value already happened in handleExitFromFunction
        newState.writeValue(memoryOfField, offset, rValueType, rValue);
      }
    }

    return newState;
  }

  private Integer getFunctionReturnValue(SMGState smgState, CType type) {

    SMGObject tmpMemory = smgState.getFunctionReturnObject();

    return  smgState.readValue(tmpMemory, 0, type);
  }

  private SMGState handleFunctionCall(SMGState smgState, CFunctionCallEdge callEdge)
      throws UnrecognizedCCodeException, SMGInconsistentException {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();

    logger.log(Level.FINEST, "Handling function call: " + functionEntryNode.getFunctionName());

    SMGState newState = new SMGState(smgState);

    CFunctionDeclaration functionDeclaration = functionEntryNode.getFunctionDefinition();
    newState.addStackFrame(functionDeclaration);

    //TODO Refactor

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
      Integer value = evaluateExpressionValue(newState, callEdge, exp, smgState);

      if (value == null) {
        value = SMGValueFactory.getNewValue();
      }

      newState.writeValue(newObject, 0, cType, value);
    }

    //TODO  take missing explicit values into Consideration
    return newState;
  }

  private SMGState handleAssumption(SMGState smgState, CExpression expression, CFAEdge cfaEdge,
      boolean truthValue) throws UnrecognizedCCodeException {

    // convert an expression like [a + 753 != 951] to [a != 951 - 753]
    expression = optimizeAssumeForEvaluation(expression);

    // get the value of the expression (either true[-1], false[0], or unknown[null])
    Integer value = evaluateAssumptionValue(smgState, cfaEdge, expression);

    if (value == null) {
      //TODO derive further information of value (in essence, implement smg.replace(symbValue, symbValue for y == x
      // and addNeq for y != x))

      return smgState;
    } else if ((truthValue && value == -1) || (!truthValue && value == 0)) {
      return smgState;
    } else {
      // This signals that there are no new States reachable from this State i. e. the
      // Assumption does not hold.
      return null;
    }
  }

  private SMGState handleStatement(SMGState pState, CStatementEdge pCfaEdge) throws UnrecognizedCCodeException {
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
      CRightHandSide rValue) throws UnrecognizedCCodeException {

    SMGState newState;
    logger.log(Level.FINEST, "Handling assignment:", lValue.toASTString(), "=", rValue.toASTString());

    LValueAssignmentVisitor visitor = new LValueAssignmentVisitor(cfaEdge, state);

    SMGObject memoryOfField = lValue.accept(visitor);
    Integer fieldOffset = visitor.offset;
    CType fieldType = getRealExpressionType(rValue);

    if (memoryOfField == null || fieldOffset == null || fieldType == null) {
      return new SMGState(state);
    }

    newState = handleAssignmentToField(state, cfaEdge, memoryOfField,
                                       fieldOffset, fieldType, rValue);

    return newState;
  }

  private class LValueAssignmentVisitor extends DefaultCExpressionVisitor<SMGObject, UnrecognizedCCodeException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    private Integer offset;

    public LValueAssignmentVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGObject visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public SMGObject visit(CIdExpression variableName) throws UnrecognizedCCodeException {

      // a = ...
      return handleVariableAssignment(variableName);
    }

    private SMGObject handleVariableAssignment(CIdExpression variableName) throws UnrecognizedCCodeException {
      logger.log(Level.FINEST, ">>> Handling statement: variable assignment");

      offset = 0;

      lParam = variableName.toASTString();
      lParamIsGlobal = smgState.isGlobal(variableName.getName());

      return smgState.getObjectForVisibleVariable(variableName.getName());
    }

    @Override
    public SMGObject visit(CUnaryExpression lValue) throws UnrecognizedCCodeException {

      if (lValue.getOperator() == CUnaryExpression.UnaryOperator.STAR) {
        // *a = ...
        return handlePointerAssignment(lValue);
      }

      throw new UnrecognizedCCodeException(lValue.toASTString() + " is not an lValue", cfaEdge, lValue);
    }

    private SMGObject handlePointerAssignment(CUnaryExpression lValue) throws UnrecognizedCCodeException {
      logger.log(Level.FINEST, ">>> Handling statement: assignment to dereferenced pointer");

      CExpression addressExpression = lValue.getOperand();

      Address address = evaluateAddress(smgState, cfaEdge, addressExpression);

      if (address == null) {
        return null;
      }

      offset = address.getOffset();

      return address.getObject();
    }

    @Override
    public SMGObject visit(CFieldReference lValue) throws UnrecognizedCCodeException {
      // a->b = ...
      return handleAssignmentToFieldReference(lValue);
    }

    private SMGObject handleAssignmentToFieldReference(CFieldReference fieldReference)
        throws UnrecognizedCCodeException {
      logger.log(Level.FINEST, ">>> Handling statement: assignment to field reference");

      CType ownerType = getRealExpressionType(fieldReference.getFieldOwner());

      Field field = getField(ownerType, fieldReference.getFieldName());

      offset = field.getOffset();

      return getMemoryOfField(smgState, cfaEdge, fieldReference);
    }

    @Override
    public SMGObject visit(CArraySubscriptExpression lValue) throws UnrecognizedCCodeException {

      // a[i] = ...
      return handleArrayAssignment(lValue);
    }

    private SMGObject handleArrayAssignment(CArraySubscriptExpression lValue) throws UnrecognizedCCodeException {
      logger.log(Level.FINEST, ">>> Handling statement: assignment to array Cell");

      Pair<SMGObject, Integer> memoryAndOffset = evaluateArraySubscriptExpression(smgState, cfaEdge, lValue);

      if (memoryAndOffset == null) {
        return null;
      }

      offset = memoryAndOffset.getSecond();

      return memoryAndOffset.getFirst();
    }
  }

  private Pair<SMGObject, Integer> evaluateArraySubscriptExpression(SMGState smgState, CFAEdge cfaEdge,
      CArraySubscriptExpression exp) throws UnrecognizedCCodeException {

    Pair<SMGObject, Integer> arrayMemoryAndOffset =
        evaluateArrayExpression(smgState, cfaEdge, exp.getArrayExpression());

    if (arrayMemoryAndOffset == null) {
      return null;
    }

    Integer offset = arrayMemoryAndOffset.getSecond();

    Integer subscriptValue = evaluateExplicitValue(smgState, cfaEdge, exp.getSubscriptExpression());

    if (subscriptValue == null) {
      return null;
    }

    offset = offset + subscriptValue * machineModel.getSizeof(exp.getExpressionType());

    return Pair.of(arrayMemoryAndOffset.getFirst(), offset);
  }

  private Pair<SMGObject, Integer> evaluateArrayExpression(SMGState smgState, CFAEdge cfaEdge,
      CExpression arrayExpression) throws UnrecognizedCCodeException {

    CType arrayExpressionType = getRealExpressionType(arrayExpression);

    SMGObject memoryOfArray = null;

    Integer offset = null;

    if (arrayExpressionType instanceof CPointerType) {

      Address address = evaluateAddress(smgState, cfaEdge, arrayExpression);

      if (address == null) {
        return null;
      }

      memoryOfArray = address.getObject();

      offset = address.getOffset();

    } else if (arrayExpressionType instanceof CArrayType) {

      ArrayMemoryVisitor visitor = new ArrayMemoryVisitor(cfaEdge, smgState);

      memoryOfArray = arrayExpression.accept(visitor);

      offset = visitor.offset;

    }

    if (memoryOfArray == null || offset == null) {
      return null;
    }

    return Pair.of(memoryOfArray, offset);
  }

  private SMGObject getMemoryOfField(SMGState smgState, CFAEdge cfaEdge, CFieldReference fieldReference) {

    CExpression fieldOwner = fieldReference.getFieldOwner();

    CType ownerType = getRealExpressionType(fieldOwner);

    SMGObject memoryOfFieldOwner = null;


    if (fieldOwner instanceof CIdExpression) {
      memoryOfFieldOwner = smgState.getObjectForVisibleVariable(((CIdExpression) fieldOwner).getName());

    } else if (fieldOwner instanceof CFieldReference) {
      memoryOfFieldOwner = getMemoryOfField(smgState, cfaEdge, (CFieldReference) fieldOwner);
    }

    //TODO Refactor

    if (fieldReference.isPointerDereference()) {

      Integer address = null;

      if (fieldOwner instanceof CIdExpression) {

        address = smgState.readValue(memoryOfFieldOwner, 0, ownerType);

      } else if (fieldOwner instanceof CFieldReference) {

        CFieldReference ownerFieldReference = (CFieldReference) fieldOwner;
        CType ownerOfOwenrType = getRealExpressionType(ownerFieldReference.getFieldOwner());
        String fieldName = ownerFieldReference.getFieldName();

        Field field = getField(ownerOfOwenrType, fieldName);

        address = smgState.readValue(memoryOfFieldOwner, field.getOffset(), field.getType());

      }

      if (memoryOfFieldOwner == null || address == null) { return null; }

      return smgState.getMemoryOfAddress(address);

    } else {
      return memoryOfFieldOwner;
    }
  }

  private Field getField(CType ownerType, String fieldName) {

    if (ownerType instanceof CElaboratedType) {
      return getField(((CElaboratedType) ownerType).getRealType(), fieldName);
    } else if (ownerType instanceof CCompositeType) {
      return getField((CCompositeType)ownerType, fieldName);
    } else if (ownerType instanceof CPointerType) {
      return getField(((CPointerType) ownerType).getType(), fieldName);
    }

    return null;
  }

  private Field getField(CCompositeType ownerType, String fieldName) {

    List<CCompositeTypeMemberDeclaration> membersOfType =  ownerType.getMembers();

    int offset = 0;

    for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
      String memberName = typeMember.getName();
      if (memberName.equals(fieldName)) {
        return new Field(offset, getRealExpressionType(typeMember.getType()));
      }
      offset = offset + machineModel.getSizeof(getRealExpressionType(typeMember.getType()));
    }

    return null;
  }

  private SMGState handleAssignmentToField(SMGState state, CFAEdge cfaEdge,
      SMGObject memoryOfField, int fieldOffset, CType fieldType, CRightHandSide rValue)
      throws UnrecognizedCCodeException {

    SMGState newState = new SMGState(state);

    if (memoryOfField.getSizeInBytes() < machineModel.getSizeof(fieldType)) {
      logger.log(Level.WARNING, "Attempting to write "  +  machineModel.getSizeof(fieldType) +
          " bytes into a field with size " + memoryOfField.getSizeInBytes() + "bytes.\n" +
              "Line " + cfaEdge.getLineNumber() + ": " + cfaEdge.getRawStatement());
    }

    Integer value = evaluateExpressionValue(newState, cfaEdge, rValue);

    if (value == null) {

      value = SMGValueFactory.getNewValue();

      if (newValueIsNeqZero(newState, cfaEdge, rValue)) {
        newState.setUnequal(value, 0);
      }
    }


    // If Assignment contained malloc, handle possible fail with
    // alternate State
    if (possibleMallocFail) {
      possibleMallocFail = false;
      SMGState otherState = new SMGState(state);
      otherState.writeValue(memoryOfField, fieldOffset, fieldType, 0);
      mallocFailState = otherState;
    }

    if (!newState.isExplicitValueKnown(value)) {
      // If the lParam String of the assignment is known,
      // search for the explicit Value
      if (lParam != null) {
        searchForExplicitValue = true;
        symbolicValue = value;
      }
    }

    newState.writeValue(memoryOfField, fieldOffset, fieldType, value);

    return newState;
  }

  private Integer evaluateExplicitValue(SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws UnrecognizedCCodeException {

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(smgState, cfaEdge);

    BigInteger value = rValue.accept(visitor);

    //TODO Warn if value bigger than int Value.
    return value != null ? value.intValue() : null;
  }


  private boolean newValueIsNeqZero(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue) throws UnrecognizedCCodeException {

    return rValue.accept(new IsNotZeroVisitor(newState, cfaEdge));
  }

  private Integer evaluateExpressionValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws UnrecognizedCCodeException {
    return evaluateExpressionValue(newState, cfaEdge, rValue, newState);
  }

  private Integer evaluateExpressionValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue, SMGState smgState)
      throws UnrecognizedCCodeException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType) {

      Address address = evaluateAddress(smgState, cfaEdge, rValue);

      if (address == null) {
        return null;
      }

      // This method may only be called, if the result,
      // if there is a result, is written into the SMG.
      // Otherwise, we would add an Address to the SMG
      // without a necessary Has-Value-Edge to the address value.

      if (!newState.containsValue(address.getValue())) {
        newState.addAddress(address.getObject(), address.getOffset(), address.getValue());
      }

      return address.getValue();

    } else {
      return evaluateNonAddressValue(smgState, cfaEdge, rValue);
    }
  }

  private Integer evaluateNonAddressValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws UnrecognizedCCodeException {

    ExpressionValueVisitor visitor = new ExpressionValueVisitor(cfaEdge, newState);

    Integer symbolicValue = rValue.accept(visitor);

    if (!newState.isExplicitValueKnown(symbolicValue)) {
      Integer explicitValue = evaluateExplicitValue(newState, cfaEdge, rValue);

      if (explicitValue != null) {

        if (symbolicValue == null) {

          if (newState.isSymbolicValueKnown(explicitValue)) {
            symbolicValue = newState.getSymbolicValue(explicitValue);
          } else {
            symbolicValue = SMGValueFactory.getNewValue();
          }
        }

        newState.assignExplicitValue(symbolicValue, explicitValue);
      }
    }

    return symbolicValue;
  }

  private Integer evaluateAssumptionValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws UnrecognizedCCodeException {

    ExpressionValueVisitor visitor = new AssumeVisitor(cfaEdge, newState);
    return rValue.accept(visitor);
  }

  private Address evaluateAddress(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue) throws UnrecognizedCCodeException {

    PointerAddressVisitor visitor = new PointerAddressVisitor(cfaEdge, newState);
    Integer address = rValue.accept(visitor);

    if (address == null || visitor.offset == null || visitor.object == null) {
      return null;
    }

    return new Address(address, visitor.object, visitor.offset);
  }

  private SMGState handleDeclaration(SMGState smgState, CDeclarationEdge edge) throws UnrecognizedCCodeException, SMGInconsistentException {
    logger.log(Level.FINEST, ">>> Handling declaration");
    SMGState newState = new SMGState(smgState);

    CDeclaration cDecl = edge.getDeclaration();

    if (cDecl instanceof CVariableDeclaration) {
      CVariableDeclaration cVarDecl = (CVariableDeclaration) cDecl;
      logger.log(Level.FINEST, "Handling variable declaration:", cVarDecl.toASTString());
      String varName = cVarDecl.getName();
      CType cType = getRealExpressionType(cVarDecl);

      CInitializer newInitializer = cVarDecl.getInitializer();
      SMGObject newObject;

      if (cVarDecl.isGlobal()) {
        newObject = smgState.createObject(machineModel.getSizeof(cType), varName);
        logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to global objects");
        newState.addGlobalObject(newObject);

        if (newInitializer == null) {
          // global variables without initializer are set to 0 in C
          newState.writeValue(newObject, 0, cType, 0);
        }

      } else {
        newObject = newState.addLocalVariable(cType, varName);
        logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to current stack");
      }

      if (newInitializer != null) {
        logger.log(Level.FINEST, "Handling variable declaration: handling initializer");

        if (newInitializer instanceof CInitializerExpression) {
         newState = handleAssignmentToField(newState, edge, newObject, 0, cType,
              ((CInitializerExpression) newInitializer).getExpression());
         lParam = cVarDecl.toASTString();
         lParamIsGlobal = cVarDecl.isGlobal();
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
      type = ((CTypedefType)type).getRealType();
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
      CBinaryExpression binaryExpression = (CBinaryExpression)expression;

      BinaryOperator operator = binaryExpression.getOperator();
      CExpression leftOperand = binaryExpression.getOperand1();
      CExpression riteOperand = binaryExpression.getOperand2();


      if (operator == BinaryOperator.EQUALS || operator == BinaryOperator.NOT_EQUALS) {
        if (leftOperand instanceof CBinaryExpression && riteOperand instanceof CLiteralExpression) {
          CBinaryExpression expr = (CBinaryExpression)leftOperand;

          BinaryOperator operation = expr.getOperator();
          CExpression leftAddend = expr.getOperand1();
          CExpression riteAddend = expr.getOperand2();

          // [(a + 753) != 951] => [a != 951 + 753]

          if (riteAddend instanceof CLiteralExpression && (operation == BinaryOperator.PLUS || operation == BinaryOperator.MINUS)) {
            BinaryOperator newOperation = (operation == BinaryOperator.PLUS) ? BinaryOperator.MINUS : BinaryOperator.PLUS;

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
      return !exp.getValue().equals(BigInteger.ZERO) ;
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

  private class PointerAddressVisitor extends ExpressionValueVisitor
      implements CRightHandSideVisitor<Integer, UnrecognizedCCodeException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    // memory and offset this address points to.
    Integer offset = 0;
    SMGObject object = null;

    public PointerAddressVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
      cfaEdge = super.getCfaEdge();
      smgState = super.getSmgState();
    }

    @Override
    protected Integer visitDefault(CExpression exp) {
      return null;
    }

    @Override
    public Integer visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      Integer value = super.visit(exp);

      if (value != null && value == 0) {
        object = smgState.getMemoryOfAddress(0);
        offset = 0;
      }

      return value;
    }

    @Override
    public Integer visit(CIdExpression idExpression) throws UnrecognizedCCodeException {

      Integer address = super.visit(idExpression);

      offset = smgState.getOffset(address);
      object = smgState.getMemoryOfAddress(address);

      return address;
    }

    @Override
    public Integer visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();
      CType unaryOperandType = getRealExpressionType(unaryOperand);
      CType expType = getRealExpressionType(unaryExpression);

      switch (unaryOperator) {

      case AMPER:
        return handleAmper(unaryOperand);

      case STAR:
        if (unaryOperandType instanceof CPointerType) {

           Integer address = dereferencePointer(unaryOperand, expType);
           offset = smgState.getOffset(address);
           object = smgState.getMemoryOfAddress(address);
           return address;
        } else if (unaryOperandType instanceof CArrayType) {

          Integer address = dereferenceArray(unaryOperand, expType);
          offset = smgState.getOffset(address);
          object = smgState.getMemoryOfAddress(address);
          return address;

        } else {
          throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
              + unaryOperand.toASTString()
              + " as pointer type", cfaEdge, unaryExpression);
        }
      case SIZEOF:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of "
            + unaryOperand.toASTString()
            + " as pointer type", cfaEdge, unaryExpression);

      case MINUS:
      case NOT:
      case TILDE:
      default:
        // Can't evaluate these Addresses
        return null;
      }
    }

    private Integer handleAmper(CExpression lValue) throws UnrecognizedCCodeException {
      if (lValue instanceof CIdExpression) {
        // &a
        return createAddressOfVariable((CIdExpression) lValue);
      } else if (lValue instanceof CUnaryExpression
          && ((CUnaryExpression) lValue).getOperator() == UnaryOperator.STAR) {
        // &(*(a))
        return ((CUnaryExpression) lValue).getOperand().accept(this);
      } else if (lValue instanceof CFieldReference) {
        // &(a.b)
        return createAddressOfField((CFieldReference) lValue);
      } else {
        return null;
      }
    }

    private Integer createAddressOfField(CFieldReference lValue) {

      SMGObject memoryOfField = getMemoryOfField(smgState, cfaEdge, lValue);
      Field field = getField(getRealExpressionType(lValue.getFieldOwner()), lValue.getFieldName());

      offset = field.getOffset();
      object = memoryOfField;

      if (offset == null || object == null) {
        return null;
      }

      Integer address = smgState.getAddress(object, offset);

      if (address == null) {
        address = SMGValueFactory.getNewValue();
      }

      return address;
    }

    private Integer createAddressOfVariable(CIdExpression idExpression) {

      SMGObject variableObject = smgState.getObjectForVisibleVariable(idExpression.getName());

      if (variableObject == null) {
        return null;
      } else {

        Integer address = smgState.getAddress(variableObject, 0);

        if (address == null) {
          address = SMGValueFactory.getNewValue();
        }

        object = variableObject;
        offset = 0;

        return address;
      }
    }

    @Override
    public Integer visit(CBinaryExpression binaryExp) throws UnrecognizedCCodeException {

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
        return null; // If both or neither are Addresses,
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
        return null;
      }

      switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        Integer addressValue = address.accept(this);
        Integer pointerOffsetValue = pointerOffset.accept(new ExplicitValueVisitor(smgState, cfaEdge)).intValue();

        if (addressValue == null || pointerOffsetValue == null) {
          return null;
        }

        switch (binaryOperator) {
        case PLUS:
          offset = offset + pointerOffsetValue * machineModel.getSizeof(addressType);
          return SMGValueFactory.getNewValue();
        case MINUS:
          offset = offset - pointerOffsetValue * machineModel.getSizeof(addressType);
          return SMGValueFactory.getNewValue();
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
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + binaryExp + " as pointer type", cfaEdge, binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      default:
        return null;
      }
    }

    @Override
    public Integer visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {

      Integer address = super.visit(exp);

      offset = smgState.getOffset(address);
      object = smgState.getMemoryOfAddress(address);

      return address;
    }

    @Override
    public Integer visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
      String functionName = fileNameExpression.toASTString();
      Address address = null;

      if (builtins.isABuiltIn(functionName)) {
        switch (functionName) {
        case "malloc":
          possibleMallocFail = true;
          address = builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge);
          break;
        case "calloc":
          possibleMallocFail = true;
          address = builtins.evaluateCalloc(pIastFunctionCallExpression, smgState, cfaEdge);
          break;
        case "memset":
          address = builtins.evaluateMemset(pIastFunctionCallExpression, smgState, cfaEdge);
          break;
        }

        if (address == null) {
          throw new AssertionError();
        }

        offset = address.getOffset();
        object = address.getObject();
        return address.getValue();
      } else {
        return null;
      }
    }

    @Override
    public Integer visit(CCastExpression cast) throws UnrecognizedCCodeException {
      return cast.getOperand().accept(this);
    }

    @Override
    public Integer visit(CFieldReference fieldReference) throws UnrecognizedCCodeException {

      Integer address = super.visit(fieldReference);

      offset = smgState.getOffset(address);
      object = smgState.getMemoryOfAddress(address);

      return address;
    }
  }

  private class ArrayMemoryVisitor extends DefaultCExpressionVisitor<SMGObject, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<SMGObject, UnrecognizedCCodeException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    Integer offset = 0;

    public ArrayMemoryVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected SMGObject visitDefault(CExpression exp) {
      return null;
    }

    @Override
    public SMGObject visit(CIdExpression idExpression) throws UnrecognizedCCodeException {
      return smgState.getObjectForVisibleVariable(idExpression.getName());
    }

    @Override
    public SMGObject visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();
      CType unaryOperandType = getRealExpressionType(unaryOperand);

      boolean unaryOperandIsPointer = unaryOperandType instanceof CPointerType;

      switch (unaryOperator) {

      case AMPER:
        return null;

      case STAR:
        if (unaryOperandIsPointer) {
          PointerAddressVisitor visitor = new PointerAddressVisitor(cfaEdge, smgState);

          unaryOperand.accept(visitor);

          offset = visitor.offset;
          return visitor.object;
        } else {
          throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + unaryExpression + " as array type", cfaEdge, unaryExpression);
        }
      case SIZEOF:
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + unaryExpression + " as array type", cfaEdge, unaryExpression);
      case MINUS:
      case NOT:
      case TILDE:
      default:
        // Can't evaluate these ArrayExpressions
        return null;
      }
    }

    @Override
    public SMGObject visit(CBinaryExpression binExp) throws UnrecognizedCCodeException {

      BinaryOperator binaryOperator = binExp.getOperator();
      CExpression lVarInBinaryExp = binExp.getOperand1();
      CExpression rVarInBinaryExp = binExp.getOperand2();
      CType lVarInBinaryExpType = getRealExpressionType(lVarInBinaryExp);
      CType rVarInBinaryExpType = getRealExpressionType(rVarInBinaryExp);

      boolean lVarIsAddress = lVarInBinaryExpType instanceof CArrayType;
      boolean rVarIsAddress = rVarInBinaryExpType instanceof CArrayType;

      CExpression address = null;
      CExpression pointerOffset = null;
      CType addressType = null;

      if (lVarIsAddress == rVarIsAddress) {
        return null; // If both or neither are ArrayTyps,
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
        return null;
      }

      switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        SMGObject arrayMemory = address.accept(this);
        Integer expresionOffsetValue = pointerOffset.accept(new ExplicitValueVisitor(smgState, cfaEdge)).intValue();

        if (arrayMemory == null || expresionOffsetValue == null) { return null; }

        switch (binaryOperator) {
        case PLUS:
          offset = offset + expresionOffsetValue * machineModel.getSizeof(addressType);
          return arrayMemory;
        case MINUS:
          offset = offset - expresionOffsetValue * machineModel.getSizeof(addressType);
          return arrayMemory;
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
        throw new UnrecognizedCCodeException("Misinterpreted the expression type of " + binExp + " as array type", cfaEdge, binExp);
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      default:
        return null;
      }
    }

    @Override
    public SMGObject visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {

      SMGObject arrayMemory = exp.getArrayExpression().accept(this);

      offset =
          offset + exp.getSubscriptExpression().accept(new ExplicitValueVisitor(smgState, cfaEdge)).intValue()
              * machineModel.getSizeof(getRealExpressionType(exp));

      return arrayMemory;
    }

    @Override
    public SMGObject visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public SMGObject visit(CCastExpression cast) throws UnrecognizedCCodeException {
      return cast.getOperand().accept(this);
    }

    @Override
    public SMGObject visit(CFieldReference fieldReference) throws UnrecognizedCCodeException {

      SMGObject memoryOfField = getMemoryOfField(smgState, cfaEdge, fieldReference);
      Field field = getField(getRealExpressionType(fieldReference.getFieldOwner()), fieldReference.getFieldName());

      offset = field.getOffset();

      return memoryOfField;
    }
  }

  private class AssumeVisitor extends ExpressionValueVisitor {

    private final SMGState smgState;

    public AssumeVisitor(CFAEdge pEdge, SMGState pSmgState) {
      super(pEdge, pSmgState);
      smgState = getSmgState();
    }

    @Override
    public Integer visit(CBinaryExpression exp) throws UnrecognizedCCodeException {

      BinaryOperator binaryOperator = exp.getOperator();

      switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:

        CExpression lVarInBinaryExp = exp.getOperand1();
        CExpression rVarInBinaryExp = exp.getOperand2();

        Integer lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) { return null; }

        Integer rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) { return null; }

        int l = lVal;
        int r = rVal;

        boolean isZero;
        boolean isOne;

        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = l == r;
          isOne = smgState.isUnequal(l, r);
          break;
        case EQUALS:
          isOne = l == r;
          isZero = smgState.isUnequal(l, r);
          break;
        default:
          throw new AssertionError();
        }

        if (isZero) {
          // return 0 if the expression does not hold
          return 0;
        } else if (isOne) {
          // return a symbolic Value representing 1 if the expression does hold
          return -1;
        } else {
          // otherwise return null
          return null;
        }

      default:
        return super.visit(exp);
      }
    }
  }

  private class ExpressionValueVisitor extends DefaultCExpressionVisitor<Integer, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<Integer, UnrecognizedCCodeException> {

    private final CFAEdge cfaEdge;
    private final SMGState smgState;

    public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
      cfaEdge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected Integer visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public Integer visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {

      Pair<SMGObject, Integer> arrayMemAndOff = evaluateArraySubscriptExpression(smgState, cfaEdge, exp);

      if (arrayMemAndOff == null) {
        return null;
      }

      Integer value = smgState.readValue(arrayMemAndOff.getFirst(), arrayMemAndOff.getSecond(), getRealExpressionType(exp));

      return value;

    }

    @Override
    public Integer visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {

      BigInteger value = exp.getValue();

      if (smgState.isSymbolicValueKnown(value.intValue())) {
        return smgState.getSymbolicValue(value.intValue());
      }

      boolean isZero = value.equals(BigInteger.ZERO);

      return isZero ? 0 : null;
    }

    @Override
    public Integer visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {

     char value = exp.getCharacter();

     if (smgState.isSymbolicValueKnown(value)) {
       return smgState.getSymbolicValue(value);
     }

      return (value == 0) ? 0 : null;
    }

    @Override
    public Integer visit(CFieldReference fieldReference) throws UnrecognizedCCodeException {

      SMGObject memoryOfField = getMemoryOfField(smgState, cfaEdge, fieldReference);
      Field field = getField(getRealExpressionType(fieldReference.getFieldOwner()), fieldReference.getFieldName());
      return smgState.readValue(memoryOfField, field.getOffset(), field.getType());
    }

    @Override
    public Integer visit(CFloatLiteralExpression exp) throws UnrecognizedCCodeException {

      boolean isZero = exp.getValue().equals(BigDecimal.ZERO);
      return isZero ? 0 : null;
    }

    @Override
    public Integer visit(CIdExpression idExpression) throws UnrecognizedCCodeException {

      Integer value = null;

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {

        value = (int) ((CEnumerator) decl).getValue();
        return value == 0 ? 0 : null;

      } else if (decl instanceof CVariableDeclaration
          || decl instanceof CParameterDeclaration) {

        SMGObject variableObject = smgState.getObjectForVisibleVariable(idExpression.getName());

        if (variableObject == null) {
          return null;
        }

        value = smgState.readValue(variableObject, 0, getRealExpressionType(idExpression));
      }

      return value;
    }

    @Override
    public Integer visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();
      CType unaryOperandType = getRealExpressionType(unaryOperand);
      CType expType = getRealExpressionType(unaryExpression);

      switch (unaryOperator) {

      case AMPER:
        throw new UnrecognizedCCodeException("Can't use & of expression " + unaryOperand.toASTString(), cfaEdge, unaryExpression);
      case STAR:
        if (unaryOperandType instanceof CPointerType) {
          return dereferencePointer(unaryOperand, expType);
        } else if (unaryOperandType instanceof CArrayType) {
          return dereferenceArray(unaryOperand, expType);
        } else {
          throw new UnrecognizedCCodeException(cfaEdge, unaryExpression);
        }
      case MINUS:
        Integer value = unaryOperand.accept(this);
        return (value != null && value.equals(0)) ? 0 : null;

      case NOT:
      case TILDE:
      case SIZEOF:
        value = machineModel.getSizeof(getRealExpressionType(unaryOperand));
        return (value != null && value.equals(0)) ? 0 : null;
      default:
        return null;
      }
    }

    @Override
    public Integer visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        return  machineModel.getSizeof(type) == 0 ? 0 : null;
      default:
        return null;
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public Integer visit(CBinaryExpression exp) throws UnrecognizedCCodeException {

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
        Integer lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) { return null; }

        Integer rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) { return null; }

        boolean isZero;

        switch (binaryOperator) {
        case PLUS:
        case SHIFT_LEFT:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
          isZero = lVal == 0 && rVal == 0;
          return (isZero) ? 0 : null;

        case MINUS:
        case MODULO:
          isZero = (lVal == rVal);
          return (isZero) ? 0 : null;

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal == 0) { return null; }

          isZero = lVal == 0;
          return (isZero) ? 0 : null;

        case MULTIPLY:
        case BINARY_AND:
          isZero = lVal == 0 || rVal == 0;
          return (isZero) ? 0 : null;

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

        Integer lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) { return null; }

        Integer rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) { return null; }

        int l = lVal;
        int r = rVal;

        boolean isZero;
        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = (l == r);
          break;
        case EQUALS:
          isZero = smgState.isUnequal(l, r);
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
          return 0;
        } else {
          return null;
        }
      }

      default:
        return null;
      }
    }

    @Override
    public Integer visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
      String functionName = fileNameExpression.toASTString();

      // If Calloc and Malloc have not been properly declared,
      // they may be shown to return an void pointer
      if (builtins.isABuiltIn(functionName)) {
        switch (functionName) {
        case "__VERIFIER_BUILTIN_PLOT":
          builtins.evaluateVBPlot(pIastFunctionCallExpression, smgState);
          break;
        case "malloc":
          possibleMallocFail = true;
          return builtins.evaluateMalloc(pIastFunctionCallExpression, smgState, cfaEdge).getValue();
        case "calloc":
          possibleMallocFail = true;
          return builtins.evaluateCalloc(pIastFunctionCallExpression, smgState, cfaEdge).getValue();
        }
      } else {
        return null;
      }

      return null;
    }

    @Override
    public Integer visit(CCastExpression cast) throws UnrecognizedCCodeException {
      return cast.getOperand().accept(this);
    }

    protected Integer dereferenceArray(CRightHandSide exp, CType derefType) throws UnrecognizedCCodeException {

      ArrayMemoryVisitor v = new ArrayMemoryVisitor(cfaEdge, smgState);

      SMGObject object = exp.accept(v);
      Integer offset = v.offset;

      if (object == null || offset == null) {
        // We can't resolve the field to dereference, therefore
        // we must assume, that it is invalid
        smgState.setUnkownDereference();
        return null;
      }

       Integer address = smgState.readValue(object, offset, derefType);

       return address;
     }

    protected Integer dereferencePointer(CRightHandSide exp, CType derefType) throws UnrecognizedCCodeException {

      Address address = evaluateAddress(smgState, cfaEdge, exp);

      if (address == null) {
        // We can't resolve the field to dereference , therefore
        // we must assume, that it is invalid
        smgState.setUnkownDereference();
        return null;
      }

      SMGObject object = address.getObject();
      Integer offset = address.getOffset();

      Integer value = smgState.readValue(object, offset, derefType);

      return value;
    }

    public SMGState getSmgState() {
      return smgState;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }

  private class ExplicitValueVisitor extends DefaultCExpressionVisitor<BigInteger, UnrecognizedCCodeException>
      implements CRightHandSideVisitor<BigInteger, UnrecognizedCCodeException> {

    private final SMGState smgState;
    private final CFAEdge  cfaEdge;

    public ExplicitValueVisitor(SMGState pSmgState, CFAEdge pCfaEdge) {
      smgState = pSmgState;
      cfaEdge = pCfaEdge;
    }

    @Override
    protected BigInteger visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public BigInteger visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {
      return exp.getValue();
    }

    @Override
    public BigInteger visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
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
        BigInteger lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) { return null; }

        BigInteger rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) { return null; }

        switch (binaryOperator) {
        case PLUS:
          return lVal.add(rVal);

        case MINUS:
          return lVal.subtract(rVal);

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal.equals(BigInteger.ZERO)) { return null; }

          return lVal.divide(rVal);

        case MULTIPLY:
          return lVal.multiply(rVal);

        case SHIFT_LEFT:
          return lVal.shiftLeft(rVal.intValue());

        case BINARY_AND:
          return lVal.and(rVal);

        case BINARY_OR:
          return lVal.or(rVal);

        case BINARY_XOR:
          return lVal.xor(rVal);

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

        BigInteger lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) { return null; }

        BigInteger rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) { return null; }

        boolean result;
        switch (binaryOperator) {
        case EQUALS:
          result = (lVal.equals(rVal));
          break;
        case NOT_EQUALS:
          result = !(lVal.equals(rVal));
          break;
        case GREATER_THAN:
          result = (lVal.intValue() > rVal.intValue());
          break;
        case GREATER_EQUAL:
          result = (lVal.intValue() >= rVal.intValue());
          break;
        case LESS_THAN:
          result = (lVal.intValue() < rVal.intValue());
          break;
        case LESS_EQUAL:
          result = (lVal.intValue() <= rVal.intValue());
          break;

        default:
          throw new AssertionError();
        }

        // return 1 if expression holds, 0 otherwise
        return (result ? BigInteger.ONE : BigInteger.ZERO);
      }

      case MODULO:
      case SHIFT_RIGHT:
      default:
        // TODO check which cases can be handled
        return null;
      }
    }

    @Override
    public BigInteger visit(CIdExpression idExpression) throws UnrecognizedCCodeException {

      CSimpleDeclaration decl = idExpression.getDeclaration();

      if (decl instanceof CEnumerator) {

        return BigInteger.valueOf(((CEnumerator) decl).getValue());

      } else if (decl instanceof CVariableDeclaration
          || decl instanceof CParameterDeclaration) {

        return getExplicitValueFromSymbolicValue(idExpression);
      }

      return null;
    }

    private BigInteger getExplicitValueFromSymbolicValue(CExpression exp) throws UnrecognizedCCodeException {

      CType expType = getRealExpressionType(exp);

      if (expType instanceof CPointerType || expType instanceof CArrayType) {
        // We do not have explicit Values for these.
        return null;
      }

      ExpressionValueVisitor visitor = new ExpressionValueVisitor(cfaEdge, smgState);

      Integer symbolicValue = exp.accept(visitor);

      if (smgState.isExplicitValueKnown(symbolicValue)) {
        return BigInteger.valueOf(smgState.getExplicitValue(symbolicValue));
      } else {
        return null;
      }
    }

    @Override
    public BigInteger visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();


      BigInteger value = null;

      switch (unaryOperator) {
      case MINUS:
        value = unaryOperand.accept(this);
        return (value != null) ? value.negate() : null;

      case NOT:
        value = unaryOperand.accept(this);

        if (value == null) {
          return null;
        } else {
          return (value.equals(BigInteger.ZERO)) ? BigInteger.ONE : BigInteger.ZERO;
        }

      case AMPER:
        // valid expression, but we don't have explicit values for addresses.
        return null;

      case STAR:
        // valid expression, but we don't have explicit values for symbolic Values.
        return getExplicitValueFromSymbolicValue(unaryExpression);

      case SIZEOF:
        return BigInteger.valueOf(machineModel.getSizeof(getRealExpressionType(unaryOperand)));
      case TILDE:
      default:
        // TODO handle unimplemented operators
        return null;
      }
    }

    @Override
    public BigInteger visit(CArraySubscriptExpression exp) throws UnrecognizedCCodeException {
      return getExplicitValueFromSymbolicValue(exp);
    }

    @Override
    public BigInteger visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {
      // TODO Check if correct
      return BigInteger.valueOf(exp.getValue());
    }

    @Override
    public BigInteger visit(CFieldReference exp) throws UnrecognizedCCodeException {
      return getExplicitValueFromSymbolicValue(exp);
    }

    @Override
    public BigInteger visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

      TypeIdOperator typeOperator = typeIdExp.getOperator();
      CType type = typeIdExp.getType();

      switch (typeOperator) {
      case SIZEOF:
        return BigInteger.valueOf(machineModel.getSizeof(type));
      default:
        return null;
        //TODO Investigate the other Operators.
      }
    }

    @Override
    public BigInteger visit(CCastExpression pE) throws UnrecognizedCCodeException {
      return pE.getOperand().accept(this);
    }

    @Override
    public BigInteger visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }
  }

  @Override
  public Collection<SMGState> strengthen(AbstractState element, List<AbstractState> elements,
      CFAEdge cfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {

    assert element instanceof SMGState;

    SMGState smgState = (SMGState) element;

    for (AbstractState ae : elements) {
      if (ae instanceof ExplicitState) {
        return strengthen(smgState, (ExplicitState) ae, cfaEdge);
      }
    }
    return null;
  }

  private String getScopedVariableName(String variableName, String functionName, boolean global) {

    if (global) {
      return variableName;
    }

    return functionName + "::" + variableName;
  }

  private Collection<SMGState> strengthen(SMGState smgState, ExplicitState explState, CFAEdge cfaEdge)
      throws UnrecognizedCCodeException {

    Collection<SMGState> sharpenedStates = null;

    if (searchForExplicitValue && symbolicValue != null && lParam != null) {
      sharpenedStates = searchForExplicitValue(smgState, explState, cfaEdge, symbolicValue, lParam);
    }

    searchForExplicitValue = false;
    symbolicValue = null;
    lParam = null;

    return sharpenedStates;
  }

  private Collection<SMGState> searchForExplicitValue(SMGState smgState, ExplicitState explState, CFAEdge cfaEdge,
      Integer symbolicValue, String lParam) throws UnrecognizedCCodeException {

    SMGState sharpenedState = new SMGState(smgState);

    if (smgState.isExplicitValueKnown(symbolicValue)) {
      return null;
    }

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    String scopedVariableName = getScopedVariableName(lParam, functionName, lParamIsGlobal);

    Long explicitValue = null;

    if (explState.contains(scopedVariableName)) {
      explicitValue = explState.getValueFor(scopedVariableName);
    }

    if (explicitValue == null) {
      return null;
    }

    sharpenedState.assignExplicitValue(symbolicValue, explicitValue.intValue());

    return Collections.singleton(sharpenedState);
  }

  /**
   * A class to represent a field. This class is mainly used
   * to store field Information.
   */
  private static class Field {

    /**
     * the offset of this field relative to the memory
     * this field belongs to.
     */
    private final Integer offset;

    /**
     * The type of this field, it determines its size
     * and the way information stored in this field is read.
     */
    private final CType type;

    public Field(Integer pOffset, CType pType) {
      offset = pOffset;
      type = pType;
    }

    public Integer getOffset() {
      return offset;
    }

    public CType getType() {
      return type;
    }
  }

  /**
   * A class to represent an Address. This class is mainly used
   * to store Address Information.
   */
  private static class Address {

    /**
     * The symbolic value representing this address.
     */
    private final Integer value;

    /**
     * The SMGObject representing the Memory this address belongs to.
     */
    private final SMGObject object;

    /**
     * The offset relative to the beginning of object in byte.
     */
    private final Integer offset;

    public Address(Integer pValue, SMGObject pObject, Integer pOffset) {
      value = pValue;
      object = pObject;
      offset = pOffset;
    }

    public Integer getValue() {
      return value;
    }

    public SMGObject getObject() {
      return object;
    }

    public Integer getOffset() {
      return offset;
    }
  }
}
