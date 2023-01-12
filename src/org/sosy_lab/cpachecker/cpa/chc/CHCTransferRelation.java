// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.chc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class CHCTransferRelation extends SingleEdgeTransferRelation {

  final LogManager logger;

  public CHCTransferRelation(LogManager logger) {
    this.logger = logger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    logger.log(
        Level.FINEST,
        "\n * "
            + cfaEdge.getEdgeType()
            + ", "
            + "description: \""
            + cfaEdge.getDescription()
            + "\", "
            + "from "
            + cfaEdge.getPredecessor().getNodeNumber()
            + " "
            + "to "
            + cfaEdge.getSuccessor().getNodeNumber()
            + ".");

    CHCState currentState = (CHCState) state;

    CHCState newState = null;

    switch (cfaEdge.getEdgeType()) {
      case AssumeEdge:
        return handleAssumeEdge(currentState, (AssumeEdge) cfaEdge);

      case FunctionCallEdge:
        newState = handleFunctionCallEdge(currentState, (FunctionCallEdge) cfaEdge);
        break;

      case FunctionReturnEdge:
        newState = handleFunctionReturnEdge(currentState, (FunctionReturnEdge) cfaEdge);
        break;

      default:
        newState = handleSimpleEdge(currentState, cfaEdge);
    }

    if (newState == null) {
      return ImmutableSet.of();
    } else {
      return Collections.singleton(newState);
    }
  }

  private Collection<CHCState> handleAssumeEdge(CHCState currentState, AssumeEdge cfaEdge) {
    List<Constraint> cns = ConstraintManager.getConstraint(cfaEdge);
    return createStatesFromConstraints(currentState, cfaEdge.getSuccessor().getNodeNumber(), cns);
  }

  private Collection<CHCState> createStatesFromConstraints(
      CHCState current, int nodeId, List<Constraint> cns) {
    CHCState newState;
    if (cns.size() > 1) {
      ImmutableList.Builder<CHCState> newStates = ImmutableList.builderWithExpectedSize(2);
      for (Constraint cn : cns) {
        newState = new CHCState(current);
        newState.setNodeNumber(nodeId);
        newState.updateConstraint(cn);
        if (!newState.isBottom()) {
          newStates.add(newState);
        }
      }
      return newStates.build();
    } else {
      newState = new CHCState(current);
      newState.setNodeNumber(nodeId);
      newState.updateConstraint(cns.get(0));
      if (newState.isBottom()) {
        return ImmutableSet.of();
      } else {
        return Collections.singleton(newState);
      }
    }
  }

  /** handler for simple edges */
  private CHCState handleSimpleEdge(CHCState state, CFAEdge cfaEdge) throws CPATransferException {

    CHCState newState = new CHCState(state);

    switch (cfaEdge.getEdgeType()) {
      case DeclarationEdge:
        newState.setNodeNumber(cfaEdge.getSuccessor().getNodeNumber());
        newState.updateConstraint(ConstraintManager.getConstraint((CDeclarationEdge) cfaEdge));
        return newState;

      case StatementEdge:
        return handleStatementEdge(state, (CStatementEdge) cfaEdge);

      case ReturnStatementEdge:
        newState.setNodeNumber(cfaEdge.getSuccessor().getNodeNumber());
        newState.updateConstraint(ConstraintManager.getConstraint((AReturnStatementEdge) cfaEdge));
        return newState;

      case BlankEdge:
      case CallToReturnEdge:
        return state;

      default:
        throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
  }

  /**
   * This function handles statements like "a = 0;" and "b = !a;" and calls of external functions.
   */
  private CHCState handleStatementEdge(CHCState state, CStatementEdge cfaEdge) {

    CHCState newState = new CHCState(state);
    newState.setNodeNumber(cfaEdge.getSuccessor().getNodeNumber());
    final CStatement statement = cfaEdge.getStatement();

    // assignment
    if (statement instanceof CAssignment) {
      final CAssignment ca = (CAssignment) statement;
      final CRightHandSide rhs = ca.getRightHandSide();
      // regular assignment, "a = ..."
      if (rhs instanceof CExpression) {
        newState.updateConstraint(ConstraintManager.getConstraint(ca));
        if (newState.isBottom()) {
          return null;
        }
        // call to external function
        // (internal function calls are handled as FunctionCallEdges)
      } else if (rhs instanceof CFunctionCallExpression) {
        newState.updateConstraint(
            ConstraintManager.getConstraint(ca.getLeftHandSide(), (CFunctionCallExpression) rhs));
      } else {
        throw new AssertionError("unhandled assignment: " + cfaEdge.getRawStatement());
      }
    }
    return newState;
  }

  private CHCState handleFunctionCallEdge(CHCState state, FunctionCallEdge fcallEdge) {

    FunctionEntryNode functionEntryNode = fcallEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<? extends AExpression> arguments = fcallEdge.getArguments();

    Collection<Constraint> cnList = ConstraintManager.getConstraint(paramNames, arguments);
    CHCState newState = new CHCState();

    newState.setNodeNumber(fcallEdge.getSuccessor().getNodeNumber());
    newState.setCaller(state);

    for (Constraint c : cnList) {
      newState.updateConstraint(c);
      if (newState.isBottom()) {
        return null;
      }
    }
    return newState;
  }

  private CHCState handleFunctionReturnEdge(CHCState state, FunctionReturnEdge fRetEdge)
      throws UnrecognizedCodeException {

    CHCState newState = new CHCState(state.getCaller());
    newState.setNodeNumber(fRetEdge.getSuccessor().getNodeNumber());
    newState.addConstraint(state.getConstraint());
    newState.updateConstraint(ConstraintManager.getConstraint(fRetEdge));

    if (newState.isBottom()) {
      return null;
    }

    return newState;
  }
}
