/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
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
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


public class OctTransferRelation extends ForwardingTransferRelation<OctState, Precision> {

  /**
   * counter for temporary variables which should be increased after every
   * declaration of a new temporary variable
   */
  private static int temporaryVariableCounter = 0;

  /**
   * Class constructor.
   */
  public OctTransferRelation() {}

  @Override
  protected OctState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // Binary operation
    if (expression instanceof CBinaryExpression) {
      return handleBinaryBooleanExpression((CBinaryExpression) expression, truthAssumption);

      // Unary operation
    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExp = ((CUnaryExpression) expression);

      switch (unaryExp.getOperator()) {
      // invert truth assumption
      case NOT:
        return handleAssumption(cfaEdge, unaryExp.getOperand(), !truthAssumption);

        // do not change anything besides the expression, plus and minus have no effect
        // on the == 0 equality check
      case PLUS:
      case MINUS:
        return handleAssumption(cfaEdge, unaryExp.getOperand(), truthAssumption);

        // TODO check if some cases could be handled
      case SIZEOF:
      case TILDE:
      case AMPER:
        return state;
      default:
        throw new CPATransferException("Unhandled case: " + unaryExp.getOperator());
      }

      // An expression which cannot be simplified anymore
    } else if (expression instanceof CIdExpression
        || expression instanceof CFieldReference
        || (expression instanceof CPointerExpression && ((CPointerExpression) expression).getOperand() instanceof CIdExpression)) {
      String varName = expression.toASTString();
      if (expression instanceof CPointerExpression) {
        varName = ((CPointerExpression) expression).getOperand().toASTString();
      }
      return handleSingleBooleanExpression(varName, truthAssumption);

      // A constant value
    } else if (expression instanceof CLiteralExpression) {
      if (expression instanceof CIntegerLiteralExpression) {
        return handleLiteralBooleanExpression(((CIntegerLiteralExpression) expression).asLong(), truthAssumption);
      } else if (expression instanceof CCharLiteralExpression) {
        return handleLiteralBooleanExpression(((CCharLiteralExpression) expression).getCharacter(), truthAssumption);
      } else {
        return state;
      }

      // a cast, we ignore this cast and call this method again with the casts operand
    } else if (expression instanceof CCastExpression) {
      return handleAssumption(cfaEdge, ((CCastExpression) expression).getOperand(), truthAssumption);
    }

