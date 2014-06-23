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
package org.sosy_lab.cpachecker.cpa.apron;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Iterables.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.annotation.Nullable;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
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
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.apron.ApronState.Type;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidCFAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import apron.DoubleScalar;
import apron.Interval;
import apron.Linexpr0;
import apron.Linterm0;
import apron.Scalar;
import apron.Tcons0;
import apron.Texpr0BinNode;
import apron.Texpr0CstNode;
import apron.Texpr0DimNode;
import apron.Texpr0Intern;
import apron.Texpr0Node;
import apron.Texpr0UnNode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

public class ApronTransferRelation extends ForwardingTransferRelation<Set<ApronState>, ApronState, ApronPrecision> {

  private static final String FUNCTION_RETURN_VAR = "___cpa_temp_result_var_";

  /**
   * This is used for making smaller and greater constraint with octagons
   */
  private static final Texpr0CstNode constantMin = new Texpr0CstNode(new DoubleScalar(0.00000000000001));

  /**
   * set of functions that may not appear in the source code
   * the value of the map entry is the explanation for the user
   */
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  private final LogManager logger;

  private final Map<CFAEdge, Loop> loopEntryEdges;

  /**
   * Class constructor.
   * @throws InvalidCFAException
   * @throws InvalidConfigurationException
   */
  public ApronTransferRelation(LogManager log, CFA cfa) throws InvalidCFAException {
    logger = log;

    if (!cfa.getLoopStructure().isPresent()) {
      throw new InvalidCFAException("OctagonCPA does not work without loop information!");
    }

    Multimap<String, Loop> loops = cfa.getLoopStructure().get();
    Map<CFAEdge, Loop> entryEdges = new HashMap<>();

    for (Loop l : loops.values()) {
      // function edges do not count as incoming/outgoing edges
      Iterable<CFAEdge> incomingEdges = filter(l.getIncomingEdges(),
                                               not(instanceOf(CFunctionReturnEdge.class)));

      for (CFAEdge e : incomingEdges) {
          entryEdges.put(e, l);
      }
    }
    loopEntryEdges = Collections.unmodifiableMap(entryEdges);
  }

  boolean done = false;

  @Override
  public Collection<ApronState> getAbstractSuccessors(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException {

    setInfo(abstractState, abstractPrecision, cfaEdge);

    // TODO the creation of the additional ApronManager which then is never used
    // should not be necessary, however, without this constructor call the library
    // does not work properly
    if (!done) {
    try {
      new ApronManager(Configuration.defaultConfiguration());
    } catch (InvalidConfigurationException e) {
      e.printStackTrace();
    }
    done = true;
    }


    final Collection<ApronState> preCheck = preCheck();
    if (preCheck != null) { return preCheck; }

    final Collection<ApronState> successors = new ArrayList<>();

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge:
      final AssumeEdge assumption = (AssumeEdge) cfaEdge;
      successors.addAll(handleAssumption(assumption, assumption.getExpression(), assumption.getTruthAssumption()));
      break;

    case FunctionCallEdge:
      final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
      final FunctionEntryNode succ = fnkCall.getSuccessor();
      final String calledFunctionName = succ.getFunctionName();
      successors.addAll(handleFunctionCallEdge(fnkCall, fnkCall.getArguments(),
          succ.getFunctionParameters(), calledFunctionName));
      break;

    case FunctionReturnEdge:
      final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
      final FunctionReturnEdge fnkReturnEdge = (FunctionReturnEdge) cfaEdge;
      final FunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
      successors.addAll(handleFunctionReturnEdge(fnkReturnEdge,
          summaryEdge, summaryEdge.getExpression(), callerFunctionName));

      break;

    case MultiEdge:
      successors.addAll(handleMultiEdge((MultiEdge) cfaEdge));
      break;

    default:
      successors.addAll(handleSimpleEdge(cfaEdge));
    }

    successors.removeAll(Collections.singleton(null));

    // remove all states whose constraints cannot be satisfied
    Iterator<ApronState> states = successors.iterator();
    while (states.hasNext()) {
      ApronState st = states.next();
      if (st.isEmpty()) {
        states.remove();
        logger.log(Level.FINER, "removing state because of unsatisfiable constraints:\n" +
                                 st + "________________\nEdge was:\n" + cfaEdge.getDescription());
      }
    }


    Set<ApronState> returnStates = new HashSet<>(successors);
    if (loopEntryEdges.get(cfaEdge) != null) {
      returnStates.clear();
      for (ApronState s : successors) {
        returnStates.add(new ApronState(s.getApronNativeState(),
                                     s.getManager(),
                                     s.getIntegerVariableToIndexMap(),
                                     s.getRealVariableToIndexMap(),
                                     s.getVariableToTypeMap(),
                                     new ApronState.Block(),
                                     logger));
      }
    }

    resetInfo();

    return returnStates;
  }

