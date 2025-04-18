// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqControlFlowStatement.SeqControlFlowStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.SeqStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqInjectedStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqThreadBeginsAtomicStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqThreadJoinsThreadStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.case_block.injected.SeqThreadLocksMutexStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.GhostVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation.ThreadSimulationVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;

public class SeqCaseBlockStatementBuilder {

  public static ImmutableList<SeqCaseBlockStatement> buildStatementsFromThreadNode(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      ThreadNode pThreadNode,
      CLeftHandSide pPcLeftHandSide,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostVariables pGhostVariables) {

    ImmutableList.Builder<SeqCaseBlockStatement> rStatements = ImmutableList.builder();

    int leavingEdgesSize = pThreadNode.leavingEdges().size();
    for (int i = 0; i < leavingEdgesSize; i++) {
      ThreadEdge threadEdge = pThreadNode.leavingEdges().get(i);

      // handle const CPAchecker_TMP first because it requires successor nodes and edges
      if (MPORUtil.isConstCpaCheckerTmpDeclaration(threadEdge.cfaEdge)) {
        rStatements.add(
            SeqCaseBlockStatementBuilder.buildConstCpaCheckerTmpStatement(
                threadEdge, pPcLeftHandSide, pCoveredNodes, pSubstituteEdges));

        // we exclude all function summaries, the calling context is handled by return edges
      } else if (!(threadEdge.cfaEdge instanceof FunctionSummaryEdge)) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substitute = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          SeqCaseBlockStatement statement =
              SeqCaseBlockStatementBuilder.buildCaseBlockStatementFromEdge(
                  pThread,
                  pAllThreads,
                  i == 0,
                  i == leavingEdgesSize - 1,
                  threadEdge,
                  substitute,
                  pGhostVariables);
          rStatements.add(statement);
        }
      }
    }
    return rStatements.build();
  }

  private static SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
      ThreadEdge pThreadEdge,
      CLeftHandSide pPcLeftHandSide,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    // ensure there are two single successors that are both statement edges
    ThreadNode successorA = pThreadEdge.getSuccessor();
    assert successorA.leavingEdges().size() == 1;
    ThreadEdge statementA = successorA.leavingEdges().iterator().next();
    assert statementA.cfaEdge instanceof CStatementEdge;
    ThreadNode successorB = statementA.getSuccessor();
    assert successorB.leavingEdges().size() == 1;
    ThreadEdge statementB = successorB.leavingEdges().iterator().next();
    assert statementB.cfaEdge instanceof CStatementEdge;

    // add successors to visited edges / nodes
    pCoveredNodes.add(successorA);
    pCoveredNodes.add(successorB);

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(pThreadEdge));
    CFAEdge cfaEdge = substituteEdge.cfaEdge;
    assert cfaEdge instanceof CDeclarationEdge : "cfaEdge must declare const CPAchecker_TMP";
    SubstituteEdge subA = Objects.requireNonNull(pSubstituteEdges.get(statementA));
    SubstituteEdge subB = Objects.requireNonNull(pSubstituteEdges.get(statementB));
    int newTargetPc = statementB.getSuccessor().pc;

    // ensure that declaration is variable declaration and cast accordingly
    CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
    CDeclaration declaration = declarationEdge.getDeclaration();
    assert declaration instanceof CVariableDeclaration : "declarationEdge must declare variable";
    CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

    return new SeqConstCpaCheckerTmpStatement(
        variableDeclaration,
        subA,
        subB,
        pPcLeftHandSide,
        ImmutableSet.of(substituteEdge, subA, subB),
        newTargetPc);
  }

  private static SeqCaseBlockStatement buildCaseBlockStatementFromEdge(
      final MPORThread pThread,
      final ImmutableSet<MPORThread> pAllThreads,
      boolean pFirstEdge,
      boolean pLastEdge,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      GhostVariables pGhostVariables) {

    CFAEdge edge = pThreadEdge.cfaEdge;
    int targetPc = pThreadEdge.getSuccessor().pc;
    CFANode successor = pThreadEdge.getSuccessor().cfaNode;
    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThread.id);

    if (yieldsBlankCaseBlock(pThread, pSubstituteEdge, successor)) {
      return buildBlankStatement(pcLeftHandSide, targetPc);

    } else {
      if (pSubstituteEdge.cfaEdge instanceof CAssumeEdge assumeEdge) {
        return buildAssumeStatement(
            pFirstEdge, pLastEdge, assumeEdge, pSubstituteEdge, pcLeftHandSide, targetPc);

      } else if (pSubstituteEdge.cfaEdge instanceof CDeclarationEdge declarationEdge) {
        return buildLocalVariableDeclarationWithInitializerStatement(
            declarationEdge, pSubstituteEdge, pcLeftHandSide, targetPc);

      } else if (pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge) {
        return handleFunctionCallEdge(
            pThread.id, pThreadEdge, pSubstituteEdge, targetPc, pGhostVariables);

      } else if (pSubstituteEdge.cfaEdge instanceof CReturnStatementEdge) {
        return buildReturnValueAssignmentStatement(
            pThreadEdge, pSubstituteEdge, targetPc, pcLeftHandSide, pGhostVariables.function);

      } else if (PthreadUtil.isExplicitlyHandledPthreadFunction(edge)) {
        return buildStatementFromPthreadFunction(
            pThread,
            pAllThreads,
            pThreadEdge,
            pSubstituteEdge,
            targetPc,
            pGhostVariables.pc,
            pGhostVariables.thread);
      }
    }
    // "leftover" edges should be statement edges
    assert pSubstituteEdge.cfaEdge instanceof CStatementEdge;
    return new SeqDefaultStatement(
        (CStatementEdge) pSubstituteEdge.cfaEdge,
        pcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        targetPc);
  }

  public static SeqBlankStatement buildBlankStatement(
      CLeftHandSide pPcLeftHandSide, int pTargetPc) {
    return new SeqBlankStatement(pPcLeftHandSide, pTargetPc);
  }

  private static SeqAssumeStatement buildAssumeStatement(
      boolean pFirstEdge,
      boolean pLastEdge,
      CAssumeEdge pAssumeEdge,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    // the CFA converts the assumptions into 2 assume edges, even with if ... else if ... else
    checkArgument(pFirstEdge || pLastEdge, "either pFirstEdge and pLastEdge must be true");

    SeqControlFlowStatement statement;
    if (pFirstEdge) {
      // if (condition) for first assume edge
      statement = new SeqControlFlowStatement(pAssumeEdge, SeqControlFlowStatementType.IF);
    } else {
      // use else ... for last (= second) assume edge
      statement = new SeqControlFlowStatement();
    }
    return new SeqAssumeStatement(
        statement, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqLocalVariableDeclarationWithInitializerStatement
      buildLocalVariableDeclarationWithInitializerStatement(
          CDeclarationEdge pDeclarationEdge,
          SubstituteEdge pSubstituteEdge,
          CLeftHandSide pPcLeftHandSide,
          int pTargetPc) {

    // "leftover" declarations should be local variables with an initializer
    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    checkArgument(
        declaration instanceof CVariableDeclaration, "pDeclarationEdge must declare variable");
    return new SeqLocalVariableDeclarationWithInitializerStatement(
        (CVariableDeclaration) declaration,
        pPcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private static SeqCaseBlockStatement handleFunctionCallEdge(
      int pThreadId,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      GhostVariables pGhostVariables) {

    CLeftHandSide pcLeftHandSide = pGhostVariables.pc.get(pThreadId);
    // function calls -> store parameters in ghost variables
    if (MPORUtil.isReachErrorCall(pThreadEdge)) {
      // inject non-inlined reach_error
      return new SeqReachErrorStatement(
          pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
    }
    assert pGhostVariables.function.parameterAssignments.containsKey(pThreadEdge);
    ImmutableList<FunctionParameterAssignment> assignments =
        Objects.requireNonNull(pGhostVariables.function.parameterAssignments.get(pThreadEdge));
    if (assignments.isEmpty()) {
      return new SeqBlankStatement(pcLeftHandSide, pTargetPc);
    }
    return new SeqParameterAssignmentStatements(
        assignments, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqCaseBlockStatement buildReturnValueAssignmentStatement(
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      FunctionStatements pFunctionVariables) {

    // TODO add support and test for pthread_join(id, &start_routine_return)
    //  where start_routine_return is assigned the return value of the threads start routine
    // returning from non-start-routine function: assign return value to return vars
    ImmutableSet<FunctionReturnValueAssignment> assignments =
        Objects.requireNonNull(pFunctionVariables.returnValueAssignments.get(pThreadEdge));
    if (assignments.isEmpty()) { // -> function does not return anything, i.e. return;
      return new SeqBlankStatement(pPcLeftHandSide, pTargetPc);
    } else {
      FunctionReturnValueAssignment assignment = assignments.iterator().next();
      return new SeqReturnValueAssignmentStatement(
          assignment.statement, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
    }
  }

  private static SeqCaseBlockStatement buildStatementFromPthreadFunction(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      PcVariables pPcVariables,
      ThreadSimulationVariables pThreadVariables) {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    PthreadFunctionType pthreadFunctionType = PthreadUtil.getPthreadFuncType(cfaEdge);
    CLeftHandSide pcLeftHandSide = pPcVariables.get(pThread.id);

    return switch (pthreadFunctionType) {
      case PTHREAD_CREATE ->
          buildThreadCreationStatement(
              pThread, pAllThreads, pSubstituteEdge, pTargetPc, pPcVariables);
      case PTHREAD_MUTEX_LOCK ->
          buildMutexLockStatement(
              pThread, pThreadEdge, pSubstituteEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexUnlockStatement(pSubstituteEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      case PTHREAD_JOIN ->
          buildThreadJoinStatement(
              pThread, pSubstituteEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      case __VERIFIER_ATOMIC_BEGIN ->
          buildAtomicBeginStatement(
              pThread, pSubstituteEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      case __VERIFIER_ATOMIC_END ->
          buildAtomicEndStatement(pSubstituteEdge, pTargetPc, pcLeftHandSide, pThreadVariables);
      default ->
          throw new AssertionError(
              "unhandled relevant pthread method: " + pthreadFunctionType.name);
    };
  }

  private static SeqThreadCreationStatement buildThreadCreationStatement(
      MPORThread pThread,
      ImmutableSet<MPORThread> pAllThreads,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      PcVariables pPcVariables) {

    CExpression pthreadT = PthreadUtil.extractPthreadT(pSubstituteEdge.cfaEdge);
    MPORThread createdThread = PthreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadT));
    return new SeqThreadCreationStatement(
        createdThread.id, pThread.id, pPcVariables, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public static Optional<SeqInjectedStatement> tryBuildInjectedStatement(
      SeqCaseBlockStatement pStatement) {

    if (pStatement instanceof SeqAtomicBeginStatement atomicBeginStatement) {
      return Optional.of(buildThreadBeginsAtomicStatement(atomicBeginStatement.threadBeginsAtomic));

    } else if (pStatement instanceof SeqMutexLockStatement mutexLockStatement) {
      return Optional.of(buildThreadLocksMutexStatement(mutexLockStatement.threadLocksMutex));

    } else if (pStatement instanceof SeqThreadJoinStatement threadJoinStatement) {
      return Optional.of(buildThreadJoinsThreadStatement(threadJoinStatement.threadJoinsThread));
    }
    return Optional.empty();
  }

  private static SeqThreadBeginsAtomicStatement buildThreadBeginsAtomicStatement(
      CIdExpression pThreadBeginsAtomic) {

    return new SeqThreadBeginsAtomicStatement(pThreadBeginsAtomic);
  }

  private static SeqThreadLocksMutexStatement buildThreadLocksMutexStatement(
      CIdExpression pThreadLocksMutex) {

    return new SeqThreadLocksMutexStatement(pThreadLocksMutex);
  }

  private static SeqThreadJoinsThreadStatement buildThreadJoinsThreadStatement(
      CIdExpression pThreadJoinsThread) {

    return new SeqThreadJoinsThreadStatement(pThreadJoinsThread);
  }

  private static SeqMutexLockStatement buildMutexLockStatement(
      MPORThread pThread,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSimulationVariables pThreadVariables) {

    CIdExpression lockedMutexT = PthreadUtil.extractPthreadMutexT(pThreadEdge.cfaEdge);
    assert pThreadVariables.locked.containsKey(lockedMutexT);
    CIdExpression mutexLocked =
        Objects.requireNonNull(pThreadVariables.locked.get(lockedMutexT)).idExpression;
    assert pThreadVariables.locks.containsKey(pThread);
    assert Objects.requireNonNull(pThreadVariables.locks.get(pThread)).containsKey(lockedMutexT);
    CIdExpression threadLocksMutex =
        Objects.requireNonNull(
                Objects.requireNonNull(pThreadVariables.locks.get(pThread)).get(lockedMutexT))
            .idExpression;
    return new SeqMutexLockStatement(
        mutexLocked,
        threadLocksMutex,
        pPcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private static SeqMutexUnlockStatement buildMutexUnlockStatement(
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSimulationVariables pThreadVariables) {

    CIdExpression unlockedMutexT = PthreadUtil.extractPthreadMutexT(pSubstituteEdge.cfaEdge);
    assert pThreadVariables.locked.containsKey(unlockedMutexT);
    // assign 0 to locked variable
    CExpressionAssignmentStatement lockedFalse =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            Objects.requireNonNull(pThreadVariables.locked.get(unlockedMutexT)).idExpression,
            SeqIntegerLiteralExpression.INT_0);
    return new SeqMutexUnlockStatement(
        lockedFalse, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqThreadJoinStatement buildThreadJoinStatement(
      MPORThread pThread,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSimulationVariables pThreadVariables) {

    MPORThread targetThread =
        PthreadUtil.extractThread(pThreadVariables.joins.keySet(), pSubstituteEdge.cfaEdge);
    CIdExpression threadJoins =
        Objects.requireNonNull(
                Objects.requireNonNull(pThreadVariables.joins.get(pThread)).get(targetThread))
            .idExpression;
    return new SeqThreadJoinStatement(
        threadJoins, ImmutableSet.of(pSubstituteEdge), pTargetPc, pPcLeftHandSide);
  }

  private static SeqAtomicBeginStatement buildAtomicBeginStatement(
      MPORThread pThread,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSimulationVariables pThreadVariables) {

    assert pThreadVariables.atomicLocked.isPresent();
    CIdExpression atomicLocked =
        Objects.requireNonNull(pThreadVariables.atomicLocked.orElseThrow().idExpression);
    assert pThreadVariables.begins.containsKey(pThread);
    CIdExpression beginsVar =
        Objects.requireNonNull(pThreadVariables.begins.get(pThread)).idExpression;
    return new SeqAtomicBeginStatement(
        atomicLocked, beginsVar, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqAtomicEndStatement buildAtomicEndStatement(
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSimulationVariables pThreadVariables) {

    assert pThreadVariables.atomicLocked.isPresent();
    // assign 0 to ATOMIC_LOCKED variable
    CExpressionAssignmentStatement atomicLockedFalse =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            Objects.requireNonNull(pThreadVariables.atomicLocked.orElseThrow().idExpression),
            SeqIntegerLiteralExpression.INT_0);
    return new SeqAtomicEndStatement(
        atomicLockedFalse, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public static SeqScalarPcAssumeStatement buildScalarPcAssumeStatement(SeqStatement pStatement) {
    return new SeqScalarPcAssumeStatement(pStatement);
  }

  /**
   * Returns true if the resulting case block has only pc adjustments, i.e. no code changing the
   * input program state.
   */
  private static boolean yieldsBlankCaseBlock(
      MPORThread pThread, SubstituteEdge pSubstituteEdge, CFANode pSuccessor) {

    // exiting start routine of thread -> blank, just set pc[i] = -1;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().getType().equals(pThread.startRoutine)) {
      // TODO this needs to be refactored once we support start routine return values
      //  that can be used with pthread_join -> block may not be blank but sets the return value
      return true;

    } else if (pSuccessor.getFunctionName().equals(SeqToken.reach_error)) {
      // if we enter reach_error, include only call edge (to inject reach_error)
      return !(pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge);

    } else if (pSubstituteEdge.cfaEdge instanceof BlankEdge) {
      // blank edges have no code
      assert pSubstituteEdge.cfaEdge.getCode().isEmpty();
      return true;

    } else if (PthreadUtil.assignsPthreadMutexInitializer(pSubstituteEdge.cfaEdge)) {
      // PTHREAD_MUTEX_INITIALIZER are similar to pthread_mutex_init, we exclude it
      return true;

    } else if (pSubstituteEdge.cfaEdge instanceof CDeclarationEdge declarationEdge) {
      // IMPORTANT: this step (checking for declaration edges) must come after checking for
      // PTHREAD_MUTEX_INITIALIZER (which may be inside a CDeclarationEdge too!)
      CDeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof CVariableDeclaration variableDeclaration) {
        // all variables, functions, structs... are declared outside the main function,
        // EXCEPT local variables that have an initializer:
        return !(!variableDeclaration.isGlobal() && variableDeclaration.getInitializer() != null);
      }
      return true;

    } else if (PthreadUtil.callsAnyPthreadFunction(pSubstituteEdge.cfaEdge)) {
      // not explicitly handled PthreadFunc -> empty case code
      return !PthreadUtil.isExplicitlyHandledPthreadFunction(pSubstituteEdge.cfaEdge);
    }
    return false;
  }
}
