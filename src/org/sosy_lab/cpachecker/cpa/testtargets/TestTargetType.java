// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.testtargets;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

public enum TestTargetType {
  ASSUME {
    @Override
    public Predicate<CFAEdge> getEdgeCriterion() {
      return edge -> edge instanceof AssumeEdge;
    }

    @Override
    public Predicate<CFAEdge> getEdgeCriterion(final String pProp) {
      return getEdgeCriterion();
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

    @Override
    public Predicate<CFAEdge> getEdgeCriterion(final String pProp) {
      return getEdgeCriterion();
    }
  },
  FUN_CALL {
    @Override
    public Predicate<CFAEdge> getEdgeCriterion() {
      return Predicates.alwaysFalse();
    }

    @Override
    public Predicate<CFAEdge> getEdgeCriterion(final String funName) {
      return edge -> (edge instanceof CStatementEdge
          && ((CStatementEdge) edge).getStatement() instanceof CFunctionCall
          && ((CFunctionCall) ((CStatementEdge) edge).getStatement()).getFunctionCallExpression()
              .getFunctionNameExpression()
              .toASTString()
              .equals(funName))
          || (edge instanceof CFunctionCallEdge
              && ((CFunctionCallEdge) edge).getRawAST().isPresent()
              && ((CFunctionCallEdge) edge).getRawAST().get().getFunctionCallExpression()
                  .getFunctionNameExpression()
                  .toASTString()
                  .equals(funName));

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

    @Override
    public Predicate<CFAEdge> getEdgeCriterion(final String pProp) {
      return getEdgeCriterion();
    }
  };

  public abstract Predicate<CFAEdge> getEdgeCriterion();

  public abstract Predicate<CFAEdge> getEdgeCriterion(String prop);
}