  @Override
  protected Set<ApronState> handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException{
    return Collections.singleton(state);
  }

  @SuppressWarnings("deprecation")
  @Override
  protected Set<ApronState> handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {

    if (expression instanceof CLiteralExpression) {
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

    } else if (expression instanceof CBinaryExpression) {
      return handleBinaryAssumption(expression, truthAssumption);

    } else {
      Set<Texpr0Node> coeffs = expression.accept(new CApronExpressionVisitor());
      if (coeffs.isEmpty()) {
        return Collections.singleton(state);
      }
      Set<ApronState> possibleStates = new HashSet<>();
      for (Texpr0Node coeff : coeffs) {
        if(truthAssumption) {
          possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ, coeff)));
        } else {
          possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP, coeff)));
          possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP, new Texpr0UnNode(Texpr0UnNode.OP_NEG, coeff))));
        }
      }
      return possibleStates;
    }
  }

  private Set<ApronState> handleBinaryAssumption(CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    CBinaryExpression binExp = (CBinaryExpression) expression;

    Double leftVal = binExp.getOperand1().accept(new CLiteralExpressionVisitor());
    Double rightVal = binExp.getOperand2().accept(new CLiteralExpressionVisitor());
    if (leftVal != null && rightVal != null) {
      return handleLiteralBinExpAssumption(leftVal, rightVal, binExp.getOperator(), truthAssumption);
    }
    Set<Texpr0Node> leftCoeffs = binExp.getOperand1().accept(new CApronExpressionVisitor());
    Set<Texpr0Node> rightCoeffs = binExp.getOperand2().accept(new CApronExpressionVisitor());

    if (leftCoeffs.isEmpty() || rightCoeffs.isEmpty()) {
      return Collections.singleton(state);
    }

    Set<ApronState> possibleStates = new HashSet<>();
    for (Texpr0Node left : leftCoeffs) {
      for (Texpr0Node right : rightCoeffs) {
        switch (binExp.getOperator()) {
        case BINARY_AND:
        case BINARY_OR:
        case BINARY_XOR:
        case SHIFT_RIGHT:
        case SHIFT_LEFT:
          return Collections.singleton(state);

        case EQUALS: {
          if (truthAssumption) {
            possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ,
                                                              new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                 left,
                                                                                                 right)))));
          } else {
            if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)left).dim)
                || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)right).dim)) {
              Texpr0BinNode increasedRight = new Texpr0BinNode(Texpr0BinNode.OP_ADD, right, constantMin);
              Texpr0BinNode increasedLeft = new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, constantMin);

              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                                                                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                   left,
                                                                                                   increasedRight)))));
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                                                                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                   right,
                                                                                                   increasedLeft)))));
            } else {
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.DISEQ,
                                                                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                   left,
                                                                                                   right)))));
            }
          }
          break;
        }
        case GREATER_EQUAL: {
          if (truthAssumption) {
            Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                     left,
                                                     right)));
            possibleStates.add(state.addConstraint(act));
          } else {
            Tcons0 act = new Tcons0(Tcons0.SUP,
                                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                     right,
                                                     left)));
            possibleStates.add(state.addConstraint(act));
          }
          break;
        }
        case GREATER_THAN: {
          if (truthAssumption) {
            Tcons0 act;
            if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)left).dim)
                || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)right).dim)) {
              Texpr0BinNode increasedRight = new Texpr0BinNode(Texpr0BinNode.OP_ADD, right, constantMin);
              act = new Tcons0(Tcons0.SUP,
                               new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                left,
                                                increasedRight)));
            } else {
              act = new Tcons0(Tcons0.SUP,
                               new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                left,
                                                right)));
            }

            possibleStates.add(state.addConstraint(act));
          } else {
            Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                     right,
                                                     left)));
            possibleStates.add(state.addConstraint(act));
          }
          break;
        }
        case LESS_EQUAL: {
          if (truthAssumption) {
            Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                     right,
                                                     left)));
            possibleStates.add(state.addConstraint(act));
          } else {
            Tcons0 act = new Tcons0(Tcons0.SUP,
                                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                     left,
                                                     right)));
            possibleStates.add(state.addConstraint(act));
          }
          break;
        }
        case LESS_THAN: {
          if (truthAssumption) {
            Tcons0 act;
            if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)left).dim)
                || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)right).dim)) {
              Texpr0BinNode increasedLeft = new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, constantMin);
              act = new Tcons0(Tcons0.SUP,
                               new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                right,
                                                increasedLeft)));
            } else {
              act = new Tcons0(Tcons0.SUP,
                               new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                right,
                                                left)));
            }

            possibleStates.add(state.addConstraint(act));
          } else {
            Tcons0 act = new Tcons0(Tcons0.SUPEQ,
                                    new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                     left,
                                                     right)));
            possibleStates.add(state.addConstraint(act));
          }
          break;
        }

        case NOT_EQUALS:  {
          if (truthAssumption) {
            if (left instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)left).dim)
                || right instanceof Texpr0DimNode && !state.isInt(((Texpr0DimNode)right).dim)) {
              Texpr0BinNode increasedRight = new Texpr0BinNode(Texpr0BinNode.OP_ADD, right, constantMin);
              Texpr0BinNode increasedLeft = new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, constantMin);

              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                                                                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                   left,
                                                                                                   increasedRight)))));
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                                                                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                   right,
                                                                                                   increasedLeft)))));
            } else {
              possibleStates.add(state.addConstraint(new Tcons0(Tcons0.DISEQ,
                                                                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                   left,
                                                                                                   right)))));
            }
          } else {
            possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ,
                                                              new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                 left,
                                                                                                 right)))));
          }
          break;
        }

        case DIVIDE:
        case MINUS:
        case MODULO:
        case MULTIPLY:
        case PLUS:
          Texpr0BinNode innerExp = null;
          switch (binExp.getOperator()) {
          case DIVIDE:
            innerExp = new Texpr0BinNode(Texpr0BinNode.OP_DIV, left, right);
            break;
          case MINUS:
            innerExp = new Texpr0BinNode(Texpr0BinNode.OP_SUB, left, right);
            break;
          case MODULO:
            innerExp = new Texpr0BinNode(Texpr0BinNode.OP_MOD, left, right);
            break;
          case MULTIPLY:
            innerExp = new Texpr0BinNode(Texpr0BinNode.OP_MUL, left, right);
            break;
          case PLUS:
            innerExp = new Texpr0BinNode(Texpr0BinNode.OP_ADD, left, right);
            break;
          }

          if (truthAssumption) {
            possibleStates.add(state.addConstraint(new Tcons0(Tcons0.EQ, innerExp)));
          } else {
            possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                                                              new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                 innerExp,
                                                                                                 constantMin)))));
            possibleStates.add(state.addConstraint(new Tcons0(Tcons0.SUP,
                                                              new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                                                 constantMin,
                                                                                                 innerExp)))));
          }

          break;
        default:
          throw new CFAGenerationRuntimeException("unhandled case in switch statement: " + binExp.getOperator());
        }
      }
    }

    return possibleStates;
  }

  private Set<ApronState> handleLiteralBinExpAssumption(double pLeftVal, double pRightVal, BinaryOperator pBinaryOperator, boolean truthAssumption) {
    boolean result;
    switch(pBinaryOperator) {
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
      return Collections.singleton(state);
    case NOT_EQUALS:
      result = pLeftVal != pRightVal;
      break;
    case EQUALS:
      result = pLeftVal == pRightVal;
      break;
    case GREATER_EQUAL:
      result = pLeftVal >= pRightVal;
      break;
    case GREATER_THAN:
      result = pLeftVal > pRightVal;
      break;
    case LESS_EQUAL:
      result = pLeftVal <= pRightVal;
      break;
    case LESS_THAN:
      result = pLeftVal < pRightVal;
      break;
    case MINUS:
      result = (pLeftVal - pRightVal) != 0;
      break;
    case MODULO:
      result = (pLeftVal % pRightVal) != 0;
      break;
    case MULTIPLY:
      result = (pLeftVal * pRightVal) != 0;
      break;
    case DIVIDE:
      result = (pLeftVal / pRightVal) != 0;
      break;
    case PLUS:
      result = (pLeftVal + pRightVal) != 0;
      break;
    default:
      throw new CFAGenerationRuntimeException("unhandled switch case: " + pBinaryOperator);
    }
    if ((truthAssumption && result)
        || (!truthAssumption && !result)) {
      return Collections.singleton(state);
    } else {
      return Collections.emptySet();
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
  private Set<ApronState> handleLiteralBooleanExpression(long value, boolean truthAssumption, ApronState state) {
    if (value == 0) {
      if (truthAssumption) {
        return Collections.singleton(state);
      } else {
        return Collections.emptySet();
      }
    } else {
      if (truthAssumption) {
        return Collections.emptySet();
      } else {
        return Collections.singleton(state);
      }
    }
  }

  private ApronState.Type getCorrespondingOctStateType(CType type) {
    if (type instanceof CSimpleType
        && (((CSimpleType)type).getType() == CBasicType.FLOAT
            || ((CSimpleType)type).getType() == CBasicType.DOUBLE)) {
      return Type.FLOAT;
    } else {
      return Type.INT;
    }
  }

  private boolean isHandleableVariable(CExpression var) {
    if (var instanceof CArraySubscriptExpression
        || var instanceof CFieldReference
        || var instanceof CPointerExpression
        || (var instanceof CStringLiteralExpression)
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

  @Override
  protected Set<ApronState> handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {

    CFunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    if (!cfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert parameters.size() == arguments.size();
    } else {
      assert parameters.size() <= arguments.size();
    }

    Set<ApronState> possibleStates = new HashSet<>();
    possibleStates.add(state.declareVariable(buildVarName(calledFunctionName, FUNCTION_RETURN_VAR),
                                                      getCorrespondingOctStateType(cfaEdge.getSuccessor().getFunctionDefinition().getType().getReturnType())));

    // declare all parameters as variables
    for (int i = 0; i < parameters.size(); i++) {
      CExpression arg = arguments.get(i);
      if (!isHandleAbleType(parameters.get(i).getType())) {
        continue;
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = buildVarName(calledFunctionName, nameOfParam);

      if (!precision.isTracked(formalParamName, parameters.get(i).getType())) {
        continue;
      }

      Set<Texpr0Node> coeffsList = arg.accept(new CApronExpressionVisitor());

      Set<ApronState> newPossibleStates = new HashSet<>();

      for (ApronState st : possibleStates) {

        ApronState tmpState = st.declareVariable(formalParamName, getCorrespondingOctStateType(parameters.get(i).getType()));

        if (coeffsList.isEmpty()) {
          newPossibleStates.add(tmpState);
        }

        for (Texpr0Node coeffs : coeffsList) {
          tmpState = tmpState.makeAssignment(formalParamName, coeffs);
          newPossibleStates.add(tmpState);
        }
      }

      possibleStates = newPossibleStates;
    }

    return possibleStates;
  }

  @Override
  protected Set<ApronState> handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    CFunctionCall exprOnSummary = fnkCall.getExpression();

    String calledFunctionName = cfaEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement) exprOnSummary);
      CLeftHandSide op1 = binExp.getLeftHandSide();
      String assignedVarName = buildVarName(op1, callerFunctionName);

      // we do not know anything about pointers, so assignments to pointers
      // are not possible for us
      if (!isHandleableVariable(op1)
          || !precision.isTracked(assignedVarName, op1.getExpressionType())) {
        return Collections.singleton(state.removeLocalVars(calledFunctionName));
      }

      String returnVarName = buildVarName(calledFunctionName, FUNCTION_RETURN_VAR);

      Texpr0Node right = new Texpr0DimNode(state.getVariableIndexFor(returnVarName));

      state = state.makeAssignment(assignedVarName, right);


    // g(b), do nothing
    } else if (exprOnSummary instanceof CFunctionCallStatement) {

    } else {
      throw new UnrecognizedCCodeException("on function return", cfaEdge, exprOnSummary);
    }

    return Collections.singleton(state.removeLocalVars(calledFunctionName));
  }

  @Override
  protected Set<ApronState> handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    if (cfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration declaration = (CVariableDeclaration) decl;

      // get the variable name in the declarator
      String variableName = declaration.getName();

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointeror struct variables to the list since we don't track them
      if (!isHandleAbleType(declaration.getType())) { return Collections.singleton(state); }

      // make the fullyqualifiedname
      if (!decl.isGlobal()) {
        variableName = buildVarName(functionName, variableName);
      }

      if (!precision.isTracked(variableName, declaration.getType())) {
        return Collections.singleton(state);
      }

      CInitializer init = declaration.getInitializer();

      // for global declarations, there may be forwards declarations, so we do
      // not need to declarate them a second time, but if there is an initializer
      // we assign it to the before declared variable
      if (!state.existsVariable(variableName) && (init == null || init instanceof CInitializerExpression)) {
        state = state.declareVariable(variableName, getCorrespondingOctStateType(declaration.getType()));
      }

      Set<ApronState> possibleStates = new HashSet<>();
      if (init != null) {
        if (init instanceof CInitializerExpression) {
          CExpression exp = ((CInitializerExpression) init).getExpression();

          Set<Texpr0Node> initCoeffs = exp.accept(new CApronExpressionVisitor());

          if (initCoeffs.isEmpty()) {
            possibleStates.add(state);
          }

          for (Texpr0Node coeffs : initCoeffs) {
            ApronState st = state.makeAssignment(variableName, coeffs);
            assert !st.isEmpty() : "states with assignments / declarations should never be empty";
            possibleStates.add(st);
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
        possibleStates.add(state.makeAssignment(variableName, new Texpr0CstNode()));
      }

      if (possibleStates.isEmpty()) {
        assert !state.isEmpty() : "states with assignments / declarations should never be empty";
        possibleStates.add(state);
      }

      return possibleStates;

    } else if (cfaEdge.getDeclaration() instanceof CTypeDeclaration
        || cfaEdge.getDeclaration() instanceof CFunctionDeclaration) { return Collections.singleton(state); }

    throw new AssertionError(cfaEdge.getDeclaration() + " (" + cfaEdge.getDeclaration().getClass() + ")");
  }

  @Override
  protected Set<ApronState> handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
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

      String variableName = buildVarName(left, functionName);

      // as pointers do not get declarated in the beginning we can just
      // ignore them here
      if (!isHandleableVariable(left)
          || !precision.isTracked(variableName, left.getExpressionType())) {
        assert !state.existsVariable(variableName) : "variablename '" + variableName + "' is in map although it can not be handled";
        return Collections.singleton(state);
      } else {
        Set<Texpr0Node> coeffsList = right.accept(new CApronExpressionVisitor());

        if (coeffsList.isEmpty()) {
          return Collections.singleton(state);
        }

        Set<ApronState> possibleStates = new HashSet<>();
        for (Texpr0Node coeffs : coeffsList) {
          // if we cannot determine coefficients, we cannot make any assumptions about
          // the value of the assigned variable and reset its value to unknown
          ApronState st = state.makeAssignment(variableName, coeffs);
          assert !st.isEmpty() : "states with assignments / declarations should never be empty";
          possibleStates.add(st);
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
  protected Set<ApronState> handleReturnStatementEdge(CReturnStatementEdge cfaEdge, @Nullable CExpression expression)
      throws CPATransferException {

    // this is for functions without return value, which just have returns
    // in them to end the function
    if (expression == null) {
      return Collections.singleton(state);
    }

    String tempVarName = buildVarName(cfaEdge.getPredecessor().getFunctionName(), FUNCTION_RETURN_VAR);

    // main function has no __cpa_temp_result_var as the result of the main function
    // is not important for us, we skip here
    if (!state.existsVariable(tempVarName)) {
      return Collections.singleton(state);
    }

    Set<ApronState> possibleStates = new HashSet<>();
    Set<Texpr0Node> coeffsList = expression.accept(new CApronExpressionVisitor());

    if (coeffsList.isEmpty()) {
      return Collections.singleton(state);
    }

    for (Texpr0Node coeffs : coeffsList) {
        possibleStates.add(state.makeAssignment(tempVarName, coeffs));
    }
    return possibleStates;
  }

  /**
   * This edge is the return edge from a function to the caller
   */
  @Override
  protected Set<ApronState> handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    return Collections.emptySet();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  class CApronExpressionVisitor extends DefaultCExpressionVisitor<Set<Texpr0Node>, CPATransferException>
      implements CRightHandSideVisitor<Set<Texpr0Node>, CPATransferException> {

    /**
     * This method creates the Visitor, which evaluates all coefficients for a given
     * Expression.
     *
     * @param state
     */
    public CApronExpressionVisitor() {
    }


    @Override
    protected Set<Texpr0Node> visitDefault(CExpression pExp) throws CPATransferException {
      return Collections.emptySet();
    }

    @Override
    public Set<Texpr0Node> visit(CBinaryExpression e) throws CPATransferException {
      Set<Texpr0Node> left = e.getOperand1().accept(this);
      Set<Texpr0Node> right = e.getOperand2().accept(this);

      Set<Texpr0Node> returnCoefficients = new HashSet<>();
      for (Texpr0Node leftCoeffs : left) {
        for (Texpr0Node rightCoeffs : right) {
          switch (e.getOperator()) {
          case BINARY_AND:
          case BINARY_OR:
          case BINARY_XOR:
          case SHIFT_LEFT:
          case SHIFT_RIGHT:
            return Collections.emptySet();
          case MODULO:
            returnCoefficients.add(new Texpr0BinNode(Texpr0BinNode.OP_MOD, leftCoeffs, rightCoeffs));
            break;
          case DIVIDE:
            returnCoefficients.add(new Texpr0BinNode(Texpr0BinNode.OP_DIV, leftCoeffs, rightCoeffs));
            break;
          case MULTIPLY:
            returnCoefficients.add(new Texpr0BinNode(Texpr0BinNode.OP_MUL, leftCoeffs, rightCoeffs));
            break;
          case MINUS:
            returnCoefficients.add(new Texpr0BinNode(Texpr0BinNode.OP_SUB, leftCoeffs, rightCoeffs));
            break;
          case PLUS:
            returnCoefficients.add(new Texpr0BinNode(Texpr0BinNode.OP_ADD, leftCoeffs, rightCoeffs));
            break;
          case EQUALS: {
            Tcons0 constraint = new Tcons0(Tcons0.EQ,
                                           new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                                              leftCoeffs,
                                                                              rightCoeffs)));

            if (!state.satisfies(constraint)) {
              returnCoefficients.add(new Texpr0CstNode());
            }

            if (!state.addConstraint(constraint).isEmpty()) {
              returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
            }

            break;
          }
          case GREATER_EQUAL: {
            Tcons0 constraint = new Tcons0(Tcons0.SUPEQ,
                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                   leftCoeffs,
                                                   rightCoeffs)));

            if (!state.satisfies(constraint)) {
            returnCoefficients.add(new Texpr0CstNode());
            }

            if (!state.addConstraint(constraint).isEmpty()) {
            returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
            }

            break;
          }
          case GREATER_THAN: {
            Tcons0 constraint = new Tcons0(Tcons0.SUP,
                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                   leftCoeffs,
                                                   rightCoeffs)));

            if (!state.satisfies(constraint)) {
            returnCoefficients.add(new Texpr0CstNode());
            }

            if (!state.addConstraint(constraint).isEmpty()) {
            returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
            }

            break;
          }
          case LESS_EQUAL: {
            Tcons0 constraint = new Tcons0(Tcons0.SUPEQ,
                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                   rightCoeffs,
                                                   leftCoeffs)));

            if (!state.satisfies(constraint)) {
            returnCoefficients.add(new Texpr0CstNode());
            }

            if (!state.addConstraint(constraint).isEmpty()) {
            returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
            }

            break;
          }
          case LESS_THAN: {
            Tcons0 constraint = new Tcons0(Tcons0.SUP,
                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                   rightCoeffs,
                                                   leftCoeffs)));

            if (!state.satisfies(constraint)) {
            returnCoefficients.add(new Texpr0CstNode());
            }

            if (!state.addConstraint(constraint).isEmpty()) {
            returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
            }

            break;
          }
          case NOT_EQUALS: {
            Tcons0 constraint = new Tcons0(Tcons0.DISEQ,
                new Texpr0Intern(new Texpr0BinNode(Texpr0BinNode.OP_SUB,
                                                   leftCoeffs,
                                                   rightCoeffs)));

            if (!state.satisfies(constraint)) {
            returnCoefficients.add(new Texpr0CstNode());
            }

            if (!state.addConstraint(constraint).isEmpty()) {
            returnCoefficients.add(new Texpr0CstNode(new Interval(1, 1)));
            }

            break;
          }

          default:
            throw new AssertionError("Unhandled case statement");
          }
        }
      }
      return returnCoefficients;
    }

    /**
     * Only unpack the cast and continue with the casts operand
     */
    @Override
    public Set<Texpr0Node> visit(CCastExpression e) throws CPATransferException {
      return e.getOperand().accept(this);
    }

    @Override
    public Set<Texpr0Node> visit(CIdExpression e) throws CPATransferException {
      String varName = buildVarName(e, functionName);
      Integer varIndex = state.getVariableIndexFor(varName);
      if (varIndex == -1) { return Collections.emptySet(); }
      return Collections.singleton((Texpr0Node)new Texpr0DimNode(varIndex));
    }

    @Override
    public Set<Texpr0Node> visit(CCharLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Texpr0Node.fromLinexpr0(new Linexpr0(new Linterm0[0], new DoubleScalar(e.getCharacter()))));
    }

    @Override
    public Set<Texpr0Node> visit(CFloatLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Texpr0Node.fromLinexpr0(new Linexpr0(new Linterm0[0], new DoubleScalar(e.getValue().doubleValue()))));
    }

    @Override
    public Set<Texpr0Node> visit(CIntegerLiteralExpression e) throws CPATransferException {
      return Collections.singleton(Texpr0Node.fromLinexpr0(new Linexpr0(new Linterm0[0], new DoubleScalar(e.getValue().doubleValue()))));
    }

    @SuppressWarnings("deprecation")
    @Override
    public Set<Texpr0Node> visit(CUnaryExpression e) throws CPATransferException {
      Set<Texpr0Node> operand = e.getOperand().accept(this);

      switch (e.getOperator()) {
      case AMPER:
      case SIZEOF:
      case TILDE:
        return Collections.emptySet();

      case MINUS:
        Set<Texpr0Node> returnCoefficients = new HashSet<>();
        for (Texpr0Node coeffs : operand) {
          returnCoefficients.add(new Texpr0UnNode(Texpr0UnNode.OP_NEG, coeffs));
        }
        return returnCoefficients;

      default:
        throw new AssertionError("Unhandled case in switch clause.");
      }
    }

    @Override
    public Set<Texpr0Node> visit(CFunctionCallExpression e) throws CPATransferException {
      if (e.getFunctionNameExpression() instanceof CIdExpression) {
        String functionName = ((CIdExpression)e.getFunctionNameExpression()).getName();
        if (functionName.equals("__VERIFIER_nondet_int")) {
          Scalar sup = Scalar.create();
          sup.setInfty(1);
          Scalar inf = Scalar.create();
          inf.setInfty(-1);
          Interval interval = new Interval(inf, sup);
          return Collections.singleton((Texpr0Node)new Texpr0CstNode(interval));
        } else if (functionName.equals("__VERIFIER_nondet_uint")) {
          Interval interval = new Interval();
          Scalar sup = Scalar.create();
          sup.setInfty(1);
          interval.setSup(sup);
          return Collections.singleton((Texpr0Node)new Texpr0CstNode(interval));
        } else if (functionName.equals("__VERIFIER_nondet_bool")) {
          Interval interval = new Interval(0, 1);
          return Collections.singleton((Texpr0Node)new Texpr0CstNode(interval));
        }
      }
      return Collections.emptySet();
    }
  }

  static class CLiteralExpressionVisitor extends DefaultCExpressionVisitor<Double, CPATransferException> {

    @Override
    protected Double visitDefault(CExpression pExp) throws CPATransferException {
      return null;
    }

    @Override
    public Double visit(CFloatLiteralExpression e) throws CPATransferException {
      return e.getValue().doubleValue();
    }

    @Override
    public Double visit(CIntegerLiteralExpression e) throws CPATransferException {
      return e.getValue().doubleValue();
    }

    @Override
    public Double visit(CCharLiteralExpression e) throws CPATransferException {
      return (double) e.getCharacter();
    }

    @Override
    public Double visit(CBinaryExpression e) throws CPATransferException {
      Double left = e.getOperand1().accept(this);
      Double right = e.getOperand2().accept(this);
      if (left == null || right == null) {
        return null;
      }
      switch (e.getOperator()) {
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        return null;
      case DIVIDE:
        return left / right;
      case EQUALS:
        return left.equals(right) ? 1.0 : 0;
      case GREATER_EQUAL:
        return left >= right ? 1.0 : 0;
      case GREATER_THAN:
        return left > right ? 1.0 : 0;
      case LESS_EQUAL:
        return left <= right ? 1.0 : 0;
      case LESS_THAN:
        return left < right ? 1.0 : 0;
      case NOT_EQUALS:
        break;
      case MINUS:
        return left - right;
      case MODULO:
        return left % right;
      case MULTIPLY:
        return left * right;
      case PLUS:
        return left + right;
      default:
        break;
      }
      return null;
    }

    @Override
    public Double visit(CUnaryExpression e) throws CPATransferException {
      Double op = e.getOperand().accept(this);
      if (op == null) {
        return null;
      }

      switch (e.getOperator()) {
      case ALIGNOF:
      case AMPER:
      case TILDE:
      case SIZEOF:
        return null;
      case MINUS:
        return -op;
      default:
        break;
      }
      return null;
    }

    @Override
    public Double visit(CCastExpression e) throws CPATransferException {
      Double op = e.getOperand().accept(this);
      if (op != null
          && e.getExpressionType() instanceof CSimpleType
          && ((((CSimpleType)e.getExpressionType()).getType() == CBasicType.INT)
              ||(((CSimpleType)e.getExpressionType()).getType() == CBasicType.CHAR))) {
        return (double) op.intValue();
      }
      return op;
    }
  }
}
