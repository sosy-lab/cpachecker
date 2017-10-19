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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
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

  RCUSearchTransfer(Configuration config, LogManager pLogger) throws InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    RCUSearchState result = (RCUSearchState) state;
    logger.log(Level.ALL, "EDGE TYPE: " + cfaEdge.getEdgeType());
    logger.log(Level.ALL, "EDGE CONT: " + cfaEdge.getRawStatement());
    switch(cfaEdge.getEdgeType()) {
      case StatementEdge:
        result = handleStatementEdge((CStatementEdge) cfaEdge,
                            cfaEdge.getPredecessor().getFunctionName(),
                            (RCUSearchState) state, logger);
        break;
      case FunctionCallEdge:
        result = handleFunctionCallEdge((CFunctionCallEdge) cfaEdge,
                                cfaEdge.getPredecessor().getFunctionName(),
                                (RCUSearchState) state, logger);
        break;
      case FunctionReturnEdge:
        if (cfaEdge instanceof CFunctionCallEdge) {
          result = handleFunctionCallEdge((CFunctionCallEdge) cfaEdge,
              cfaEdge.getSuccessor().getFunctionName(),
              (RCUSearchState) state, logger);
        } else {
          result = handleFunctionReturnEdge((CFunctionReturnEdge) cfaEdge,
              cfaEdge.getSuccessor().getFunctionName(),
              (RCUSearchState) state, logger);
        }
      default:
        break;
    }
    return Collections.singleton(result);
  }
  private RCUSearchState handleFunctionReturnEdge(CFunctionReturnEdge pEdge,
                                               String pFunctionName,
                                               RCUSearchState state,
                                               LogManager pLogger) {
    CFunctionCall expr = pEdge.getSummaryEdge().getExpression();
    pLogger.log(Level.ALL, "CALL EXPR: " + expr);

    if (expr instanceof CFunctionCallAssignmentStatement) {
      // p = ldv_rcu_dereference(gp);
      return handleFunctionCallAssignmentStatement(pFunctionName, state, pLogger,
          (CFunctionCallAssignmentStatement) expr);
    } else {
      pLogger.log(Level.ALL, "ORDINARY CALL: " + expr);
      return state;
    }
  }

  private RCUSearchState handleFunctionCallEdge(CFunctionCallEdge pEdge,
                                             String pFunctionName,
                                             RCUSearchState state,
                                             LogManager pLogger) {
    // rcu_assign_pointer(gp, p)
    CFunctionCallExpression fc = pEdge.getSummaryEdge().getExpression().getFunctionCallExpression();
    CFunctionDeclaration fd = fc.getDeclaration();
    Set<MemoryLocation> pRcuPointers = new HashSet<>(state.getRcuPointers());
    if (fd.getName().contains(assign)) {
      List<CExpression> params = fc.getParameterExpressions();

      //TODO: not-really-a-clever-hack detected

      MemoryLocation loc = MemoryLocation.valueOf(pFunctionName, params.get(0).toString());
      pRcuPointers.add(loc);

      loc = MemoryLocation.valueOf(pFunctionName, params.get(1).toString());
      pRcuPointers.add(loc);
    }

    return new RCUSearchState(pRcuPointers);
  }

  private RCUSearchState handleStatementEdge(CStatementEdge pEdge,
                                          String pFunctionName,
                                          RCUSearchState state,
                                          LogManager pLogger) {
    // p = rcu_dereference(gp)
    CStatement statement = pEdge.getStatement();
    pLogger.log(Level.ALL, "HANDLE_STATEMENT: " + statement.getClass()
                            + ' ' + statement.toString());
    if (statement instanceof CFunctionCallAssignmentStatement) {
      return handleFunctionCallAssignmentStatement(pFunctionName, state, pLogger,
          (CFunctionCallAssignmentStatement) statement);
    }
    return state;
  }

  private RCUSearchState handleFunctionCallAssignmentStatement(
      String pFunctionName,
      RCUSearchState state,
      LogManager pLogger,
      CFunctionCallAssignmentStatement pStatement) {
    CFunctionCallAssignmentStatement assignment = pStatement;
    CLeftHandSide leftHandSide = assignment.getLeftHandSide();
    Set<MemoryLocation> pRcuPointers = state.getRcuPointers();
    if (leftHandSide.getExpressionType() instanceof CPointerType) {
      CFunctionCallExpression funcExpr = assignment.getFunctionCallExpression();

      pLogger.log(Level.ALL,"FUNC NAME EXPR: " + funcExpr.getFunctionNameExpression());

      if (funcExpr.getFunctionNameExpression().toString().contains(deref)) {
        CExpression rcuPtr = funcExpr.getParameterExpressions().get(0);

        //TODO: not-really-a-clever-hack detected

        MemoryLocation loc = MemoryLocation.valueOf(pFunctionName, rcuPtr.toString());

        pLogger.log(Level.ALL, "RCU PTR: " + rcuPtr);
        pLogger.log(Level.ALL, "MEM LOC: " + loc);

        pRcuPointers.add(loc);

        loc = MemoryLocation.valueOf(pFunctionName, leftHandSide.toString());

        pLogger.log(Level.ALL, "LHS PTR: " + leftHandSide.toString());
        pLogger.log(Level.ALL, "LHS MEM LOC: " + loc);

        pRcuPointers.add(loc);
      }
    }
    if (!pRcuPointers.equals(state.getRcuPointers())) {
      return new RCUSearchState(pRcuPointers);
    } else {
      return state;
    }
  }
}
