// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqElseExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqIfExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.expression.single_control.SeqSingleControlExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.GhostElements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.CondSignaledFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadNode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class SeqThreadStatementBuilder {

  public static ImmutableList<SeqThreadStatement> buildStatementsFromThreadNode(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<MPORThread> pAllThreads,
      ThreadNode pThreadNode,
      CLeftHandSide pPcLeftHandSide,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges,
      GhostElements pGhostElements) {

    ImmutableList.Builder<SeqThreadStatement> rStatements = ImmutableList.builder();

    ImmutableList<ThreadEdge> leavingEdges = pThreadNode.leavingEdges();
    int numLeavingEdges = leavingEdges.size();
    for (int i = 0; i < numLeavingEdges; i++) {
      ThreadEdge threadEdge = leavingEdges.get(i);

      // handle const CPAchecker_TMP first because it requires successor nodes and edges
      if (MPORUtil.isConstCpaCheckerTmpDeclaration(threadEdge.cfaEdge)) {
        rStatements.add(
            SeqThreadStatementBuilder.buildConstCpaCheckerTmpStatement(
                pOptions, threadEdge, pPcLeftHandSide, pCoveredNodes, pSubstituteEdges));

        // we exclude all function summaries, the calling context is handled by return edges
      } else if (!(threadEdge.cfaEdge instanceof FunctionSummaryEdge)) {
        if (pSubstituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substitute = Objects.requireNonNull(pSubstituteEdges.get(threadEdge));
          rStatements.add(
              SeqThreadStatementBuilder.buildStatementFromThreadEdge(
                  pOptions,
                  pThread,
                  pAllThreads,
                  i == 0,
                  i == numLeavingEdges - 1,
                  threadEdge,
                  substitute,
                  pGhostElements));
        }
      }
    }
    return rStatements.build();
  }

  // const CPAchecker_TMP ==========================================================================

  private static SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      CLeftHandSide pPcLeftHandSide,
      Set<ThreadNode> pCoveredNodes,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    // ensure there are two single successors that are both statement edges
    ThreadNode threadNodeA = pThreadEdge.getSuccessor();
    assert threadNodeA.leavingEdges().size() == 1;
    ThreadEdge threadEdgeA = threadNodeA.firstLeavingEdge();
    assert threadEdgeA.cfaEdge instanceof CStatementEdge;
    ThreadNode threadNodeB = threadEdgeA.getSuccessor();
    assert threadNodeB.leavingEdges().size() == 1;
    ThreadEdge threadEdgeB = threadNodeB.firstLeavingEdge();
    assert threadEdgeB.cfaEdge instanceof CStatementEdge;

    CStatementEdge statementEdgeB = (CStatementEdge) threadEdgeB.cfaEdge;
    if (statementEdgeB.getStatement() instanceof CFunctionCallStatement) {
      // add successors to visited edges / nodes
      pCoveredNodes.add(threadNodeA);
      return buildTwoPartConstCpaCheckerTmpStatement(
          pOptions, pThreadEdge, pPcLeftHandSide, threadEdgeA, pSubstituteEdges);
    } else {
      // add successors to visited edges / nodes
      pCoveredNodes.add(threadNodeA);
      pCoveredNodes.add(threadNodeB);
      return buildThreePartConstCpaCheckerTmpStatement(
          pOptions, pThreadEdge, threadEdgeA, threadEdgeB, pPcLeftHandSide, pSubstituteEdges);
    }
  }

  private static SeqConstCpaCheckerTmpStatement buildTwoPartConstCpaCheckerTmpStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      CLeftHandSide pPcLeftHandSide,
      ThreadEdge pThreadEdgeA,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(pThreadEdge));
    CFAEdge cfaEdge = substituteEdge.cfaEdge;
    assert cfaEdge instanceof CDeclarationEdge : "cfaEdge must declare const CPAchecker_TMP";
    SubstituteEdge substituteEdgeA = Objects.requireNonNull(pSubstituteEdges.get(pThreadEdgeA));
    int newTargetPc = pThreadEdgeA.getSuccessor().pc;

    return buildConstCpaCheckerTmpStatement(
        pOptions,
        cfaEdge,
        substituteEdgeA,
        Optional.empty(),
        ImmutableSet.of(substituteEdge, substituteEdgeA),
        pPcLeftHandSide,
        newTargetPc);
  }

  private static SeqConstCpaCheckerTmpStatement buildThreePartConstCpaCheckerTmpStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      ThreadEdge pThreadEdgeA,
      ThreadEdge pThreadEdgeB,
      CLeftHandSide pPcLeftHandSide,
      ImmutableMap<ThreadEdge, SubstituteEdge> pSubstituteEdges) {

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge substituteEdge = Objects.requireNonNull(pSubstituteEdges.get(pThreadEdge));
    CFAEdge cfaEdge = substituteEdge.cfaEdge;
    assert cfaEdge instanceof CDeclarationEdge : "cfaEdge must declare const CPAchecker_TMP";
    SubstituteEdge substituteEdgeA = Objects.requireNonNull(pSubstituteEdges.get(pThreadEdgeA));
    SubstituteEdge substituteEdgeB = Objects.requireNonNull(pSubstituteEdges.get(pThreadEdgeB));
    int newTargetPc = pThreadEdgeB.getSuccessor().pc;

    return buildConstCpaCheckerTmpStatement(
        pOptions,
        cfaEdge,
        substituteEdgeA,
        Optional.of(substituteEdgeB),
        ImmutableSet.of(substituteEdge, substituteEdgeA, substituteEdgeB),
        pPcLeftHandSide,
        newTargetPc);
  }

  private static SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
      MPOROptions pOptions,
      CFAEdge pCfaEdge,
      SubstituteEdge pSubstituteEdgeA,
      Optional<SubstituteEdge> pSubstituteEdgeB,
      ImmutableSet<SubstituteEdge> pAllSubstituteEdges,
      CLeftHandSide pPcLeftHandSide,
      int pNewTargetPc) {

    // ensure that declaration is variable declaration and cast accordingly
    CDeclarationEdge declarationEdge = (CDeclarationEdge) pCfaEdge;
    CDeclaration declaration = declarationEdge.getDeclaration();
    assert declaration instanceof CVariableDeclaration : "declarationEdge must declare variable";
    CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

    return new SeqConstCpaCheckerTmpStatement(
        pOptions,
        variableDeclaration,
        pSubstituteEdgeA,
        pSubstituteEdgeB,
        pPcLeftHandSide,
        pAllSubstituteEdges,
        pNewTargetPc);
  }

  // Statement build methods =======================================================================

  private static SeqThreadStatement buildStatementFromThreadEdge(
      MPOROptions pOptions,
      final MPORThread pThread,
      final ImmutableList<MPORThread> pAllThreads,
      boolean pFirstEdge,
      boolean pLastEdge,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      GhostElements pGhostElements) {

    CFAEdge cfaEdge = pThreadEdge.cfaEdge;
    int targetPc = pThreadEdge.getSuccessor().pc;
    CFANode successor = pThreadEdge.getSuccessor().cfaNode;
    CLeftHandSide pcLeftHandSide =
        pGhostElements.getPcVariables().getPcLeftHandSide(pThread.getId());

    if (yieldsNoStatement(pThread, pSubstituteEdge, successor)) {
      return buildBlankStatement(pOptions, pcLeftHandSide, targetPc);

    } else {
      if (pSubstituteEdge.cfaEdge instanceof CAssumeEdge assumeEdge) {
        return buildAssumeStatement(
            pOptions, pFirstEdge, pLastEdge, assumeEdge, pSubstituteEdge, pcLeftHandSide, targetPc);

      } else if (pSubstituteEdge.cfaEdge instanceof CDeclarationEdge declarationEdge) {
        return buildLocalVariableDeclarationWithInitializerStatement(
            pOptions, declarationEdge, pSubstituteEdge, pcLeftHandSide, targetPc);

      } else if (pSubstituteEdge.cfaEdge instanceof CFunctionCallEdge) {
        return buildFunctionCallStatement(
            pOptions,
            pThread,
            pThreadEdge,
            pSubstituteEdge,
            targetPc,
            pGhostElements.getPcVariables(),
            pGhostElements.getFunctionStatementsByThread(pThread));

      } else if (pSubstituteEdge.cfaEdge instanceof CReturnStatementEdge) {
        return buildReturnValueAssignmentStatement(
            pOptions,
            pThreadEdge,
            pSubstituteEdge,
            targetPc,
            pcLeftHandSide,
            pGhostElements.getFunctionStatementsByThread(pThread));

      } else if (PthreadUtil.isExplicitlyHandledPthreadFunction(cfaEdge)) {
        return buildStatementFromPthreadFunction(
            pOptions, pThread, pAllThreads, pThreadEdge, pSubstituteEdge, targetPc, pGhostElements);
      }
    }
    assert pSubstituteEdge.cfaEdge instanceof CStatementEdge
        : "leftover CFAEdge must be CStatementEdge";
    return new SeqDefaultStatement(
        pOptions,
        (CStatementEdge) pSubstituteEdge.cfaEdge,
        pcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        targetPc);
  }

  public static SeqBlankStatement buildBlankStatement(
      MPOROptions pOptions, CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    return new SeqBlankStatement(pOptions, pPcLeftHandSide, pTargetPc);
  }

  private static SeqAssumeStatement buildAssumeStatement(
      MPOROptions pOptions,
      boolean pFirstEdge,
      boolean pLastEdge,
      CAssumeEdge pAssumeEdge,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    // the CFA converts the assumptions into 2 assume edges, even with if ... else if ... else
    checkArgument(pFirstEdge || pLastEdge, "either pFirstEdge and pLastEdge must be true");

    SeqSingleControlExpression expression;
    if (pFirstEdge) {
      // if (condition) for first assume edge
      expression = new SeqIfExpression(pAssumeEdge.getExpression());
    } else {
      // use else ... for last (= second) assume edge
      expression = new SeqElseExpression();
    }
    return new SeqAssumeStatement(
        pOptions, expression, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqLocalVariableDeclarationWithInitializerStatement
      buildLocalVariableDeclarationWithInitializerStatement(
          MPOROptions pOptions,
          CDeclarationEdge pDeclarationEdge,
          SubstituteEdge pSubstituteEdge,
          CLeftHandSide pPcLeftHandSide,
          int pTargetPc) {

    // "leftover" declarations should be local variables with an initializer
    CDeclaration declaration = pDeclarationEdge.getDeclaration();
    checkArgument(
        declaration instanceof CVariableDeclaration, "pDeclarationEdge must declare variable");
    return new SeqLocalVariableDeclarationWithInitializerStatement(
        pOptions,
        (CVariableDeclaration) declaration,
        pPcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private static SeqThreadStatement buildFunctionCallStatement(
      MPOROptions pOptions,
      MPORThread pThread,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      ProgramCounterVariables pProgramCounterVariables,
      FunctionStatements pFunctionStatements) {

    CLeftHandSide pcLeftHandSide = pProgramCounterVariables.getPcLeftHandSide(pThread.getId());
    // function calls -> store parameters in ghost variables
    if (MPORUtil.isReachErrorCall(pThreadEdge.cfaEdge)) {
      // inject non-inlined reach_error
      return new SeqReachErrorStatement(
          pOptions, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
    }
    if (pFunctionStatements.parameterAssignments.containsKey(pThreadEdge)) {
      // handle function with parameters
      ImmutableList<FunctionParameterAssignment> assignments =
          pFunctionStatements.parameterAssignments.get(pThreadEdge);
      assert !assignments.isEmpty() : "function has no parameters";
      if (MPORUtil.isAssumeAbortIfNotCall(pThreadEdge.cfaEdge)) {
        // add separate assume call - it triggers loop head assumption re-evaluation
        return new SeqAssumeAbortIfNotStatement(
            pOptions, assignments, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      }
      return new SeqParameterAssignmentStatements(
          pOptions, assignments, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
    } else {
      // handle function without parameters
      assert CFAUtils.getParameterDeclarationsFromCfaEdge(pThreadEdge.cfaEdge).isEmpty()
          : "function has parameters, but they are not present in pFunctionStatements";
      return new SeqBlankStatement(pOptions, pcLeftHandSide, pTargetPc);
    }
  }

  private static SeqThreadStatement buildReturnValueAssignmentStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      FunctionStatements pFunctionStatements) {

    // returning from non-start-routine function: assign return value to return vars
    if (pFunctionStatements.returnValueAssignments.containsKey(pThreadEdge)) {
      FunctionReturnValueAssignment assignment =
          Objects.requireNonNull(pFunctionStatements.returnValueAssignments.get(pThreadEdge));
      return new SeqReturnValueAssignmentStatement(
          pOptions,
          assignment.statement,
          pPcLeftHandSide,
          ImmutableSet.of(pSubstituteEdge),
          pTargetPc);
    } else {
      // -> function does not return anything, i.e. return;
      return new SeqBlankStatement(pOptions, pPcLeftHandSide, pTargetPc);
    }
  }

  private static SeqThreadStatement buildStatementFromPthreadFunction(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<MPORThread> pAllThreads,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      GhostElements pGhostElements) {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    PthreadFunctionType pthreadFunctionType = PthreadUtil.getPthreadFunctionType(cfaEdge);
    CLeftHandSide pcLeftHandSide =
        pGhostElements.getPcVariables().getPcLeftHandSide(pThread.getId());

    return switch (pthreadFunctionType) {
      case PTHREAD_COND_SIGNAL ->
          buildCondSignalStatement(
              pOptions,
              pThreadEdge,
              pSubstituteEdge,
              pTargetPc,
              pcLeftHandSide,
              pGhostElements.getThreadSyncFlags());
      case PTHREAD_COND_WAIT ->
          throw new AssertionError(
              "pthread_cond_wait is handled separately, it requires two clauses");
      case PTHREAD_CREATE ->
          buildThreadCreationStatement(
              pOptions,
              pThread,
              pAllThreads,
              pThreadEdge,
              pSubstituteEdge,
              pTargetPc,
              pGhostElements.getFunctionStatementsByThread(pThread),
              pGhostElements.getPcVariables());
      case PTHREAD_EXIT ->
          buildThreadExitStatement(
              pOptions,
              pThreadEdge,
              pSubstituteEdge,
              pTargetPc,
              pGhostElements.getFunctionStatementsByThread(pThread),
              pcLeftHandSide);
      case PTHREAD_JOIN ->
          buildThreadJoinStatement(
              pOptions, pThread, pAllThreads, pSubstituteEdge, pTargetPc, pGhostElements);
      case PTHREAD_MUTEX_LOCK ->
          buildMutexLockStatement(
              pOptions,
              pThreadEdge,
              pSubstituteEdge,
              pTargetPc,
              pcLeftHandSide,
              pGhostElements.getThreadSyncFlags());
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexUnlockStatement(
              pOptions,
              pSubstituteEdge,
              pTargetPc,
              pcLeftHandSide,
              pGhostElements.getThreadSyncFlags());
      case PTHREAD_RWLOCK_RDLOCK, PTHREAD_RWLOCK_UNLOCK, PTHREAD_RWLOCK_WRLOCK ->
          buildRwLockStatement(
              pOptions,
              pSubstituteEdge,
              pTargetPc,
              pcLeftHandSide,
              pGhostElements.getThreadSyncFlags(),
              pthreadFunctionType);
      case __VERIFIER_ATOMIC_BEGIN ->
          buildAtomicBeginStatement(pOptions, pSubstituteEdge, pTargetPc, pcLeftHandSide);
      case __VERIFIER_ATOMIC_END ->
          buildAtomicEndStatement(pOptions, pSubstituteEdge, pTargetPc, pcLeftHandSide);
      default ->
          throw new AssertionError(
              "unhandled relevant pthread method: " + pthreadFunctionType.name);
    };
  }

  private static SeqCondSignalStatement buildCondSignalStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSyncFlags pThreadSyncFlags) {

    CIdExpression pthreadCondT =
        PthreadUtil.extractPthreadObject(pThreadEdge.cfaEdge, PthreadObjectType.PTHREAD_COND_T);
    CondSignaledFlag condSignaledFlag = pThreadSyncFlags.getCondSignaledFlag(pthreadCondT);
    return new SeqCondSignalStatement(
        pOptions, condSignaledFlag, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public static SeqCondWaitStatement buildCondWaitStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSyncFlags pThreadSyncFlags) {

    CIdExpression pthreadCondT =
        PthreadUtil.extractPthreadObject(pThreadEdge.cfaEdge, PthreadObjectType.PTHREAD_COND_T);
    CondSignaledFlag condSignaledFlag = pThreadSyncFlags.getCondSignaledFlag(pthreadCondT);

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(pThreadEdge.cfaEdge, PthreadObjectType.PTHREAD_MUTEX_T);
    MutexLockedFlag mutexLockedFlag = pThreadSyncFlags.getMutexLockedFlag(pthreadMutexT);

    return new SeqCondWaitStatement(
        pOptions,
        condSignaledFlag,
        mutexLockedFlag,
        pPcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private static SeqThreadCreationStatement buildThreadCreationStatement(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<MPORThread> pAllThreads,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      FunctionStatements pFunctionStatements,
      ProgramCounterVariables pPcVariables) {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    checkArgument(
        cfaEdge instanceof CFunctionCallEdge || cfaEdge instanceof CStatementEdge,
        "cfaEdge must be CFunctionCallEdge or CStatementEdge");
    CExpression pthreadTObject =
        PthreadUtil.extractPthreadObject(cfaEdge, PthreadObjectType.PTHREAD_T);
    MPORThread createdThread =
        ThreadUtil.getThreadByObject(pAllThreads, Optional.of(pthreadTObject));
    Optional<FunctionParameterAssignment> startRoutineArgAssignment =
        pFunctionStatements.tryGetStartRoutineArgAssignmentByThreadEdge(pThreadEdge);
    return new SeqThreadCreationStatement(
        pOptions,
        startRoutineArgAssignment,
        createdThread,
        pThread,
        pPcVariables,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private static SeqThreadExitStatement buildThreadExitStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      FunctionStatements pFunctionStatements,
      CLeftHandSide pPcLeftHandSide) {

    checkArgument(
        pFunctionStatements.startRoutineExitAssignments.containsKey(pThreadEdge),
        "could not find pThreadEdge in returnValueAssignments");

    return new SeqThreadExitStatement(
        pOptions,
        pFunctionStatements.startRoutineExitAssignments.get(pThreadEdge),
        pPcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private static SeqThreadJoinStatement buildThreadJoinStatement(
      MPOROptions pOptions,
      MPORThread pThread,
      ImmutableList<MPORThread> pAllThreads,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      GhostElements pGhostElements) {

    MPORThread targetThread = ThreadUtil.getThreadByCfaEdge(pAllThreads, pSubstituteEdge.cfaEdge);
    return new SeqThreadJoinStatement(
        pOptions,
        targetThread.startRoutineExitVariable,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc,
        pGhostElements.getPcVariables().getThreadNotActiveExpression(targetThread.getId()),
        pGhostElements.getPcVariables().getPcLeftHandSide(pThread.getId()));
  }

  private static SeqMutexLockStatement buildMutexLockStatement(
      MPOROptions pOptions,
      ThreadEdge pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSyncFlags pThreadSyncFlags) {

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(pThreadEdge.cfaEdge, PthreadObjectType.PTHREAD_MUTEX_T);
    MutexLockedFlag mutexLockedFlag = pThreadSyncFlags.getMutexLockedFlag(pthreadMutexT);
    return new SeqMutexLockStatement(
        pOptions, mutexLockedFlag, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public static SeqMutexUnlockStatement buildMutexUnlockStatement(
      MPOROptions pOptions,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSyncFlags pThreadSyncFlags) {

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(
            pSubstituteEdge.cfaEdge, PthreadObjectType.PTHREAD_MUTEX_T);
    // TODO goblint-regression/13-privatized_68-pfscan_protected_loop_minimal_interval_true
    //  causes issues here, the pthreadMutexT is a parameter which is not in the locked map
    MutexLockedFlag mutexLocked = pThreadSyncFlags.getMutexLockedFlag(pthreadMutexT);
    return new SeqMutexUnlockStatement(
        pOptions, mutexLocked, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqThreadStatement buildRwLockStatement(
      MPOROptions pOptions,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide,
      ThreadSyncFlags pThreadSyncFlags,
      PthreadFunctionType pPthreadFunctionType) {

    CIdExpression rwLockT =
        PthreadUtil.extractPthreadObject(
            pSubstituteEdge.cfaEdge, PthreadObjectType.PTHREAD_RWLOCK_T);
    RwLockNumReadersWritersFlag rwLockFlags = pThreadSyncFlags.getRwLockFlag(rwLockT);
    return switch (pPthreadFunctionType) {
      case PTHREAD_RWLOCK_RDLOCK ->
          new SeqRwLockRdLockStatement(
              pOptions, rwLockFlags, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case PTHREAD_RWLOCK_UNLOCK ->
          new SeqRwLockUnlockStatement(
              pOptions, rwLockFlags, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case PTHREAD_RWLOCK_WRLOCK ->
          new SeqRwLockWrLockStatement(
              pOptions, rwLockFlags, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      default ->
          throw new AssertionError(
              String.format("pPthreadFunctionType is no rwlock method: %s", pPthreadFunctionType));
    };
  }

  private static SeqAtomicBeginStatement buildAtomicBeginStatement(
      MPOROptions pOptions,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide) {

    return new SeqAtomicBeginStatement(
        pOptions, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private static SeqAtomicEndStatement buildAtomicEndStatement(
      MPOROptions pOptions,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      CLeftHandSide pPcLeftHandSide) {

    return new SeqAtomicEndStatement(
        pOptions, pPcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  /**
   * Returns true if the resulting statement has only pc adjustments, i.e. no code changing the
   * input program state.
   */
  private static boolean yieldsNoStatement(
      MPORThread pThread, SubstituteEdge pSubstituteEdge, CFANode pSuccessor) {

    // exiting start_routine of thread -> blank, just set pc[i] = -1;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().equals(pThread.startRoutine)) {
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

    } else if (PthreadUtil.isPthreadMutexInitializerAssignment(pSubstituteEdge.cfaEdge)) {
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

    } else if (PthreadUtil.isCallToAnyPthreadFunction(pSubstituteEdge.cfaEdge)) {
      // not explicitly handled PthreadFunc -> empty case code
      return !PthreadUtil.isExplicitlyHandledPthreadFunction(pSubstituteEdge.cfaEdge);
    }
    return false;
  }
}
