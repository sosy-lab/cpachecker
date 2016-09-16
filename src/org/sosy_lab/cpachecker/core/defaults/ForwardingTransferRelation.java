/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/** This Transfer-Relation forwards the method 'getAbstractSuccessors()'
 * to an edge-specific sub-method ('AssumeEdge', 'DeclarationEdge', ...).
 * It handles all casting of the edges and their information.
 * There is always an abstract method, that calls either the matching
 * C- or Java-Methods, depending on the type of the edge.
 * A developer should override the methods to get a valid analysis.
 *
 * The following structure shows the control-flow (work-flow) of this class.
 *
 * The tuple (C,J) represents the call of C- or Java-specific methods.
 * A user can either override the method itself, or the C- or Java-specific method.
 * If a C- or Java-specific method is called, but not overridden, it throws an assertion.
 *
 * 1. setInfo
 * 2. preCheck
 *
 * 3. getAbstractSuccessors:
 *   - handleAssumption -> C,J
 *   - handleFunctionCallEdge -> C,J
 *   - handleFunctionReturnEdge -> C,J
 *   - handleMultiEdge
 *   - handleSimpleEdge:
 *     -- handleDeclarationEdge -> C,J
 *     -- handleStatementEdge -> C,J
 *     -- handleReturnStatementEdge -> C,J
 *     -- handleBlankEdge
 *     -- handleFunctionSummaryEdge
 *
 * 4. postProcessing
 * 5. resetInfo
 *
 * Generics:
 *  - S type of intermediate result, should be equal to T or Collection<T>,
 *      should be converted/copied into an Object of type Collection<T> in method 'postProcessing'.
 *  - T type of State
 *  - P type of Precision
 */
