// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.mutex.MutexCPA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class PORTransferRelation implements TransferRelation {
  private final LocationCPA locationCPA;
  private final CallstackCPA callstackCPA;
  private final MutexCPA mutexCPA;
  private final TransferRelation wrappedTransferRelation;
  private final Solver solver;
  private final PathFormulaManager pathFormulaManager;
  private final CFA cfa;

  public PORTransferRelation(
      ConfigurableProgramAnalysis wrappedCpa,
      Configuration pConfig,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    wrappedTransferRelation = wrappedCpa.getTransferRelation();
    locationCPA = LocationCPA.create(pCfa, pConfig);
    callstackCPA = new CallstackCPA(pConfig, pLogger);
    if (wrappedCpa instanceof MutexCPA wrappedMutexCpa) {
      this.mutexCPA = wrappedMutexCpa;
    } else if (wrappedCpa instanceof WrapperCPA wrappedWrapperCPA) {
      this.mutexCPA = wrappedWrapperCPA.retrieveWrappedCpa(MutexCPA.class);
    } else {
      this.mutexCPA = null;
    }

    cfa = pCfa;
    solver = Solver.create(pConfig, pLogger, pShutdownNotifier);
    FormulaManagerView formulaManager = solver.getFormulaManager();
    pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManager, pConfig, pLogger, pShutdownNotifier, pCfa, AnalysisDirection.FORWARD);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof PORState porState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    if (!(precision instanceof PORPrecision porPrecision)) {
      throw new CPATransferException("Precision is not PORPrecision");
    }

    Collection<CFAEdge> sourceSet = porState.getSourceSet(porPrecision);
    ArrayList<AbstractState> allSuccessors = new ArrayList<>();
    for (CFAEdge edge : sourceSet) {
      allSuccessors.addAll(getAbstractSuccessorsForEdge(state, precision, edge));
    }
    return allSuccessors;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof PORState originalState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    if (!(precision instanceof PORPrecision porPrecision)) {
      throw new CPATransferException("Precision is not PORPrecision");
    }

    // Determine which thread this edge belongs to (populated by getOutgoingEdges)
    final Integer pid = originalState.getEdgePid(cfaEdge);
    if (pid == null) {
      throw new CPATransferException("Could not determine thread for edge " + cfaEdge);
    }

    // Call wrapped CPA transfer relation
    Collection<? extends AbstractState> wrappedSuccessors =
        wrappedTransferRelation.getAbstractSuccessorsForEdge(
            originalState.getWrappedState(),
            porPrecision.getWrappedPrecision(),
            cfaEdge
        );

    if (wrappedSuccessors.isEmpty()) {
      return ImmutableList.of();
    }

    PORState prevState = originalState;

    switch (cfaEdge.getEdgeType()) {
      case StatementEdge -> {
        AStatement statement = ((AStatementEdge) cfaEdge).getStatement();
        if (statement instanceof AFunctionCall pAFunctionCall) {
          AExpression functionNameExp =
              pAFunctionCall.getFunctionCallExpression().getFunctionNameExpression();
          if (functionNameExp instanceof AIdExpression pFunctionName) {
            final String functionName = pFunctionName.getName();
            final var params =
                pAFunctionCall.getFunctionCallExpression().getParameterExpressions();

            if (PthreadFunctions.isCreateFunction(functionName)) {
              String handle = PthreadFunctions.extractCreateHandle(params);
              String threadFunc = PthreadFunctions.extractCreateFunctionName(params);
              prevState = addNewThread(prevState, handle, threadFunc);

            } else if (PthreadFunctions.isJoinFunction(functionName)) {
              String handle = PthreadFunctions.extractJoinHandle(params);
              prevState = prevState.joinThread(handle);
              if (prevState == null) {
                return ImmutableList.of();
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
          locationCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(loc, precision, cfaEdge);
      final var nextStacks =
          callstackCPA
              .getTransferRelation()
              .getAbstractSuccessorsForEdge(stack, precision, cfaEdge);
      final var nextFormula = pathFormulaManager.makeAnd(pathFormula, cfaEdge);

      List<PORState> successors =
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
                                      nextFormula)))
              .toList();

      // Combine POR successors with wrapped CPA successors
      ImmutableList.Builder<PORState> combinedSuccessors = ImmutableList.builder();
      for (PORState porSuccessor : successors) {
        for (AbstractState wrappedSuccessor : wrappedSuccessors) {
          combinedSuccessors.add(porSuccessor.withWrappedState(wrappedSuccessor));
        }
      }

      return combinedSuccessors.build();
    }

    throw new CPATransferException("Thread state not found for PID " + pid);
  }

  PORState initial(AbstractState wrappedInitialState) {
    return addNewThread(PORState.empty(wrappedInitialState, cfa), null, "main");
  }

  PORState addNewThread(final PORState old, final String handle, final String functionName) {
    CFANode functionCallNode =
        Preconditions.checkNotNull(
            cfa.getFunctionHead(functionName), "Function '%s' was not found.", functionName);

    CallstackState initialStack =
        (CallstackState)
            callstackCPA.getInitialState(
                functionCallNode, StateSpacePartition.getDefaultPartition());
    LocationState initialLoc =
        locationCPA.getInitialState(functionCallNode, StateSpacePartition.getDefaultPartition());

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
