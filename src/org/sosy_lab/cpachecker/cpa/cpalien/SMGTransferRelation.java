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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class SMGTransferRelation implements TransferRelation {

  private LogManager logger;
  private MachineModel machineModel;

  public SMGTransferRelation(LogManager pLogger, MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision,
      CFAEdge pCfaEdge) throws CPATransferException, InterruptedException {
    // TODO Auto-generated method stub
    logger.log(Level.FINEST, "SMG GetSuccessor >>");
    logger.log(Level.FINEST, "Edge:" + pCfaEdge.getEdgeType());
    logger.log(Level.FINEST, "Code:" + pCfaEdge.getCode());

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

  private AbstractState handleStatement(SMGState pState, CStatementEdge pCfaEdge) {
    logger.log(Level.FINEST,  ">>> Handling statement");
    SMGState newState;

    CStatement cStmt = pCfaEdge.getStatement();

    if (cStmt instanceof CAssignment){
      CAssignment cAssignment = (CAssignment)cStmt;
      CExpression lValue = cAssignment.getLeftHandSide();
      CRightHandSide rValue = cAssignment.getRightHandSide();

      newState = handleAssignment(pState, pCfaEdge, lValue, rValue);
    }
    else
    {
      newState = new SMGState(pState);
    }

    newState.visualize("line-" + pCfaEdge.getLineNumber());

    return newState;
  }

  private SMGState handleAssignment(SMGState pState, CStatementEdge pCfaEdge, CExpression pLValue,
      CRightHandSide pRValue) {
    SMGState newState;
    logger.log(Level.FINEST, "Handling assignment: " + pLValue.toASTString() + " = " + pRValue.toASTString());

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
      CRightHandSide pRValue, CType pType) {
    SMGState newState = new SMGState(pState);

    SMGObject assigned = pState.getObjectForVariable(pVariableName);
    if (assigned.getSizeInBytes() < machineModel.getSizeof(pType)){
      //TODO: Warn about the attempted assignment, probably a result of invalid
      //      cast
    }

    Integer value = new Integer(4); //TODO: Implement a real value creation from the expression

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
      logger.log(Level.FINEST, "Handling variable declaration: " + cVarDecl.toASTString());
      String varName = cVarDecl.getName();
      CType cType = cVarDecl.getType();

      SMGObject newObject = new SMGObject(machineModel.getSizeof(cType) , varName);
      CInitializer newInitializer = cVarDecl.getInitializer();

      logger.log(Level.FINEST, "Handling variable declaration: adding '" + newObject + "' to current stack");
      newState.addStackObject( newObject );

      if (newInitializer != null){
        logger.log(Level.FINEST, "Handling variable declaration: handling initializer");
        //TODO: Handle initializers
      }
    }
    return newState;
  }

  @Override
  public Collection<SMGState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    // TODO Auto-generated method stub
    return null;
  }

}
