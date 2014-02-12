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

import org.sosy_lab.cpachecker.cfa.ast.IAStatement;

import com.google.common.base.Optional;


public class AStatementEdge extends AbstractCFAEdge {

  protected final IAStatement statement;

  protected AStatementEdge(String pRawStatement, IAStatement pStatement,
      int pLineNumber, CFANode pPredecessor, CFANode pSuccessor) {

    super(pRawStatement, pLineNumber, pPredecessor, pSuccessor);
    statement = pStatement;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.StatementEdge;
  }

  public IAStatement getStatement() {
    return statement;
  }

  @Override
  public Optional<? extends IAStatement> getRawAST() {
    return Optional.of(statement);
  }

  @Override
  public String getCode() {
    return statement.toASTString();
  }

}
