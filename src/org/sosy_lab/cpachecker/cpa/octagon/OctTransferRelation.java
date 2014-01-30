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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.LogManager;
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
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.ImmutableMap;


public class OctTransferRelation extends ForwardingTransferRelation<OctState, Precision> {

  private static final String TEMP_BOOLEAN_VAR_NAME = "___cpa_temp_bool_var_";
  private static final String FUNCTION_RETURN_VAR = "___cpa_temp_result_var_";

  /**
   * counter for temporary variables which should be increased after every
   * declaration of a new temporary variable
   */
  private static int temporaryVariableCounter = 0;

  /**
   * set of functions that may not appear in the source code
   * the value of the map entry is the explanation for the user
   */
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  private Collection<OctState> possibleStates = new ArrayList<>();

  private final LogManager logger;

  /**
   * Class constructor.
   */
  public OctTransferRelation(LogManager log) {
    logger = log;
  }

  @Override
  public Collection<OctState> getAbstractSuccessors(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException {

    setInfo(abstractState, abstractPrecision, cfaEdge);

    final Collection<OctState> preCheck = preCheck();
    if (preCheck != null) { return preCheck; }

    final Collection<OctState> successors = new ArrayList<>();

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge:
      final AssumeEdge assumption = (AssumeEdge) cfaEdge;
      successors.add(handleAssumption(assumption, assumption.getExpression(), assumption.getTruthAssumption()));
      break;

    case FunctionCallEdge:
      final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
      final FunctionEntryNode succ = fnkCall.getSuccessor();
      final String calledFunctionName = succ.getFunctionName();
      successors.add(handleFunctionCallEdge(fnkCall, fnkCall.getArguments(),
          succ.getFunctionParameters(), calledFunctionName));
      break;

    case FunctionReturnEdge:
      final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      final FunctionReturnEdge fnkReturnEdge = (FunctionReturnEdge) cfaEdge;
      final FunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
      successors.add(handleFunctionReturnEdge(fnkReturnEdge,
          summaryEdge, summaryEdge.getExpression(), callerFunctionName));

      break;

    case MultiEdge:
      successors.add(handleMultiEdge((MultiEdge) cfaEdge));
      break;

    default:
      successors.add(handleSimpleEdge(cfaEdge));
    }

    successors.addAll(possibleStates);
    possibleStates.clear();
    successors.removeAll(Collections.singleton(null));

    // remove all states whose constraints cannot be satisfied
    Iterator<OctState> states = successors.iterator();
    while (states.hasNext()) {
      OctState st = states.next();
      if (st.isEmpty()) {
        states.remove();
        logger.log(Level.FINER, "removing state because of unsatisfiable constraints:\n" +
                                 st + "________________\nEdge was:\n" + cfaEdge.getDescription());
      }
    }

    for (OctState st : successors) {
      postProcessing(st);
    }

    resetInfo();

    return successors;
  }

