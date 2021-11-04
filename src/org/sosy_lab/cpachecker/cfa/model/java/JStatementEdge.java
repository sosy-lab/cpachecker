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
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JStatementEdge extends AStatementEdge {


  private static final long serialVersionUID = -785179844865167134L;

  public JStatementEdge(String pRawStatement, JStatement pStatement,
      FileLocation pFileLocation, CFANode pPredecessor, CFANode pSuccessor) {

    super(pRawStatement, pStatement, pFileLocation, pPredecessor, pSuccessor);
  }



  @Override
  public JStatement getStatement() {
    return (JStatement) statement;
  }

  @Override
  public Optional<JStatement> getRawAST() {
    return Optional.of((JStatement)statement);
  }
}
