/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.base.Predicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

public enum TestTargetType {
  ASSUME {
    @Override
    public Predicate<CFAEdge> getEdgeCriterion() {
      return edge -> edge instanceof AssumeEdge;
    }
  },
  ERROR_CALL {
    @Override
    public Predicate<CFAEdge> getEdgeCriterion() {
      return edge ->
          edge instanceof CStatementEdge
              && ((CStatementEdge) edge).getStatement() instanceof CFunctionCall
              && ((CFunctionCall) ((CStatementEdge) edge).getStatement())
                  .getFunctionCallExpression()
                  .getFunctionNameExpression()
                  .toASTString()
                  .equals("__VERIFIER_error");
    }
  },
  STATEMENT {
    @Override
    public Predicate<CFAEdge> getEdgeCriterion() {
      return edge ->
          edge.getEdgeType() == CFAEdgeType.DeclarationEdge
              || edge.getEdgeType() == CFAEdgeType.ReturnStatementEdge
              || edge.getEdgeType() == CFAEdgeType.StatementEdge;
    }
  };

  public abstract Predicate<CFAEdge> getEdgeCriterion();
}
