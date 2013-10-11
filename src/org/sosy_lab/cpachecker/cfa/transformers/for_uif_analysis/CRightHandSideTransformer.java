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
package org.sosy_lab.cpachecker.cfa.transformers.for_uif_analysis;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


class CRightHandSideTransformer implements CRightHandSideVisitor<ARightHandSide, UnrecognizedCCodeException> {

  @Override
  public ARightHandSide visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CCastExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CFieldReference e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CIdExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CCharLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CFloatLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CStringLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CTypeIdInitializerExpression e)
  throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public ARightHandSide visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CFunctionCallExpression visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression oldFunctionNameExpression = e.getFunctionNameExpression();
    final CExpression functionNameExpression = (CExpression) oldFunctionNameExpression.accept(expressionTransformer);

    List<CExpression> parameters = null;
    int i = 0;
    for (CExpression oldParameter : e.getParameterExpressions()) {
      final CExpression parameter = (CExpression) oldParameter.accept(expressionTransformer);
      if (parameter != oldParameter && parameters == null) {
        parameters = new ArrayList<>();
        parameters.addAll(e.getParameterExpressions().subList(0, i));
      }
      if (parameters != null) {
        parameters.add(parameter);
      }
      ++i;
    }

    return functionNameExpression == oldFunctionNameExpression && parameters == null ? e :
           new CFunctionCallExpression(e.getFileLocation(),
                                       e.getExpressionType(),
                                       functionNameExpression,
                                       parameters,
                                       e.getDeclaration());
  }


  CRightHandSideTransformer(final CExpressionTransformer expressionTransformer) {
    this.expressionTransformer = expressionTransformer;
  }

  private final CExpressionTransformer expressionTransformer;
}
