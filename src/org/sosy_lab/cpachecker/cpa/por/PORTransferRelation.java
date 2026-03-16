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
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
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

public class PORTransferRelation implements TransferRelation {
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
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision)
      throws CPATransferException, InterruptedException {
    if (!(state instanceof PORState porState)) {
      throw new CPATransferException("State is not a PORState.");
    }
    Collection<CFAEdge> sourceSet = porState.getSourceSet(precision);
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
    if (state instanceof PORState originalState) {
      var sourceSet = originalState.getSourceSet(precision);
      if (!sourceSet.contains(cfaEdge)) {
        return ImmutableList.of(); // POR algorithm says we should not explore this edge
      }

      // Determine which thread this edge belongs to (populated by getOutgoingEdges)
      Integer edgePid = originalState.getEdgePid(cfaEdge);
      final Integer pid = edgePid != null ? edgePid : lastPid;
      if (pid == null) {
        throw new CPATransferException("Could not determine thread for edge " + cfaEdge);
      }
      lastPid = pid;

      PORState prevState = originalState;
      MutexState newMutexState = null;

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
                  // joining a thread that has not terminated yet, skip this edge
                  return ImmutableList.of();
                }

              } else if (!params.isEmpty()) {
                String mutexName = MutexFunctions.extractMutexName(params.get(0));
                if (mutexName != null) {
                  MutexState currentMutex = prevState.getMutexState();
                  if (MutexFunctions.isInitFunction(functionName)) {
                    newMutexState = currentMutex.withInit(mutexName);
                  } else if (MutexFunctions.isLockFunction(functionName)) {
                    if (currentMutex.isLockedByOther(mutexName, pid)) {
                      // Blocked: another thread holds the mutex
                      return ImmutableList.of();
                    }
                    newMutexState = currentMutex.withLock(mutexName, pid);
                  } else if (MutexFunctions.isUnlockFunction(functionName)) {
                    newMutexState = currentMutex.withUnlock(mutexName);
                  } else if (MutexFunctions.isDestroyFunction(functionName)) {
                    newMutexState = currentMutex.withDestroy(mutexName);
                  }
                }
              }
            }
          }
        }
        default -> {
        }
      }

      final PORState old = prevState;
      final MutexState finalMutexState =
          newMutexState != null ? newMutexState : old.getMutexState();
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
                                        nextFormula,
                                        finalMutexState)));

        return nextStates.toList();
      }
    }
    throw new CPATransferException("State is not a PORState.");
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
