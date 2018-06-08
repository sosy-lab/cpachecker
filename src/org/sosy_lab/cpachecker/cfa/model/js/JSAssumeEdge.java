/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.model.js;

import com.google.common.base.Optional;
import java.util.function.BiFunction;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.model.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JSAssumeEdge extends AssumeEdge {

  private static final long serialVersionUID = 708785392078306310L;

  public static BiFunction<CFANode, CFANode, AbstractCFAEdge> assume(
      final JSExpression pCondition, final boolean pTruthAssumption) {
    return (pPredecessor, pSuccessor) ->
        new JSAssumeEdge(
            pCondition.toASTString(),
            pCondition.getFileLocation(),
            pPredecessor,
            pSuccessor,
            pCondition,
            pTruthAssumption);
  }

  public JSAssumeEdge(
      String pRawStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      JSExpression pExpression,
      boolean pTruthAssumption) {

    super(
        pRawStatement,
        pFileLocation,
        pPredecessor,
        pSuccessor,
        pExpression,
        pTruthAssumption,
        false,
        false);
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.AssumeEdge;
  }

  @Override
  public JSExpression getExpression() {
    return (JSExpression) expression;
  }

  /**
   * TODO Warning: for instances with {@link #getTruthAssumption()} == false, the return value of
   * this method does not represent exactly the return value of {@link #getRawStatement()} (it
   * misses the outer negation of the expression).
   */
  @Override
  public Optional<JSExpression> getRawAST() {
    return Optional.of((JSExpression) expression);
  }
}
