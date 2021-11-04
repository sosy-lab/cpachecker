// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.java;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JAssumeEdge extends AssumeEdge {

  private static final long serialVersionUID = -2456773904604276548L;

  public JAssumeEdge(String pRawStatement, FileLocation pFileLocation, CFANode pPredecessor,
      CFANode pSuccessor, JExpression pExpression, boolean pTruthAssumption) {

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
  public JExpression getExpression() {
    return (JExpression) expression;
  }


  /**
   * TODO
   * Warning: for instances with {@link #getTruthAssumption()} == false, the
   * return value of this method does not represent exactly the return value
   * of {@link #getRawStatement()} (it misses the outer negation of the expression).
   */
  @Override
  public Optional<JExpression> getRawAST() {
    return Optional.of((JExpression)expression);
  }
}
