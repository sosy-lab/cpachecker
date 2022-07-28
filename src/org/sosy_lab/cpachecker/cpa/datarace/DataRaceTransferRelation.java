// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DataRaceTransferRelation extends SingleEdgeTransferRelation {

  // These functions need special handling that is not currently provided by the DataRaceCPA.
  // When one of these functions is encountered we are therefore unable to tell if a data race
  // is present or not, so the analysis is terminated. TODO: Add support for these functions
  private static final ImmutableSet<String> UNSUPPORTED_FUNCTIONS =
      ImmutableSet.of(
          "pthread_mutex_trylock",
          "pthread_rwlock_rdlock",
          "pthread_rwlock_timedrdlock",
          "pthread_rwlock_timedwrlock",
          "pthread_rwlock_wrlock");

  // These are the functions declared in <pthread.h> that are used in sv-benchmarks programs
  private static final ImmutableSet<String> THREAD_SAFE_FUNCTIONS =
      ImmutableSet.of(
          "pthread_mutex_lock",
          "pthread_mutex_unlock",
          "pthread_create",
          "pthread_mutexattr_init",
          "pthread_mutexattr_settype",
          "pthread_mutex_init",
          "pthread_rwlock_wrlock",
          "pthread_rwlock_unlock",
          "pthread_rwlock_rdlock",
          "pthread_mutex_trylock",
          "pthread_join",
          "pthread_cond_wait",
          "pthread_cond_signal",
          "pthread_mutex_destroy",
          "pthread_attr_init",
          "pthread_attr_setdetachstate",
          "pthread_attr_destroy",
          "pthread_cond_init",
          "pthread_cond_destroy",
          "pthread_self",
          "pthread_cleanup_push",
          "pthread_cleanup_pop",
          "pthread_cond_broadcast",
          "pthread_getspecific",
          "pthread_setspecific",
          "pthread_key_create",
          "pthread_exit",
          "pthread_equal",
          "pthread_mutexattr_destroy");

  private final EdgeAnalyzer edgeAnalyzer;

  public DataRaceTransferRelation(EdgeAnalyzer pEdgeAnalyzer) {
    edgeAnalyzer = pEdgeAnalyzer;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    // Can only update state with info from ThreadingCPA
    return ImmutableSet.of(pState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    if (cfaEdge == null) {
      return ImmutableSet.of(pState);
    }
    DataRaceState state = (DataRaceState) pState;
    ImmutableSet.Builder<DataRaceState> strengthenedStates = ImmutableSet.builder();

    Map<String, Integer> threadEpochs = state.getThreadEpochs();
    ImmutableSet.Builder<ThreadSynchronization> newThreadSynchronizationsBuilder =
        ImmutableSet.builder();

    for (ThreadingState threadingState :
        AbstractStates.projectToType(otherStates, ThreadingState.class)) {

      Set<String> threadIds = threadingState.getThreadIds();
      String activeThread = getActiveThread(cfaEdge, threadingState);
      ImmutableMap<String, Integer> newThreadEpochs =
          getNewThreadEpochs(
              threadEpochs, threadIds, activeThread, newThreadSynchronizationsBuilder);

      if (newThreadEpochs.size() == 1) {
        // No data race possible in sequential part
        strengthenedStates.add(
            new DataRaceState(
                ImmutableSet.of(),
                ImmutableMap.of(),
                newThreadEpochs,
                ImmutableSet.of(),
                state.hasDataRace()));
        continue;
      }

      ImmutableSet.Builder<MemoryAccess> memoryAccessBuilder = ImmutableSet.builder();
      ImmutableMap.Builder<MemoryAccess, MemoryAccess> subsequentWritesBuilder =
          prepareSubsequentWritesBuilder(state, threadIds);
      Set<MemoryAccess> newMemoryAccesses =
          getNewAccesses(
              threadEpochs, activeThread, cfaEdge, threadingState.getLocksForThread(activeThread));

      for (MemoryAccess access : state.getMemoryAccesses()) {
        if (threadIds.contains(access.getThreadId())) {
          memoryAccessBuilder.add(access);
          if (access.isWrite() && !state.getSubsequentWrites().containsKey(access)) {
            for (MemoryAccess newAccess : newMemoryAccesses) {
              if (access.getMemoryLocation().equals(newAccess.getMemoryLocation())) {
                if (newAccess.isWrite()) {
                  subsequentWritesBuilder.put(access, newAccess);
                } else if (!access.getThreadId().equals(newAccess.getThreadId())) {
                  // Unnecessary if both accesses were made by the same thread, because then
                  // happens-before is established even without synchronizes-with
                  newThreadSynchronizationsBuilder.add(
                      new ThreadSynchronization(
                          access.getThreadId(),
                          newAccess.getThreadId(),
                          access.getAccessEpoch(),
                          newAccess.getAccessEpoch()));
                }
              }
            }
          }
        }
      }

      boolean hasDataRace = state.hasDataRace();
      Set<ThreadSynchronization> newThreadSynchronizations =
          newThreadSynchronizationsBuilder.addAll(state.getThreadSynchronizations()).build();
      for (MemoryAccess access : memoryAccessBuilder.build()) {
        if (hasDataRace) {
          break;
        }
        // In particular, this skips all new memory accesses
        if (access.getThreadId().equals(activeThread)) {
          continue;
        }
        for (MemoryAccess newAccess : newMemoryAccesses) {
          if (access.getMemoryLocation().equals(newAccess.getMemoryLocation())
              && (access.isWrite() || newAccess.isWrite())
              && Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()
              && !access.happensBefore(newAccess, newThreadSynchronizations)) {
            hasDataRace = true;
            break;
          }
        }
      }

      strengthenedStates.add(
          new DataRaceState(
              memoryAccessBuilder.addAll(newMemoryAccesses).build(),
              subsequentWritesBuilder.build(),
              newThreadEpochs,
              newThreadSynchronizations,
              hasDataRace));
    }

    return strengthenedStates.build();
  }

  private ImmutableMap.Builder<MemoryAccess, MemoryAccess> prepareSubsequentWritesBuilder(
      DataRaceState current, Set<String> threadIds) {
    ImmutableMap.Builder<MemoryAccess, MemoryAccess> subsequentWritesBuilder =
        ImmutableMap.builder();
    // TODO: So far the value is never used. Can it lead to wrong results if an entry is removed
    //       because the thread doing the subsequent write is terminated?
    for (Entry<MemoryAccess, MemoryAccess> entry : current.getSubsequentWrites().entrySet()) {
      if (threadIds.contains(entry.getKey().getThreadId())
          && threadIds.contains(entry.getValue().getThreadId())) {
        subsequentWritesBuilder.put(entry);
      }
    }
    return subsequentWritesBuilder;
  }

  /**
   * Collects the memory locations accessed by the given CFA edge and builds the corresponding
   * {@link MemoryAccess}es.
   *
   * <p>Throws CPATransferException if an unsupported function is encountered.
   */
  private Set<MemoryAccess> getNewAccesses(
      Map<String, Integer> threadEpochs, String activeThread, CFAEdge edge, Set<String> locks)
      throws CPATransferException {
    ImmutableSet.Builder<MemoryLocation> accessedLocationBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<MemoryLocation> modifiedLocationBuilder = ImmutableSet.builder();
    ImmutableSet.Builder<MemoryAccess> newAccessBuilder = ImmutableSet.builder();

    switch (edge.getEdgeType()) {
      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        accessedLocationBuilder.addAll(
            edgeAnalyzer.getInvolvedVariableTypes(assumeEdge.getExpression(), assumeEdge).keySet());
        break;
      case DeclarationEdge:
        ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;
        ADeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
          MemoryLocation declaredVariable =
              MemoryLocation.fromQualifiedName(variableDeclaration.getQualifiedName());
          CInitializer initializer = variableDeclaration.getInitializer();
          newAccessBuilder.add(
              new MemoryAccess(
                  activeThread,
                  declaredVariable,
                  initializer != null,
                  locks,
                  edge,
                  threadEpochs.get(activeThread)));
          if (initializer != null) {
            if (initializer instanceof CInitializerExpression
                && ((CInitializerExpression) initializer).getExpression()
                    instanceof CUnaryExpression) {
              CUnaryExpression initializerExpression =
                  (CUnaryExpression) ((CInitializerExpression) initializer).getExpression();
              if (initializerExpression.getOperator().equals(UnaryOperator.AMPER)) {
                // Address-of is not considered accessing its operand
                break;
              }
            }
            accessedLocationBuilder.addAll(
                edgeAnalyzer.getInvolvedVariableTypes(initializer, declarationEdge).keySet());
          }
        }
        break;
      case FunctionCallEdge:
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        String functionName = getFunctionName(functionCallEdge.getFunctionCall());
        if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
          throw new CPATransferException("DataRaceCPA does not support function " + functionName);
        }
        if (functionCallEdge.getFunctionCall() instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement functionCallAssignmentStatement =
              (AFunctionCallAssignmentStatement) functionCallEdge.getFunctionCall();
          if (THREAD_SAFE_FUNCTIONS.contains(functionName)) {
            accessedLocationBuilder.addAll(
                edgeAnalyzer
                    .getInvolvedVariableTypes(
                        functionCallAssignmentStatement.getLeftHandSide(), functionCallEdge)
                    .keySet());
          } else {
            accessedLocationBuilder.addAll(
                edgeAnalyzer
                    .getInvolvedVariableTypes(functionCallAssignmentStatement, functionCallEdge)
                    .keySet());
          }
        } else {
          if (!THREAD_SAFE_FUNCTIONS.contains(functionName)) {
            for (AExpression argument : functionCallEdge.getArguments()) {
              accessedLocationBuilder.addAll(
                  edgeAnalyzer.getInvolvedVariableTypes(argument, functionCallEdge).keySet());
            }
          }
        }
        break;
      case ReturnStatementEdge:
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getExpression().isPresent()) {
          AExpression returnExpression = returnStatementEdge.getExpression().get();
          accessedLocationBuilder.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(returnExpression, returnStatementEdge)
                  .keySet());
        }
        break;
      case StatementEdge:
        AStatementEdge statementEdge = (AStatementEdge) edge;
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionAssignmentStatement) {
          AExpressionAssignmentStatement expressionAssignmentStatement =
              (AExpressionAssignmentStatement) statement;
          if (expressionAssignmentStatement.getRightHandSide() instanceof CUnaryExpression
              && ((CUnaryExpression) expressionAssignmentStatement.getRightHandSide())
                  .getOperator()
                  .equals(UnaryOperator.AMPER)) {
            // Address-of is not considered accessing its operand
            accessedLocationBuilder.addAll(
                edgeAnalyzer
                    .getInvolvedVariableTypes(
                        expressionAssignmentStatement.getLeftHandSide(), statementEdge)
                    .keySet());
          } else {
            accessedLocationBuilder.addAll(
                edgeAnalyzer
                    .getInvolvedVariableTypes(expressionAssignmentStatement, statementEdge)
                    .keySet());
          }
          modifiedLocationBuilder.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      expressionAssignmentStatement.getLeftHandSide(), statementEdge)
                  .keySet());
        } else if (statement instanceof AExpressionStatement) {
          accessedLocationBuilder.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      ((AExpressionStatement) statement).getExpression(), statementEdge)
                  .keySet());
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement functionCallAssignmentStatement =
              (AFunctionCallAssignmentStatement) statement;
          functionName = getFunctionName(functionCallAssignmentStatement);
          if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
            throw new CPATransferException("DataRaceCPA does not support function " + functionName);
          }
          if (THREAD_SAFE_FUNCTIONS.contains(functionName)) {
            accessedLocationBuilder.addAll(
                edgeAnalyzer
                    .getInvolvedVariableTypes(
                        functionCallAssignmentStatement.getLeftHandSide(), statementEdge)
                    .keySet());
          } else {
            accessedLocationBuilder.addAll(
                edgeAnalyzer
                    .getInvolvedVariableTypes(functionCallAssignmentStatement, statementEdge)
                    .keySet());
          }
          modifiedLocationBuilder.addAll(
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      functionCallAssignmentStatement.getLeftHandSide(), statementEdge)
                  .keySet());
        } else if (statement instanceof AFunctionCallStatement) {
          AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
          functionName = getFunctionName(functionCallStatement);
          if (UNSUPPORTED_FUNCTIONS.contains(functionName)) {
            throw new CPATransferException("DataRaceCPA does not support function " + functionName);
          }
          if (!THREAD_SAFE_FUNCTIONS.contains(functionName)) {
            for (AExpression expression :
                functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
              accessedLocationBuilder.addAll(
                  edgeAnalyzer.getInvolvedVariableTypes(expression, statementEdge).keySet());
            }
          }
        }
        break;
      case FunctionReturnEdge:
      case BlankEdge:
      case CallToReturnEdge:
        break;
      default:
        throw new AssertionError("Unknown edge type: " + edge.getEdgeType());
    }

    Set<MemoryLocation> accessedLocations = accessedLocationBuilder.build();
    Set<MemoryLocation> modifiedLocations = modifiedLocationBuilder.build();
    assert accessedLocations.containsAll(modifiedLocations);

    for (MemoryLocation location : accessedLocations) {
      newAccessBuilder.add(
          new MemoryAccess(
              activeThread,
              location,
              modifiedLocations.contains(location),
              locks,
              edge,
              threadEpochs.get(activeThread)));
    }
    return newAccessBuilder.build();
  }

  /**
   * Updates the currently tracked thread information with information from the ThreadingCPA.
   *
   * @param threadEpochs The current map of thread ID -> thread epoch.
   * @param threadIds The IDs of currently existing thread as obtained from the ThreadingCPA.
   * @param activeThread The ID of the current active thread.
   * @return The new thread ID -> thread epoch map.
   */
  private ImmutableMap<String, Integer> getNewThreadEpochs(
      Map<String, Integer> threadEpochs,
      Set<String> threadIds,
      String activeThread,
      ImmutableSet.Builder<ThreadSynchronization> threadSynchronizations) {
    Set<String> added = Sets.difference(threadIds, threadEpochs.keySet());
    assert added.size() < 2 : "Multiple thread creations in same step not supported";
    Set<String> removed = Sets.difference(threadEpochs.keySet(), threadIds);
    assert !removed.contains(activeThread) : "Thread active after join";

    ImmutableMap.Builder<String, Integer> threadsBuilder = ImmutableMap.builder();
    if (!added.isEmpty()) {
      String threadId = added.iterator().next();
      threadsBuilder.put(threadId, 0);
      threadSynchronizations.add(
          new ThreadSynchronization(activeThread, threadId, threadEpochs.get(activeThread) + 1, 0));
    }
    for (Entry<String, Integer> entry : threadEpochs.entrySet()) {
      if (!removed.contains(entry.getKey())) {
        if (entry.getKey().equals(activeThread)) {
          threadsBuilder.put(activeThread, entry.getValue() + 1);
        } else {
          threadsBuilder.put(entry);
        }
      }
    }
    return threadsBuilder.buildOrThrow();
  }

  /**
   * Tries to determine the function name for a given AFunctionCall.
   *
   * <p>Note that it is usually possible to just look up the name from the declaration of the
   * contained function call expression but there are niche cases where this is not the case, which
   * is why this function is necessary.
   */
  private String getFunctionName(AFunctionCall pFunctionCall) {
    AFunctionCallExpression functionCallExpression = pFunctionCall.getFunctionCallExpression();
    if (functionCallExpression.getDeclaration() != null) {
      return functionCallExpression.getDeclaration().getName();
    } else {
      AExpression functionNameExpression = functionCallExpression.getFunctionNameExpression();
      if (functionNameExpression instanceof AIdExpression) {
        return ((AIdExpression) functionNameExpression).getName();
      } else if (functionNameExpression instanceof AUnaryExpression) {
        AUnaryExpression unaryFunctionNameExpression = (AUnaryExpression) functionNameExpression;
        if (unaryFunctionNameExpression.getOperand() instanceof AIdExpression) {
          return ((AIdExpression) unaryFunctionNameExpression.getOperand()).getName();
        }
      } else if (functionNameExpression instanceof APointerExpression) {
        APointerExpression pointerExpression = (APointerExpression) functionNameExpression;
        if (pointerExpression.getOperand() instanceof AIdExpression) {
          return ((AIdExpression) pointerExpression.getOperand()).getName();
        }
      }
    }
    throw new AssertionError("Unable to determine function name.");
  }

  /**
   * Search for the thread where the given edge is available.
   *
   * <p>This method is necessary, because neither ThreadingState::getActiveThread nor
   * ThreadingTransferRelation::getActiveThread are guaranteed to give the correct result during
   * strengthening.
   */
  private String getActiveThread(final CFAEdge cfaEdge, final ThreadingState threadingState) {
    for (String id : threadingState.getThreadIds()) {
      if (Iterables.contains(threadingState.getThreadLocation(id).getIngoingEdges(), cfaEdge)) {
        return id;
      }
    }
    throw new AssertionError("Unable to determine active thread");
  }
}
