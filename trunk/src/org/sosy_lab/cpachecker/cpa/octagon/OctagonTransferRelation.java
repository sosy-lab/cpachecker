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
package org.sosy_lab.cpachecker.cpa.octagon;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonState.Type;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.IOctagonCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctagonIntervalCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctagonSimpleCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.coefficients.OctagonUniversalCoefficients;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonDoubleValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonIntValue;
import org.sosy_lab.cpachecker.cpa.octagon.values.OctagonNumericValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

@SuppressWarnings("rawtypes")
public class OctagonTransferRelation extends ForwardingTransferRelation<Collection<OctagonState>, OctagonState, VariableTrackingPrecision> {

  private static final String TEMP_VAR_PREFIX = "___cpa_temp_var_";

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
      = ImmutableMap.of();

  private final LogManager logger;

  private final Set<CFANode> loopHeads;

  public OctagonTransferRelation(LogManager log, LoopStructure loops) {
    logger = log;

    Builder<CFANode> builder = new ImmutableSet.Builder<>();
    for (Loop l : loops.getAllLoops()) {
      // function edges do not count as incoming/outgoing edges
          builder.addAll(l.getLoopHeads());
    }
    loopHeads = builder.build();
  }

  @Override
  protected Collection<OctagonState> postProcessing(Collection<OctagonState> successors, CFAEdge edge) {
    assert !successors.contains(null); // TODO is this assertion equal to next line?
    assert !successors.removeAll(Collections.singleton(null));

    // remove all states whose constraints cannot be satisfied
    Iterator<OctagonState> states = successors.iterator();
    while (states.hasNext()) {
      OctagonState st = states.next();
      if (st.isEmpty()) {
        states.remove();
        logger.log(Level.FINER, "removing state because of unsatisfiable constraints:\n" +
                                 st + "________________\nEdge was:\n" + edge.getDescription());
      }
    }

    Set<OctagonState> cleanedUpStates = new HashSet<>();
    // TODO overapproximation here, we should not need to remove those vars
    // instead it would be much better if we could omit creating them, p.e. through
    // creating the temporary vars in the cfa, before analyzing the program
    for (OctagonState st : successors) {
      cleanedUpStates.add(st.removeTempVars(functionName, TEMP_VAR_PREFIX));
    }

    if (loopHeads.contains(edge.getSuccessor())) {
      Set<OctagonState> newStates = new HashSet<>();
      for (OctagonState s : cleanedUpStates) {
        newStates.add(s.asLoopHead());
      }
      cleanedUpStates = newStates;
    }

    return cleanedUpStates;
  }

  @Override
  protected Set<OctagonState> handleBlankEdge(BlankEdge cfaEdge) {
    return Collections.singleton(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected Set<OctagonState> handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // Binary operation
    if (expression instanceof CBinaryExpression) {
      return handleBinaryBooleanExpression((CBinaryExpression) expression, truthAssumption,state);

      // Unary operation
    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExp = ((CUnaryExpression) expression);

      switch (unaryExp.getOperator()) {
        // do not change anything besides the expression, minus has no effect
        // on the == 0 equality check
      case MINUS:
        return handleAssumption(cfaEdge, unaryExp.getOperand(), truthAssumption);

        // TODO check if some cases could be handled
      case SIZEOF:
      case TILDE:
      case AMPER:
        return Collections.singleton(state);
      default:
        throw new CPATransferException("Unhandled case: " + unaryExp.getOperator());
      }

      // An expression which cannot be simplified anymore
    } else if (expression instanceof CIdExpression
        || expression instanceof CFieldReference
        || (expression instanceof CPointerExpression && ((CPointerExpression) expression).getOperand() instanceof CIdExpression)) {
      if (isHandleableVariable(expression)) {
        MemoryLocation varName = buildVarName((CLeftHandSide) expression, functionName);
        return handleSingleBooleanExpression(varName, truthAssumption, state);
      } else {
        return Collections.singleton(state);
      }

      // A constant value
    } else if (expression instanceof CLiteralExpression) {
      if (expression instanceof CIntegerLiteralExpression) {
        return handleLiteralBooleanExpression(((CIntegerLiteralExpression) expression).asLong(), truthAssumption, state);

      } else if (expression instanceof CCharLiteralExpression) {
        return handleLiteralBooleanExpression(((CCharLiteralExpression) expression).getCharacter(), truthAssumption, state);

      } else if (expression instanceof CFloatLiteralExpression) {
        // only when the float is exactly zero the condition is wrong, for all other float values it is true
        int val = Math.abs(((CFloatLiteralExpression)expression).getValue().signum());
        return handleLiteralBooleanExpression(val, truthAssumption, state);
      } else {
        return Collections.singleton(state);
      }

      // a cast, we ignore this cast and call this method again with the casts operand
    } else if (expression instanceof CCastExpression) {
      return handleAssumption(cfaEdge, ((CCastExpression) expression).getOperand(), truthAssumption);

    } else {
      throw new UnrecognizedCCodeException("Unknown expression type in assumption", cfaEdge, expression);
    }
  }

  /**
   * If only one literal is the complete boolean expression, we only need to check
   * this literal if it is equal to zero, depending on the truth assumption we
   * either return the unchanged state or null if the following branch is not reachable.
   *
   * @param value The long value of the CLiteralExpression
   * @param truthAssumption indicates if we are in the then or else branch of the assumption
   * @return an OctState or null
   */
  private Set<OctagonState> handleLiteralBooleanExpression(long value, boolean truthAssumption, OctagonState state) {
    if ((value == 0) == truthAssumption) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(state);
    }
  }

  private OctagonState.Type getCorrespondingOctStateType(CType type) {
    if (type instanceof CSimpleType &&
        (((CSimpleType)type).getType() == CBasicType.FLOAT
          || ((CSimpleType)type).getType() == CBasicType.DOUBLE)) {
      return Type.FLOAT;
    } else {
      return Type.INT;
    }
  }

