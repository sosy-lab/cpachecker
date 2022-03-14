// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Like toASTString, but with original names.
 *
 * <p>NOT necessarily equivalent to specific parts of the original code file.
 */
enum CStatementToOriginalCodeVisitor implements CStatementVisitor<String, NoException> {
  INSTANCE;

  @Override
  public String visit(CExpressionStatement pIastExpressionStatement) {
    return pIastExpressionStatement
            .getExpression()
            .accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER)
        + ";";
  }

  @Override
  public String visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {

    CExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor =
        CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER;

    String leftHandSide =
        pIastExpressionAssignmentStatement.getLeftHandSide().accept(expressionToOrinalCodeVisitor);
    String rightHandSide =
        pIastExpressionAssignmentStatement.getRightHandSide().accept(expressionToOrinalCodeVisitor);

    return leftHandSide + " == " + rightHandSide + "; ";
  }

  @Override
  public String visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {

    CExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor =
        CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER;

    String leftHandSide =
        pIastFunctionCallAssignmentStatement
            .getLeftHandSide()
            .accept(expressionToOrinalCodeVisitor);
    String rightHandSide =
        handleFunctionCallExpression(
            pIastFunctionCallAssignmentStatement.getFunctionCallExpression());

    return leftHandSide + " == " + rightHandSide + "; ";
  }

  @Override
  public String visit(CFunctionCallStatement pIastFunctionCallStatement) {
    return handleFunctionCallExpression(pIastFunctionCallStatement.getFunctionCallExpression())
        + ";";
  }

  private static String handleFunctionCallExpression(
      CFunctionCallExpression pFunctionCallExpression) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append(parenthesize(pFunctionCallExpression.getFunctionNameExpression()));
    lASTString.append("(");
    Joiner.on(", ")
        .appendTo(
            lASTString,
            transform(
                pFunctionCallExpression.getParameterExpressions(),
                pInput -> pInput.accept(CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER)));
    lASTString.append(")");

    return lASTString.toString();
  }

  private static String parenthesize(CExpression pInput) {
    return CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER.parenthesize(pInput);
  }
}
