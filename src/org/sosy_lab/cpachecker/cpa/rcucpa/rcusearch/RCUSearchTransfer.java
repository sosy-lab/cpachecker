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
package org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.rcucpa.LocationIdentifierConverter;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.rcucpa")
public class RCUSearchTransfer extends SingleEdgeTransferRelation {

  @Option(secure = true, name = "assign", description = "Name of a function responsible for "
      + "assignment to RCU pointers")
  private String assign = "ldv_rcu_assign_pointer";

  @Option(name = "deref", secure = true, description = "Name of a function responsible for "
      + "dereferences of RCU pointers")
  private String deref = "ldv_rcu_dereference";

  private LogManager logger;
  private PointerTransferRelation pointerTransfer;
  private RCUSearchStatistics stats;

  RCUSearchTransfer(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);
    pointerTransfer = PointerTransferRelation.INSTANCE;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    stats.transferTimer.start();
    stats.rcuSearchTimer.start();
    RCUSearchState oldRcuSearchState = (RCUSearchState) state;
    Set<MemoryLocation> oldPointers = oldRcuSearchState.getRcuPointers();
    Set<MemoryLocation> newPointers = new TreeSet<>();

    switch(cfaEdge.getEdgeType()) {
      case StatementEdge:
        newPointers =
            handleStatementEdge(
                (CStatementEdge) cfaEdge,
                            cfaEdge.getPredecessor().getFunctionName(),
                oldRcuSearchState);
        break;
      case FunctionCallEdge:
        // rcu_assign_pointer(gp, p)
        CFunctionCallExpression fc =
            ((CFunctionCallEdge) cfaEdge).getSummaryEdge()
                .getExpression()
                .getFunctionCallExpression();
        newPointers =
            handleFunctionCallExpression(
                cfaEdge.getPredecessor().getFunctionName(),
                oldRcuSearchState,
                fc);
        break;
      case FunctionReturnEdge:
        CFunctionCall expr = ((CFunctionReturnEdge) cfaEdge).getSummaryEdge().getExpression();
        newPointers =
            handleFunctionReturnEdge(
                expr,
                cfaEdge.getSuccessor().getFunctionName(),
                oldRcuSearchState);
        //$FALL-THROUGH$
      default:
        break;
    }

    // TODO: Not sure why, but we need to leave only new pointers
    newPointers.removeAll(oldPointers);
    // pointerTransfer.setUseFakeLocs(!new_res.isEmpty());
    stats.rcuSearchTimer.stop();

    stats.pointerTimer.start();
    PointerState oldPointerState =
        (PointerState) oldRcuSearchState.getWrappedState();
    Collection<? extends AbstractState> states =
        pointerTransfer.getAbstractSuccessorsForEdge(oldPointerState, precision, cfaEdge);
    if (states.size() == 0) {
      // Infeasible assumption
      stats.pointerTimer.stop();
      stats.transferTimer.stop();
      return ImmutableSet.of();
    }
    assert states.size() == 1;
    PointerState pointerState = (PointerState) states.iterator().next();
    stats.pointerTimer.stop();

    RCUSearchState successor;

    if (newPointers.equals(oldPointers) && pointerState.equals(oldPointerState)) {
      successor = oldRcuSearchState;
    } else {
      successor = new RCUSearchState(ImmutableSet.copyOf(newPointers), pointerState);
    }
    stats.transferTimer.stop();

