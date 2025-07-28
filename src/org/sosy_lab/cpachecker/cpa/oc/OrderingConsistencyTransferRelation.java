// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Optional;
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

public class OrderingConsistencyTransferRelation extends SingleEdgeTransferRelation {
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final Solver solver;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;

  public OrderingConsistencyTransferRelation(
      Configuration pConfig, CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    //    pConfig.inject(this);
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
    if (state instanceof OrderingConsistencyState prevState) {
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
                  checkState(
                      params.get(2) instanceof CUnaryExpression
                          && ((CUnaryExpression) params.get(2)).getOperator()
                              == UnaryOperator.AMPER,
                      "Malformed pthread_create (Thread not unary expression with reference): %s",
                      params.get(2));
                  checkState(
                      ((CUnaryExpression) params.get(2)).getOperand() instanceof CIdExpression,
                      "Malformed pthread_create (Thread not CIdExpression): %s",
                      ((CUnaryExpression) params.get(2)).getOperand());
                  final var eventList = prevState.waitingThreads().get(prevState.nextThreadToStep().map(i -> i.getFirstNotNull()).orElse(0)).pMemoryEvents();
                  final var lastEvent = eventList.get(eventList.size() - 1);
                  prevState =
                      addNewThread(
                          prevState,
                          ((CIdExpression) ((CUnaryExpression) params.get(2)).getOperand())
                              .getName(),
                          Optional.of(lastEvent)
                          );
                }
                default -> {
                  // nothing to do
                }
              }
            }
          }
        }
        default -> {}
      }

      final var old = prevState;

      final var nextThreadToStep = old.nextThreadToStep();
      if (nextThreadToStep.isPresent()) {
        final var pid = nextThreadToStep.get().getFirstNotNull();
        final var loc = nextThreadToStep.get().getSecondNotNull().pLocationState();
        final var stack = nextThreadToStep.get().getSecondNotNull().pCallstackState();
        final var pathFormula = nextThreadToStep.get().getSecondNotNull().pPathFormula();
        final var accesses = nextThreadToStep.get().getSecondNotNull().pMemoryEvents();

        final var nextLocs =
            locationCPA.getTransferRelation().getAbstractSuccessorsForEdge(loc, precision, cfaEdge);
        final var nextStacks =
            callstackCPA
                .getTransferRelation()
                .getAbstractSuccessorsForEdge(stack, precision, cfaEdge);
        final var nextFormula = pathFormulaManager.makeAnd(pathFormula, cfaEdge);
        final var nextAccesses = ImmutableList.copyOf(Iterables.concat(accesses, EdgeCloner.getAccesses(cfaEdge).stream().map(pMemoryEvent ->
          pMemoryEvent.withGuard(nextFormula)
        ).toList()));

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
                                        nextFormula,
                                        nextAccesses)));

        return nextStates.toList();
      }
    }
    return ImmutableList.of();
  }

  OrderingConsistencyState initial() {
    return addNewThread(OrderingConsistencyState.empty(), "main", Optional.empty());
  }

  OrderingConsistencyState addNewThread(
      final OrderingConsistencyState old, final String functionName, final Optional<MemoryEvent> hbBeforeEvent) {
    CFANode functioncallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '" + functionName + "' was not found.");

    CallstackState initialStack =
        (CallstackState)
            callstackCPA.getInitialState(
                functioncallNode, StateSpacePartition.getDefaultPartition());
    LocationState initialLoc =
        locationCPA.getInitialState(functioncallNode, StateSpacePartition.getDefaultPartition());

    PathFormula emptyFormula = pathFormulaManager.makeEmptyPathFormula();

    return old.addNewThread(initialLoc, initialStack, emptyFormula, hbBeforeEvent);
  }

  public Solver getSolver() {
    return solver;
  }

  public PathFormulaManager getPathFormulaManager() {
    return pathFormulaManager;
  }
}
