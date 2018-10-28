/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

public class ExpressionListCFABuilder implements ExpressionListAppendable {
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public List<JSExpression> append(
      final JavaScriptCFABuilder pBuilder, final List<Expression> pExpressions) {
    // TODO more than 2 expressions
    // TODO consider side effects in expressions
    if (pExpressions.size() == 1) {
      return ImmutableList.of(pBuilder.append(pExpressions.get(0)));
    }
    if (!hasSideEffects(pExpressions.get(1))) {
      assert pExpressions.size() == 2;
      return ImmutableList.of(
          pBuilder.append(pExpressions.get(0)), pBuilder.append(pExpressions.get(1)));
    }
    // Expressions that are evaluated before the expression with the side effect might be effected.
    //
    // For example:
    //   The expression `++x` has a side effect on the AST that influences all read accesses of `x`:
    //
    //     result = x + (++x)
    //
    //   In this example, `x` on the left side of the expression has to be evaluated before `x` is
    //   incremented by `++x`. If `x` is `0` before the example is evaluated then the expression
    //   evaluates to `0 + 1`, which is `1`.
    //
    //   Without considering side effects, the example would be transformed to:
    //
    //     x = x + 1
    //     result = x + x;
    //
    //   But in case `x` is `0` before the example is evaluated, the result is `1 + 1`,
    //   which is `2` and not `1` as expected.
    //   To consider the side effect, the result of reading `x` the first time has to be stored in
    //   a temporary variable before the side effect edges and nodes are added to the AST:
    //
    //     var tmp = x;
    //     x = x + 1
    //     result = tmp + x;
    //
    return pExpressions
        .stream()
        .map(
            pExpression -> {
              final String tmpVariableName = pBuilder.generateVariableName();
              final JSVariableDeclaration tmpVariableDeclaration =
                  new JSVariableDeclaration(
                      FileLocation.DUMMY,
                      ScopeConverter.toCFAScope(pBuilder.getScope()),
                      tmpVariableName,
                      tmpVariableName,
                      pBuilder.getScope().qualifiedVariableNameOf(tmpVariableName),
                      new JSInitializerExpression(
                          FileLocation.DUMMY, pBuilder.append(pExpression)));
              pBuilder.appendEdge(
                  (pPredecessor, pSuccessor) ->
                      new JSDeclarationEdge(
                          tmpVariableDeclaration.toASTString(),
                          FileLocation.DUMMY,
                          pPredecessor,
                          pSuccessor,
                          tmpVariableDeclaration));
              return new JSIdExpression(
                  FileLocation.DUMMY, tmpVariableName, tmpVariableDeclaration);
            })
        .collect(Collectors.toList());
  }

  private boolean hasSideEffects(final Expression pExpression) {
    return true; // TODO
  }
}
