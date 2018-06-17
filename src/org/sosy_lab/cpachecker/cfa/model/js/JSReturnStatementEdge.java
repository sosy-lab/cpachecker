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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.js.JSAssignment;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;
import org.sosy_lab.cpachecker.cfa.ast.js.JSReturnStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.JumpExitEdge;

public class JSReturnStatementEdge extends AReturnStatementEdge implements JumpExitEdge {

  private static final long serialVersionUID = 5236439106970614426L;

  public JSReturnStatementEdge(
      String pRawStatement,
      JSReturnStatement pRawAST,
      FileLocation pFileLocation,
      CFANode pPredecessor,
      FunctionExitNode pSuccessor) {

    super(pRawStatement, pRawAST, pFileLocation, pPredecessor, pSuccessor);
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JSExpression> getExpression() {
    return (Optional<JSExpression>) rawAST.getReturnValue();
  }

  @SuppressWarnings("unchecked") // safe because Optional is covariant
  @Override
  public Optional<JSAssignment> asAssignment() {
    return (Optional<JSAssignment>) super.asAssignment();
  }

  @Override
  public Optional<JSReturnStatement> getRawAST() {
    return Optional.of((JSReturnStatement) rawAST);
  }
}
