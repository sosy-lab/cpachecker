/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.chc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
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
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;


public class CHCTransferRelation implements TransferRelation {

  final LogManager logger;

  public CHCTransferRelation(LogManager logger){
    this.logger = logger;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState state, Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    logger.log(Level.FINEST,
      "\n * " + cfaEdge.getEdgeType() + ", " +
      "description: \"" + cfaEdge.getDescription() + "\", " +
      "from " + cfaEdge.getPredecessor().getNodeNumber()  + " "  +
      "to " +   cfaEdge.getSuccessor().getNodeNumber() + ".");

    CHCState currentState = (CHCState)state;
    CHCPrecision crPrecision = (CHCPrecision)precision;

    CHCState newState = null;

    switch (cfaEdge.getEdgeType()) {

    case AssumeEdge:
      return handleAssumeEdge(currentState, crPrecision, (AssumeEdge)cfaEdge);

    case FunctionCallEdge:
      newState = handleFunctionCallEdge(currentState, crPrecision, (FunctionCallEdge)cfaEdge);
      break;

    case FunctionReturnEdge:
      newState = handleFunctionReturnEdge(currentState, crPrecision, (FunctionReturnEdge)cfaEdge);
      break;

    case MultiEdge:
      MultiEdge me = (MultiEdge) cfaEdge;
      for (CFAEdge innerEdge : me) {
        newState = handleSimpleEdge(currentState, crPrecision, innerEdge);
      }
      break;

    default:
      newState = handleSimpleEdge(currentState, crPrecision, cfaEdge);
    }

    if (newState == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(newState);
    }
  }


  private Collection<CHCState> handleAssumeEdge(CHCState currentState,
      CHCPrecision crPrecision, AssumeEdge cfaEdge) {
    ArrayList<Constraint> cns = ConstraintManager.getConstraint(cfaEdge);
    return createStatesFromConstraints(
        currentState,
        cfaEdge.getSuccessor().getNodeNumber(),
        cns);
  }


  private Collection<CHCState> createStatesFromConstraints(CHCState current,
      int nodeId, ArrayList<Constraint> cns) {
    CHCState newState;
    if (cns.size() > 1) {
      ArrayList<CHCState> newStates = new ArrayList<>(2);
      for (Constraint cn : cns) {
        newState = new CHCState(current);
        newState.setNodeNumber(nodeId);
        newState.updateConstraint(cn);
        if (! newState.isBottom()) {
          newStates.add(newState);
        }
      }
      if (newStates.isEmpty()) {
        return Collections.emptySet();
      } else {
        return newStates;
      }
    } else {
      newState = new CHCState(current);
      newState.setNodeNumber(nodeId);
      newState.updateConstraint(cns.get(0));
      if (newState.isBottom()) {
        return Collections.emptySet();
      } else {
        return Collections.singleton(newState);
      }
    }
  }


  /** handler for simple edges */
  private CHCState handleSimpleEdge(CHCState state, CHCPrecision prec, CFAEdge cfaEdge)
    throws CPATransferException {

    CHCState newState = new CHCState(state);

    switch (cfaEdge.getEdgeType()) {
    case DeclarationEdge:
      newState.setNodeNumber(cfaEdge.getSuccessor().getNodeNumber());
      newState.updateConstraint(ConstraintManager.getConstraint((CDeclarationEdge) cfaEdge));
      return newState;

    case StatementEdge:
      return handleStatementEdge(state, prec, (CStatementEdge) cfaEdge);

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


  /** This function handles statements like "a = 0;" and "b = !a;" and
   * calls of external functions. */
  private CHCState handleStatementEdge(CHCState state, CHCPrecision prec, CStatementEdge cfaEdge) {

    CHCState newState = new CHCState(state);
    newState.setNodeNumber(cfaEdge.getSuccessor().getNodeNumber());
    final CStatement statement = cfaEdge.getStatement();

      // assignment
      if (statement instanceof CAssignment) {
      final CAssignment ca = (CAssignment)statement;
      final CRightHandSide rhs = ca.getRightHandSide();
        // regular assignment, "a = ..."
        if (rhs instanceof CExpression) {
          newState.updateConstraint(ConstraintManager.getConstraint(ca));
          if (newState.isBottom()) {
            return null;
          }
        // call to external function
        //(internal function calls are handled as FunctionCallEdges)
        } else if (rhs instanceof CFunctionCallExpression) {
          newState.updateConstraint(
            ConstraintManager.getConstraint(ca.getLeftHandSide(),
                  (CFunctionCallExpression)rhs));
        } else {
          throw new AssertionError("unhandled assignment: " + cfaEdge.getRawStatement());
        }
      }
    return newState;
  }


  private CHCState handleFunctionCallEdge(CHCState state, CHCPrecision prec, FunctionCallEdge fcallEdge) {

    FunctionEntryNode functionEntryNode = fcallEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<? extends IAExpression> arguments = fcallEdge.getArguments();

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


  private CHCState handleFunctionReturnEdge(CHCState state, CHCPrecision prec, FunctionReturnEdge fRetEdge)
    throws UnrecognizedCCodeException {

    CHCState newState = new CHCState(state.getCaller());
    newState.setNodeNumber(fRetEdge.getSuccessor().getNodeNumber());
    newState.addConstraint(state.getConstraint());
    newState.updateConstraint(ConstraintManager.getConstraint(fRetEdge));

    if (newState.isBottom()) {
      return null;
    }

    return newState;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return Collections.singleton(pState);
  }

}
