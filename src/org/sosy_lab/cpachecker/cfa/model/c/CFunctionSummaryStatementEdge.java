// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CFunctionSummaryStatementEdge extends CStatementEdge implements CCfaEdge {
  private static final long serialVersionUID = -5161504275097530533L;
  private final String functionName;
  private final CFunctionCall fcall;

  public CFunctionSummaryStatementEdge(
      String pRawStatement,
      CStatement pStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor,
      CFunctionCall fcall,
      String functionName) {
    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
    this.functionName = checkNotNull(functionName);
    this.fcall = checkNotNull(fcall);
  }

  public String getFunctionName() {
    return functionName;
  }

  public CFunctionCall getFunctionCall() {
    return fcall;
  }

  @Override
  public <R, X extends Exception> R accept(CCfaEdgeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
