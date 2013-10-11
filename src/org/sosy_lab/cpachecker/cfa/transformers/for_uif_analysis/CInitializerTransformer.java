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

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


class CInitializerTransformer implements CInitializerVisitor<CInitializer, UnrecognizedCCodeException> {

  @Override
  public CInitializer visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
    		                                    "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CCastExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CFieldReference e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CIdExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CCharLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CFloatLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CIntegerLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CStringLiteralExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CTypeIdExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CTypeIdInitializerExpression e)
  throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    throw new UnsupportedOperationException("the method shouldn't be called: use the one from " +
                                            "CExpressionTransformer instead");
  }

  @Override
  public CInitializer visit(final CInitializerExpression e) throws UnrecognizedCCodeException {
    final CExpression oldExpression = e.getExpression();
    final CExpression expression = (CExpression) oldExpression.accept(expressionTransformer);

    return expression == oldExpression ? e :
           new CInitializerExpression(e.getFileLocation(), expression);
  }

  @Override
  public CInitializerList visit(final CInitializerList e) throws UnrecognizedCCodeException {
    List<CInitializer> initializers = null;
    int i = 0;
    for (CInitializer oldInitializer : e.getInitializers()) {
      final CInitializer initializer = oldInitializer.accept(this);
      if (initializer != oldInitializer && initializers == null) {
        initializers = new ArrayList<>();
        initializers.addAll(e.getInitializers().subList(0, i));
      }
      if (initializers != null) {
        initializers.add(initializer);
      }
      ++i;
    }

    return initializers == null ? e :
           new CInitializerList(e.getFileLocation(),
                                initializers);
  }

  @Override
  public CDesignatedInitializer visit(final CDesignatedInitializer e) throws UnrecognizedCCodeException {
    final CInitializer oldRhs = e.getRightHandSide();
    final CInitializer rhs = oldRhs.accept(this);

    return rhs == oldRhs ? e :
           new CDesignatedInitializer(e.getFileLocation(),
                                      e.getLeftHandSide(),
                                      rhs);
  }

  CInitializerTransformer(final CExpressionTransformer expressionTransformer) {
    this.expressionTransformer = expressionTransformer;
  }

  private final CExpressionTransformer expressionTransformer;
}