public abstract class ForwardingTransferRelation<S, T extends AbstractState, P extends Precision>
    extends SingleEdgeTransferRelation {

  /** the given state, casted to correct type, for local access */
  protected @Nullable T state;

  /** the given precision, casted to correct type, for local access */
  protected @Nullable P precision;

  /** the function BEFORE the current edge */
  protected @Nullable String functionName;

  protected T getState() {
    return checkNotNull(state);
  }

  protected P getPrecision() {
    return checkNotNull(precision);
  }

  protected String getFunctionName() {
    return checkNotNull(functionName);
  }


  /**
   * This is the main method that delegates the control-flow to the
   * corresponding edge-type-specific methods.
   * In most cases there is no need to override this method. */
  @Override
  public Collection<T> getAbstractSuccessorsForEdge(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException {

    setInfo(abstractState, abstractPrecision, cfaEdge);

    final Collection<T> preCheck = preCheck(state, precision);
    if (preCheck != null) { return preCheck; }

    final S successor;

    switch (cfaEdge.getEdgeType()) {

      case AssumeEdge:
        final AssumeEdge assumption = (AssumeEdge) cfaEdge;
        successor =
            handleAssumption(
                assumption, assumption.getExpression(), assumption.getTruthAssumption());
        break;

      case FunctionCallEdge:
        final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
        final FunctionEntryNode succ = fnkCall.getSuccessor();
        final String calledFunctionName = succ.getFunctionName();
        successor =
            handleFunctionCallEdge(
                fnkCall, fnkCall.getArguments(), succ.getFunctionParameters(), calledFunctionName);
        break;

      case FunctionReturnEdge:
        final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
        final FunctionReturnEdge fnkReturnEdge = (FunctionReturnEdge) cfaEdge;
        final FunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
        successor =
            handleFunctionReturnEdge(
                fnkReturnEdge, summaryEdge, summaryEdge.getExpression(), callerFunctionName);
        break;

      case DeclarationEdge:
        final ADeclarationEdge declarationEdge = (ADeclarationEdge) cfaEdge;
        successor = handleDeclarationEdge(declarationEdge, declarationEdge.getDeclaration());
        break;

      case StatementEdge:
        final AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
        successor = handleStatementEdge(statementEdge, statementEdge.getStatement());
        break;

      case ReturnStatementEdge:
        // this statement is a function return, e.g. return (a);
        // note that this is different from return edge,
        // this is a statement edge, which leads the function to the
        // last node of its CFA, where return edge is from that last node
        // to the return site of the caller function
        final AReturnStatementEdge returnEdge = (AReturnStatementEdge) cfaEdge;
        successor = handleReturnStatementEdge(returnEdge);
        break;

      case BlankEdge:
        successor = handleBlankEdge((BlankEdge) cfaEdge);
        break;

      case CallToReturnEdge:
        successor = handleFunctionSummaryEdge((FunctionSummaryEdge) cfaEdge);
        break;

      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    final Collection<T> result = postProcessing(successor, cfaEdge);

    resetInfo();

    return result;
  }


  @SuppressWarnings("unchecked")
  protected void setInfo(final AbstractState abstractState,
      final Precision abstractPrecision, final CFAEdge cfaEdge) {
    state = (T) abstractState;
    precision = (P) abstractPrecision;
    functionName = cfaEdge.getPredecessor().getFunctionName();
  }

  protected void resetInfo() {
    state = null;
    precision = null;
    functionName = null;
  }

  /** This is a fast check, if the edge should be analyzed.
   * It returns NULL for further processing,
   * otherwise the return-value for skipping.  */
  @SuppressWarnings("unused")
  protected @Nullable Collection<T> preCheck(T state, P precision) {
    return null;
  }

  /** This method should convert/cast/copy the intermediate result into a Collection<T>.
   * This method can modify the successor, if needed. */
  @SuppressWarnings({"unchecked", "unused"})
  protected Collection<T> postProcessing(@Nullable S successor, CFAEdge edge) {
    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton((T)successor);
    }
  }


  /** This function handles assumptions like "if(a==b)" and "if(a!=0)".
   * If the assumption is not fulfilled, NULL should be returned. */
  protected @Nullable S handleAssumption(
      AssumeEdge cfaEdge, AExpression expression, boolean truthAssumption)
      throws CPATransferException {

    Pair<AExpression, Boolean> simplifiedExpression = simplifyAssumption(expression, truthAssumption);
    expression = simplifiedExpression.getFirst();
    truthAssumption = simplifiedExpression.getSecond();

    if (cfaEdge instanceof CAssumeEdge) {
      return handleAssumption((CAssumeEdge) cfaEdge, (CExpression) expression, truthAssumption);

    } else if (cfaEdge instanceof JAssumeEdge) {
      return handleAssumption((JAssumeEdge) cfaEdge, (JExpression) expression, truthAssumption);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CAssumeEdge}
   *
   * @param cfaEdge the edge to handle
   * @param expression the condition of the edge
   * @param truthAssumption indicates if this is the then or the else branch
   * @throws CPATransferException may be thrown in subclasses
   */
  protected @Nullable S handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JAssumeEdge}
   *
   * @param cfaEdge the edge to handle
   * @param expression the condition of the edge
   * @param truthAssumption indicates if this is the then or the else branch
   * @throws CPATransferException may be thrown in subclasses
   */
  protected @Nullable S handleAssumption(
      JAssumeEdge cfaEdge, JExpression expression, boolean truthAssumption)
      throws CPATransferException {
    return notImplemented();
  }


  /** This function handles functioncalls like "f(x)", that calls "f(int a)". */
  @SuppressWarnings("unchecked")
  protected S handleFunctionCallEdge(FunctionCallEdge cfaEdge,
      List<? extends AExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    if (cfaEdge instanceof CFunctionCallEdge) {
      return handleFunctionCallEdge((CFunctionCallEdge) cfaEdge,
          (List<CExpression>) arguments, (List<CParameterDeclaration>) parameters,
          calledFunctionName);

    } else if (cfaEdge instanceof JMethodCallEdge) {
      return handleFunctionCallEdge((JMethodCallEdge) cfaEdge,
          (List<JExpression>) arguments, (List<JParameterDeclaration>) parameters,
          calledFunctionName);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CFunctionCallEdge}.
   *
   * @param cfaEdge the edge to be handled
   * @param arguments the arguments given to the function
   * @param parameters the parameters of the function
   * @param calledFunctionName the name of the function
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JMethodCallEdge}.
   *
   * @param cfaEdge the edge to be handled
   * @param arguments the arguments given to the function
   * @param parameters the parameters of the function
   * @param calledFunctionName the name of the function
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionCallEdge(JMethodCallEdge cfaEdge,
      List<JExpression> arguments, List<JParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    return notImplemented();
  }


  /** This function handles functionReturns like "y=f(x)". */
  protected S handleFunctionReturnEdge(FunctionReturnEdge cfaEdge,
      FunctionSummaryEdge fnkCall, AFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionReturnEdge) {
      return handleFunctionReturnEdge((CFunctionReturnEdge) cfaEdge,
          (CFunctionSummaryEdge) fnkCall, (CFunctionCall) summaryExpr, callerFunctionName);

    } else if (cfaEdge instanceof JMethodReturnEdge) {
      return handleFunctionReturnEdge((JMethodReturnEdge) cfaEdge,
          (JMethodSummaryEdge) fnkCall, (JMethodOrConstructorInvocation) summaryExpr, callerFunctionName);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CFunctionReturnEdge}
   *
   * @param cfaEdge the edge to handle
   * @param fnkCall the summary edge of the formerly called function
   * @param summaryExpr the function call
   * @param callerFunctionName the name of the called function
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
          throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JMethodReturnEdge}
   *
   * @param cfaEdge the edge to handle
   * @param fnkCall the summary edge of the formerly called function
   * @param summaryExpr the function call
   * @param callerFunctionName the name of the called function
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionReturnEdge(JMethodReturnEdge cfaEdge,
      JMethodSummaryEdge fnkCall, JMethodOrConstructorInvocation summaryExpr, String callerFunctionName)
          throws CPATransferException {
    return notImplemented();
  }


  /** This function handles declarations like "int a = 0;" and "int b = !a;". */
  protected S handleDeclarationEdge(ADeclarationEdge cfaEdge, ADeclaration decl)
      throws CPATransferException {
    if (cfaEdge instanceof CDeclarationEdge) {
      return handleDeclarationEdge((CDeclarationEdge) cfaEdge, (CDeclaration) decl);

    } else if (cfaEdge instanceof JDeclarationEdge) {
      return handleDeclarationEdge((JDeclarationEdge) cfaEdge, (JDeclaration) decl);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CDeclarationEdge}
   *
   * @param cfaEdge the edge to handle
   * @param decl the declaration at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JDeclarationEdge}
   *
   * @param cfaEdge the edge to handle
   * @param decl the declaration at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleDeclarationEdge(JDeclarationEdge cfaEdge, JDeclaration decl)
      throws CPATransferException {
    return notImplemented();
  }

  /** This function handles statements like "a = 0;" and "b = !a;"
   * and calls of external functions. */
  protected S handleStatementEdge(AStatementEdge cfaEdge, AStatement statement)
      throws CPATransferException {
    if (cfaEdge instanceof CStatementEdge) {
      return handleStatementEdge((CStatementEdge) cfaEdge, (CStatement) statement);

    } else if (cfaEdge instanceof JStatementEdge) {
      return handleStatementEdge((JStatementEdge) cfaEdge, (JStatement) statement);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @param statement the statement at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @param statement the statement at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleStatementEdge(JStatementEdge cfaEdge, JStatement statement)
      throws CPATransferException {
    return notImplemented();
  }


  /** This function handles functionStatements like "return (x)". */
  protected S handleReturnStatementEdge(AReturnStatementEdge cfaEdge)
      throws CPATransferException {
    if (cfaEdge instanceof CReturnStatementEdge) {
      return handleReturnStatementEdge((CReturnStatementEdge) cfaEdge);

    } else if (cfaEdge instanceof JReturnStatementEdge) {
      return handleReturnStatementEdge((JReturnStatementEdge) cfaEdge);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CReturnStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JReturnStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleReturnStatementEdge(JReturnStatementEdge cfaEdge)
      throws CPATransferException {
    return notImplemented();
  }


  /**
   * This function handles blank edges, that are used for plain connectors
   * in the CFA. This default implementation returns the input-state.
   * A blank edge can also be a default-return-edge for a function "void f()".
   * In that case the successor-node is a FunctionExitNode.
   *
   * @param cfaEdge the edge to handle
   */
  @SuppressWarnings("unchecked")
  protected S handleBlankEdge(BlankEdge cfaEdge) {
    return (S)state;
  }

  protected S handleFunctionSummaryEdge(FunctionSummaryEdge cfaEdge) throws CPATransferException {
    if (cfaEdge instanceof CFunctionSummaryEdge) {
      return handleFunctionSummaryEdge((CFunctionSummaryEdge)cfaEdge);
    } else if (cfaEdge instanceof JMethodSummaryEdge) {
      return handleFunctionSummaryEdge((JMethodSummaryEdge)cfaEdge);
    } else {
      throw new AssertionError("unkown error");
    }
  }

  /**
   * Handle the {@link CFunctionSummaryEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge) throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handle the {@link JMethodSummaryEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionSummaryEdge(JMethodSummaryEdge cfaEdge) throws CPATransferException {
    return notImplemented();
  }

  public static boolean isGlobal(final AExpression exp) {
    if (exp instanceof CExpression) {
      return isGlobal((CExpression) exp);
    } else if (exp instanceof JExpression) {
      return isGlobal((JExpression) exp);
    } else {
      throw new AssertionError("unknown expression: " + exp);
    }
  }

  protected static boolean isGlobal(final CExpression exp) {
    if (exp instanceof CIdExpression) {
      CSimpleDeclaration decl = ((CIdExpression) exp).getDeclaration();
      if (decl instanceof CDeclaration) { return ((CDeclaration) decl).isGlobal(); }
    }
    return false;
  }

  protected static boolean isGlobal(final JExpression exp) {
    if (exp instanceof JIdExpression) {
      JSimpleDeclaration decl = ((JIdExpression) exp).getDeclaration();

      if (decl instanceof ADeclaration) {
        return ((ADeclaration) decl).isGlobal();
      }
    }

    return false;
  }

  protected static Pair<AExpression, Boolean> simplifyAssumption(AExpression pExpression, boolean pAssumeTruth) {
    if (isBooleanExpression(pExpression)) {
      if (pExpression instanceof CBinaryExpression) {
        CBinaryExpression binExp = (CBinaryExpression) pExpression;
        BinaryOperator operator = binExp.getOperator();
        if (isBooleanExpression(binExp.getOperand1())
            && binExp.getOperand2().equals(CIntegerLiteralExpression.ZERO)) {
          if (operator == BinaryOperator.EQUALS) {
            return simplifyAssumption(binExp.getOperand1(), !pAssumeTruth);
          } else if (operator == BinaryOperator.NOT_EQUALS) {
            return simplifyAssumption(binExp.getOperand1(), pAssumeTruth);
          } //TODO what else?
        } else if (isBooleanExpression(binExp.getOperand2())
            && binExp.getOperand1().equals(CIntegerLiteralExpression.ZERO)) {
          if (operator == BinaryOperator.EQUALS) {
            return simplifyAssumption(binExp.getOperand2(), !pAssumeTruth);
          } else if (operator == BinaryOperator.NOT_EQUALS) {
            return simplifyAssumption(binExp.getOperand2(), pAssumeTruth);
          } //TODO what else?
        }
      }
    }
    return Pair.of(pExpression, pAssumeTruth);
  }

  private static final ImmutableSet<ABinaryOperator> BOOLEAN_BINARY_OPERATORS =
      ImmutableSet.of(
          org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.EQUALS,
          org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.NOT_EQUALS,
          org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_EQUAL,
          org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.GREATER_THAN,
          org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_EQUAL,
          org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_THAN,
          org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.EQUALS,
          org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.NOT_EQUALS,
          org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.GREATER_EQUAL,
          org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.GREATER_THAN,
          org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.LESS_EQUAL,
          org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator.LESS_THAN);

  private static boolean isBooleanExpression(AExpression pExpression) {
    return pExpression instanceof ABinaryExpression
        && BOOLEAN_BINARY_OPERATORS.contains(((ABinaryExpression) pExpression).getOperator());
  }

  private S notImplemented() throws AssertionError {
    throw new AssertionError(
        "this method is not implemented in subclass " + this.getClass().getSimpleName());
  }
}