  /**
   * This method handles all binary boolean expressions.
   */
  private Set<OctagonState> handleBinaryBooleanExpression(CBinaryExpression binExp, boolean truthAssumption, OctagonState state) throws CPATransferException {

    // IMPORTANT: for this switch we assume that in each conditional statement, there is only one
    // condition, (this simplification is added in the cfa creation phase)
    switch (binExp.getOperator()) {
    // TODO check which cases can be handled
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
    case MODULO:
      return Collections.singleton(state);

    // for the following cases we first create a temporary variable where
    // the result of the operation is saved, afterwards, the equality with == 0
    // is checked
    case MINUS:
    case PLUS:
    case MULTIPLY:
      case DIVIDE:
        MemoryLocation tempVarName =
            MemoryLocation.valueOf(functionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_");
        temporaryVariableCounter++;
        COctagonCoefficientVisitor coeffVisitor =
            new COctagonCoefficientVisitor(state, functionName);
        Set<Pair<IOctagonCoefficients, OctagonState>> coeffsList = binExp.accept(coeffVisitor);
        Set<OctagonState> possibleStates = new HashSet<>();
        for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsList) {
        IOctagonCoefficients coeffs = pairs.getFirst();

        // we have an undefined value, so there is no need to make any assumptions about it
        if (coeffs.equals(OctagonUniversalCoefficients.INSTANCE)) {
          return Collections.singleton(state);
        }

        OctagonState tmp = pairs.getSecond().declareVariable(tempVarName,
                                                         getCorrespondingOctStateType(binExp.getExpressionType()));
        tmp = tmp.makeAssignment(tempVarName, coeffs.expandToSize(tmp.sizeOfVariables(), tmp));
        possibleStates.addAll(handleSingleBooleanExpression(tempVarName,
                                                         truthAssumption,
                                                         tmp));
      }
      return possibleStates;

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
        return Collections.singleton(state);
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
  private Set<OctagonState> handleBinaryAssumptionWithLiteral(CExpression left, CExpression right, BinaryOperator op,
      boolean truthAssumption, OctagonState state) throws CPATransferException {

    // we cannot cope with string literals so we do not know anything about the assumption
    // => just return the previous state
    if (left instanceof CStringLiteralExpression || right instanceof CStringLiteralExpression) { return Collections.singleton(state); }

    // both are literals
    if (left instanceof CLiteralExpression && right instanceof CLiteralExpression) {
      return handleBinaryAssumptionWithTwoLiterals((CLiteralExpression)left, (CLiteralExpression)right, op, truthAssumption);
    } else if (left instanceof CLiteralExpression) {
      //change operator so we can call handleBinaryAssumptionWithOneLiteral
      switch (op) {
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
        break;

      // we do not need to change the binary operator for other cases
      // (== and != stay the same when swapping the operands)
      default:
          break;
      }
      return handleBinaryAssumptionWithOneLiteral(right, (CLiteralExpression) left, op, truthAssumption, state);

      // literal is on the right position, variable on the left;
    } else if (right instanceof CLiteralExpression) {
      return handleBinaryAssumptionWithOneLiteral(left, (CLiteralExpression) right, op, truthAssumption, state);
    }

    // if we did not return anything up to now we were not able to handle it
    // => just return the previous state
    return Collections.singleton(state);
  }

  private boolean isHandleableVariable(CExpression var) {
    if (var instanceof CArraySubscriptExpression
        || var instanceof CFieldReference
        || var instanceof CPointerExpression
        || var instanceof CStringLiteralExpression
        || (var instanceof CFieldReference && ((CFieldReference)var).isPointerDereference())) {
      return false;
    }
    return isHandleAbleType(var.getExpressionType());
  }

  private boolean isHandleAbleType(CType type) {
    type = type.getCanonicalType();
    while (type instanceof CTypedefType) {
      type = ((CTypedefType) type).getRealType();
    }
    if (type instanceof CPointerType
        || type instanceof CCompositeType
        || type instanceof CArrayType) {
      return false;
    }

    return true;
  }

  /**
   * This method assumes the literal is on the righthandside! So take care while
   * calling this method, and if necessary change the operator to its opposite.
   * (p.e. a < 4)
   */
  private Set<OctagonState> handleBinaryAssumptionWithOneLiteral(CExpression left, CLiteralExpression right, BinaryOperator op,
      boolean truthAssumption, OctagonState state) throws CPATransferException {

    // we cannot handle pointers, so just ignore them
    if (left.getExpressionType() instanceof CPointerType
        || (left instanceof CFieldReference && ((CFieldReference) left).isPointerDereference())) {
      return Collections.singleton(state);
    }

    MemoryLocation leftVarName = null;
    List<OctagonState> states = new ArrayList<>();
    states.add(state);

    // check left side
    if (left instanceof CIdExpression || left instanceof CFieldReference) {
      leftVarName = buildVarName((CLeftHandSide) left, functionName);

      // create a temp var for the left side of the expression
    } else {
      COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(state, functionName);
      Set<Pair<IOctagonCoefficients, OctagonState>> coeffsLeft = left.accept(coeffVisitor);

      MemoryLocation tempLeft =
          MemoryLocation.valueOf(functionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      List<OctagonState> tmpList = new ArrayList<>();
      for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsLeft) {
        IOctagonCoefficients coeffs = pairs.getFirst();

        // we cannot do any comparison with an unknown value, so just quit here
        if (coeffs.equals(OctagonUniversalCoefficients.INSTANCE)) {
          return Collections.singleton(state);
        }

        OctagonState tmpState = pairs.getSecond().declareVariable(tempLeft, getCorrespondingOctStateType(left.getExpressionType()));
        tmpList.add(tmpState.makeAssignment(tempLeft, coeffs.expandToSize(tmpState.sizeOfVariables(), tmpState)));
      }
      states = tmpList;
      leftVarName = tempLeft;
    }

    OctagonNumericValue rightVal = OctagonIntValue.ZERO;
    if (right instanceof CIntegerLiteralExpression) {
      rightVal = OctagonIntValue.of(((CIntegerLiteralExpression) right).asLong());
    } else if (right instanceof CCharLiteralExpression) {
      rightVal = OctagonIntValue.of(((CCharLiteralExpression) right).getCharacter());
    } else if (right instanceof CFloatLiteralExpression) {
      rightVal = new OctagonDoubleValue(((CFloatLiteralExpression) right).getValue().doubleValue());

    // we cannot handle strings, so just return the previous state
    } else {
      return Collections.singleton(state);
    }

    Set<OctagonState> possibleStates = new HashSet<>();
    for (OctagonState actState : states) {
      switch (op) {
      case EQUALS:
        if (truthAssumption) {
          possibleStates.add(actState.addEqConstraint(leftVarName, rightVal));
        } else {
          possibleStates.addAll(actState.addIneqConstraint(leftVarName, rightVal));
        }
        break;

      case NOT_EQUALS:
        if (truthAssumption) {
          possibleStates.addAll(actState.addIneqConstraint(leftVarName, rightVal));
        } else {
          possibleStates.add(actState.addEqConstraint(leftVarName, rightVal));
        }
        break;

      case LESS_EQUAL:
        if (truthAssumption) {
          possibleStates.add(actState.addSmallerEqConstraint(leftVarName, rightVal));
        } else {
          possibleStates.add(actState.addGreaterConstraint(leftVarName, rightVal));
        }
        break;

      case LESS_THAN:
        if (truthAssumption) {
          possibleStates.add(actState.addSmallerConstraint(leftVarName, rightVal));
        } else {
          possibleStates.add(actState.addGreaterEqConstraint(leftVarName, rightVal));
        }
        break;

      case GREATER_EQUAL:
        if (truthAssumption) {
          possibleStates.add(actState.addGreaterEqConstraint(leftVarName, rightVal));
        } else {
          possibleStates.add(actState.addSmallerConstraint(leftVarName, rightVal));
        }
        break;

      case GREATER_THAN:
        if (truthAssumption) {
          possibleStates.add(actState.addGreaterConstraint(leftVarName, rightVal));
        } else {
          possibleStates.add(actState.addSmallerEqConstraint(leftVarName, rightVal));
        }
        break;

      default:
        throw new CPATransferException("Unhandled case statement: " + op);
      }
    }

    return possibleStates;
  }

  /**
   * This method handles binary assumptions with two literals (p.e. 1 < 3). As
   * this method is only a submethod of handleBinaryAssumptionWithOneLiteral it
   * assumes that the literal is eiter a CIntegerLiteralExpression or
   * a CCharLiteralExpression.
   */
  private Set<OctagonState> handleBinaryAssumptionWithTwoLiterals(CLiteralExpression left, CLiteralExpression right, BinaryOperator op,
      boolean truthAssumption) {
    OctagonNumericValue leftVal = OctagonIntValue.ZERO;
    if (left instanceof CIntegerLiteralExpression) {
      leftVal = OctagonIntValue.of(((CIntegerLiteralExpression) left).asLong());
    } else if (left instanceof CCharLiteralExpression) {
      leftVal = OctagonIntValue.of(((CCharLiteralExpression) left).getCharacter());
    } else if (left instanceof CFloatLiteralExpression) {
      leftVal = new OctagonDoubleValue(((CFloatLiteralExpression)left).getValue().doubleValue());
    }

    OctagonNumericValue rightVal = OctagonIntValue.ZERO;
    if (right instanceof CIntegerLiteralExpression) {
      rightVal = OctagonIntValue.of(((CIntegerLiteralExpression) right).asLong());
    } else if (right instanceof CCharLiteralExpression) {
      rightVal = OctagonIntValue.of(((CCharLiteralExpression) right).getCharacter());
    } else if (right instanceof CFloatLiteralExpression) {
      rightVal = new OctagonDoubleValue(((CFloatLiteralExpression)right).getValue().doubleValue());
    }

    if (truthAssumption == isOperatorSatisfied(op, leftVal, rightVal)) {
      return Collections.singleton(state);
    } else {
      return Collections.emptySet();
    }
  }

  private static boolean isOperatorSatisfied(final BinaryOperator op,
      final OctagonNumericValue leftVal, final OctagonNumericValue rightVal) {
    switch (op) {
    case EQUALS:
      return leftVal.isEqual(rightVal);
    case GREATER_EQUAL:
      return leftVal.greaterEqual(rightVal);
    case GREATER_THAN:
      return leftVal.greaterThan(rightVal);
    case LESS_EQUAL:
      return leftVal.lessEqual(rightVal);
    case LESS_THAN:
      return leftVal.lessThan(rightVal);
    case NOT_EQUALS:
      return !leftVal.isEqual(rightVal);
    default:
      throw new AssertionError("Unhandled case: " + op);
    }
  }

  /**
   * This method handles all binary assumptions without literals (p.e. a < b)
    */
  private Set<OctagonState> handleBinaryAssumptionWithoutLiteral(CBinaryExpression binExp, boolean truthAssumption,
      CExpression left, CExpression right, OctagonState state)
      throws CPATransferException {
    CBinaryExpression.BinaryOperator op = binExp.getOperator();
    MemoryLocation leftVarName = null;
    MemoryLocation rightVarName = null;

    // we cannot handle pointers, so just ignore them
    // TODO make program unsafe?
    if (!isHandleableVariable(left) || !isHandleableVariable(right)) {
      return Collections.singleton(state);
    }

    Set<OctagonState> states = new HashSet<>();
    states.add(state);

    // check left side
    if (left instanceof CIdExpression || left instanceof CFieldReference) {
      leftVarName = buildVarName((CLeftHandSide) left, functionName);

      // create a temp var for the left side of the expression
    } else {
      COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(state, functionName);
      Set<Pair<IOctagonCoefficients, OctagonState>> coeffsLeft = left.accept(coeffVisitor);

      MemoryLocation tempLeft =
          MemoryLocation.valueOf(functionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_");
      temporaryVariableCounter++;
      Set<OctagonState> tmpSet = new HashSet<>();
      for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsLeft) {
        IOctagonCoefficients coeffs = pairs.getFirst();

        // we cannot do any comparison with an unknown value, so just quit here
        if (coeffs.equals(OctagonUniversalCoefficients.INSTANCE)) {
          return Collections.singleton(state);
        }

        OctagonState tmp = pairs.getSecond().declareVariable(tempLeft, getCorrespondingOctStateType(left.getExpressionType()));
        tmpSet.add(tmp.makeAssignment(tempLeft, coeffs.expandToSize(tmp.sizeOfVariables(), tmp)));
      }
      states = tmpSet;
      leftVarName = tempLeft;
    }

    // check right side
    if (right instanceof CIdExpression || right instanceof CFieldReference) {
      rightVarName = buildVarName((CLeftHandSide) right, functionName);

      // create a temp var for the right side of the expression
    } else {

      // create the temp var name for the right side of the expression before the loop
      // so we have the same name everywhere for the variable
      MemoryLocation tempRight = MemoryLocation.valueOf(functionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_", 0);
      temporaryVariableCounter++;
      Set<OctagonState> tmpSet = new HashSet<>();

      for (OctagonState st : states) {
        COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(st, functionName);
        Set<Pair<IOctagonCoefficients, OctagonState>> coeffsRight = right.accept(coeffVisitor);

        for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsRight) {
          IOctagonCoefficients coeffs = pairs.getFirst();

          // we cannot do any comparison with an unknown value, so just quit here
          if (coeffs.equals(OctagonUniversalCoefficients.INSTANCE)) {
            return Collections.singleton(state);
          }

          OctagonState tmp = pairs.getSecond().declareVariable(tempRight, getCorrespondingOctStateType(right.getExpressionType()));
          tmpSet.add(tmp.makeAssignment(tempRight, coeffs.expandToSize(tmp.sizeOfVariables(), tmp)));
        }
      }
      states = tmpSet;
      rightVarName = tempRight;
    }

    // performance optimization, this is something like x == x
    if (leftVarName.equals(rightVarName)) {
      switch (op) {
      case EQUALS:
      case GREATER_EQUAL:
      case LESS_EQUAL:
       if (truthAssumption) {
         return Collections.singleton(state);
       } else {
         return Collections.emptySet();
       }
      case NOT_EQUALS:
      case LESS_THAN:
      case GREATER_THAN:
        if (truthAssumption) {
          return Collections.emptySet();
        } else {
          return Collections.singleton(state);
        }
      default:
      }
    }

    Set<OctagonState> possibleStates = new HashSet<>();
    // Comparison part, left and right are now definitely available
    for (OctagonState actState : states) {
      switch (op) {
      case EQUALS:
        if (truthAssumption) {
          possibleStates.add(actState.addEqConstraint(rightVarName, leftVarName));
        } else {
          possibleStates.addAll(actState.addIneqConstraint(rightVarName, leftVarName));
        }
        break;

      case GREATER_EQUAL:
        if (truthAssumption) {
          possibleStates.add(actState.addGreaterEqConstraint(rightVarName, leftVarName));
        } else {
          possibleStates.add(actState.addSmallerConstraint(rightVarName, leftVarName));
        }
        break;

      case GREATER_THAN:
        if (truthAssumption) {
          possibleStates.add(actState.addGreaterConstraint(rightVarName, leftVarName));
        } else {
          possibleStates.add(actState.addSmallerEqConstraint(rightVarName, leftVarName));
        }
        break;

      case LESS_EQUAL:
        if (truthAssumption) {
          possibleStates.add(actState.addSmallerEqConstraint(rightVarName, leftVarName));
        } else {
          possibleStates.add(actState.addGreaterConstraint(rightVarName, leftVarName));
        }
        break;

      case LESS_THAN:
        if (truthAssumption) {
          possibleStates.add(actState.addSmallerConstraint(rightVarName, leftVarName));
        } else {
          possibleStates.add(actState.addGreaterEqConstraint(rightVarName, leftVarName));
        }
        break;

      case NOT_EQUALS:
        if (truthAssumption) {
          possibleStates.addAll(actState.addIneqConstraint(rightVarName, leftVarName));
        } else {
          possibleStates.add(actState.addEqConstraint(rightVarName, leftVarName));
        }
        break;

      default:
        throw new CPATransferException("Unhandled case: " + binExp.getOperator());
      }
    }

    return possibleStates;
  }

  /**
   * This method handles all expressions which are assumptions without beeing
   * binary expressions (p.e if(1) or if(1+2) or if (a))
   */
  private Set<OctagonState> handleSingleBooleanExpression(MemoryLocation variableName, boolean truthAssumption, OctagonState state) {
    // if (a)
    if (truthAssumption) {
      return state.addIneqConstraint(variableName, OctagonIntValue.ZERO);

      // if (!a)
    } else {
      return Collections.singleton(state.addEqConstraint(variableName, OctagonIntValue.ZERO));
    }
  }

  @Override
  protected Set<OctagonState> handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {

    CFunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    CFunctionType functionType = cfaEdge.getSuccessor().getFunctionDefinition().getType();

    if (!functionType.takesVarArgs()) {
      assert parameters.size() == arguments.size();
    } else {
      assert parameters.size() <= arguments.size();
    }

    Set<OctagonState> possibleStates = new HashSet<>();

    CType returnType = functionType.getReturnType().getCanonicalType();
    if (isHandleAbleType(returnType)
        && !(returnType instanceof CVoidType)) {
      state =
          state.declareVariable(
              MemoryLocation.valueOf(
                  calledFunctionName, functionEntryNode.getReturnVariable().get().getName()),
              getCorrespondingOctStateType(
                  cfaEdge.getSuccessor().getFunctionDefinition().getType().getReturnType()));
    }

    List<Pair<MemoryLocation, CExpression>> handleAbleParams = new LinkedList<>();

    // declare all parameters as variables
    for (int i = 0; i < parameters.size(); i++) {
      if (!isHandleAbleType(parameters.get(i).getType())) {
        continue;
      }

      MemoryLocation nameOfParam = MemoryLocation.valueOf(calledFunctionName, paramNames.get(i));
      CType typeOfParam = parameters.get(i).getType();

      if (!precision.isTracking(nameOfParam, typeOfParam, functionEntryNode)
          || !isHandleAbleType(parameters.get(i).getType())) {
        continue;
      }

      state = state.declareVariable(nameOfParam, getCorrespondingOctStateType(typeOfParam));
      handleAbleParams.add(Pair.of(nameOfParam, arguments.get(i)));
    }

    possibleStates.add(state);
    for (Pair<MemoryLocation, CExpression> pair : handleAbleParams) {
      MemoryLocation paramName = pair.getFirst();
      CExpression argument = pair.getSecond();

      Set<OctagonState> newPossibleStates = new HashSet<>();
      for (OctagonState st : possibleStates) {

        // create the value assigned to the parameter
        COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(st, calledFunctionName);
        Set<Pair<IOctagonCoefficients, OctagonState>> coeffsList = argument.accept(coeffVisitor);

        // create new states for all possible parameter values
        for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsList) {
          newPossibleStates.add(pairs.getSecond().makeAssignment(paramName, pairs.getFirst()));
        }
      }
      possibleStates = newPossibleStates;
    }

    return possibleStates;
  }

  @Override
  protected Set<OctagonState> handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    CFunctionCall exprOnSummary = fnkCall.getExpression();

    String calledFunctionName = cfaEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement) exprOnSummary);
      CLeftHandSide op1 = binExp.getLeftHandSide();
      MemoryLocation assignedVarName = buildVarName(op1, callerFunctionName);

      // we do not know anything about pointers, so assignments to pointers
      // are not possible for us
      if (!isHandleableVariable(op1)
          || !precision.isTracking(assignedVarName, op1.getExpressionType(), cfaEdge.getSuccessor())) {
        return Collections.singleton(state.removeLocalVars(calledFunctionName));
      }

      int returnVarIndex =
          state.getVariableIndexFor(
              MemoryLocation.valueOf(
                  calledFunctionName,
                  fnkCall.getFunctionEntry().getReturnVariable().get().getName()));

      if (returnVarIndex == -1) {
        state = state.forget(assignedVarName);
        return Collections.singleton(state.removeLocalVars(calledFunctionName));
      }

      IOctagonCoefficients right = new OctagonSimpleCoefficients(state.sizeOfVariables(), returnVarIndex, OctagonIntValue.ONE, state);

      state = state.makeAssignment(assignedVarName, right);


    // g(b), do nothing
    } else if (exprOnSummary instanceof CFunctionCallStatement) {

    } else {
      throw new UnrecognizedCCodeException("on function return", cfaEdge, exprOnSummary);
    }

    return Collections.singleton(state.removeLocalVars(calledFunctionName));
  }

  @Override
  protected Set<OctagonState> handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    if (cfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration declaration = (CVariableDeclaration) decl;

      // get the variable name in the declarator
      MemoryLocation variableName;

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointeror struct variables to the list since we don't track them
      if (!isHandleAbleType(declaration.getType())) { return Collections.singleton(state); }

      // make the fullyqualifiedname
      if (!decl.isGlobal()) {
        variableName = MemoryLocation.valueOf(functionName, declaration.getName());
      } else {
        variableName = MemoryLocation.valueOf(declaration.getName());
      }

      if (!precision.isTracking(variableName, declaration.getType(), cfaEdge.getSuccessor())) {
        return Collections.singleton(state);
      }

      CInitializer init = declaration.getInitializer();

      // for global declarations, there may be forwards declarations, so we do
      // not need to declarate them a second time, but if there is an initializer
      // we assign it to the before declared variable
      if (!state.existsVariable(variableName) && (init == null || init instanceof CInitializerExpression)) {
        state = state.declareVariable(variableName, getCorrespondingOctStateType(declaration.getType()));
      }

      Set<OctagonState> possibleStates = new HashSet<>();

      if (init != null) {
        if (init instanceof CInitializerExpression) {
          CExpression exp = ((CInitializerExpression) init).getExpression();

          COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(state, functionName);
          Set<Pair<IOctagonCoefficients, OctagonState>> initCoeffs = exp.accept(coeffVisitor);

          for (Pair<IOctagonCoefficients, OctagonState> pairs : initCoeffs) {
            possibleStates.add(pairs.getSecond().makeAssignment(variableName, pairs.getFirst()));
          }

          // if there is an initializerlist, the variable is either an array or a struct/union
          // we cannot handle them, so simply return the previous state
        } else if (init instanceof CInitializerList) {
            return Collections.singleton(state);

        } else {
          throw new AssertionError("Unhandled Expression Type: " + init.getClass());
        }

        // global variables without initializer are set to 0 in C
      } else if (decl.isGlobal()) {
        possibleStates.add(state.makeAssignment(variableName, new OctagonSimpleCoefficients(state.sizeOfVariables(), state).expandToSize(state.sizeOfVariables(), state)));
      }

      if (possibleStates.isEmpty()) {
        possibleStates.add(state);
      }

      return possibleStates;

    } else if (cfaEdge.getDeclaration() instanceof CTypeDeclaration
        || cfaEdge.getDeclaration() instanceof CFunctionDeclaration) { return Collections.singleton(state); }

    throw new AssertionError(cfaEdge.getDeclaration() + " (" + cfaEdge.getDeclaration().getClass() + ")");
  }

  @Override
  protected Set<OctagonState> handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    // check if there are functioncalls we cannot handle
    if (statement instanceof CFunctionCall) {
      CExpression fn = ((CFunctionCall)statement).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        }
      }
    }

    // expression is a binary operation, e.g. a = b;
    if (statement instanceof CAssignment) {
      CLeftHandSide left = ((CAssignment) statement).getLeftHandSide();
      CRightHandSide right = ((CAssignment) statement).getRightHandSide();

      MemoryLocation variableName = buildVarName(left, functionName);

      // as pointers do not get declarated in the beginning we can just
      // ignore them here
      if (!isHandleableVariable(left)
          || !precision.isTracking(variableName, left.getExpressionType(), cfaEdge.getSuccessor())) {
        assert !state.existsVariable(variableName) : "variablename '" + variableName + "' is in map although it can not be handled";
        return Collections.singleton(state);
      } else {
        COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(state, functionName);
        Set<Pair<IOctagonCoefficients, OctagonState>> coeffsList = right.accept(coeffVisitor);

        Set<OctagonState> possibleStates = new HashSet<>();
        for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsList) {
          OctagonState newState = pairs.getSecond().makeAssignment(variableName, pairs.getFirst());

          // if state is empty after assignment there was probably an overflow, so we have
          // to forget the variables value and then break out of this loop so we do not
          // have to evaluete further states even if there were some
          if (newState.isEmpty()) {
            possibleStates.add(state.forget(variableName));
            break;
          }

          possibleStates.add(newState);
        }

        return possibleStates;
      }

      // external function call, or p.e. a;
      // => do nothing
    } else if (statement instanceof CFunctionCallStatement
        || statement instanceof CExpressionStatement) {
      return Collections.singleton(state);

    }

    throw new UnrecognizedCCodeException("unknown statement", cfaEdge, statement);
  }

  private MemoryLocation buildVarName(CLeftHandSide left, String functionName) {

    String variableName = null;
    if (left instanceof CArraySubscriptExpression) {
      variableName = ((CArraySubscriptExpression) left).getArrayExpression().toASTString();
    } else if (left instanceof CPointerExpression) {
      variableName = ((CPointerExpression) left).getOperand().toASTString();
    } else if (left instanceof CFieldReference) {
      variableName = ((CFieldReference) left).getFieldOwner().toASTString();
    } else {
      variableName = ((CIdExpression) left).toASTString();
    }

    if (!isGlobal(left)) {
      return MemoryLocation.valueOf(functionName, variableName);
    } else {
      return MemoryLocation.valueOf(variableName);
    }

  }

  /**
   * This is a return statement in a function
   */
  @Override
  protected Set<OctagonState> handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {

    // this is for functions without return value, which just have returns
    // in them to end the function
    if (!cfaEdge.getExpression().isPresent()) {
      return Collections.singleton(state);
    }

    MemoryLocation tempVarName =
        MemoryLocation.valueOf(
            cfaEdge.getPredecessor().getFunctionName(),
            ((CIdExpression) cfaEdge.asAssignment().get().getLeftHandSide()).getName());

    // main function has no __cpa_temp_result_var as the result of the main function
    // is not important for us, we skip here
    if (!state.existsVariable(tempVarName)) {
      return Collections.singleton(state);
    }

    Set<OctagonState> possibleStates = new HashSet<>();
    COctagonCoefficientVisitor coeffVisitor = new COctagonCoefficientVisitor(state, cfaEdge.getPredecessor().getFunctionName());
    Set<Pair<IOctagonCoefficients, OctagonState>> coeffsList = cfaEdge.getExpression().get().accept(coeffVisitor);

    for (Pair<IOctagonCoefficients, OctagonState> pairs : coeffsList) {
        possibleStates.add(pairs.getSecond().makeAssignment(tempVarName, pairs.getFirst()));
    }

    return possibleStates;
  }

  /**
   * This edge is the return edge from a function to the caller
   */
  @Override
  protected Set<OctagonState> handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    return Collections.emptySet();
  }

  class COctagonCoefficientVisitor extends DefaultCExpressionVisitor<Set<Pair<IOctagonCoefficients, OctagonState>>, CPATransferException>
      implements CRightHandSideVisitor<Set<Pair<IOctagonCoefficients, OctagonState>>, CPATransferException> {

    private OctagonState visitorState;
    private String visitorFunctionName;

    /**
     * This method creates the Visitor, which evaluates all coefficients for a given
     * Expression.
     */
    public COctagonCoefficientVisitor(OctagonState pState, String pFunctionName) {
      visitorState = pState;
      visitorFunctionName = pFunctionName;
    }

    public OctagonState getState() {
      return visitorState;
    }


    @Override
    protected Set<Pair<IOctagonCoefficients, OctagonState>> visitDefault(CExpression pExp) throws CPATransferException {
      return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CBinaryExpression e) throws CPATransferException {
      // do not even evaluate the members of this binary expression if we cannot
      // handle the operator
      switch (e.getOperator()) {
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case MODULO:
        return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
      default:
        // nothing to do
      }

      Set<Pair<IOctagonCoefficients, OctagonState>> left = e.getOperand1().accept(this);
      Set<Pair<IOctagonCoefficients, Set<Pair<IOctagonCoefficients, OctagonState>>>> right = new HashSet<>();

      int origSize = left.size();
      left = FluentIterable.from(left).filter(new NotInstanceOfEmptyCoefficients()).toSet();

      if (left.isEmpty() || origSize != left.size()) {
        return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
      } else {
        for (Pair<IOctagonCoefficients, OctagonState> pair : left) {
          Set<Pair<IOctagonCoefficients, OctagonState>> tmpRight = e.getOperand2().accept(new COctagonCoefficientVisitor(pair.getSecond(), visitorFunctionName));
          origSize = tmpRight.size();
          tmpRight = FluentIterable.from(tmpRight).filter(new NotInstanceOfEmptyCoefficients()).toSet();
          if (tmpRight.isEmpty() || origSize != tmpRight.size()) {
            return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
          } else {
            right.add(Pair.of(pair.getFirst(), tmpRight));
          }
        }
      }

      switch (e.getOperator()) {
      case EQUALS:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case LESS_EQUAL:
      case LESS_THAN:
      case NOT_EQUALS: {
        Set<Pair<IOctagonCoefficients, OctagonState>> returnCoefficients = new HashSet<>();
            MemoryLocation tempVarLeft =
                MemoryLocation.valueOf(
                    visitorFunctionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_");
        temporaryVariableCounter++;
        BinaryOperator binOp = e.getOperator();

        for (Pair<IOctagonCoefficients, Set<Pair<IOctagonCoefficients, OctagonState>>> pairs : right) {
          IOctagonCoefficients leftCoeffs = pairs.getFirst();
          for (Pair<IOctagonCoefficients, OctagonState> rightPair : pairs.getSecond()) {
            OctagonState visitorState = rightPair.getSecond();
            IOctagonCoefficients rightCoeffs = rightPair.getFirst();

            // shortcut for statements like x == x
            if (leftCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState).equals(rightCoeffs)) {
              switch (binOp) {
              case EQUALS:
              case GREATER_EQUAL:
              case LESS_EQUAL:
                returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(visitorState.sizeOfVariables(), visitorState), visitorState));
                break;
              case NOT_EQUALS:
              case LESS_THAN:
              case GREATER_THAN:
                returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(visitorState.sizeOfVariables(), visitorState), visitorState));
                break;
               // unused default statements, all possible values for this switch
               // statement are handled above
              default:
                throw new AssertionError("Unhandled case in switch clause.");
              }
              continue;
            }

            // we do not need to create a temporary variable if the left or
            // right coefficients are already a variable
            if (leftCoeffs.hasOnlyOneValue() && !leftCoeffs.hasOnlyConstantValue()) {
              tempVarLeft = visitorState.getVariableNameFor(leftCoeffs.getVariableIndex());

            } else if (rightCoeffs.hasOnlyOneValue() && !rightCoeffs.hasOnlyConstantValue()) {
              tempVarLeft = visitorState.getVariableNameFor(rightCoeffs.getVariableIndex());
              rightCoeffs = leftCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState);

              //because we change the sides of the operands, we have to change the
              //operator, too
              switch (binOp) {
              case GREATER_EQUAL: binOp = BinaryOperator.LESS_EQUAL; break;
              case GREATER_THAN: binOp = BinaryOperator.LESS_THAN; break;
              case LESS_EQUAL: binOp = BinaryOperator.GREATER_EQUAL; break;
              case LESS_THAN: binOp = BinaryOperator.GREATER_THAN; break;
              default: break;
              }

            } else {
              visitorState = visitorState.declareVariable(tempVarLeft, getCorrespondingOctStateType(e.getOperand1().getExpressionType()));
              visitorState = visitorState.makeAssignment(tempVarLeft, leftCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState));
              rightCoeffs = rightCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState);
            }

            returnCoefficients.addAll(handleLogicalOperators(tempVarLeft, binOp, visitorState, rightCoeffs));
          }
        }
        return returnCoefficients;
      }
      case DIVIDE:
      case MULTIPLY:
      case MINUS:
      case PLUS: {
        Set<Pair<IOctagonCoefficients, OctagonState>> returnCoefficients = new HashSet<>();
        for (Pair<IOctagonCoefficients, Set<Pair<IOctagonCoefficients, OctagonState>>> pairs : right) {
          IOctagonCoefficients leftCoeffs = pairs.getFirst();
          for (Pair<IOctagonCoefficients, OctagonState> rightPair : pairs.getSecond()) {
            IOctagonCoefficients rightCoeffs = rightPair.getFirst();
            OctagonState visitorState = rightPair.getSecond();

            if (leftCoeffs.size() < rightCoeffs.size()) {
              leftCoeffs = leftCoeffs.expandToSize(rightCoeffs.size(), visitorState);
            } else {
              rightCoeffs = rightCoeffs.expandToSize(leftCoeffs.size(), visitorState);
            }
            if (e.getOperator() == BinaryOperator.MINUS) {
              returnCoefficients.add(Pair.of(leftCoeffs.sub(rightCoeffs), visitorState));
            } else if (e.getOperator() == BinaryOperator.PLUS) {
              returnCoefficients.add(Pair.of(leftCoeffs.add(rightCoeffs), visitorState));

              // TODO these are some more or less untested optimizations which should mostly
              // be necessary for floats, after some testing this should be enabled by default
            } else if (e.getOperator() == BinaryOperator.MULTIPLY) {

              if (leftCoeffs.hasOnlyOneValue() || rightCoeffs.hasOnlyOneValue()) {
                returnCoefficients.add(Pair.of(leftCoeffs.mul(rightCoeffs), visitorState));
              } else {
                MemoryLocation tempVarLeft = MemoryLocation.valueOf(visitorFunctionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_", 0);
                temporaryVariableCounter++;
                visitorState = visitorState.declareVariable(tempVarLeft, getCorrespondingOctStateType(e.getOperand1().getExpressionType()));
                visitorState = visitorState.makeAssignment(tempVarLeft, leftCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState));
                returnCoefficients.add(Pair.of(new OctagonSimpleCoefficients(visitorState.sizeOfVariables(),
                                                                         visitorState.getVariableIndexFor(tempVarLeft),
                                                                         OctagonIntValue.ONE,
                                                                         visitorState).mul(rightCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState)),
                                                visitorState));
              }

            } else if (e.getOperator() == BinaryOperator.DIVIDE) {

              if (rightCoeffs.hasOnlyOneValue()) {
                returnCoefficients.add(Pair.of(leftCoeffs.div(rightCoeffs), visitorState));
              } else {
                MemoryLocation tempVarRight = MemoryLocation.valueOf(visitorFunctionName, TEMP_VAR_PREFIX + temporaryVariableCounter + "_", 0);
                temporaryVariableCounter++;
                visitorState = visitorState.declareVariable(tempVarRight, getCorrespondingOctStateType(e.getOperand2().getExpressionType()));
                visitorState = visitorState.makeAssignment(tempVarRight, rightCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState));
                IOctagonCoefficients expandedleftCoeffs = leftCoeffs.expandToSize(visitorState.sizeOfVariables(), visitorState);
                returnCoefficients.add(Pair.of(expandedleftCoeffs.div(new OctagonSimpleCoefficients(visitorState.sizeOfVariables(),
                                                                                        visitorState.getVariableIndexFor(tempVarRight),
                                                                                        OctagonIntValue.ONE,
                                                                                        visitorState)),
                                                visitorState));
              }
            }
          }
        }

        return returnCoefficients;
      }
      default:
        throw new AssertionError("Unhandled case statement");
      }
    }

    private Set<Pair<IOctagonCoefficients, OctagonState>> handleLogicalOperators(MemoryLocation pTempVarLeft, BinaryOperator binOp, OctagonState state, IOctagonCoefficients constraintCoeffs) {
      Set<Pair<IOctagonCoefficients, OctagonState>> returnCoefficients = new HashSet<>();
      OctagonState tmpState;
      switch (binOp) {
      case EQUALS:
        tmpState = state.addEqConstraint(pTempVarLeft, constraintCoeffs);
        if (tmpState.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(state.sizeOfVariables(), state), state));
        } else {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(state.sizeOfVariables(), tmpState), tmpState));

          // just because we know the value may be equal to the rightcoeffs, it does not
          // have to be equal, so we need to check on smaller/greater rightCoeffs
          // and eventually return more states
          OctagonState smaller = state.addSmallerConstraint(pTempVarLeft, constraintCoeffs);
          if (!smaller.isEmpty()) {
            returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(smaller.sizeOfVariables(), smaller), smaller));
          } else {
            OctagonState greater = state.addGreaterConstraint(pTempVarLeft, constraintCoeffs);
            if (!greater.isEmpty()) {
            returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(greater.sizeOfVariables(), greater), greater));
            }
          }
        }
        break;
      case GREATER_EQUAL:
        tmpState = state.addGreaterEqConstraint(pTempVarLeft, constraintCoeffs);
        if (tmpState.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(state.sizeOfVariables(), state), state));
        } else {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(state.sizeOfVariables(), tmpState), tmpState));


          // just because we know the value may be greater equal than the rightcoeffs, it does not
          // have to be greater equal, so we need to check on smaller rightCoeffs
          // and eventually return more states
          OctagonState smaller = state.addSmallerConstraint(pTempVarLeft, constraintCoeffs);
          if (!smaller.isEmpty()) {
            returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(smaller.sizeOfVariables(), smaller), smaller));
          }
        }
        break;
      case GREATER_THAN:
        tmpState = state.addGreaterConstraint(pTempVarLeft, constraintCoeffs);
        if (tmpState.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(state.sizeOfVariables(), state), state));
        } else {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(state.sizeOfVariables(), tmpState), tmpState));

          // just because we know the value may be greater than the rightcoeffs, it does not
          // have to be greater, so we need to check on smaller equal rightCoeffs
          // and eventually return more states
          OctagonState smaller = state.addSmallerEqConstraint(pTempVarLeft, constraintCoeffs);
          if (!smaller.isEmpty()) {
            returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(smaller.sizeOfVariables(), smaller), smaller));
          }
        }
        break;
      case LESS_EQUAL:
        tmpState = state.addSmallerEqConstraint(pTempVarLeft, constraintCoeffs);
        if (tmpState.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(state.sizeOfVariables(), state), state));
        } else {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(state.sizeOfVariables(), tmpState), tmpState));

          // just because we know the value may be smaller equal than the rightcoeffs, it does not
          // have to be smaller equal, so we need to check on greater rightCoeffs
          // and eventually return more states
          OctagonState greater = state.addGreaterConstraint(pTempVarLeft, constraintCoeffs);
          if (!greater.isEmpty()) {
            returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(greater.sizeOfVariables(), greater), greater));
          }
        }
        break;
      case LESS_THAN:
        tmpState = state.addSmallerConstraint(pTempVarLeft, constraintCoeffs);
        if (tmpState.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(state.sizeOfVariables(), state), state));
        } else {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(state.sizeOfVariables(), tmpState), tmpState));

          // just because we know the value may be smaller than the rightcoeffs, it does not
          // have to be smaller, so we need to check on greater equal rightCoeffs
          // and eventually return more states
          OctagonState greater = state.addGreaterEqConstraint(pTempVarLeft, constraintCoeffs);
          if (!greater.isEmpty()) {
            returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(greater.sizeOfVariables(), greater), greater));
          }
        }
        break;
      case NOT_EQUALS:
        OctagonState smaller = state.addSmallerConstraint(pTempVarLeft, constraintCoeffs);
        OctagonState bigger = state.addGreaterConstraint(pTempVarLeft, constraintCoeffs);
        OctagonState equal = state.addEqConstraint(pTempVarLeft, constraintCoeffs);

        if (!smaller.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(smaller.sizeOfVariables(), smaller), smaller));
        }

        if (!bigger.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolTRUECoeffs(bigger.sizeOfVariables(), bigger), bigger));
        }

        if (!equal.isEmpty()) {
          returnCoefficients.add(Pair.of((IOctagonCoefficients)OctagonSimpleCoefficients.getBoolFALSECoeffs(equal.sizeOfVariables(), equal), equal));
        }
        break;
      default:
        throw new AssertionError("Unhandled case statement");
      }

      return returnCoefficients;
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CIdExpression e) throws CPATransferException {
      MemoryLocation varName = buildVarName(e, functionName);
      Integer varIndex = visitorState.getVariableIndexFor(varName);
      // TODO following if-part may be imprecise, check again
      if (varIndex == -1) {
        varName = buildVarName(e, visitorFunctionName);
        varIndex = visitorState.getVariableIndexFor(varName);
      }

      if (varIndex == -1) { return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState)); }
      return Collections.singleton(Pair.of((IOctagonCoefficients)new OctagonSimpleCoefficients(visitorState.sizeOfVariables(), varIndex, OctagonIntValue.ONE, visitorState), visitorState));
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CCharLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Pair.of((IOctagonCoefficients)new OctagonSimpleCoefficients(visitorState.sizeOfVariables(), OctagonIntValue.of(e.getValue()), visitorState), visitorState));
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CFloatLiteralExpression e) throws CPATransferException {
        return Collections.singleton(Pair.of((IOctagonCoefficients)new OctagonSimpleCoefficients(visitorState.sizeOfVariables(), new OctagonDoubleValue(e.getValue().doubleValue()), visitorState), visitorState));
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CIntegerLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Pair.of((IOctagonCoefficients)new OctagonSimpleCoefficients(visitorState.sizeOfVariables(), OctagonIntValue.of(e.asLong()), visitorState), visitorState));
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CUnaryExpression e) throws CPATransferException {

      switch (e.getOperator()) {
      case AMPER:
      case SIZEOF:
      case TILDE:
        return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
      default:
        // nothing to do
      }

      // only minus operantor is handled after here
      assert e.getOperator() == UnaryOperator.MINUS;

      Set<Pair<IOctagonCoefficients, OctagonState>> operand = e.getOperand().accept(this);

      int origSize = operand.size();
      operand = FluentIterable.from(operand).filter(new NotInstanceOfEmptyCoefficients()).toSet();

      // after filtering out all emptycoefficients, we check if there were some
      // if yes we can quit immediately as there is at least one state where the
      // value is undefined, the other states could only be more precise and are
      // therefore irrelevant
      if (operand.isEmpty() || origSize != operand.size()) {
        return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
      }

      Set<Pair<IOctagonCoefficients, OctagonState>> returnValues = new HashSet<>();

      // we negate all coefficients and afterwards return the computed results
      for (Pair<IOctagonCoefficients, OctagonState> pair : operand) {
        returnValues.add(Pair.of(pair.getFirst().mul(OctagonIntValue.NEG_ONE), pair.getSecond()));
      }

      return returnValues;
    }

    @Override
    public Set<Pair<IOctagonCoefficients, OctagonState>> visit(CFunctionCallExpression e) throws CPATransferException {
      if (e.getFunctionNameExpression() instanceof CIdExpression) {
        String functionName = ((CIdExpression)e.getFunctionNameExpression()).getName();
        if (functionName.equals("__VERIFIER_nondet_uint")) {
          return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonIntervalCoefficients.getNondetUIntCoeffs(visitorState.sizeOfVariables(), visitorState), visitorState));
        } else if (functionName.equals("__VERIFIER_nondet_bool")) {
          return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonIntervalCoefficients.getNondetBoolCoeffs(visitorState.sizeOfVariables(), visitorState), visitorState));
        }
      }
      return Collections.singleton(Pair.of((IOctagonCoefficients)OctagonUniversalCoefficients.INSTANCE, visitorState));
    }
  }

  /**
   * Predicate implementation which filters out OctUniversalCoefficients of a given fluentiterable.
   */
  static class NotInstanceOfEmptyCoefficients implements Predicate<Pair<IOctagonCoefficients, OctagonState>> {
    @Override
    public boolean apply(Pair<IOctagonCoefficients, OctagonState> pInput) {
      return !(pInput.getFirst() instanceof OctagonUniversalCoefficients);
    }
  }
}
