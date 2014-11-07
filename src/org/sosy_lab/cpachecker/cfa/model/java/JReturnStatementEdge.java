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
package org.sosy_lab.cpachecker.cfa.model.java;


import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

import com.google.common.base.Optional;

public class JReturnStatementEdge extends AReturnStatementEdge {


  public JReturnStatementEdge(String pRawStatement, JReturnStatement pRawAST,
      FileLocation pFileLocation, CFANode pPredecessor, FunctionExitNode pSuccessor) {

    super(pRawStatement, pRawAST, pFileLocation, pPredecessor, pSuccessor);

  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JExpression> getExpression() {
    return (Optional<JExpression>)rawAST.getReturnValue();
  }

  @Override
  public Optional<JReturnStatement> getRawAST() {
    return Optional.of((JReturnStatement)rawAST);
  }

}
