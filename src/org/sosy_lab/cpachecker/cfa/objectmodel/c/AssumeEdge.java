/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.objectmodel.c;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class AssumeEdge extends AbstractCFAEdge {

  private final boolean truthAssumption;
  private final IASTExpression expression;

  public AssumeEdge(String pRawStatement, int pLineNumber, CFANode pPredecessor,
      CFANode pSuccessor, IASTExpression pExpression, boolean pTruthAssumption) {

    super("[" + pRawStatement + "]", pLineNumber, pPredecessor, pSuccessor);
    truthAssumption = pTruthAssumption;
    expression = pExpression;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.AssumeEdge;
  }

  public boolean getTruthAssumption() {
    return truthAssumption;
  }

  public IASTExpression getExpression() {
    return expression;
  }

  @Override
  public String getCode() {
    if (truthAssumption) {
      return expression.toASTString();
    }
    return "!(" + expression.toASTString() + ")";
  }

  @Override
  public String getDescription() {
    return "[" + getCode() + "]";
  }

  /**
   * TODO
   * Warning: for instances with {@link #getTruthAssumption()} == false, the
   * return value of this method does not represent exactly the return value
   * of {@link #getRawStatement()} (it misses the outer negation of the expression).
   */
  @Override
  public IASTExpression getRawAST() {
    return expression;
  }
}