  @Override
  protected OctState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // Binary operation
    if (expression instanceof CBinaryExpression) {
      return handleBinaryBooleanExpression((CBinaryExpression) expression, truthAssumption,state);

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
      if (isHandleableVariable(expression)) {
        String varName = buildVarName((CLeftHandSide) expression, functionName);
        return handleSingleBooleanExpression(varName, truthAssumption, state);
      } else {
        return state;
      }

      // A constant value
    } else if (expression instanceof CLiteralExpression) {
      if (expression instanceof CIntegerLiteralExpression) {
        return handleLiteralBooleanExpression(((CIntegerLiteralExpression) expression).asLong(), truthAssumption, state);
      } else if (expression instanceof CCharLiteralExpression) {
        return handleLiteralBooleanExpression(((CCharLiteralExpression) expression).getCharacter(), truthAssumption, state);
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
  private OctState handleLiteralBooleanExpression(long value, boolean truthAssumption, OctState state) {
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
   * This method handles all binary boolean expressions.
   */
  private OctState handleBinaryBooleanExpression(CBinaryExpression binExp, boolean truthAssumption, OctState state) throws CPATransferException {

    // IMPORTANT: for this switch we assume that in each conditional statement, there is only one
    // condition, (this simplification is added in the cfa creation phase)
    switch (binExp.getOperator()) {
    // TODO check which cases can be handled
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
      return state;

    // for the following cases we first create a temporary variable where
    // the result of the operation is saved, afterwards, the equality with == 0
    // is checked
    case MINUS:
    case PLUS:
    case DIVIDE:
    case MODULO:
    case MULTIPLY:
      String tempVarName = buildVarName(functionName, TEMP_BOOLEAN_VAR_NAME + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state = state.declareVariable(tempVarName, binExp.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()+1)));
      return handleSingleBooleanExpression(tempVarName, truthAssumption, state);

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

      if (!isHandleableVariable(left) || !isHandleableVariable(right)) {
        return state;
      }

      if (left instanceof CLiteralExpression || right instanceof CLiteralExpression) {
        return handleBinaryAssumptionWithLiteral(left, right, op, truthAssumption, state);
      } else {
        return handleBinaryAssumptionWithoutLiteral(binExp, truthAssumption, left, right, state);
      }

    default:
      throw new CPATransferException("Unhandled case: " + binExp.getOperator());
    }
  }

  /**
   * This method is a helper method for handleBinaryBooleanExpression. It handles
   * all Assumptions with one literal and one variable or with two literals.
   * (p.e. a < 4; 4 < a; 3 < 4)
   */
  private OctState handleBinaryAssumptionWithLiteral(CExpression left, CExpression right, BinaryOperator op,
      boolean truthAssumption, OctState state) throws CPATransferException {

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
      return handleBinaryAssumptionWithOneLiteral(right, (CLiteralExpression) left, op, truthAssumption, state);

      // literal is on the right position, variable on the left;
    } else if (right instanceof CLiteralExpression) {
      return handleBinaryAssumptionWithOneLiteral(left, (CLiteralExpression) right, op, truthAssumption, state);
    }

    // if we did not return anything up to now we were not able to handle it
    // => just return the previous state
    return state;
  }

  private boolean isHandleableVariable(CExpression var) {
    if (var instanceof CArraySubscriptExpression
        || var instanceof CFieldReference
        || var instanceof CPointerExpression) {
      return false;
    }
    return isHandleAbleType(var.getExpressionType());
  }

  private boolean isHandleAbleType(CType type) {
    type = type.getCanonicalType();
    if (type instanceof CPointerType
        || type instanceof CCompositeType
        || type instanceof CArrayType) {
      return false;
    } else if (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
      if (type instanceof CPointerType
          || type instanceof CCompositeType) {
        return false;
      }
    }
    return true;
  }

  /**
   * This method assumes the literal is on the righthandside! So take care while
   * calling this method, and if necessary change the operator to its opposite.
   * (p.e. a < 4)
   */
  private OctState handleBinaryAssumptionWithOneLiteral(CExpression left, CLiteralExpression right, BinaryOperator op,
      boolean truthAssumption, OctState state) throws CPATransferException {

    // we cannot handle pointers, so just ignore them
    if (left.getExpressionType() instanceof CPointerType
        || (left instanceof CFieldReference && ((CFieldReference) left).isPointerDereference())) {
      return state;
    }

    String leftVarName = null;
    // check left side
    if (left instanceof CIdExpression || left instanceof CFieldReference) {
      leftVarName = buildVarName((CLeftHandSide) left, functionName);

      // create a temp var for the left side of the expression
    } else {
      String tempLeft = buildVarName(functionName, TEMP_BOOLEAN_VAR_NAME + temporaryVariableCounter + "_");
      IOctCoefficients coeffsLeft = left.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()+1));

      // we cannot do any comparison with an unknown value, so just quit here
      // TODO remove if check???
      if (coeffsLeft == null) {
        return state;
      } else {
        temporaryVariableCounter++;
        state = state.declareVariable(tempLeft, coeffsLeft);
        leftVarName = tempLeft;
      }
    }

    long rightVal = 0;
    if (right instanceof CIntegerLiteralExpression) {
      rightVal = ((CIntegerLiteralExpression) right).asLong();
    } else if (right instanceof CCharLiteralExpression) {
      rightVal = ((CCharLiteralExpression) right).getCharacter();
    }

    IOctCoefficients test = state.getVariableToCoeffMap().get(leftVarName);
    // in cases like
    // a = [0, INFINITY]
    // the test (a > 0) will fail normally as zero is included in the range for a
    // for addressing this problem we check if only a part of the range of a is
    // bigger than the compared value, and return a smaller range which is
    // afterwards assigned to a instead of prooving any constraints
    if (test instanceof OctIntervalCoefficients) {
      test = handleIntervalCoeffsInAssumptions((OctIntervalCoefficients) test, new OctSimpleCoefficients(test.size(), rightVal), op, truthAssumption);
      if (test != null) {
        return state.makeAssignment(leftVarName, test);
      }
    }

