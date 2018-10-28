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
import com.google.common.collect.ImmutableList.Builder;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.wst.jsdt.core.dom.Expression;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.js.JSDeclarationEdge;

public class ExpressionListCFABuilder implements ExpressionListAppendable {
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public List<JSExpression> append(
      final JavaScriptCFABuilder pBuilder, final List<Expression> pExpressions) {
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
    // A side effect in an expression might change the result of any previous expression in the
    // expression-list. Therefore, every expression previous to an expression with a side effect
    // is assigned to a temporary variable.
    final Builder<JSExpression> resultBuilder = ImmutableList.builder();
    final List<JSExpression> sideEffectFreeExpressions = new ArrayList<>();
    for (final Expression e : pExpressions) {
      // An expression has a side effect if an edge is added to the CFA.
      // A copy of the builder is used to check if the expression has a side effect.
      // If a side effect has occurred then it is undone by removing the added edges.
      // Thereby, the exit node of the builder gets invalid (removed).
      // A copy of the builder is used to be able to continue from the old exit node after the
      // side effect has been undone.
      final JavaScriptCFABuilder forwardLookingBuilder = pBuilder.copy();
      final CFANode oldExitNode = pBuilder.getExitNode();
      assert oldExitNode.getNumLeavingEdges() == 0 : "unexpected leaving edges on an exit node";
      final JSExpression jsExpression = forwardLookingBuilder.append(e);
      if (oldExitNode.getNumLeavingEdges() > 0) {
        // undo side effect
        while (oldExitNode.getNumLeavingEdges() > 0) {
          final CFAEdge sideEffectEdge = oldExitNode.getLeavingEdge(0);
          CFACreationUtils.removeEdgeFromNodes(sideEffectEdge);
          CFACreationUtils.removeChainOfNodesFromCFA(sideEffectEdge.getSuccessor());
        }
        // Append temporary assignments of previous (side effect free) expressions.
        for (final JSExpression sideEffectFreeExpression : sideEffectFreeExpressions) {
          final JSVariableDeclaration tmpVariableDeclaration =
              pBuilder.declareVariable(
                  new JSInitializerExpression(FileLocation.DUMMY, sideEffectFreeExpression));
          pBuilder.appendEdge(JSDeclarationEdge.of(tmpVariableDeclaration));
          resultBuilder.add(new JSIdExpression(FileLocation.DUMMY, tmpVariableDeclaration));
        }
        sideEffectFreeExpressions.clear();
        // Redo side effect.
        final JSExpression appendedExpression = pBuilder.append(e);
        resultBuilder.add(appendedExpression);
        // The resulting expression of appending itself is side effect free.
        // Nevertheless, it might have to be appended using a temporary assignment later,
        // in case a following expression in the expressions-list has a side effect.
        //
        // For example:
        //   The expressions `x, ++x, ++x` have two side effects `x = x + 1`, whereas the second
        //   `++x` influences the resulting expression `x` of the first `++x`.
        //   If `x` is `0` before the elevation of the expressions `x, ++x, ++x` then the expected
        //   result is `0, 1, 2`.
        //   The resulting expression `x` of the first `++x` has to be assigned to a temporary
        //   variable (`tmp1` in the following example) to store the value of `x` before the second
        //   incrementation:
        //
        //     var tmp0 = x;
        //     x = x + 1;
        //     var tmp1 = x;
        //     x = x + 1;
        //
        //   The result is `tmp0, tmp1, x` which evaluates to the expected result `0, 1, 2`.
        //   Without `tmp1` the result would be `tmp0, x, x` which evaluates to the false result
        //   `0, 2, 2`.
        //
        sideEffectFreeExpressions.add(appendedExpression);
      } else {
        pBuilder.append(forwardLookingBuilder);
        sideEffectFreeExpressions.add(jsExpression);
        resultBuilder.add(jsExpression);
      }
    }
    return resultBuilder.build();
  }

}