    return Collections.singleton(successor);
  }

  private Set<MemoryLocation>
      handleFunctionReturnEdge(CFunctionCall expr, String pFunctionName,
      RCUSearchState state) {
    logger.log(Level.ALL, "CALL EXPR: " + expr);

    if (expr instanceof CFunctionCallAssignmentStatement) {
      // p = ldv_rcu_dereference(gp);
      return handleFunctionCallAssignmentStatement(
          pFunctionName,
          state,
          (CFunctionCallAssignmentStatement) expr);
    } else {
      logger.log(Level.ALL, "ORDINARY CALL: " + expr);
      return new TreeSet<>();
    }
  }

  private Set<MemoryLocation> handleFunctionCallExpression(
      String pFunctionName,
      RCUSearchState state,
      CFunctionCallExpression pFc) {
    CFunctionDeclaration fd = pFc.getDeclaration();
    Set<MemoryLocation> pRcuPointers = new TreeSet<>();
    if (fd != null) {
      logger.log(Level.ALL, "Handling function: " + fd.getName());
      if(fd.getName().contains(assign)) {
        logger.log(Level.ALL, "Handling rcu_assign_pointer");
        List<CExpression> params = pFc.getParameterExpressions();
        PointerState pointerState = (PointerState) state.getWrappedState();

        addMemoryLocation(pFunctionName, pointerState, pRcuPointers, params.get(0));
        addMemoryLocation(pFunctionName, pointerState, pRcuPointers, params.get(1));
      }
    }

    return pRcuPointers;
  }

  private void addMemoryLocation(String pFunctionName,
                                 PointerState pointerState,
                                 Set<MemoryLocation> pRcuPointers,
                                 CExpression expression) {
    IdentifierCreator identifierCreator = new IdentifierCreator(pFunctionName);
    AbstractIdentifier id = expression.accept(identifierCreator);
    MemoryLocation location = LocationIdentifierConverter.toLocation(id);

    if (pointerState.isKnownLocation(location)) {
      pRcuPointers.add(location);
    } else {
      identifierCreator.setCurrentFunction("");
      id = expression.accept(identifierCreator);
      MemoryLocation loc = LocationIdentifierConverter.toLocation(id);
      if (loc != null) {
        pRcuPointers.add(loc);
      }
    }
  }

  private Set<MemoryLocation> handleStatementEdge(CStatementEdge pEdge,
                                          String pFunctionName,
      RCUSearchState state) {
    // p = rcu_dereference(gp)
    CStatement statement = pEdge.getStatement();
    logger.log(
        Level.ALL,
        "HANDLE_STATEMENT: "
            + statement
                .getClass()
                            + ' ' + statement.toString());
    if (statement instanceof CFunctionCallAssignmentStatement) {
      return handleFunctionCallAssignmentStatement(
          pFunctionName,
          state,
          (CFunctionCallAssignmentStatement) statement);
    } else if (statement instanceof CFunctionCallStatement) {
      logger.log(Level.ALL, "HANDLE_STATEMENT: Not an assignment; ", statement.getClass());
      return handleFunctionCallExpression(pFunctionName, state, ((CFunctionCallStatement)
          statement).getFunctionCallExpression());
    }
    return new TreeSet<>();
  }

  private Set<MemoryLocation> handleFunctionCallAssignmentStatement(
      String pFunctionName,
      RCUSearchState state,
      CFunctionCallAssignmentStatement pStatement) {
    CLeftHandSide leftHandSide = pStatement.getLeftHandSide();
    Set<MemoryLocation> pRcuPointers = new TreeSet<>();

    if (leftHandSide.getExpressionType() instanceof CPointerType) {
      CFunctionCallExpression funcExpr = pStatement.getFunctionCallExpression();

      logger.log(Level.ALL, "FUNC NAME EXPR: " + funcExpr.getFunctionNameExpression());

      if (funcExpr.getDeclaration().getName().contains(deref)) {
        CExpression rcuPtr = funcExpr.getParameterExpressions().get(0);
        PointerState pointerState = (PointerState) state.getWrappedState();

        addMemoryLocation(pFunctionName, pointerState, pRcuPointers, rcuPtr);

        addMemoryLocation(pFunctionName, pointerState, pRcuPointers, leftHandSide);
      }
    }
    return pRcuPointers;
  }

  void initialize(PointerTransferRelation pPointerTransfer, RCUSearchStatistics pStats) {
    pointerTransfer = pPointerTransfer;
    stats = pStats;
  }
}
