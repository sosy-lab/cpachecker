// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.c;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class CStatementEdge extends AStatementEdge implements CCfaEdge {

  private static final long serialVersionUID = -2606975234598958304L;

  public CStatementEdge(
      String pRawStatement,
      CStatement pStatement,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      CFANode pSuccessor) {

    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
  }

  @Override
  public CStatement getStatement() {
    return (CStatement) statement;
  }

  @Override
  public <R, X extends Exception> R accept(CCfaEdgeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
