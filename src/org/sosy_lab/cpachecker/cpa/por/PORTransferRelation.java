// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class PORTransferRelation extends SingleEdgeTransferRelation {
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final Solver solver;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;

  private Integer lastPid = null;

  public PORTransferRelation(
      Configuration pConfig, CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);
    cfa = pCfa;
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManager, pConfig, pLogger, pShutdownNotifier, pCfa, AnalysisDirection.FORWARD);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (state instanceof PORState originalState) {
      // Determine which thread this edge belongs to (populated by getOutgoingEdges)
      Integer edgePid = originalState.getEdgePid(cfaEdge);
      final Integer pid = edgePid != null ? edgePid : lastPid;
      if (pid == null) {
        throw new CPATransferException("Could not determine thread for edge " + cfaEdge);
      }
      lastPid = pid;

      PORState prevState = originalState;

      switch (cfaEdge.getEdgeType()) {
        case StatementEdge -> {
          AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
          if (statement instanceof AFunctionCall pAFunctionCall) {
            AExpression functionNameExp =
                pAFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
            if (functionNameExp instanceof AIdExpression pFunctionName) {
              final String functionName = pFunctionName.getName();
              switch (functionName) {
                case "pthread_create" -> {
                  final var params =
                      pAFunctionCall.getFunctionCallExpression().getParameterExpressions();
                  checkState(
                      params.size() == 4,
                      "Malformed pthread_create (not 4 params): %s",
                      pAFunctionCall);
                  checkState(params.get(0) instanceof CUnaryExpression cUnaryExpression
                          && cUnaryExpression.getOperator() == UnaryOperator.AMPER
                          && cUnaryExpression.getOperand() instanceof CIdExpression,
                      "Malformed/unsupported pthread_create (Thread handle not unary expression with variable reference): %s",
                      params.get(0));
                  checkState(
                      params.get(2) instanceof CUnaryExpression cUnaryExpression
                          && cUnaryExpression.getOperator() == UnaryOperator.AMPER,
                      "Malformed pthread_create (Thread not unary expression with reference): %s",
                      params.get(2));
                  checkState(
                      ((CUnaryExpression) params.get(2)).getOperand() instanceof CIdExpression,
                      "Malformed pthread_create (Thread not CIdExpression): %s",
                      ((CUnaryExpression) params.get(2)).getOperand());
                  prevState =
                      addNewThread(
                          prevState,
                          ((CIdExpression) ((CUnaryExpression) params.get(
                              0)).getOperand()).getDeclaration().getQualifiedName(),
                          ((CIdExpression) ((CUnaryExpression) params.get(2)).getOperand())
                              .getName());
                }
                case "pthread_join" -> {
                  final var params =
                      pAFunctionCall.getFunctionCallExpression().getParameterExpressions();
                  checkState(params.size() == 2, "Malformed pthread_join (not 2 params): %s",
                      pAFunctionCall);
                  final var handleParam = params.get(0);
                  checkState(handleParam instanceof CUnaryExpression cUnaryExpression
                          && cUnaryExpression.getOperator() == UnaryOperator.AMPER
                          && cUnaryExpression.getOperand() instanceof CIdExpression,
                      "Malformed/unsupported pthread_join (Thread handle not unary expression with variable reference): %s",
                      handleParam);
                  prevState = prevState.joinThread(
                      ((CIdExpression) ((CUnaryExpression) handleParam).getOperand()).getDeclaration()
                          .getQualifiedName());
                  if (prevState == null) {
                    // joining a thread that has not terminated yet, skip this edge
                    return ImmutableList.of();
                  }
                }
                default -> {
                  // nothing to do
                }
              }
            }
          }
        }
        default -> {
        }
      }

      final PORState old = prevState;
      final PORThreadState threadState = old.threads().get(pid);

      if (threadState != null) {
        final var loc = threadState.pLocationState();
        final var stack = threadState.pCallstackState();
        final var pathFormula = threadState.pPathFormula();

        final var nextLocs =
            locationCPA.getTransferRelation().getAbstractSuccessorsForEdge(loc, precision, cfaEdge);
        final var nextStacks =
            callstackCPA
                .getTransferRelation()
                .getAbstractSuccessorsForEdge(stack, precision, cfaEdge);
        final var nextFormula = pathFormulaManager.makeAnd(pathFormula, cfaEdge);

        final var nextStates =
            nextLocs.stream()
                .flatMap(
                    nextLoc ->
                        nextStacks.stream()
                            .map(
                                nextStack ->
                                    old.stepThread(
                                        pid,
                                        (LocationState) nextLoc,
                                        (CallstackState) nextStack,
                                        nextFormula)));

        return nextStates.toList();
      }
    }
    return ImmutableList.of();
  }

  PORState initial() {
    return addNewThread(PORState.empty(cfa), null, "main");
  }

  PORState addNewThread(
      final PORState old,
      final String handle,
      final String functionName) {
    CFANode functioncallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '%s' was not found.", functionName);

    CallstackState initialStack =
        (CallstackState)
            callstackCPA.getInitialState(
                functioncallNode, StateSpacePartition.getDefaultPartition());
    LocationState initialLoc =
        locationCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition());

    PathFormula emptyFormula = pathFormulaManager.makeEmptyPathFormula();

    return old.addNewThread(handle, initialLoc, initialStack, emptyFormula);
  }

  public Solver getSolver() {
    return solver;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }
}
