// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.atexit;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.functionpointer.ExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.FunctionPointerTarget;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState.UnknownTarget;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

// TODO: Handle return value for atexit. The function should return 0 if the registration succeeds
//   and non-zero otherwise.
// TODO: Add an option for the maximum number of functions that can be registered. According to the
//   standard at least 32 functions need to be supported.

/**
 * Transfer relation for the atexit CPA
 *
 * <p>When atexit is called we store the target of the function pointer on the stack and return a
 * new {@link AtExitState}. We use strengthening to get the target from the {@link
 * org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA FunctionPointerCPA} after
 * evaluating the argument with {@link ExpressionValueVisitor}.
 */
public class AtExitTransferRelation extends SingleEdgeTransferRelation {
  @SuppressWarnings("unused")
  private final LogManager logger;

  @SuppressWarnings("unused")
  public AtExitTransferRelation(LogManager pLogger, Configuration pConfig)
      throws InvalidConfigurationException {
    // TODO: Do we need configuration options for this?
    logger = pLogger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    return ImmutableList.of(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    for (AbstractState other : otherStates) {
      if (state instanceof AtExitState atExitState
          && other instanceof FunctionPointerState fnState) {
        if (cfaEdge instanceof CStatementEdge stmtEdge
            && stmtEdge.getStatement() instanceof CFunctionCall callStmt
            && callStmt.getFunctionCallExpression().getFunctionNameExpression()
                instanceof CIdExpression fnExpr
            && fnExpr.getName().equals("atexit")) {
          // We've found a statement of the form "int r = atexit(argExpr)" or "atexit(argExpr)"
          // Evaluate argExpr to get a target for the function pointer and store it on the stack
          CExpression argExpr =
              callStmt.getFunctionCallExpression().getParameterExpressions().get(0);
          ExpressionValueVisitor evaluator =
              new ExpressionValueVisitor(fnState.createBuilder(), UnknownTarget.getInstance());
          FunctionPointerTarget target = argExpr.accept(evaluator);
          return ImmutableList.of(atExitState.push(target));
        }

        if (cfaEdge instanceof CStatementEdge stmtEdge
            && stmtEdge.getStatement() instanceof CFunctionCallAssignmentStatement callAssignStmt
            && callAssignStmt.getLeftHandSide() instanceof CIdExpression
            && callAssignStmt.getRightHandSide().getFunctionNameExpression()
                instanceof CIdExpression fnExpr
            && fnExpr.getName().equals("__CPACHECKER_atexit_next")) {
          // Remove the last element from the stack. We have to do this here (and not when
          // calculating the successor) to make sure that the function pointer CPA can still access
          // the element.
          // FIXME: Find a way to do this that does not depend on the order of the CPAs
          return ImmutableList.of(atExitState.pop());
        }
      }
    }
    return ImmutableList.of(state);
  }
}
