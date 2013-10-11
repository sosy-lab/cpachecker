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
package org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


public class CStatementTransformer implements CStatementVisitor<CStatement, UnrecognizedCCodeException> {

  @Override
  public CExpressionStatement visit(final CExpressionStatement s) throws UnrecognizedCCodeException {
    final CExpression oldExpression = s.getExpression();
    final CExpression expression = (CExpression) oldExpression.accept(expressionVisitor);

    return expression == oldExpression ? s :
           new CExpressionStatement(s.getFileLocation(), expression);
  }

  @Override
  public CExpressionAssignmentStatement visit(final CExpressionAssignmentStatement s)
  throws UnrecognizedCCodeException {
    final CExpression oldLhs = s.getLeftHandSide();
    final CExpression oldRhs = s.getRightHandSide();
    final CExpression lhs = (CExpression) oldLhs.accept(expressionVisitor);
    final CExpression rhs = (CExpression) oldRhs.accept(expressionVisitor);

    return lhs == oldLhs && rhs == oldRhs ? s :
           new CExpressionAssignmentStatement(s.getFileLocation(), lhs, rhs);
  }

  private CFunctionCallExpression visitCFunctionCallExpression(final CFunctionCallExpression e)
  throws UnrecognizedCCodeException {
    final CExpression oldFunctionName = e.getFunctionNameExpression();
    final CExpression functionName = (CExpression) oldFunctionName.accept(expressionVisitor);
    final CType oldExpressionType = e.getExpressionType();
    final CType expressionType = oldExpressionType.accept(typeVisitor);

    List<CExpression> parameters = null;
    int i = 0;
    for (CExpression oldParameter : e.getParameterExpressions()) {
      final CExpression parameter = (CExpression) oldParameter.accept(expressionVisitor);
      if (parameter != oldParameter && parameters == null) {
        parameters = new ArrayList<>();
        parameters.addAll(e.getParameterExpressions().subList(0, i));
      }
      if (parameters != null) {
        parameters.add(parameter);
      }
      ++i;
    }

    return oldFunctionName == functionName && parameters == null && expressionType == oldExpressionType ? e :
           new CFunctionCallExpression(e.getFileLocation(),
                                       expressionType,
                                       functionName,
                                       parameters != null ? parameters : e.getParameterExpressions(),
                                       e.getDeclaration());
  }

  @Override
  public CFunctionCallAssignmentStatement visit(final CFunctionCallAssignmentStatement s)
  throws UnrecognizedCCodeException {
    final CExpression oldLhs = s.getLeftHandSide();
    final CFunctionCallExpression oldFunctionCall = s.getRightHandSide();
    final CExpression lhs = (CExpression) oldLhs.accept(expressionVisitor);
    final CFunctionCallExpression functionCall = visitCFunctionCallExpression(oldFunctionCall);

    return lhs == oldLhs && functionCall == oldFunctionCall ? s :
           new CFunctionCallAssignmentStatement(s.getFileLocation(),
                                                lhs,
                                                functionCall);
  }

  @Override
  public CFunctionCallStatement visit(final CFunctionCallStatement s) throws UnrecognizedCCodeException {
    final CFunctionCallExpression oldFunctionCall = s.getFunctionCallExpression();
    final CFunctionCallExpression functionCall = visitCFunctionCallExpression(oldFunctionCall);

    return functionCall == oldFunctionCall ? s : new CFunctionCallStatement(s.getFileLocation(), functionCall);
  }

  public CStatementTransformer(final CTypeVisitor<CType, RuntimeException> typeVisitor,
                               final CExpressionVisitor<CAstNode, UnrecognizedCCodeException> expressionVisitor) {
    this.typeVisitor = typeVisitor;
    this.expressionVisitor = expressionVisitor;
  }

  private final CExpressionVisitor<CAstNode, UnrecognizedCCodeException> expressionVisitor;
  private final CTypeVisitor<CType, RuntimeException> typeVisitor;
}