    switch (op) {
    case EQUALS:
      if (truthAssumption) {
        return state.addEqConstraint(leftVarName, rightVal);
      } else {
        possibleStates.addAll(state.addIneqConstraint(leftVarName, rightVal));
        return null;
      }

    case NOT_EQUALS:
      if (truthAssumption) {
        possibleStates.addAll(state.addIneqConstraint(leftVarName, rightVal));
        return null;
      } else {
        return state.addEqConstraint(leftVarName, rightVal);
      }

    case LESS_EQUAL:
      if (truthAssumption) {
        return state.addSmallerEqConstraint(leftVarName, rightVal);
      } else {
        return state.addGreaterConstraint(leftVarName, rightVal);
      }

    case LESS_THAN:
      if (truthAssumption) {
        return state.addSmallerConstraint(leftVarName, rightVal);
      } else {
        return state.addGreaterEqConstraint(leftVarName, rightVal);
      }

    case GREATER_EQUAL:
      if (truthAssumption) {
        return state.addGreaterEqConstraint(leftVarName, rightVal);
      } else {
        return state.addSmallerConstraint(leftVarName, rightVal);
      }

    case GREATER_THAN:
      if (truthAssumption) {
        return state.addGreaterConstraint(leftVarName, rightVal);
      } else {
        return state.addSmallerEqConstraint(leftVarName, rightVal);
      }

    default:
      throw new CPATransferException("Unhandled case statement: " + op);
    }

  }

  private IOctCoefficients handleIntervalCoeffsInAssumptions(OctIntervalCoefficients leftCoeffs,
      IOctCoefficients rightCoeffs, BinaryOperator op, boolean truthAssumption) {
    switch (op) {

    case LESS_THAN:
      if (truthAssumption) {
        return leftCoeffs.smallerPart(rightCoeffs);
      }
      break;
    case GREATER_EQUAL:
      if (!truthAssumption) {
        return leftCoeffs.smallerPart(rightCoeffs);
      }
      break;

    case GREATER_THAN:
      if (truthAssumption) {
        return leftCoeffs.greaterPart(rightCoeffs);
      }
      break;
    case LESS_EQUAL:
      if (!truthAssumption) {
        return leftCoeffs.greaterPart(rightCoeffs);
      }
      break;

    default:
      // no exception here, every case which cannot be handled should return null
      // so normal assumption handling can handle these cases
    }
    return null;
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
      CExpression left, CExpression right, OctState state)
      throws CPATransferException {
    CBinaryExpression.BinaryOperator op = binExp.getOperator();
    String leftVarName = null;
    String rightVarName = null;

    // we cannot handle pointers, so just ignore them
    // TODO make program unsafe?
    if (left.getExpressionType() instanceof CPointerType || right.getExpressionType() instanceof CPointerType
        || (left instanceof CFieldReference && ((CFieldReference) left).isPointerDereference())
        || (right instanceof CFieldReference && ((CFieldReference) right).isPointerDereference())) {
      return state;
    }

    // check left side
    if (left instanceof CIdExpression || left instanceof CFieldReference) {
      leftVarName = buildVarName((CLeftHandSide) left, functionName);

      // create a temp var for the left side of the expression
    } else {
      String tempLeft = buildVarName(functionName, TEMP_BOOLEAN_VAR_NAME + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state = state.declareVariable(tempLeft, left.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()+1)));

      leftVarName = tempLeft;
    }

    // check right side
    if (right instanceof CIdExpression || right instanceof CFieldReference) {
      rightVarName = buildVarName((CLeftHandSide) right, functionName);

      // create a temp var for the right side of the expression
    } else {
      String tempRight = buildVarName(functionName, TEMP_BOOLEAN_VAR_NAME + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      state = state.declareVariable(tempRight, right.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()+1)));
      rightVarName = tempRight;
    }

    IOctCoefficients test = state.getVariableToCoeffMap().get(leftVarName);
    IOctCoefficients rightSide = state.getVariableToCoeffMap().get(rightVarName);
    // in cases like
    // a = [0, INFINITY]
    // the test (a > 0) will fail normally as zero is included in the range for a
    // for addressing this problem we check if only a part of the range of a is
    // bigger than the compared value, and return a smaller range which is
    // afterwards assigned to a instead of prooving any constraints
    if (test instanceof OctIntervalCoefficients && rightSide != null) {
      test = handleIntervalCoeffsInAssumptions((OctIntervalCoefficients) test, rightSide, op, truthAssumption);
      if (test != null) {
        return state.makeAssignment(leftVarName, test);
      }
    }

    // Comparison part, left and right are now definitely available
    switch (op) {
    case EQUALS:
      if (truthAssumption) {
        return state.addEqConstraint(rightVarName, leftVarName);
      } else {
        possibleStates.addAll(state.addIneqConstraint(rightVarName, leftVarName));
        return null;
      }

    case GREATER_EQUAL:
      if (truthAssumption) {
        return state.addGreaterEqConstraint(rightVarName, leftVarName);
      } else {
        return state.addSmallerConstraint(rightVarName, leftVarName);
      }

    case GREATER_THAN:
      if (truthAssumption) {
        return state.addGreaterConstraint(rightVarName, leftVarName);
      } else {
        return state.addSmallerEqConstraint(rightVarName, leftVarName);
      }

    case LESS_EQUAL:
      if (truthAssumption) {
        return state.addSmallerEqConstraint(rightVarName, leftVarName);
      } else {
        return state.addGreaterConstraint(rightVarName, leftVarName);
      }

    case LESS_THAN:
      if (truthAssumption) {
        return state.addSmallerConstraint(rightVarName, leftVarName);
      } else {
        return state.addGreaterEqConstraint(rightVarName, leftVarName);
      }

    case NOT_EQUALS:
      if (truthAssumption) {
        possibleStates.addAll(state.addIneqConstraint(rightVarName, leftVarName));
        return null;
      } else {
        return state.addEqConstraint(rightVarName, leftVarName);
      }

    default:
      throw new CPATransferException("Unhandled case: " + binExp.getOperator());
    }
  }

  /**
   * This method handles all expressions which are assumptions without beeing
   * binary expressions (p.e if(1) or if(1+2) or if (a))
   */
  private OctState handleSingleBooleanExpression(String variableName, boolean truthAssumption, OctState state) {
    // if (a)
    if (truthAssumption) {
      possibleStates.addAll(state.addIneqConstraint(variableName, 0));
      return null;

      // if (!a)
    } else {
      return state.addEqConstraint(variableName, 0);
    }
  }

  @Override
  protected OctState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {

    CFunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    if (!cfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (parameters.size() == arguments.size());
    } else {
      assert (parameters.size() <= arguments.size());
    }

    // declare all parameters as variables
    for (int i = 0; i < parameters.size(); i++) {
      CExpression arg = arguments.get(i);
      if (!isHandleAbleType(parameters.get(i).getType())) {
        continue;
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = buildVarName(calledFunctionName, nameOfParam);

      state = state.declareVariable(formalParamName, arg.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()+1)));
    }

    return state.declareVariable(buildVarName(calledFunctionName, FUNCTION_RETURN_VAR), null);
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

      // we do not know anything about pointers, so assignments to pointers
      // are not possible for us
      if (!isHandleableVariable(op1)) {
        return state.removeLocalVars(calledFunctionName);
      }

      String returnVarName = buildVarName(calledFunctionName, FUNCTION_RETURN_VAR);

      String assignedVarName = buildVarName(op1, callerFunctionName);
      IOctCoefficients right = new OctSimpleCoefficients(state.sizeOfVariables(), state.getVariableIndexFor(returnVarName), 1);

      state = state.makeAssignment(assignedVarName, right);


    // g(b), do nothing
    } else if (exprOnSummary instanceof CFunctionCallStatement) {

    } else {
      throw new UnrecognizedCCodeException("on function return", cfaEdge, exprOnSummary);
    }

    return state.removeLocalVars(calledFunctionName);
  }

  @Override
  protected OctState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    if (cfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration declaration = (CVariableDeclaration) decl;

      // get the variable name in the declarator
      String variableName = declaration.getName();

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointeror struct variables to the list since we don't track them
      if (!isHandleAbleType(declaration.getType())) { return state; }

      // make the fullyqualifiedname
      if (!decl.isGlobal()) {
        variableName = buildVarName(functionName, variableName);
      }

      // for global declarations, there may be forwards declarations, so we do
      // not need to declarate them a second time, but if there is an initializer
      // we assign it to the before declared variable
      // another case where a variablename already exists, is if it is declarated
      // inside a loop
      boolean isDeclarationNecessary = !state.existsVariable(variableName);


      IOctCoefficients initCoeffs = null;

      CInitializer init = declaration.getInitializer();
      if (init != null) {
        if (init instanceof CInitializerExpression) {
          CExpression exp = ((CInitializerExpression) init).getExpression();

          initCoeffs = exp.accept(new COctagonCoefficientVisitor(state, isDeclarationNecessary ? state.sizeOfVariables()+1 : state.sizeOfVariables()));

          // if there is an initializerlist, the variable is either an array or a struct/union
          // we cannot handle them, so simply return the previous state
        } else if (init instanceof CInitializerList) {
          if (isDeclarationNecessary) {
            return state.declareVariable(variableName, null);
          } else {
            return state;
          }

        } else {
          throw new AssertionError("Unhandled Expression Type: " + init.getClass());
        }


        // global variables without initializer are set to 0 in C
      } else if (decl.isGlobal()) {
        initCoeffs = new OctSimpleCoefficients(isDeclarationNecessary ? state.sizeOfVariables() + 1 : state.sizeOfVariables());
      }

      if (isDeclarationNecessary) {
        return state.declareVariable(variableName, initCoeffs);
      } else if (initCoeffs != null) {
        return state.makeAssignment(variableName, initCoeffs);
      } else {
        return state.forget(variableName);
      }

    } else if (cfaEdge.getDeclaration() instanceof CTypeDeclaration
        || cfaEdge.getDeclaration() instanceof CFunctionDeclaration) { return state; }

    throw new AssertionError(cfaEdge.getDeclaration() + " (" + cfaEdge.getDeclaration().getClass() + ")");
  }

  @Override
  protected OctState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {

    boolean isNonedUInt = false;

    // check if there are functioncalls we cannot handle
    if (statement instanceof CFunctionCall) {
      CExpression fn = ((CFunctionCall)statement).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        }
        isNonedUInt = func.equals("__VERIFIER_nondet_uint");
      }
    }

    // expression is a binary operation, e.g. a = b;
    if (statement instanceof CAssignment) {
      CLeftHandSide left = ((CAssignment) statement).getLeftHandSide();
      CRightHandSide right = ((CAssignment) statement).getRightHandSide();

      IOctCoefficients coeffs = right.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()));


      String variableName = buildVarName(left, functionName);

      // as pointers do not get declarated in the beginning we can just
      // ignore them here
      if (!isHandleableVariable(left)) {
        assert !state.existsVariable(variableName) : "variablename '" + variableName + "' is in map although it can not be handled";
        return state;
      } else {
        // if we cannot determine coefficients, we cannot make any assumptions about
        // the value of the assigned variable and reset its value to unknown
        if (coeffs == null) {
          if (isNonedUInt) {
            return state.forget(variableName).makeAssignment(variableName, OctIntervalCoefficients.getUIntCoeffs(state.sizeOfVariables()));
          }
          return state.forget(variableName);
        } else {
          // TODO is assignment to field valid?
          return state.makeAssignment(variableName, coeffs);
        }
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

  private String buildVarName(CLeftHandSide left, String functionName) {
    String variableName = null;
    if (left instanceof CArraySubscriptExpression) {
      variableName = ((CArraySubscriptExpression) left).getArrayExpression().toASTString();

      if (!isGlobal(((CArraySubscriptExpression) left).getArrayExpression())) {
        variableName = buildVarName(functionName, variableName);
      }
    } else if (left instanceof CPointerExpression) {
      variableName = ((CPointerExpression) left).getOperand().toASTString();

      if (!isGlobal(((CPointerExpression) left).getOperand())) {
        variableName = buildVarName(functionName, variableName);
      }
    } else if (left instanceof CFieldReference) {
      variableName = ((CFieldReference) left).getFieldOwner().toASTString();

      if (!isGlobal(((CFieldReference) left).getFieldOwner())) {
        variableName = buildVarName(functionName, variableName);
      }
    } else {
      variableName = ((CIdExpression) left).toASTString();

      if (!isGlobal(left)) {
        variableName = buildVarName(functionName, variableName);
      }
    }

    return variableName;
  }

  /**
   * This is a return statement in a function
   */
  @Override
  protected OctState handleReturnStatementEdge(CReturnStatementEdge cfaEdge, @Nullable CExpression expression)
      throws CPATransferException {

    // this is for functions without return value, which just have returns
    // in them to end the function
    if (expression == null) {
      return state;
    }

    String tempVarName = buildVarName(cfaEdge.getPredecessor().getFunctionName(), FUNCTION_RETURN_VAR);
    IOctCoefficients coeffs = expression.accept(new COctagonCoefficientVisitor(state, state.sizeOfVariables()));

    // main function has no __cpa_temp_result_var as the result of the main function
    // is not important for us, we skip here
    if (!state.existsVariable(tempVarName)) {
      return state;
    }

    if (coeffs == null) {
      return state.forget(tempVarName);
    } else {
      return state.makeAssignment(tempVarName, coeffs);
    }
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

  class COctagonCoefficientVisitor extends DefaultCExpressionVisitor<IOctCoefficients, CPATransferException>
      implements CRightHandSideVisitor<IOctCoefficients, CPATransferException> {

    private OctState coeffState;
    private int nofVariables;

    /**
     * This method creates the Visitor, which evaluates all coefficients for a given
     * Expression.
     *
     * @param state
     */
    public COctagonCoefficientVisitor(OctState state, int nofVariables) {
      this.coeffState = state;
      this.nofVariables = nofVariables;
    }


    @Override
    protected IOctCoefficients visitDefault(CExpression pExp) throws CPATransferException {
      return null;
    }

    @Override
    public IOctCoefficients visit(CArraySubscriptExpression e) throws CPATransferException {
      // TODO check if we can handle array expressions
      return null;
    }

    @Override
    public IOctCoefficients visit(CBinaryExpression e) throws CPATransferException {
      IOctCoefficients left = e.getOperand1().accept(this);
      IOctCoefficients right = e.getOperand2().accept(this);

      if (left == null || right == null) { return null; }

      switch (e.getOperator()) {
      case BINARY_AND:
        return left.binAnd(right);
      case BINARY_OR:
        return left.binOr(right);
      case BINARY_XOR:
        return left.binXOr(right);
      case EQUALS:
        if (left.hasOnlyConstantValue() && right.hasOnlyConstantValue()) {
          return left.eq(right);
        }
        if ((e.getOperand1() instanceof CIdExpression
            || e.getOperand1() instanceof CIntegerLiteralExpression
            || e.getOperand1() instanceof CCharLiteralExpression)
           && (e.getOperand2() instanceof CIdExpression
            || e.getOperand2() instanceof CIntegerLiteralExpression
            || e.getOperand2() instanceof CCharLiteralExpression)
            ) {
          if (e.getOperand1() instanceof CIdExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), buildVarName((CIdExpression) e.getOperand1(), functionName)).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (state.addEqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CCharLiteralExpression)e.getOperand2()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (state.addEqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CIntegerLiteralExpression)e.getOperand2()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else if (e.getOperand1() instanceof CIntegerLiteralExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CIntegerLiteralExpression)e.getOperand1()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() != ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() != ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CCharLiteralExpression)e.getOperand1()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CCharLiteralExpression)e.getOperand1()).getValue() != ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CCharLiteralExpression)e.getOperand1()).getValue() != ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          }
        }
        return null;
      case GREATER_EQUAL:
        if (left.hasOnlyConstantValue() && right.hasOnlyConstantValue()) {
          return left.greaterEq(right);
        }
        if ((e.getOperand1() instanceof CIdExpression
            || e.getOperand1() instanceof CIntegerLiteralExpression
            || e.getOperand1() instanceof CCharLiteralExpression)
           && (e.getOperand2() instanceof CIdExpression
            || e.getOperand2() instanceof CIntegerLiteralExpression
            || e.getOperand2() instanceof CCharLiteralExpression)
            ) {
          if (e.getOperand1() instanceof CIdExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addGreaterEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), buildVarName((CIdExpression) e.getOperand1(), functionName)).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (state.addGreaterEqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CCharLiteralExpression)e.getOperand2()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (state.addGreaterEqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CIntegerLiteralExpression)e.getOperand2()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else if (e.getOperand1() instanceof CIntegerLiteralExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addSmallerConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CIntegerLiteralExpression)e.getOperand1()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() < ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() < ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addSmallerConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CCharLiteralExpression)e.getOperand1()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CCharLiteralExpression)e.getOperand1()).getValue() < ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CCharLiteralExpression)e.getOperand1()).getValue() < ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          }
        }
        return null;
      case GREATER_THAN:
        if (left.hasOnlyConstantValue() && right.hasOnlyConstantValue()) {
          return left.greater(right);
        }
        if ((e.getOperand1() instanceof CIdExpression
            || e.getOperand1() instanceof CIntegerLiteralExpression
            || e.getOperand1() instanceof CCharLiteralExpression)
           && (e.getOperand2() instanceof CIdExpression
            || e.getOperand2() instanceof CIntegerLiteralExpression
            || e.getOperand2() instanceof CCharLiteralExpression)
            ) {
          if (e.getOperand1() instanceof CIdExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addGreaterConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), buildVarName((CIdExpression) e.getOperand1(), functionName)).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (state.addGreaterConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CCharLiteralExpression)e.getOperand2()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (state.addGreaterConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CIntegerLiteralExpression)e.getOperand2()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else if (e.getOperand1() instanceof CIntegerLiteralExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addSmallerEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CIntegerLiteralExpression)e.getOperand1()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() <= ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() <= ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addSmallerEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CCharLiteralExpression)e.getOperand1()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CCharLiteralExpression)e.getOperand1()).getValue() <= ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CCharLiteralExpression)e.getOperand1()).getValue() <= ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          }
        }
        return null;
      case LESS_EQUAL:
        if (left.hasOnlyConstantValue() && right.hasOnlyConstantValue()) {
          return left.smallerEq(right);
        }
        if ((e.getOperand1() instanceof CIdExpression
            || e.getOperand1() instanceof CIntegerLiteralExpression
            || e.getOperand1() instanceof CCharLiteralExpression)
           && (e.getOperand2() instanceof CIdExpression
            || e.getOperand2() instanceof CIntegerLiteralExpression
            || e.getOperand2() instanceof CCharLiteralExpression)
            ) {
          if (e.getOperand1() instanceof CIdExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addSmallerEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), buildVarName((CIdExpression) e.getOperand1(), functionName)).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (state.addSmallerEqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CCharLiteralExpression)e.getOperand2()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (state.addSmallerEqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CIntegerLiteralExpression)e.getOperand2()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else if (e.getOperand1() instanceof CIntegerLiteralExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addGreaterConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CIntegerLiteralExpression)e.getOperand1()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() > ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() > ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addGreaterConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CCharLiteralExpression)e.getOperand1()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CCharLiteralExpression)e.getOperand1()).getValue() > ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CCharLiteralExpression)e.getOperand1()).getValue() > ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          }
        }
        return null;
      case LESS_THAN:
        if (left.hasOnlyConstantValue() && right.hasOnlyConstantValue()) {
          return left.smaller(right);
        }
        if ((e.getOperand1() instanceof CIdExpression
            || e.getOperand1() instanceof CIntegerLiteralExpression
            || e.getOperand1() instanceof CCharLiteralExpression)
           && (e.getOperand2() instanceof CIdExpression
            || e.getOperand2() instanceof CIntegerLiteralExpression
            || e.getOperand2() instanceof CCharLiteralExpression)
            ) {
          if (e.getOperand1() instanceof CIdExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addSmallerConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName) , buildVarName((CIdExpression) e.getOperand1(), functionName)).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (state.addSmallerConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CCharLiteralExpression)e.getOperand2()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (state.addSmallerConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CIntegerLiteralExpression)e.getOperand2()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else if (e.getOperand1() instanceof CIntegerLiteralExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addGreaterEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CIntegerLiteralExpression)e.getOperand1()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() >= ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() >= ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addGreaterEqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CCharLiteralExpression)e.getOperand1()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CCharLiteralExpression)e.getOperand1()).getValue() >= ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CCharLiteralExpression)e.getOperand1()).getValue() >= ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          }
        }
        return null;
      case NOT_EQUALS:
        if (left.hasOnlyConstantValue() && right.hasOnlyConstantValue()) {
          return left.ineq(right);
        }
        if ((e.getOperand1() instanceof CIdExpression
            || e.getOperand1() instanceof CIntegerLiteralExpression
            || e.getOperand1() instanceof CCharLiteralExpression)
           && (e.getOperand2() instanceof CIdExpression
            || e.getOperand2() instanceof CIntegerLiteralExpression
            || e.getOperand2() instanceof CCharLiteralExpression)
            ) {
          if (e.getOperand1() instanceof CIdExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addIneqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), buildVarName((CIdExpression) e.getOperand1(), functionName)).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (state.addIneqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CCharLiteralExpression)e.getOperand2()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (state.addIneqConstraint(buildVarName((CIdExpression) e.getOperand1(), functionName), ((CIntegerLiteralExpression)e.getOperand2()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else if (e.getOperand1() instanceof CIntegerLiteralExpression) {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addIneqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CIntegerLiteralExpression)e.getOperand1()).asLong()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() == ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CIntegerLiteralExpression)e.getOperand1()).asLong() == ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          } else {
            if (e.getOperand2() instanceof CIdExpression) {
              if (state.addIneqConstraint(buildVarName((CIdExpression) e.getOperand2(), functionName), ((CCharLiteralExpression)e.getOperand1()).getValue()).isEmpty()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else if (e.getOperand2() instanceof CCharLiteralExpression){
              if (((CCharLiteralExpression)e.getOperand1()).getValue() == ((CCharLiteralExpression)e.getOperand2()).getValue()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            } else {
              if (((CCharLiteralExpression)e.getOperand1()).getValue() == ((CIntegerLiteralExpression)e.getOperand2()).asLong()) {
                return new OctSimpleCoefficients(nofVariables);
              } else {
                return new OctSimpleCoefficients(nofVariables, 1);
              }
            }
          }
        }
        return null;
      case SHIFT_LEFT:
        return left.shiftLeft(right);
      case SHIFT_RIGHT:
        return left.shiftRight(right);
      case MODULO:
        return left.modulo(right);
      case MINUS:
        return left.sub(right);
      case PLUS:
        return left.add(right);
      case DIVIDE:
        return left.div(right);
      case MULTIPLY:
        return left.mult(right);

      default:
        throw new AssertionError("Unhandled case statement");
      }
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public IOctCoefficients visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    /**
     * Ignore complex casts
     */
    @Override
    public IOctCoefficients visit(CComplexCastExpression e) throws CPATransferException {
      return null;
    }

    @Override
    public IOctCoefficients visit(CFieldReference e) throws CPATransferException {
      return null;
    }

    @Override
    public IOctCoefficients visit(CIdExpression e) throws CPATransferException {
      String varName = buildVarName(e, functionName);
      Integer varIndex = coeffState.getVariableIndexFor(varName);
      if (varIndex == null) { return null; }
      IOctCoefficients oct = coeffState.getVariableToCoeffMap().get(varName);
      if (oct != null && oct.hasOnlyConstantValue()) {
        if (oct.size() == nofVariables) {
          return oct;
        } else {
          return oct.fitToSize(nofVariables);
        }
      }
      return new OctSimpleCoefficients(nofVariables, varIndex, 1);
    }

    @Override
    public IOctCoefficients visit(CCharLiteralExpression e) throws CPATransferException {
      return new OctSimpleCoefficients(nofVariables, e.getValue());
    }

    /**
     * Ignore ImaginaryExpressions
     */
    @Override
    public IOctCoefficients visit(CImaginaryLiteralExpression e) throws CPATransferException {
      return null;
    }

    /**
     * Floats can currently not be handled
     */
    @Override
    public IOctCoefficients visit(CFloatLiteralExpression e) throws CPATransferException {
      // TODO check if we can handle floats, too
      return null;
    }

    @Override
    public IOctCoefficients visit(CIntegerLiteralExpression e) throws CPATransferException {
      return new OctSimpleCoefficients(nofVariables, (int) e.asLong());
    }

    /**
     * Strings cannot be handled
     */
    @Override
    public IOctCoefficients visit(CStringLiteralExpression e) throws CPATransferException {
      return null;
    }


    @Override
    public IOctCoefficients visit(CTypeIdExpression e) throws CPATransferException {
      return null;
    }

    @Override
    public IOctCoefficients visit(CTypeIdInitializerExpression e) throws CPATransferException {
      return null;
    }

    @Override
    public IOctCoefficients visit(CUnaryExpression e) throws CPATransferException {
      IOctCoefficients operand = e.getOperand().accept(this);

      if (operand == null) { return null; }

      switch (e.getOperator()) {
      case AMPER:
      case SIZEOF:
      case TILDE:
        return null;
      case NOT:
        if (operand.hasOnlyConstantValue()) {
          return operand.eq(new OctSimpleCoefficients(nofVariables));
        }
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
    public IOctCoefficients visit(CPointerExpression e) throws CPATransferException {
      return null;
    }


    @Override
    public IOctCoefficients visit(CFunctionCallExpression e) throws CPATransferException {
      if (e.getFunctionNameExpression() instanceof CIdExpression) {
        if (((CIdExpression)e.getFunctionNameExpression()).getName().equals("__VERIFIER_nondet_uint")) {
          return OctIntervalCoefficients.getUIntCoeffs(nofVariables+1);
        }
      }
      return null;
    }
  }
}
