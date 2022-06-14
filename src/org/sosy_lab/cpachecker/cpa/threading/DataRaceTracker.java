// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.threading;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.invariants.EdgeAnalyzer;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class DataRaceTracker {

  private static class MemoryAccess {

    private final String threadId;
    private final int epoch;
    private final MemoryLocation memoryLocation;
    private final boolean isWrite;
    private final ImmutableSet<String> locks;

    MemoryAccess(
        String pThreadId,
        int pEpoch,
        MemoryLocation pMemoryLocation,
        boolean pIsWrite,
        Set<String> pLocks) {
      threadId = pThreadId;
      epoch = pEpoch;
      memoryLocation = pMemoryLocation;
      isWrite = pIsWrite;
      locks = ImmutableSet.copyOf(pLocks);
    }

    public String getThreadId() {
      return threadId;
    }

    public int getEpoch() {
      return epoch;
    }

    public MemoryLocation getMemoryLocation() {
      return memoryLocation;
    }

    public boolean isWrite() {
      return isWrite;
    }

    public Set<String> getLocks() {
      return locks;
    }

    public boolean happensBefore(MemoryAccess other, Map<String, Thread> threads) {
      if (threadId.equals(other.getThreadId())) {
        return true;
      }
      if (threads.containsKey(other.getThreadId())) {
        Thread othersThread = threads.get(other.getThreadId());
        Thread othersThreadsParent = othersThread.getParent();
        if (othersThreadsParent != null
            && othersThreadsParent.getName().equals(threadId)
            && othersThread.getCreationEpoch() > epoch) {
          return true;
        }
      }
      // TODO: Check for synchronizes-with relationship? What memory model do we assume?
      return false;
    }
  }

  private static class Thread {
    private final @Nullable Thread parent;
    private final String name;
    private final int epoch;
    private final int creationEpoch;
    private final boolean terminated;

    Thread(@Nullable Thread pParent, String pName, int pEpoch, int pCreationEpoch) {
      parent = pParent;
      name = pName;
      epoch = pEpoch;
      creationEpoch = pCreationEpoch;
      terminated = false;
    }

    public @Nullable Thread getParent() {
      return parent;
    }

    public String getName() {
      return name;
    }

    public int getEpoch() {
      return epoch;
    }

    public int getCreationEpoch() {
      return creationEpoch;
    }

    public boolean isTerminated() {
      return terminated;
    }

    public Thread increaseEpoch() {
      return new Thread(parent, name, epoch + 1, creationEpoch);
    }
  }

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

  private final ImmutableSet<MemoryAccess> memoryAccesses;
  private final ImmutableMap<String, Thread> threads;
  private final boolean hasDataRace;
  private final EdgeAnalyzer edgeAnalyzer;

  DataRaceTracker(EdgeAnalyzer pEdgeAnalyzer) {
    this(
        ImmutableSet.of(),
        ImmutableMap.of("main", new Thread(null, "main", 0, 0)),
        false,
        pEdgeAnalyzer);
  }

  private DataRaceTracker(
      Set<MemoryAccess> pMemoryAccesses,
      Map<String, Thread> pThreads,
      boolean pHasDataRace,
      EdgeAnalyzer pEdgeAnalyzer) {
    memoryAccesses = ImmutableSet.copyOf(pMemoryAccesses);
    threads = ImmutableMap.copyOf(pThreads);
    hasDataRace = pHasDataRace;
    edgeAnalyzer = pEdgeAnalyzer;
  }

  boolean hasDataRace() {
    return hasDataRace;
  }

  DataRaceTracker update(
      Set<String> threadIds, String activeThread, CFAEdge edge, Set<String> locks) {
    ImmutableMap<String, Thread> newThreads = getNewThreads(activeThread, edge);

    Set<MemoryAccess> newMemoryAccesses = getNewAccesses(activeThread, edge, locks);
    ImmutableSet.Builder<MemoryAccess> builder = ImmutableSet.builder();
    builder.addAll(newMemoryAccesses);
    for (MemoryAccess access : memoryAccesses) {
      if (threadIds.contains(access.getThreadId())) {
        builder.add(access);
      }
    }
    Set<MemoryAccess> accesses = builder.build();

    boolean nextHasDataRace = hasDataRace;

    for (MemoryAccess access : accesses) {
      if (nextHasDataRace) {
        break;
      }
      // In particular, this skips all new memory accesses
      if (access.getThreadId().equals(activeThread)) {
        continue;
      }
      for (MemoryAccess newAccess : newMemoryAccesses) {
        if (access.getMemoryLocation().equals(newAccess.getMemoryLocation())
            && Sets.intersection(access.getLocks(), newAccess.getLocks()).isEmpty()
            && (access.isWrite() || newAccess.isWrite())
            && !access.happensBefore(newAccess, threads)) {
          nextHasDataRace = true;
          break;
        }
      }
    }

    return new DataRaceTracker(accesses, newThreads, nextHasDataRace, edgeAnalyzer);
  }

  private Set<MemoryAccess> getNewAccesses(String activeThread, CFAEdge edge, Set<String> locks) {
    Set<MemoryLocation> accessedLocations = new HashSet<>();
    Set<MemoryLocation> modifiedLocations = new HashSet<>();
    Set<MemoryAccess> newAccesses = new HashSet<>();

    switch (edge.getEdgeType()) {
      case AssumeEdge:
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        accessedLocations =
            edgeAnalyzer.getInvolvedVariableTypes(assumeEdge.getExpression(), assumeEdge).keySet();
        break;
      case DeclarationEdge:
        ADeclarationEdge declarationEdge = (ADeclarationEdge) edge;
        ADeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
          MemoryLocation declaredVariable =
              MemoryLocation.fromQualifiedName(variableDeclaration.getQualifiedName());
          CInitializer initializer = variableDeclaration.getInitializer();
          newAccesses.add(
              new MemoryAccess(
                  activeThread,
                  threads.get(activeThread).getEpoch(),
                  declaredVariable,
                  initializer != null,
                  locks));
          if (initializer != null) {
            accessedLocations =
                edgeAnalyzer.getInvolvedVariableTypes(initializer, declarationEdge).keySet();
          }
        }
        break;
      case FunctionCallEdge:
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        String functionName =
            functionCallEdge.getFunctionCallExpression().getDeclaration().getName();
        if (!THREAD_SAFE_FUNCTIONS.contains(functionName)) {
          for (AExpression argument : functionCallEdge.getArguments()) {
            accessedLocations.addAll(
                edgeAnalyzer.getInvolvedVariableTypes(argument, functionCallEdge).keySet());
          }
        }
        break;
      case ReturnStatementEdge:
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) edge;
        if (returnStatementEdge.getExpression().isPresent()) {
          AExpression returnExpression = returnStatementEdge.getExpression().get();
          accessedLocations =
              edgeAnalyzer.getInvolvedVariableTypes(returnExpression, returnStatementEdge).keySet();
        }
        break;
      case StatementEdge:
        AStatementEdge statementEdge = (AStatementEdge) edge;
        AStatement statement = statementEdge.getStatement();
        if (statement instanceof AExpressionAssignmentStatement) {
          AExpressionAssignmentStatement expressionAssignmentStatement =
              (AExpressionAssignmentStatement) statement;
          accessedLocations =
              edgeAnalyzer
                  .getInvolvedVariableTypes(expressionAssignmentStatement, statementEdge)
                  .keySet();
          modifiedLocations =
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      expressionAssignmentStatement.getLeftHandSide(), statementEdge)
                  .keySet();
          assert accessedLocations.containsAll(modifiedLocations);
        } else if (statement instanceof AExpressionStatement) {
          accessedLocations =
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      ((AExpressionStatement) statement).getExpression(), statementEdge)
                  .keySet();
        } else if (statement instanceof AFunctionCallAssignmentStatement) {
          AFunctionCallAssignmentStatement functionCallAssignmentStatement =
              (AFunctionCallAssignmentStatement) statement;
          functionName =
              functionCallAssignmentStatement
                  .getFunctionCallExpression()
                  .getDeclaration()
                  .getName();
          if (THREAD_SAFE_FUNCTIONS.contains(functionName)) {
            accessedLocations =
                edgeAnalyzer
                    .getInvolvedVariableTypes(
                        functionCallAssignmentStatement.getLeftHandSide(), statementEdge)
                    .keySet();
          } else {
            accessedLocations =
                edgeAnalyzer
                    .getInvolvedVariableTypes(functionCallAssignmentStatement, statementEdge)
                    .keySet();
          }
          modifiedLocations =
              edgeAnalyzer
                  .getInvolvedVariableTypes(
                      functionCallAssignmentStatement.getLeftHandSide(), statementEdge)
                  .keySet();
          assert accessedLocations.containsAll(modifiedLocations);
        } else if (statement instanceof AFunctionCallStatement) {
          AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
          functionName =
              functionCallStatement.getFunctionCallExpression().getDeclaration().getName();
          if (!THREAD_SAFE_FUNCTIONS.contains(functionName)) {
            for (AExpression expression :
                functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
              accessedLocations =
                  edgeAnalyzer.getInvolvedVariableTypes(expression, statementEdge).keySet();
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

    for (MemoryLocation location : accessedLocations) {
      newAccesses.add(
          new MemoryAccess(
              activeThread,
              threads.get(activeThread).getEpoch(),
              location,
              modifiedLocations.contains(location),
              locks));
    }
    return newAccesses;
  }

  private ImmutableMap<String, Thread> getNewThreads(String activeThread, CFAEdge edge) {
    String newThreadName = null;
    if (edge instanceof CStatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) edge;
      if (statementEdge.getStatement() instanceof CFunctionCallStatement) {
        CFunctionCallStatement functionCallStatement =
            (CFunctionCallStatement) statementEdge.getStatement();
        if (functionCallStatement.getFunctionCallExpression().getFunctionNameExpression()
            instanceof CIdExpression) {
          CIdExpression functionNameExpression =
              (CIdExpression)
                  functionCallStatement.getFunctionCallExpression().getFunctionNameExpression();
          if (functionNameExpression.getName().equals("pthread_create")) {
            List<CExpression> parameters =
                functionCallStatement.getFunctionCallExpression().getParameterExpressions();
            if (!parameters.isEmpty() && parameters.get(0) instanceof CUnaryExpression) {
              CUnaryExpression threadReference = (CUnaryExpression) parameters.get(0);
              if (threadReference.getOperand() instanceof CIdExpression) {
                CIdExpression threadIdExpression = (CIdExpression) threadReference.getOperand();
                newThreadName = threadIdExpression.getName();
              }
            }
          }
        }
      }
    }

    // TODO: Handle thread termination (pthread_join)

    if (newThreadName == null) {
      return threads;
    }

    ImmutableMap.Builder<String, Thread> threadsBuilder = ImmutableMap.builder();
    Thread parent = threads.get(activeThread);
    threadsBuilder.put(newThreadName, new Thread(parent, newThreadName, 0, parent.getEpoch() + 1));
    threadsBuilder.put(parent.getName(), parent.increaseEpoch());
    for (Entry<String, Thread> entry : threads.entrySet()) {
      if (!entry.getKey().equals(activeThread)) {
        threadsBuilder.put(entry);
      }
    }
    return threadsBuilder.build();
  }
}
