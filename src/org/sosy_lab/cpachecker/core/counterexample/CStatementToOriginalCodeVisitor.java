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

/**
 * Like toASTString, but with original names.
 *
 * NOT necessarily equivalent to specific parts of the original code file.
 */
enum CStatementToOriginalCodeVisitor implements CStatementVisitor<String, RuntimeException> {

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

    String leftHandSide = pIastExpressionAssignmentStatement.getLeftHandSide().accept(expressionToOrinalCodeVisitor);
    String rightHandSide = pIastExpressionAssignmentStatement.getRightHandSide().accept(expressionToOrinalCodeVisitor);

    return leftHandSide + " == " + rightHandSide + "; ";
  }

  @Override
  public String visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {

    CExpressionToOrinalCodeVisitor expressionToOrinalCodeVisitor =
        CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER;

    String leftHandSide = pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(expressionToOrinalCodeVisitor);
    String rightHandSide = handleFunctionCallExpression(
        pIastFunctionCallAssignmentStatement.getFunctionCallExpression());

    return leftHandSide
        + " == "
        + rightHandSide
        + "; ";
  }

  @Override
  public String visit(CFunctionCallStatement pIastFunctionCallStatement) {
    return handleFunctionCallExpression(pIastFunctionCallStatement.getFunctionCallExpression()) + ";";
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
                pInput ->
                    pInput.<String, RuntimeException>accept(
                        CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER)));
    lASTString.append(")");

    return lASTString.toString();
  }

  private static String parenthesize(CExpression pInput) {
    return CExpressionToOrinalCodeVisitor.BASIC_TRANSFORMER.parenthesize(pInput);
  }

}
