// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
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
import org.sosy_lab.cpachecker.util.Pair;

/**
 * This Transfer-Relation forwards the method 'getAbstractSuccessors()' to an edge-specific
 * sub-method ('AssumeEdge', 'DeclarationEdge', ...). It handles all casting of the edges and their
 * information. There is always an abstract method, that calls either the matching C- or
 * Java-Methods, depending on the type of the edge. A developer should override the methods to get a
 * valid analysis.
 *
 * <p>The following structure shows the control-flow (work-flow) of this class.
 *
 * <p>The tuple (C,J) represents the call of C- or Java-specific methods. A user can either override
 * the method itself, or the C- or Java-specific method. If a C- or Java-specific method is called,
 * but not overridden, it throws an assertion.
 *
 * <ol>
 *   <li>setInfo
 *   <li>preCheck
 *   <li>getAbstractSuccessors:
 *       <ul>
 *         <li>handleAssumption -> C,J
 *         <li>handleFunctionCallEdge -> C,J
 *         <li>handleFunctionReturnEdge -> C,J
 *         <li>handleMultiEdge
 *         <li>handleSimpleEdge:
 *         <li>handleDeclarationEdge -> C,J
 *         <li>handleStatementEdge -> C,J
 *         <li>handleReturnStatementEdge -> C,J
 *         <li>handleBlankEdge
 *         <li>handleFunctionSummaryEdge
 *       </ul>
 *   <li>postProcessing
 *   <li>resetInfo
 * </ol>
 *
 * Generics:
 *
 * <ul>
 *   <li>S type of intermediate result, should be equal to T or Collection<T>, should be
 *       converted/copied into an Object of type Collection<T> in method 'postProcessing'.
 *   <li>T type of State
 *   <li>P type of Precision
 * </ul>
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
   * This is the main method that delegates the control-flow to the corresponding edge-type-specific
   * methods. In most cases there is no need to override this method.
   */
  @Override
  public Collection<T> getAbstractSuccessorsForEdge(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    setInfo(abstractState, abstractPrecision, cfaEdge);

    final Collection<T> preCheck = preCheck(state, precision);
    if (preCheck != null) {
      return preCheck;
    }

    final S successor =
        switch (cfaEdge.getEdgeType()) {
          case AssumeEdge -> {
            final AssumeEdge assumption = (AssumeEdge) cfaEdge;
            yield handleAssumption(
                assumption, assumption.getExpression(), assumption.getTruthAssumption());
          }
          case FunctionCallEdge -> {
            final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
            final FunctionEntryNode succ = fnkCall.getSuccessor();
            final String calledFunctionName = succ.getFunctionName();
            yield handleFunctionCallEdge(
                fnkCall, fnkCall.getArguments(), succ.getFunctionParameters(), calledFunctionName);
          }
          case FunctionReturnEdge -> {
            final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
            final FunctionReturnEdge fnkReturnEdge = (FunctionReturnEdge) cfaEdge;
            yield handleFunctionReturnEdge(
                fnkReturnEdge, fnkReturnEdge.getFunctionCall(), callerFunctionName);
          }
          case DeclarationEdge -> {
            final ADeclarationEdge declarationEdge = (ADeclarationEdge) cfaEdge;
            yield handleDeclarationEdge(declarationEdge, declarationEdge.getDeclaration());
          }
          case StatementEdge -> {
            final AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
            yield handleStatementEdge(statementEdge, statementEdge.getStatement());
          }
          case ReturnStatementEdge -> { // this statement is a function return, e.g. return (a);
            // note that this is different from return edge,
            // this is a statement edge, which leads the function to the
            // last node of its CFA, where return edge is from that last node
            // to the return site of the caller function
            final AReturnStatementEdge returnEdge = (AReturnStatementEdge) cfaEdge;
            yield handleReturnStatementEdge(returnEdge);
          }
          case BlankEdge -> handleBlankEdge((BlankEdge) cfaEdge);
          case CallToReturnEdge -> handleFunctionSummaryEdge((FunctionSummaryEdge) cfaEdge);
        };

    final Collection<T> result = postProcessing(successor, cfaEdge);

    resetInfo();

    return result;
  }

  @SuppressWarnings("unchecked")
  protected void setInfo(
      final AbstractState abstractState, final Precision abstractPrecision, final CFAEdge cfaEdge) {
    state = (T) abstractState;
    precision = (P) abstractPrecision;
    functionName = cfaEdge.getPredecessor().getFunctionName();
  }

  protected void resetInfo() {
    state = null;
    precision = null;
    functionName = null;
  }

  /**
   * This is a fast check, if the edge should be analyzed. It returns NULL for further processing,
   * otherwise the return-value for skipping.
   */
  @SuppressWarnings("unused")
  protected @Nullable Collection<T> preCheck(T pState, P pPrecision) {
    return null;
  }

  /**
   * This method should convert/cast/copy the intermediate result into a Collection<T>. This method
   * can modify the successor, if needed.
   */
  @SuppressWarnings({"unchecked", "unused"})
  protected Collection<T> postProcessing(@Nullable S successor, CFAEdge edge) {
    if (successor == null) {
      return ImmutableSet.of();
    } else {
      return Collections.singleton((T) successor);
    }
  }

  /**
   * This function handles assumptions like "if(a==b)" and "if(a!=0)". If the assumption is not
   * fulfilled, NULL should be returned.
   */
  protected @Nullable S handleAssumption(
      AssumeEdge cfaEdge, AExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {

    Pair<AExpression, Boolean> simplifiedExpression =
        simplifyAssumption(expression, truthAssumption);
    expression = simplifiedExpression.getFirst();
    truthAssumption = simplifiedExpression.getSecond();

    if (cfaEdge instanceof CAssumeEdge cAssumeEdge) {
      return handleAssumption(cAssumeEdge, (CExpression) expression, truthAssumption);

    } else if (cfaEdge instanceof JAssumeEdge jAssumeEdge) {
      return handleAssumption(jAssumeEdge, (JExpression) expression, truthAssumption);

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
   * @throws InterruptedException may be thrown in subclasses
   */
  protected @Nullable S handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {
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
  protected S handleFunctionCallEdge(
      FunctionCallEdge cfaEdge,
      List<? extends AExpression> arguments,
      List<? extends AParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionCallEdge cFunctionCallEdge) {
      return handleFunctionCallEdge(
          cFunctionCallEdge,
          (List<CExpression>) arguments,
          (List<CParameterDeclaration>) parameters,
          calledFunctionName);

    } else if (cfaEdge instanceof JMethodCallEdge jMethodCallEdge) {
      return handleFunctionCallEdge(
          jMethodCallEdge,
          (List<JExpression>) arguments,
          (List<JParameterDeclaration>) parameters,
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
  protected S handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
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
  protected S handleFunctionCallEdge(
      JMethodCallEdge cfaEdge,
      List<JExpression> arguments,
      List<JParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    return notImplemented();
  }

  /** This function handles functionReturns like "y=f(x)". */
  protected S handleFunctionReturnEdge(
      FunctionReturnEdge cfaEdge, AFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionReturnEdge cFunctionReturnEdge) {
      return handleFunctionReturnEdge(
          cFunctionReturnEdge, (CFunctionCall) summaryExpr, callerFunctionName);

    } else if (cfaEdge instanceof JMethodReturnEdge jMethodReturnEdge) {
      return handleFunctionReturnEdge(
          jMethodReturnEdge, (JMethodOrConstructorInvocation) summaryExpr, callerFunctionName);

    } else {
      throw new AssertionError("unknown edge");
    }
  }

  /**
   * Handles the {@link CFunctionReturnEdge}
   *
   * @param cfaEdge the edge to handle
   * @param summaryExpr the function call
   * @param callerFunctionName the name of the called function
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JMethodReturnEdge}
   *
   * @param cfaEdge the edge to handle
   * @param summaryExpr the function call
   * @param callerFunctionName the name of the called function
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleFunctionReturnEdge(
      JMethodReturnEdge cfaEdge,
      JMethodOrConstructorInvocation summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    return notImplemented();
  }

  /** This function handles declarations like "int a = 0;" and "int b = !a;". */
  protected S handleDeclarationEdge(ADeclarationEdge cfaEdge, ADeclaration decl)
      throws CPATransferException {
    if (cfaEdge instanceof CDeclarationEdge cDeclarationEdge) {
      return handleDeclarationEdge(cDeclarationEdge, (CDeclaration) decl);

    } else if (cfaEdge instanceof JDeclarationEdge jDeclarationEdge) {
      return handleDeclarationEdge(jDeclarationEdge, (JDeclaration) decl);

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

  /**
   * This function handles statements like "a = 0;" and "b = !a;" and calls of external functions.
   */
  protected S handleStatementEdge(AStatementEdge cfaEdge, AStatement statement)
      throws CPATransferException {
    if (cfaEdge instanceof CStatementEdge cStatementEdge) {
      return handleStatementEdge(cStatementEdge, (CStatement) statement);

    } else if (cfaEdge instanceof JStatementEdge jStatementEdge) {
      return handleStatementEdge(jStatementEdge, (JStatement) statement);

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
  protected S handleReturnStatementEdge(AReturnStatementEdge cfaEdge) throws CPATransferException {
    if (cfaEdge instanceof CReturnStatementEdge cReturnStatementEdge) {
      return handleReturnStatementEdge(cReturnStatementEdge);

    } else if (cfaEdge instanceof JReturnStatementEdge jReturnStatementEdge) {
      return handleReturnStatementEdge(jReturnStatementEdge);

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
  protected S handleReturnStatementEdge(CReturnStatementEdge cfaEdge) throws CPATransferException {
    return notImplemented();
  }

  /**
   * Handles the {@link JReturnStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  protected S handleReturnStatementEdge(JReturnStatementEdge cfaEdge) throws CPATransferException {
    return notImplemented();
  }

  /**
   * This function handles blank edges, that are used for plain connectors in the CFA. This default
   * implementation returns the input-state. A blank edge can also be a default-return-edge for a
   * function "void f()". In that case the successor-node is a FunctionExitNode.
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  @SuppressWarnings("unchecked")
  protected S handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    return (S) state;
  }

  protected S handleFunctionSummaryEdge(FunctionSummaryEdge cfaEdge) throws CPATransferException {
    if (cfaEdge instanceof CFunctionSummaryEdge cFunctionSummaryEdge) {
      return handleFunctionSummaryEdge(cFunctionSummaryEdge);
    } else if (cfaEdge instanceof JMethodSummaryEdge jMethodSummaryEdge) {
      return handleFunctionSummaryEdge(jMethodSummaryEdge);
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
    if (exp instanceof CExpression cExpression) {
      return isGlobal(cExpression);
    } else if (exp instanceof JExpression jExpression) {
      return isGlobal(jExpression);
    } else {
      throw new AssertionError("unknown expression: " + exp);
    }
  }

  protected static boolean isGlobal(final CExpression exp) {
    if (exp instanceof CIdExpression cIdExpression) {
      CSimpleDeclaration decl = cIdExpression.getDeclaration();
      if (decl instanceof CDeclaration cDeclaration) {
        return cDeclaration.isGlobal();
      }
    }
    return false;
  }

  protected static boolean isGlobal(final JExpression exp) {
    if (exp instanceof JIdExpression jIdExpression) {
      JSimpleDeclaration decl = jIdExpression.getDeclaration();

      if (decl instanceof ADeclaration aDeclaration) {
        return aDeclaration.isGlobal();
      }
    }

    return false;
  }

  protected static Pair<AExpression, Boolean> simplifyAssumption(
      AExpression pExpression, boolean pAssumeTruth) {
    if (isBooleanExpression(pExpression)) {
      if (pExpression instanceof CBinaryExpression binExp) {
        BinaryOperator operator = binExp.getOperator();
        if (isBooleanExpression(binExp.getOperand1())
            && binExp.getOperand2().equals(CIntegerLiteralExpression.ZERO)) {
          if (operator == BinaryOperator.EQUALS) {
            return simplifyAssumption(binExp.getOperand1(), !pAssumeTruth);
          } else if (operator == BinaryOperator.NOT_EQUALS) {
            return simplifyAssumption(binExp.getOperand1(), pAssumeTruth);
          } // TODO what else?
        } else if (isBooleanExpression(binExp.getOperand2())
            && binExp.getOperand1().equals(CIntegerLiteralExpression.ZERO)) {
          if (operator == BinaryOperator.EQUALS) {
            return simplifyAssumption(binExp.getOperand2(), !pAssumeTruth);
          } else if (operator == BinaryOperator.NOT_EQUALS) {
            return simplifyAssumption(binExp.getOperand2(), pAssumeTruth);
          } // TODO what else?
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
    return pExpression instanceof ABinaryExpression aBinaryExpression
        && BOOLEAN_BINARY_OPERATORS.contains(aBinaryExpression.getOperator());
  }

  private S notImplemented() throws AssertionError {
    throw new AssertionError(
        "this method is not implemented in subclass " + getClass().getSimpleName());
  }
}