    else {
      throw new UnrecognizedCCodeException("Unknown expression type in assumption", cfaEdge, expression);
    }
  }

  /**
   * If only one literal is the complete boolean expression, we only need to check
   * this literal if it is equal to zero, depending on the truth assumption we
   * either return the unchanged state or null if the following branch is not reachable.
   *
   * @param value The long value of the CLiteralExpression
   * @param truthAssumption
   * @return an OctState or null
   */
  private OctState handleLiteralBooleanExpression(long value, boolean truthAssumption) {
    if (value == 0) {
      if (truthAssumption) {
        return state;
      } else {
        return null;
      }
    } else {
      if (truthAssumption) {
        return null;
      } else {
        return state;
      }
    }
  }

  /**
   * This method emulates an inequality constraint for assumptions with two variables.
   * Note that it only works if both variables are Integers!
   */
  private OctState addIneqConstraint(String pRightVariableName, String pLeftVariableName) {
    OctState newOct = state.clone();
    newOct.addEqConstraint(pLeftVariableName, pRightVariableName);

    // there is no inequality constraint possible in the normal way, as workaraound
    // we added an equality constraint, and check now if the state can still be reached
    // if they are unequal the new octagon is empty, so we can return the normal state
    // otherwise we return null
    if (newOct.isEmpty()) {
      return state;
    } else {
      return null;
    }
  }

  /**
   * This method emulates an inequality constraint for assumptions with a variable
   * and a long/int.
   * Note that it only works if both variables are Integers!
   */
  private OctState addIneqConstraint(String pVariableName, long pI) {
    OctState newOct = state.clone();
    newOct.addEqConstraint(pVariableName, pI);

    // there is no inequality constraint possible in the normal way, as workaraound
    // we added an equality constraint, and check now if the state can still be reached
    // if they are unequal the new octagon is empty, so we can return the normal state
    // otherwise we return null
    if (newOct.isEmpty()) {
      return state;
    } else {
      return null;
    }
  }

  /**
   * This method handles all binary boolean expressions.
   */
  private OctState handleBinaryBooleanExpression(CBinaryExpression binExp, boolean truthAssumption) throws CPATransferException {

    // IMPORTANT: for this switch we assume that in each conditional statement, there is only one
    // condition, (this simplification is added in the cfa creation phase)
    switch (binExp.getOperator()) {
    // TODO check which cases can be handled
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
      break;

    // for the following cases we first create a temporary variable where
    // the result of the operation is saved, afterwards, the equality with == 0
    // is checked
    case MINUS:
    case PLUS:
    case DIVIDE:
    case MODULO:
    case MULTIPLY:
      String tempVarName = buildVarName(functionName, "___cpa_temp_bool_var_" + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state.declareVariable(tempVarName);
      OctCoefficients coeffs = binExp.accept(new COctagonCoefficientVisitor(state));

      if (coeffs == null) {
        state.forget(tempVarName);
        return state;
      } else {
        state.makeAssignment(tempVarName, coeffs);
        return handleSingleBooleanExpression(tempVarName, truthAssumption);
      }

      // in the following cases we have to check left and right part of the binary
      // expression, when they are not single variables but contain for example
      // another binary expression we have to create some temporary variables again
      // which will be compared afterwards
    case EQUALS:
    case NOT_EQUALS:
    case GREATER_EQUAL:
    case GREATER_THAN:
    case LESS_EQUAL:
    case LESS_THAN:
      CExpression left = binExp.getOperand1();
      CExpression right = binExp.getOperand2();
      BinaryOperator op = binExp.getOperator();

      if (left instanceof CLiteralExpression || right instanceof CLiteralExpression) {
        return handleBinaryAssumptionWithLiteral(left, right, op, truthAssumption);
      } else {
        return handleBinaryAssumptionWithoutLiteral(binExp, truthAssumption, left, right);
      }

    default:
      throw new CPATransferException("Unhandled case: " + binExp.getOperator());
    }
    return state;
  }

  /**
   * This method is a helper method for handleBinaryBooleanExpression. It handles
   * all Assumptions with one literal and one variable or with two literals.
   * (p.e. a < 4; 4 < a; 3 < 4)
   */
  private OctState handleBinaryAssumptionWithLiteral(CExpression left, CExpression right, BinaryOperator op,
      boolean truthAssumption) throws CPATransferException {

    // we cannot cope with string literals so we do not know anything about the assumption
    // => just return the previous state
    if (left instanceof CStringLiteralExpression || right instanceof CStringLiteralExpression
        || left instanceof CFloatLiteralExpression || left instanceof CFloatLiteralExpression) { return state; }

    // both are literals
    if (left instanceof CLiteralExpression && right instanceof CLiteralExpression) {
      return handleBinaryAssumptionWithTwoLiterals((CLiteralExpression)left, (CLiteralExpression)right, op, truthAssumption);
    } else if (left instanceof CLiteralExpression) {
      //change operator so we can call handleBinaryAssumptionWithOneLiteral
      switch(op) {
      case GREATER_EQUAL:
        op = BinaryOperator.LESS_EQUAL;
        break;
      case GREATER_THAN:
        op = BinaryOperator.LESS_THAN;
        break;
      case LESS_EQUAL:
        op = BinaryOperator.GREATER_EQUAL;
        break;
      case LESS_THAN:
        op = BinaryOperator.GREATER_THAN;
      }
      return handleBinaryAssumptionWithOneLiteral(right, (CLiteralExpression) left, op, truthAssumption);

      // literal is on the right position, variable on the left;
    } else if (right instanceof CLiteralExpression) {
      return handleBinaryAssumptionWithOneLiteral(left, (CLiteralExpression) right, op, truthAssumption);
    }

    // if we did not return anything up to now we were not able to handle it
    // => just return the previous state
    return state;
  }

  /**
   * This method assumes the literal is on the righthandside! So take care while
   * calling this method, and if necessary change the operator to its opposite.
   * (p.e. a < 4)
   */
  private OctState handleBinaryAssumptionWithOneLiteral(CExpression left, CLiteralExpression right, BinaryOperator op,
      boolean truthAssumption) throws CPATransferException {

    String leftVarName = null;
    // check left side
    if (left instanceof CIdExpression || left instanceof CFieldReference) {
      leftVarName = buildVarName(functionName, left.toASTString());

      // create a temp var for the left side of the expression
    } else {
      String tempLeft = buildVarName(functionName, "___cpa_temp_bool_var_" + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state.declareVariable(tempLeft);
      OctCoefficients coeffsLeft = left.accept(new COctagonCoefficientVisitor(state));

      // we cannot do any comparison with an unknown value, so just quit here
      if (coeffsLeft == null) {
        state.forget(tempLeft);
        return state;
      } else {
        state.makeAssignment(tempLeft, coeffsLeft);
        leftVarName = tempLeft;
      }
    }

    long rightVal = 0;
    if (right instanceof CIntegerLiteralExpression) {
      rightVal = ((CIntegerLiteralExpression) right).asLong();
    } else if (right instanceof CCharLiteralExpression) {
      rightVal = ((CCharLiteralExpression) right).getCharacter();
    }

    switch (op) {
    case EQUALS:
      if (truthAssumption) {
        state.addEqConstraint(leftVarName, rightVal);
      } else {
        return addIneqConstraint(leftVarName, rightVal);
      }
      break;
    case NOT_EQUALS:
      if (truthAssumption) {
        return addIneqConstraint(leftVarName, rightVal);
      } else {
        state.addEqConstraint(leftVarName, rightVal);
      }
      break;
    case LESS_EQUAL:
      if (truthAssumption) {
        state.addSmallerEqConstraint(leftVarName, rightVal);
      } else {
        state.addGreaterConstraint(leftVarName, rightVal);
      }
      break;
    case LESS_THAN:
      if (truthAssumption) {
        state.addSmallerConstraint(leftVarName, rightVal);
      } else {
        state.addGreaterEqConstraint(leftVarName, rightVal);
      }
      break;
    case GREATER_EQUAL:
      if (truthAssumption) {
        state.addGreaterConstraint(leftVarName, rightVal);
      } else {
        state.addSmallerConstraint(leftVarName, rightVal);
      }
      break;
    case GREATER_THAN:
      if (truthAssumption) {
        state.addGreaterConstraint(leftVarName, rightVal);
      } else {
        state.addSmallerEqConstraint(leftVarName, rightVal);
      }
      break;
    default:
      throw new CPATransferException("Unhandled case statement: " + op);
    }
    return state;
  }

  /**
   * This method handles binary assumptions with two literals (p.e. 1 < 3). As
   * this method is only a submethod of handleBinaryAssumptionWithOneLiteral it
   * assumes that the literal is eiter a CIntegerLiteralExpression or
   * a CCharLiteralExpression.
   */
  private OctState handleBinaryAssumptionWithTwoLiterals(CLiteralExpression left, CLiteralExpression right, BinaryOperator op,
      boolean truthAssumption) throws CPATransferException {
    long leftVal = 0;
    if (left instanceof CIntegerLiteralExpression) {
      leftVal = ((CIntegerLiteralExpression) left).asLong();
    } else if (left instanceof CCharLiteralExpression) {
      leftVal = ((CCharLiteralExpression) left).getCharacter();
    }
    long rightVal = 0;
    if (right instanceof CIntegerLiteralExpression) {
      rightVal = ((CIntegerLiteralExpression) right).asLong();
    } else if (right instanceof CCharLiteralExpression) {
      rightVal = ((CCharLiteralExpression) right).getCharacter();
    }

    switch (op) {
    case EQUALS:
      if (truthAssumption) {
        if (leftVal == rightVal) {
          return state;
        } else {
          return null;
        }
      } else {
        if (leftVal == rightVal) {
          return null;
        } else {
          return state;
        }
      }
    case GREATER_EQUAL:
      if (truthAssumption) {
        if (leftVal >= rightVal) {
          return state;
        } else {
          return null;
        }
      } else {
        if (leftVal >= rightVal) {
          return null;
        } else {
          return state;
        }
      }
    case GREATER_THAN:
      if (truthAssumption) {
        if (leftVal > rightVal) {
          return state;
        } else {
          return null;
        }
      } else {
        if (leftVal > rightVal) {
          return null;
        } else {
          return state;
        }
      }
    case LESS_EQUAL:
      if (truthAssumption) {
        if (leftVal <= rightVal) {
          return state;
        } else {
          return null;
        }
      } else {
        if (leftVal <= rightVal) {
          return null;
        } else {
          return state;
        }
      }
    case LESS_THAN:
      if (truthAssumption) {
        if (leftVal < rightVal) {
          return state;
        } else {
          return null;
        }
      } else {
        if (leftVal < rightVal) {
          return null;
        } else {
          return state;
        }
      }
    case NOT_EQUALS:
      if (truthAssumption) {
        if (leftVal != rightVal) {
          return state;
        } else {
          return null;
        }
      } else {
        if (leftVal != rightVal) {
          return null;
        } else {
          return state;
        }
      }
    default:
      throw new CPATransferException("Unhandled case: " + op);
    }
  }

  /**
   * This method handles all binary assumptions without literals (p.e. a < b)
    */
  private OctState handleBinaryAssumptionWithoutLiteral(CBinaryExpression binExp, boolean truthAssumption,
      CExpression left, CExpression right)
      throws CPATransferException {
    CBinaryExpression.BinaryOperator op = binExp.getOperator();
    String leftVarName = null;
    String rightVarName = null;

    // check left side
    if (left instanceof CIdExpression || left instanceof CFieldReference) {
      leftVarName = buildVarName(functionName, left.toASTString());

      // create a temp var for the left side of the expression
    } else {
      String tempLeft = buildVarName(functionName, "___cpa_temp_bool_var_" + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state.declareVariable(tempLeft);
      OctCoefficients coeffsLeft = left.accept(new COctagonCoefficientVisitor(state));

      // we cannot do any comparision with an unknown value, so just quit here
      if (coeffsLeft == null) {
        state.forget(tempLeft);
        return state;
      } else {
        state.makeAssignment(tempLeft, coeffsLeft);
        leftVarName = tempLeft;
      }
    }

    // check right side
    if (right instanceof CIdExpression || right instanceof CFieldReference) {
      rightVarName = buildVarName(functionName, right.toASTString());

      // create a temp var for the right side of the expression
    } else {
      String tempRight = buildVarName(functionName, "___cpa_temp_bool_var_" + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state.declareVariable(tempRight);
      OctCoefficients coeffsRight = right.accept(new COctagonCoefficientVisitor(state));

      // we cannot do any comparision with an unknown value, so just quit here
      if (coeffsRight == null) {
        state.forget(tempRight);
        return state;
      } else {
        state.makeAssignment(tempRight, coeffsRight);
        rightVarName = tempRight;
      }
    }

    // Comparison part, left and right are now definitly availbable
    switch (op) {
    case EQUALS:
      if (truthAssumption) {
        state.addEqConstraint(rightVarName, leftVarName);
      } else {
        return addIneqConstraint(rightVarName, leftVarName);
      }
      break;
    case GREATER_EQUAL:
      if (truthAssumption) {
        state.addGreaterEqConstraint(rightVarName, leftVarName);
      } else {
        state.addSmallerConstraint(rightVarName, leftVarName);
      }
      break;
    case GREATER_THAN:
      if (truthAssumption) {
        state.addGreaterConstraint(rightVarName, leftVarName);
      } else {
        state.addSmallerEqConstraint(rightVarName, leftVarName);
      }
      break;
    case LESS_EQUAL:
      if (truthAssumption) {
        state.addSmallerEqConstraint(rightVarName, leftVarName);
      } else {
        state.addGreaterConstraint(rightVarName, leftVarName);
      }
      break;
    case LESS_THAN:
      if (truthAssumption) {
        state.addSmallerConstraint(rightVarName, leftVarName);
      } else {
        state.addGreaterEqConstraint(rightVarName, leftVarName);
      }
      break;
    case NOT_EQUALS:
      if (truthAssumption) {
        return addIneqConstraint(rightVarName, leftVarName);
      } else {
        state.addEqConstraint(rightVarName, leftVarName);
      }
      break;
    default:
      throw new CPATransferException("Unhandled case: " + binExp.getOperator());
    }
    return state;
  }

  /**
   * This method handles all expressions which are assumptions without beeing
   * binary expressions (p.e if(1) or if(1+2) or if (a))
   */
  private OctState handleSingleBooleanExpression(String variableName, boolean truthAssumption) {
    // if (a)
    if (truthAssumption) {
      addIneqConstraint(variableName, 0);

      // if (!a)
    } else {
      state.addEqConstraint(variableName, 0);
    }
    return state;
  }

  @Override
  protected OctState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {

    CFunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    assert (paramNames.size() == arguments.size());

    // set previous state so we can delete all local variables from our list afterwards
    state.setPreviousState(state.clone());

    // declare all parameters as variables
    for (int i = 0; i < arguments.size(); i++) {
      CExpression arg = arguments.get(i);

      String nameOfParam = paramNames.get(i);
      String formalParamName = buildVarName(calledFunctionName, nameOfParam);

      state.declareVariable(formalParamName);

      OctCoefficients coeffs = arg.accept(new COctagonCoefficientVisitor(state));
      if (coeffs != null) {
        state.makeAssignment(formalParamName, coeffs);
      } else {
        state.forget(formalParamName);
      }
    }

    return state;
  }

  @Override
  protected OctState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    CFunctionCall exprOnSummary = fnkCall.getExpression();

    String calledFunctionName = cfaEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement) exprOnSummary);
      CLeftHandSide op1 = binExp.getLeftHandSide();


      String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

      String assignedVarName = buildVarName(calledFunctionName, op1.toASTString());
      OctCoefficients right = new OctCoefficients(state.sizeOfVariables(), state.getVariableIndexFor(returnVarName), 1);

      state.makeAssignment(assignedVarName, right);
    }

    // g(b), do nothing
    else if (exprOnSummary instanceof CFunctionCallStatement) {

    } else {
      throw new UnrecognizedCCodeException("on function return", cfaEdge, exprOnSummary);
    }

    // delete local variables
    state.removeLocalVariables(state.getPreviousState());

    return state;
  }

  @Override
  protected OctState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    if (cfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration declaration = (CVariableDeclaration) decl;

      // get the variable name in the declarator
      String varName = declaration.getName();

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointer variables to the list since we don't track them
      if (decl.getType() instanceof CPointerType) { return state; }

      // make the fullyqualifiedname
      String variableName = buildVarName(functionName, varName);
      // do the declaration of the variable, even if we have no initializer
      state.declareVariable(variableName);


      OctCoefficients v = null;

      CInitializer init = declaration.getInitializer();
      if (init != null) {
        if (init instanceof CInitializerExpression) {
          CExpression exp = ((CInitializerExpression) init).getExpression();

          v = exp.accept(new COctagonCoefficientVisitor(state));

          // if there is an initializerlist, the variable is either an array or a struct/union
          // we cannot handle them, so simply return the previous state
        } else if (init instanceof CInitializerList) {
          return state;

        } else {
          throw new AssertionError("Unhandled Expression Type: " + init.getClass());
        }


        // global variables without initializer are set to 0 in C
      } else if (decl.isGlobal()) {
        v = new OctCoefficients(state.sizeOfVariables() + 1);
      }

      if (v != null) {
        state.makeAssignment(variableName, v);
        return state;
      } else {

        state.forget(variableName);
        return state;
      }

    } else if (cfaEdge.getDeclaration() instanceof CTypeDeclaration
        || cfaEdge.getDeclaration() instanceof CFunctionDeclaration) { return state; }

    throw new AssertionError(cfaEdge.getDeclaration() + " (" + cfaEdge.getDeclaration().getClass() + ")");
  }

  @Override
  protected OctState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {

    // expression is a binary operation, e.g. a = b;
    if (statement instanceof CAssignment) {
      CLeftHandSide left = ((CAssignment) statement).getLeftHandSide();
      CRightHandSide right = ((CAssignment) statement).getRightHandSide();

      OctCoefficients coeffs = right.accept(new COctagonCoefficientVisitor(state));

      // if we cannot determine coefficients, we cannot make any assumptions about
      // the value of the assigned varible and reset its value to unknown
      if (coeffs == null) {
        state.forget(buildVarName(functionName, ((CArraySubscriptExpression) left).getArrayExpression().toASTString()));
        return state;
      }

      // TODO check if we can handle array subscripts
      if (left instanceof CArraySubscriptExpression) {
        state.forget(buildVarName(functionName, ((CArraySubscriptExpression) left).getArrayExpression().toASTString()));
        return state;
      } else if (left instanceof CPointerExpression) {
        state.makeAssignment(buildVarName(functionName, ((CPointerExpression) left).getOperand().toASTString()), coeffs);
      } else {
        // TODO is assignment to field valid?
        state.makeAssignment(buildVarName(functionName, left.toASTString()), coeffs);
      }

      // external function call, or p.e. a;
      // => do nothing
    } else if (statement instanceof CFunctionCallStatement
        || statement instanceof CExpressionStatement) {

    } else {
      throw new UnrecognizedCCodeException("unknown statement", cfaEdge, statement);
    }

    return state;
  }

  /**
   * This is a return statement in a function
   */
  @Override
  protected OctState handleReturnStatementEdge(CReturnStatementEdge cfaEdge, @Nullable CExpression expression)
      throws CPATransferException {
    String tempVarName = buildVarName(cfaEdge.getSuccessor().getFunctionName(), "___cpa_temp_result_var_");
    state.declareVariable(tempVarName);
    OctCoefficients coeffs = expression.accept(new COctagonCoefficientVisitor(state));

    if (coeffs == null) {
      state.forget(tempVarName);
    } else {
      state.makeAssignment(tempVarName, coeffs);
    }
    return state;
  }

  /**
   * This edge is the return edge from a function to the caller
   */
  @Override
  protected OctState handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    return null;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    // TODO Auto-generated method stub
    return null;
  }


  class COctagonCoefficientVisitor extends DefaultCExpressionVisitor<OctCoefficients, CPATransferException>
      implements CRightHandSideVisitor<OctCoefficients, CPATransferException> {

    OctState coeffState;

    /**
     * This method creates the Visitor, which evaluates all coefficients for a given
     * Expression.
     *
     * @param state
     */
    public COctagonCoefficientVisitor(OctState state) {
      this.coeffState = state;
    }


    @Override
    protected OctCoefficients visitDefault(CExpression pExp) throws CPATransferException {
      return null;
    }

    @Override
    public OctCoefficients visit(CArraySubscriptExpression e) throws CPATransferException {
      // TODO check if we can handle array expressions
      return null;
    }

    @Override
    public OctCoefficients visit(CBinaryExpression e) throws CPATransferException {
      OctCoefficients left = e.getOperand1().accept(this);
      OctCoefficients right = e.getOperand2().accept(this);

      if (left == null || right == null) { return null; }

      switch (e.getOperator()) {
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      case EQUALS:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case LESS_EQUAL:
      case LESS_THAN:
      case NOT_EQUALS:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case MODULO:
        return null;
      case MINUS:
        return left.sub(right);
      case PLUS:
        return left.add(right);
      case DIVIDE:
        if (right.hasOnlyConstantValue()) {
          return left.div(right.get(state.sizeOfVariables()));
        } else {
          // within a division the divisor has to be a constant value, any other
          // divisions are not possible
          return null;
        }
      case MULTIPLY:
        if (left.hasOnlyConstantValue()) {
          return right.mult(left.get(state.sizeOfVariables()));
        } else if (right.hasOnlyConstantValue()) {
          return left.mult(right.get(state.sizeOfVariables()));
        } else {
          // both operands have coefficients for variables, multiplying such
          // is not possible
          return null;
        }

      default:
        throw new AssertionError("Unhandled case statement");
      }
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public OctCoefficients visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    /**
     * Ignore complex casts
     */
    @Override
    public OctCoefficients visit(CComplexCastExpression e) throws CPATransferException {
      return null;
    }

    @Override
    public OctCoefficients visit(CFieldReference e) throws CPATransferException {
      String varName = buildVarName(functionName, e.toASTString());
      Integer varIndex = coeffState.getVariableIndexFor(varName);
      if (varIndex == null) { return null; }
      return new OctCoefficients(coeffState.sizeOfVariables() + 1, varIndex, 1);
    }

    @Override
    public OctCoefficients visit(CIdExpression e) throws CPATransferException {
      String varName = buildVarName(functionName, e.toASTString());
      Integer varIndex = coeffState.getVariableIndexFor(varName);
      if (varIndex == null) { return null; }
      return new OctCoefficients(coeffState.sizeOfVariables() + 1, varIndex, 1);
    }

    @Override
    public OctCoefficients visit(CCharLiteralExpression e) throws CPATransferException {
      return new OctCoefficients(coeffState.sizeOfVariables() + 1, coeffState.sizeOfVariables(), e.getValue());
    }

    /**
     * Ignore ImaginaryExpressions
     */
    @Override
    public OctCoefficients visit(CImaginaryLiteralExpression e) throws CPATransferException {
      return null;
    }

    /**
     * Floats can currently not be handled
     */
    @Override
    public OctCoefficients visit(CFloatLiteralExpression e) throws CPATransferException {
      // TODO check if we can handle floats, too
      return null;
    }

    @Override
    public OctCoefficients visit(CIntegerLiteralExpression e) throws CPATransferException {
      return new OctCoefficients(coeffState.sizeOfVariables() + 1, coeffState.sizeOfVariables(), (int) e.asLong());
    }

    /**
     * Strings cannot be handled
     */
    @Override
    public OctCoefficients visit(CStringLiteralExpression e) throws CPATransferException {
      return null;
    }


    @Override
    public OctCoefficients visit(CTypeIdExpression e) throws CPATransferException {
      return null;
    }

    @Override
    public OctCoefficients visit(CTypeIdInitializerExpression e) throws CPATransferException {
      return null;
    }

    @Override
    public OctCoefficients visit(CUnaryExpression e) throws CPATransferException {
      OctCoefficients operand = e.getOperand().accept(this);

      if (operand == null) { return null; }

      switch (e.getOperator()) {
      case AMPER:
      case NOT:
      case SIZEOF:
      case TILDE:
        return null;
      case PLUS:
        return operand;
      case MINUS:
        return operand.mult(-1);
      default:
        throw new AssertionError("Unhandled case in switch clause.");
      }
    }

    @Override
    public OctCoefficients visit(CPointerExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }


    @Override
    public OctCoefficients visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
      //TODO check what to do when function call appears
      return null;
    }
  }
}
