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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


@Options(prefix="cpa.cpalien")
public class SMGTransferRelation implements TransferRelation {

  private static int nextFreeId = 1;

  @Option(name="exportSMG.file", description="dump SMG for each edge")
  @FileOption(Type.OUTPUT_FILE)
  private File exportSMGFilePattern = new File("smg-%s.dot");

  final private LogManager logger;
  final private MachineModel machineModel;

  private static final Set<String> BUILTINS = new HashSet<>(Arrays.asList(
      new String[] {"__VERIFIER_BUILTIN_PLOT"}));

  public SMGTransferRelation(Configuration config, LogManager pLogger,
      MachineModel pMachineModel) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
    machineModel = pMachineModel;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
    logger.log(Level.FINEST, "SMG GetSuccessor >>");
    logger.log(Level.FINEST, "Edge:", pCfaEdge.getEdgeType());
    logger.log(Level.FINEST, "Code:", pCfaEdge.getCode());

    AbstractState successor;

    switch (pCfaEdge.getEdgeType()){
      case DeclarationEdge:
        successor = handleDeclaration((SMGState)pState, (CDeclarationEdge)pCfaEdge);
        break;
      case StatementEdge:
        successor = handleStatement((SMGState)pState, (CStatementEdge)pCfaEdge);
        break;
      default:
        successor = pState;
    }
    return Collections.singleton(successor);
  }

  private AbstractState handleStatement(SMGState pState, CStatementEdge pCfaEdge) throws UnrecognizedCCodeException {
    logger.log(Level.FINEST,  ">>> Handling statement");
    SMGState newState;

    CStatement cStmt = pCfaEdge.getStatement();

    if (cStmt instanceof CAssignment){
      CAssignment cAssignment = (CAssignment)cStmt;
      CExpression lValue = cAssignment.getLeftHandSide();
      CRightHandSide rValue = cAssignment.getRightHandSide();

      newState = handleAssignment(pState, pCfaEdge, lValue, rValue);
    }
    else if (cStmt instanceof CFunctionCallStatement){
      CFunctionCallStatement cFCall = (CFunctionCallStatement)cStmt;
      CFunctionCallExpression cFCExpression = cFCall.getFunctionCallExpression();
      CExpression fileNameExpression = cFCExpression.getFunctionNameExpression();
      String functionName = fileNameExpression.toASTString();

      if (isABuiltIn(functionName)){
        newState = handleBuiltin(pState, functionName, cFCExpression.getParameterExpressions());
      }
      else{
        newState = new SMGState(pState);
      }
    }
    else
    {
      newState = new SMGState(pState);
    }
//TODO: Emitting SMG plot on each step should be optional
//    if (exportSMGFilePattern != null) {
//      String name = "line-" + pCfaEdge.getLineNumber();
//      File outputFile = new File(String.format(exportSMGFilePattern.getAbsolutePath(), name));
//      try {
//        Files.writeFile(outputFile, newState.toDot(name));
//      } catch (IOException e){
//        logger.logUserException(Level.WARNING, e, "Could not write SMG " + name + " to file");
//      }
//    }

    return newState;
  }

  private SMGState handleBuiltin(SMGState pState, String pFunctionName, List<CExpression> pParameterExpressions) {
    SMGState newState;
    if (pFunctionName == "__VERIFIER_BUILTIN_PLOT"){
      if (exportSMGFilePattern != null) {
        String name = ((CStringLiteralExpression)pParameterExpressions.get(0)).getContentString();
        File outputFile = new File(String.format(exportSMGFilePattern.getAbsolutePath(), name));
        try {
          Files.writeFile(outputFile, pState.toDot(name));
        } catch (IOException e){
          logger.logUserException(Level.WARNING, e, "Could not write SMG " + name + " to file");
        }
      }
      newState = new SMGState(pState);
    }
    else {
      newState = new SMGState(pState);
    }

    return newState;
  }

  private boolean isABuiltIn(String pFunctionName) {
    return BUILTINS.contains(pFunctionName);
  }

  private SMGState handleAssignment(SMGState pState, CStatementEdge pCfaEdge, CExpression pLValue,
      CRightHandSide pRValue) throws UnrecognizedCCodeException {
    SMGState newState;
    logger.log(Level.FINEST, "Handling assignment:", pLValue.toASTString(), "=", pRValue.toASTString());

    if (pLValue instanceof CIdExpression){
      CIdExpression variableName = (CIdExpression)pLValue;
      newState = handleVariableAssignment(pState, pCfaEdge, variableName, pRValue, variableName.getExpressionType());
    }
    else{
      newState = new SMGState(pState);
    }
    return newState;
  }

  private SMGState handleVariableAssignment(SMGState pState, CStatementEdge pCfaEdge, CIdExpression pVariableName,
      CRightHandSide pRValue, CType pType) throws UnrecognizedCCodeException {
    SMGState newState = new SMGState(pState);

    SMGObject assigned = pState.getObjectForVariable(pVariableName);
    if (assigned.getSizeInBytes() < machineModel.getSizeof(pType)){
      //TODO: Warn about the attempted assignment, probably a result of invalid
      //      cast
    }

    CType expressionType = pRValue.getExpressionType();

    Integer value;

    if(expressionType instanceof CPointerType) {
       value = 4;
    } else {
      ExpressionValueVisitor visitor = new ExpressionValueVisitor(pCfaEdge, newState);
      value = pRValue.accept(visitor);

      if(value == null) {
        value = newState.nextFreeValue();
      }
    }



    newState.addValue(value);
    SMGEdgeHasValue newEdge = new SMGEdgeHasValue(pType, 0, assigned, value);
    newState.insertNewHasValueEdge(newEdge);

    return newState;
  }

  private AbstractState handleDeclaration(SMGState pState, CDeclarationEdge pEdge){
    logger.log(Level.FINEST, ">>> Handling declaration");
    SMGState newState = new SMGState(pState);

    CDeclaration cDecl = pEdge.getDeclaration();

    if (cDecl instanceof CVariableDeclaration){
      CVariableDeclaration cVarDecl = (CVariableDeclaration)cDecl;
      logger.log(Level.FINEST, "Handling variable declaration:", cVarDecl.toASTString());
      String varName = cVarDecl.getName();
      CType cType = cVarDecl.getType();

      SMGObject newObject = new SMGObject(machineModel.getSizeof(cType) , varName);
      CInitializer newInitializer = cVarDecl.getInitializer();

      logger.log(Level.FINEST, "Handling variable declaration: adding '", newObject, "' to current stack");
      newState.addStackObject( newObject );

      if (newInitializer != null){
        logger.log(Level.FINEST, "Handling variable declaration: handling initializer");
        //TODO: Handle initializers
      }
    }
    return newState;
  }

  private class ExpressionValueVisitor extends DefaultCExpressionVisitor<Integer, UnrecognizedCCodeException>
  implements CRightHandSideVisitor<Integer, UnrecognizedCCodeException>
  {

    private final CFAEdge edge;
    private final SMGState smgState;

    public ExpressionValueVisitor(CFAEdge pEdge, SMGState pSmgState) {
      edge = pEdge;
      smgState = pSmgState;
    }

    @Override
    protected Integer visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public Integer visit(CIntegerLiteralExpression exp) throws UnrecognizedCCodeException {

      boolean isZero = exp.getValue().equals(BigInteger.ZERO);
      return isZero ? 0 : null;
    }

    @Override
    public Integer visit(CCharLiteralExpression exp) throws UnrecognizedCCodeException {
      return (exp.getCharacter() == 0) ? 0 : null;
    }

    @Override
    public Integer visit(CFloatLiteralExpression exp) throws UnrecognizedCCodeException {

      boolean isZero = exp.getValue().equals(BigDecimal.ZERO);
      return isZero ? 0 : null;
    }

    @Override
    public Integer visit(CIdExpression idExpression) throws UnrecognizedCCodeException {

      SMGObject variableObject = smgState.getObjectForVariable(idExpression);

      if (variableObject == null) {
        return null;
      }

      Integer value = smgState.readValue(variableObject, 0, idExpression.getExpressionType());

      return value;
    }

    @Override
    public Integer visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case AMPER:
        //TODO Exception, can't Amper with a simple Type as result
      case STAR:
        // TODO New Visitor
      case MINUS:
        int value = unaryOperand.accept(this);
        return (value == 0) ? 0 : null;

      case NOT:
      case TILDE:
      case SIZEOF:
      default:
        return null;
      }
    }

    @Override
    public Integer visit(CBinaryExpression pE) throws UnrecognizedCCodeException {

      BinaryOperator binaryOperator = pE.getOperator();
      CExpression lVarInBinaryExp = pE.getOperand1();
      CExpression rVarInBinaryExp = pE.getOperand2();

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
          // TODO Actually, we need proof of inequality here
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

        long l = lVal;
        long r = rVal;


        boolean isZero;
        switch (binaryOperator) {
        case NOT_EQUALS:
          isZero = (l == r);
          break;
        case EQUALS:
        case GREATER_THAN:
        case GREATER_EQUAL:
        case LESS_THAN:
        case LESS_EQUAL:
          isZero = false;
          break;

        default:
          throw new AssertionError();
        }

        // return 0 if the expression does  hold, otherwise null
        return (isZero ? 0 : null);
      }

      default:
        return null;
      }
    }

    @Override
    public Integer visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

  }

  /**
   * Generates different IDs per Value
   *
   * @return id for value
   */
  public static int nextId() {
    nextFreeId++;
    return nextFreeId;
  }

  @Override
  public Collection<SMGState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    // TODO Auto-generated method stub
    return null;
  }
}