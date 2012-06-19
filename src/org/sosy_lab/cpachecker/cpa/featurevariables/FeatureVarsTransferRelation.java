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
package org.sosy_lab.cpachecker.cpa.featurevariables;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.MultiEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.predicates.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.base.Preconditions;

public class FeatureVarsTransferRelation implements TransferRelation {

  private final NamedRegionManager rmgr;

  public FeatureVarsTransferRelation(NamedRegionManager manager) {
    this.rmgr = manager;
  }

  /* (non-Javadoc)
   * This Transfer Relation tracks the variables defined by the precision operator.
   * It assumes that all such variables are boolean (only the case ==0 and the case !=0 are tracked).
   * Only assume edges (conditions of if-statements) and only the operators && and || are considered.
   * Statement edges can be handled if the tracked variables are assigned only once and if they are assigned constant values (e.g. ____SELECTED_FEATURE_Verify = 1;)
   * All other operators (e.g. ==, !=, &, |, ...) are ignored.
   * Only global variables (we don't make a difference between a variable that is defined globally and
   * a local variable with the same name)
   */
  @Override
  public Collection<FeatureVarsState> getAbstractSuccessors(
      AbstractState element, Precision pPrecision, CFAEdge cfaEdge)
      throws CPATransferException {
    Preconditions.checkArgument(pPrecision instanceof FeatureVarsPrecision, "precision is no FeatureVarsPrecision");
    FeatureVarsPrecision precision = (FeatureVarsPrecision) pPrecision;
    FeatureVarsState fvElement = (FeatureVarsState) element;
    if (fvElement.getRegion().isFalse()) {
      return Collections.emptyList();
    }

    //assert !fvElement.getRegion().isFalse();

    if (precision.isDisabled()) {
      // this means that no variables should be tracked (whitelist is empty)
      return Collections.singleton(fvElement);
    }

    FeatureVarsState successor;
    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor =
          handleAssumption(fvElement, assumeEdge.getExpression(), cfaEdge,
              assumeEdge.getTruthAssumption(), precision);
      break;
    }

    case MultiEdge: {
      successor = fvElement;
      for (CFAEdge innerEdge : (MultiEdge)cfaEdge) {
        successor = getAbstractSuccessor(successor, precision, innerEdge);
      }
    }

