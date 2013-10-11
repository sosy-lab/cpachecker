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
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.ForwardingCExpressionVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


class CInitializerTransformer extends ForwardingCExpressionVisitor<CAstNode, UnrecognizedCCodeException>
                              implements CInitializerVisitor<CAstNode, UnrecognizedCCodeException> {

  @Override
  public CInitializer visit(final CInitializerExpression e) throws UnrecognizedCCodeException {
    final CExpression oldExpression = e.getExpression();
    final CExpression expression = (CExpression) oldExpression.accept(this);

    return expression == oldExpression ? e :
           new CInitializerExpression(e.getFileLocation(), expression);
  }

  @Override
  public CInitializerList visit(final CInitializerList e) throws UnrecognizedCCodeException {
    List<CInitializer> initializers = null;
    int i = 0;
    for (CInitializer oldInitializer : e.getInitializers()) {
      final CInitializer initializer = (CInitializer) oldInitializer.accept(this);
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
    final CInitializer rhs = (CInitializer) oldRhs.accept(this);

    return rhs == oldRhs ? e :
           new CDesignatedInitializer(e.getFileLocation(),
                                      e.getDesignators(),
                                      rhs);
  }

  CInitializerTransformer(final CExpressionVisitor<CAstNode, UnrecognizedCCodeException> delegate) {
    super(delegate);
  }
}
