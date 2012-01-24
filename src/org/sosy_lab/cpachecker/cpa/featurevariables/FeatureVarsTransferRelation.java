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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
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
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision pPrecision, CFAEdge cfaEdge)
      throws CPATransferException {
    Preconditions.checkArgument(pPrecision instanceof FeatureVarsPrecision, "precision is no FeatureVarsPrecision");
    FeatureVarsPrecision precision = (FeatureVarsPrecision) pPrecision;
    if (precision.isDisabled()) {
      // this means that no variables should be tracked (whitelist is empty)
      return Collections.singleton(element);
    }

    FeatureVarsElement fvElement = (FeatureVarsElement) element;
    AbstractElement successor = fvElement;
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
      // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor =
          handleAssumption(fvElement, assumeEdge.getExpression(), cfaEdge,
              assumeEdge.getTruthAssumption(), precision);
      /*if (successor != null && successor != element) {
        System.out.println("FV new state: " + successor.toString()
            + " after edge " + cfaEdge.getRawStatement()
            + " in line" + cfaEdge.getLineNumber());
      }*/
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
    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  private AbstractElement handleStatementEdge(FeatureVarsElement element,
      IASTStatement pIastStatement, StatementEdge cfaEdge,
      FeatureVarsPrecision pPrecision) {

    if (!(pIastStatement instanceof IASTAssignment)) {
      return element;
    }
    IASTAssignment assignment = (IASTAssignment)pIastStatement;

    IASTExpression lhs = assignment.getLeftHandSide();
    FeatureVarsElement result = element;
    if (lhs instanceof IASTIdExpression || lhs instanceof IASTFieldReference
        || lhs instanceof IASTArraySubscriptExpression) {
      String varName = lhs.toASTString();//this.getvarName(op.getRawSignature(), functionName);
      if (pPrecision.isOnWhitelist(varName)) {
        IASTRightHandSide rhs = assignment.getRightHandSide();
        if (rhs instanceof IASTIntegerLiteralExpression) {
          String value = rhs.toASTString();
          /*
           * This will only work with the first assignment to the variable!
           * If the variable gets a second assignment we would have to delete the current value from the bdd first.
           * I do not know how to do this yet.
           */

          if (value.trim().equals("0")) {
            Region operand = rmgr.makeNot(rmgr.createPredicate(varName));
            result = new FeatureVarsElement(rmgr.makeAnd(element.getRegion(), operand), rmgr);
          } else {
            Region operand = rmgr.createPredicate(varName);
            result = new FeatureVarsElement(rmgr.makeAnd(element.getRegion(), operand), rmgr);
          }
        }
      }
    }
    if (result.getRegion().isFalse()) {
      return null; // assumption is not fulfilled / not possible
    } else {
      return result;
    }
  }

  private AbstractElement handleAssumption(FeatureVarsElement element,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue,
      FeatureVarsPrecision precision) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    FeatureVarsElement result = handleBooleanExpression(element, expression, functionName, truthValue, precision, cfaEdge);
    if (result.getRegion().isFalse()) {
      return null; // assumption is not fulfilled / not possible
    } else {
      return result;
    }
  }

  private FeatureVarsElement handleBooleanExpression(FeatureVarsElement element,
      IASTExpression op, String functionName, boolean pTruthValue,
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
      return new FeatureVarsElement(newRegion, rmgr);
    }
  }

  private Region propagateBooleanExpression(FeatureVarsElement element,
      IASTExpression op, String functionName, FeatureVarsPrecision precision, CFAEdge edge)
    throws UnrecognizedCCodeException {
    Region operand = null;
    if (op instanceof IASTIdExpression || op instanceof IASTFieldReference
        || op instanceof IASTArraySubscriptExpression) {
      String varName = op.toASTString();//this.getvarName(op.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand = rmgr.createPredicate(varName);
    } else if (op instanceof IASTUnaryExpression) {
      operand =
          propagateUnaryBooleanExpression(element, ((IASTUnaryExpression) op)
              .getOperator(), ((IASTUnaryExpression) op).getOperand(),
              functionName, precision, edge);
    } else if (op instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression) op);
      operand =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
    }
    return operand;
  }

  private Region propagateUnaryBooleanExpression(FeatureVarsElement element,
      UnaryOperator opType, IASTExpression op, String functionName,
      FeatureVarsPrecision precision, CFAEdge edge) throws UnrecognizedCCodeException {
    Region returnValue = null;
    Region operand = null;
    if (op instanceof IASTIdExpression || op instanceof IASTFieldReference
        || op instanceof IASTArraySubscriptExpression) {
      String varName = op.toASTString();//this.getvarName(op.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand = rmgr.createPredicate(varName);
    } else if (op instanceof IASTUnaryExpression) {
      operand =
          propagateUnaryBooleanExpression(element, ((IASTUnaryExpression) op)
              .getOperator(), ((IASTUnaryExpression) op).getOperand(),
              functionName, precision, edge);
    } else if (op instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression) op);
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

  private Region propagateBinaryBooleanExpression(FeatureVarsElement element,
      BinaryOperator opType, IASTExpression op1, IASTExpression op2,
      String functionName, FeatureVarsPrecision precision, CFAEdge edge)
      throws UnrecognizedCCodeException {
    // determine operand1:
    Region operand1 = null;
    if (op1 instanceof IASTIdExpression || op1 instanceof IASTFieldReference
        || op1 instanceof IASTArraySubscriptExpression) {
      String varName = op1.toASTString();// this.getvarName(op1.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand1 = rmgr.createPredicate(varName);
    } else if (op1 instanceof IASTUnaryExpression) {
      operand1 =
          propagateUnaryBooleanExpression(element, ((IASTUnaryExpression) op1)
              .getOperator(), ((IASTUnaryExpression) op1).getOperand(),
              functionName, precision, edge);
    } else if (op1 instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression) op1);
      operand1 =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
    }
    // determine operand2:
    Region operand2 = null;
    if (op2 instanceof IASTIdExpression || op2 instanceof IASTFieldReference
        || op2 instanceof IASTArraySubscriptExpression) {
      String varName = op2.toASTString(); //this.getvarName(op2.getRawSignature(), functionName);
      if (!precision.isOnWhitelist(varName)) {
        return null;
      }
      operand2 = rmgr.createPredicate(varName);
    } else if (op2 instanceof IASTUnaryExpression) {
      operand2 =
          propagateUnaryBooleanExpression(element, ((IASTUnaryExpression) op2)
              .getOperator(), ((IASTUnaryExpression) op2).getOperand(),
              functionName, precision, edge);
    } else if (op2 instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression) op2);
      operand2 =
          propagateBinaryBooleanExpression(element, binExp.getOperator(),
              binExp.getOperand1(), binExp.getOperand2(), functionName,
              precision, edge);
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
    case NOT_EQUALS:
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
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement element, List<AbstractElement> elements, CFAEdge cfaEdge,
      Precision precision) throws UnrecognizedCCodeException {
    // do nothing
    return null;
  }
}
