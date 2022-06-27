// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

public interface CFAEdgeVisitor<ReturnType> {

  public default ReturnType visit(CFAEdge cfaEdge) throws CPATransferException, InterruptedException {
    switch (cfaEdge.getEdgeType()) {
      case AssumeEdge:
        final AssumeEdge assumption = (AssumeEdge) cfaEdge;
        return handleAssumption(
            assumption, assumption.getExpression(), assumption.getTruthAssumption());

      case FunctionCallEdge:
        final FunctionCallEdge fnkCall = (FunctionCallEdge) cfaEdge;
        final FunctionEntryNode succ = fnkCall.getSuccessor();
        final String calledFunctionName = succ.getFunctionName();
        return handleFunctionCallEdge(
            fnkCall, fnkCall.getArguments(), succ.getFunctionParameters(), calledFunctionName);

      case FunctionReturnEdge:
        final String callerFunctionName = cfaEdge.getSuccessor().getFunctionName();
        final FunctionReturnEdge fnkReturnEdge = (FunctionReturnEdge) cfaEdge;
        final FunctionSummaryEdge summaryEdge = fnkReturnEdge.getSummaryEdge();
        return handleFunctionReturnEdge(
            fnkReturnEdge, summaryEdge, summaryEdge.getExpression(), callerFunctionName);

      case DeclarationEdge:
        final ADeclarationEdge declarationEdge = (ADeclarationEdge) cfaEdge;
        return handleDeclarationEdge(declarationEdge, declarationEdge.getDeclaration());

      case StatementEdge:
        final AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
        return handleStatementEdge(statementEdge, statementEdge.getStatement());

      case ReturnStatementEdge:
        // this statement is a function return, e.g. return (a);
        // note that this is different from return edge,
        // this is a statement edge, which leads the function to the
        // last node of its CFA, where return edge is from that last node
        // to the return site of the caller function
        final AReturnStatementEdge returnEdge = (AReturnStatementEdge) cfaEdge;
        return handleReturnStatementEdge(returnEdge);

      case BlankEdge:
        return handleBlankEdge((BlankEdge) cfaEdge);

      case CallToReturnEdge:
        return handleFunctionSummaryEdge((FunctionSummaryEdge) cfaEdge);

      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
  }

  /**
   * This function handles assumptions like "if(a==b)" and "if(a!=0)". If the assumption is not
   * fulfilled, NULL should be returned.
   */
  public default @Nullable ReturnType handleAssumption(
      AssumeEdge cfaEdge, AExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {

    Pair<AExpression, Boolean> simplifiedExpression =
        simplifyAssumption(expression, truthAssumption);
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
   * @throws InterruptedException may be thrown in subclasses
   */
  public default @Nullable ReturnType handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {
    return defaultValue();
  }

  /**
   * Handles the {@link JAssumeEdge}
   *
   * @param cfaEdge the edge to handle
   * @param expression the condition of the edge
   * @param truthAssumption indicates if this is the then or the else branch
   * @throws CPATransferException may be thrown in subclasses
   */
  public default @Nullable ReturnType handleAssumption(
      JAssumeEdge cfaEdge, JExpression expression, boolean truthAssumption)
      throws CPATransferException {
    return defaultValue();
  }

  /** This function handles functioncalls like "f(x)", that calls "f(int a)". */
  @SuppressWarnings("unchecked")
  public default ReturnType handleFunctionCallEdge(
      FunctionCallEdge cfaEdge,
      List<? extends AExpression> arguments,
      List<? extends AParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionCallEdge) {
      return handleFunctionCallEdge(
          (CFunctionCallEdge) cfaEdge,
          (List<CExpression>) arguments,
          (List<CParameterDeclaration>) parameters,
          calledFunctionName);

    } else if (cfaEdge instanceof JMethodCallEdge) {
      return handleFunctionCallEdge(
          (JMethodCallEdge) cfaEdge,
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
  public default ReturnType handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    return defaultValue();
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
  public default ReturnType handleFunctionCallEdge(
      JMethodCallEdge cfaEdge,
      List<JExpression> arguments,
      List<JParameterDeclaration> parameters,
      String calledFunctionName)
      throws CPATransferException {
    return defaultValue();
  }

  /** This function handles functionReturns like "y=f(x)". */
  public default ReturnType handleFunctionReturnEdge(
      FunctionReturnEdge cfaEdge,
      FunctionSummaryEdge fnkCall,
      AFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionReturnEdge) {
      return handleFunctionReturnEdge(
          (CFunctionReturnEdge) cfaEdge,
          (CFunctionSummaryEdge) fnkCall,
          (CFunctionCall) summaryExpr,
          callerFunctionName);

    } else if (cfaEdge instanceof JMethodReturnEdge) {
      return handleFunctionReturnEdge(
          (JMethodReturnEdge) cfaEdge,
          (JMethodSummaryEdge) fnkCall,
          (JMethodOrConstructorInvocation) summaryExpr,
          callerFunctionName);

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
  public default ReturnType handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    return defaultValue();
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
  public default ReturnType handleFunctionReturnEdge(
      JMethodReturnEdge cfaEdge,
      JMethodSummaryEdge fnkCall,
      JMethodOrConstructorInvocation summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    return defaultValue();
  }

  /** This function handles declarations like "int a = 0;" and "int b = !a;". */
  public default ReturnType handleDeclarationEdge(ADeclarationEdge cfaEdge, ADeclaration decl)
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
  public default ReturnType handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {
    return defaultValue();
  }

  /**
   * Handles the {@link JDeclarationEdge}
   *
   * @param cfaEdge the edge to handle
   * @param decl the declaration at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  public default ReturnType handleDeclarationEdge(JDeclarationEdge cfaEdge, JDeclaration decl)
      throws CPATransferException {
    return defaultValue();
  }

  /**
   * This function handles statements like "a = 0;" and "b = !a;" and calls of external functions.
   */
  public default ReturnType handleStatementEdge(AStatementEdge cfaEdge, AStatement statement)
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
  public default ReturnType handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    return defaultValue();
  }

  /**
   * Handles the {@link JStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @param statement the statement at the given edge
   * @throws CPATransferException may be thrown in subclasses
   */
  public default ReturnType handleStatementEdge(JStatementEdge cfaEdge, JStatement statement)
      throws CPATransferException {
    return defaultValue();
  }

  /** This function handles functionStatements like "return (x)". */
  public default ReturnType handleReturnStatementEdge(AReturnStatementEdge cfaEdge)
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
  public default ReturnType handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {
    return defaultValue();
  }

  /**
   * Handles the {@link JReturnStatementEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  public default ReturnType handleReturnStatementEdge(JReturnStatementEdge cfaEdge)
      throws CPATransferException {
    return defaultValue();
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
  public default ReturnType handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    return defaultValue();
  }

  public default ReturnType handleFunctionSummaryEdge(FunctionSummaryEdge cfaEdge)
      throws CPATransferException {
    if (cfaEdge instanceof CFunctionSummaryEdge) {
      return handleFunctionSummaryEdge((CFunctionSummaryEdge) cfaEdge);
    } else if (cfaEdge instanceof JMethodSummaryEdge) {
      return handleFunctionSummaryEdge((JMethodSummaryEdge) cfaEdge);
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
  public default ReturnType handleFunctionSummaryEdge(CFunctionSummaryEdge cfaEdge)
      throws CPATransferException {
    return defaultValue();
  }

  /**
   * Handle the {@link JMethodSummaryEdge}
   *
   * @param cfaEdge the edge to handle
   * @throws CPATransferException may be thrown in subclasses
   */
  public default ReturnType handleFunctionSummaryEdge(JMethodSummaryEdge cfaEdge)
      throws CPATransferException {
    return defaultValue();
  }

  public static Pair<AExpression, Boolean> simplifyAssumption(
      AExpression pExpression, boolean pAssumeTruth) {
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

  public static final ImmutableSet<ABinaryOperator> BOOLEAN_BINARY_OPERATORS =
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

  public default ReturnType defaultValue() {
    throw new UnsupportedOperationException(
        "this method is not implemented in subclass " + this.getClass().getSimpleName());
  }
}
