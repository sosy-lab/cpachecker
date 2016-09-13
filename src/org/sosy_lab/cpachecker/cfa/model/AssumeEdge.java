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
package org.sosy_lab.cpachecker.cfa.model;

import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import java.util.Optional;


public class AssumeEdge extends AbstractCFAEdge {

  private final boolean truthAssumption;
  protected final AExpression expression;


  /**
   * @param pTruthAssumption If set to false, the expression is assumed to be
   *                         negated.
   */
  protected AssumeEdge(String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor,
      CFANode pSuccessor, AExpression pExpression, boolean pTruthAssumption) {

    super("[" + pRawStatement + "]", pFileLocation, pPredecessor, pSuccessor);
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

  public AExpression getExpression() {
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
  public Optional<? extends AExpression> getRawAST() {
    return Optional.of(expression);
  }
}