    default:
     successor = getAbstractSuccessor(fvElement, precision, cfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      assert !successor.getRegion().isFalse();
      return Collections.singleton(successor);
    }
  }

  private FeatureVarsState getAbstractSuccessor(
      FeatureVarsState fvElement, FeatureVarsPrecision precision, CFAEdge cfaEdge)
      throws CPATransferException {

    FeatureVarsState successor = fvElement;
    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {
    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      StatementEdge st = (StatementEdge) cfaEdge;
      successor = handleStatementEdge(fvElement, st.getStatement(), st, precision);
      break;
    }
    case ReturnStatementEdge: {
      break;
    }
      // edge is a declaration edge, e.g. int a;
    case DeclarationEdge: {
      break;
    }
    case BlankEdge: {
      break;
    }
    case FunctionCallEdge: {
      break;
    }
      // this is a return edge from function, this is different from return statement
      // of the function. See case for statement edge for details
    case FunctionReturnEdge: {
      break;
    }
    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
    assert successor != null;
    assert !successor.getRegion().isFalse();

    return successor;
  }

  private FeatureVarsState handleStatementEdge(FeatureVarsState element,
      CStatement pIastStatement, StatementEdge cfaEdge,
      FeatureVarsPrecision pPrecision) {

    if (!(pIastStatement instanceof CAssignment)) {
      return element;
    }
    CAssignment assignment = (CAssignment)pIastStatement;

    CExpression lhs = assignment.getLeftHandSide();
    FeatureVarsState result = element;
    if (lhs instanceof CIdExpression || lhs instanceof CFieldReference
        || lhs instanceof CArraySubscriptExpression) {
      String varName = lhs.toASTString();//this.getvarName(op.getRawSignature(), functionName);
      if (pPrecision.isOnWhitelist(varName)) {
        CRightHandSide rhs = assignment.getRightHandSide();
        if (rhs instanceof CIntegerLiteralExpression) {
          String value = rhs.toASTString();
          /*
           * This will only work with the first assignment to the variable!
           * If the variable gets a second assignment we would have to delete the current value from the bdd first.
           * I do not know how to do this yet.
           */

          if (value.trim().equals("0")) {
            Region operand = rmgr.makeNot(rmgr.createPredicate(varName));
            result = new FeatureVarsState(rmgr.makeAnd(element.getRegion(), operand), rmgr);
          } else {
            Region operand = rmgr.createPredicate(varName);
            result = new FeatureVarsState(rmgr.makeAnd(element.getRegion(), operand), rmgr);
          }
        }
      }
    }
    assert !result.getRegion().isFalse();
    return result;
  }

  private FeatureVarsState handleAssumption(FeatureVarsState element,
      CExpression expression, CFAEdge cfaEdge, boolean truthValue,
      FeatureVarsPrecision precision) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    FeatureVarsState result = handleBooleanExpression(element, expression, functionName, truthValue, precision, cfaEdge);
    if (result.getRegion().isFalse()) {
      return null; // assumption is not fulfilled / not possible
    } else {
      return result;
    }
  }

  private FeatureVarsState handleBooleanExpression(FeatureVarsState element,
      CExpression op, String functionName, boolean pTruthValue,
      FeatureVarsPrecision precision, CFAEdge edge) throws UnrecognizedCCodeException {
    Region operand = propagateBooleanExpression(element, op, functionName, precision, edge);
    if (operand == null) {
      return element;
    } else {
      Region newRegion = null;
      if (pTruthValue) {
        newRegion =
          rmgr.makeAnd(element.getRegion(), operand);
      } else {
        newRegion =
          rmgr.makeAnd(element.getRegion(),
                rmgr.makeNot(operand));
      }
      return new FeatureVarsState(newRegion, rmgr);
    }
  }

  private Region propagateBooleanExpression(FeatureVarsState element,
      CExpression op, String functionName, FeatureVarsPrecision precision, CFAEdge edge)
    throws UnrecognizedCCodeException {
    Region operand = null;
    if (op instanceof CIdExpression || op instanceof CFieldReference
        || op instanceof CArraySubscriptExpression) {
      String varName = op.toASTString();//this.getvarName(op.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand = rmgr.createPredicate(varName);
    } else if (op instanceof CUnaryExpression) {
      operand =
          propagateUnaryBooleanExpression(element, ((CUnaryExpression) op)
              .getOperator(), ((CUnaryExpression) op).getOperand(),
              functionName, precision, edge);
    } else if (op instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression) op);
      operand =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
    }
    return operand;
  }

  private Region propagateUnaryBooleanExpression(FeatureVarsState element,
      UnaryOperator opType, CExpression op, String functionName,
      FeatureVarsPrecision precision, CFAEdge edge) throws UnrecognizedCCodeException {
    Region returnValue = null;
    Region operand = null;
    if (op instanceof CIdExpression || op instanceof CFieldReference
        || op instanceof CArraySubscriptExpression) {
      String varName = op.toASTString();//this.getvarName(op.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand = rmgr.createPredicate(varName);
    } else if (op instanceof CUnaryExpression) {
      operand =
          propagateUnaryBooleanExpression(element, ((CUnaryExpression) op)
              .getOperator(), ((CUnaryExpression) op).getOperand(),
              functionName, precision, edge);
    } else if (op instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression) op);
      operand =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
    }
    if (operand == null) {
      return null;
    }
    switch (opType) {
    case NOT:
      returnValue = rmgr.makeNot(operand);
      break;
    case STAR:
      // *exp
      // don't know anything
      break;
    default:
      throw new UnrecognizedCCodeException("Unhandled case "
          + op.toASTString(), edge);
    }
    return returnValue;
  }

  private Region propagateBinaryBooleanExpression(FeatureVarsState element,
      BinaryOperator opType, CExpression op1, CExpression op2,
      String functionName, FeatureVarsPrecision precision, CFAEdge edge)
      throws UnrecognizedCCodeException {
    // determine operand1:
    Region operand1 = null;
    if (op1 instanceof CIdExpression || op1 instanceof CFieldReference
        || op1 instanceof CArraySubscriptExpression) {
      String varName = op1.toASTString();// this.getvarName(op1.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand1 = rmgr.createPredicate(varName);
    } else if (op1 instanceof CUnaryExpression) {
      operand1 =
          propagateUnaryBooleanExpression(element, ((CUnaryExpression) op1)
              .getOperator(), ((CUnaryExpression) op1).getOperand(),
              functionName, precision, edge);
    } else if (op1 instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression) op1);
      operand1 =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
    }
    // determine operand2:
    Region operand2 = null;
    if (op2 instanceof CIdExpression || op2 instanceof CFieldReference
        || op2 instanceof CArraySubscriptExpression) {
      String varName = op2.toASTString(); //this.getvarName(op2.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand2 = rmgr.createPredicate(varName);
    } else if (op2 instanceof CUnaryExpression) {
      operand2 =
          propagateUnaryBooleanExpression(element, ((CUnaryExpression) op2)
              .getOperator(), ((CUnaryExpression) op2).getOperand(),
              functionName, precision, edge);
    } else if (op2 instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression) op2);
      operand2 =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
    } else if (op2 instanceof CIntegerLiteralExpression) {
      CIntegerLiteralExpression number = (CIntegerLiteralExpression)op2;
      if (number.getValue().equals(BigInteger.ZERO)) {
        operand2 = rmgr.makeFalse();
      } else {
        operand2 = rmgr.makeTrue();
      }
    }
    if (operand1 == null || operand2 == null) {
      return null;
    }
    Region returnValue = null;
    // binary expression
    switch (opType) {
    case LOGICAL_AND:
      returnValue = rmgr.makeAnd(operand1, operand2);
      break;
    case LOGICAL_OR:
      returnValue = rmgr.makeOr(operand1, operand2);
      break;
    case EQUALS:
      returnValue = rmgr.makeOr(
              rmgr.makeAnd(operand1, operand2),
              rmgr.makeAnd(rmgr.makeNot(operand1), rmgr.makeNot(operand2))
          );
      break;
    case NOT_EQUALS:
      returnValue = rmgr.makeOr(
              rmgr.makeAnd(rmgr.makeNot(operand1), operand2),
              rmgr.makeAnd(operand1, rmgr.makeNot(operand2))
          );
      break;
    default:
      throw new UnrecognizedCCodeException(
          "Cases ==, != and others are not implemented", edge);
    }
    return returnValue;
  }
/*
  public String getvarName(String variableName, String functionName) {
    if(globalVars.contains(variableName)){
      return "$global::" + variableName;
    }
    return functionName + "::" + variableName;
  }*/

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge,
      Precision precision) throws UnrecognizedCCodeException {
    // do nothing
    return null;
  }
}
