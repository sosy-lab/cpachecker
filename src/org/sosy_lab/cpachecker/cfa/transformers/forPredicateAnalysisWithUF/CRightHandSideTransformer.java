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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.ForwardingCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


class CRightHandSideTransformer extends ForwardingCExpressionVisitor<CAstNode, UnrecognizedCCodeException>
                                implements CRightHandSideVisitor<CAstNode, UnrecognizedCCodeException> {

  @Override
  public CFunctionCallExpression visit(final CFunctionCallExpression e) throws UnrecognizedCCodeException {
    final CExpression oldFunctionNameExpression = e.getFunctionNameExpression();
    final CExpression functionNameExpression = (CExpression) oldFunctionNameExpression.accept(this);
    final CType oldExpressionType = e.getExpressionType();
    final CType expressionType = oldExpressionType.accept(typeVisitor);

    List<CExpression> parameters = null;
    int i = 0;
    for (CExpression oldParameter : e.getParameterExpressions()) {
      final CExpression parameter = (CExpression) oldParameter.accept(this);
      if (parameter != oldParameter && parameters == null) {
        parameters = new ArrayList<>();
        parameters.addAll(e.getParameterExpressions().subList(0, i));
      }
      if (parameters != null) {
        parameters.add(parameter);
      }
      ++i;
    }

    return functionNameExpression == oldFunctionNameExpression &&
           parameters == null  &&
           expressionType == oldExpressionType? e :
           new CFunctionCallExpression(e.getFileLocation(),
                                       expressionType,
                                       functionNameExpression,
                                       parameters != null ? parameters : e.getParameterExpressions(),
                                       e.getDeclaration());
  }

  CRightHandSideTransformer(final CTypeVisitor<CType, RuntimeException> typeVisitor,
                            final CExpressionTransformer delegate) {
    super(delegate);
    this.typeVisitor = typeVisitor;
  }

  private final CTypeVisitor<CType, RuntimeException> typeVisitor;
}
