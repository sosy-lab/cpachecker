// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.thread_statements;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionStatements;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.CondSignaledFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.ThreadSyncFlags;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record SeqThreadStatementBuilder(
    MPOROptions options,
    MPORThread thread,
    ImmutableList<MPORThread> allThreads,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    FunctionStatements functionStatements,
    ThreadSyncFlags threadSyncFlags,
    CLeftHandSide pcLeftHandSide,
    ProgramCounterVariables pcVariables) {

  public ImmutableList<CSeqThreadStatement> buildStatementsFromThreadNode(
      CFANodeForThread pThreadNode, Set<CFANodeForThread> pCoveredNodes)
      throws UnsupportedCodeException {

    ImmutableList.Builder<CSeqThreadStatement> rStatements = ImmutableList.builder();

    ImmutableList<CFAEdgeForThread> leavingEdges = pThreadNode.leavingEdges();
    int numLeavingEdges = leavingEdges.size();
    for (int i = 0; i < numLeavingEdges; i++) {
      CFAEdgeForThread threadEdge = leavingEdges.get(i);

      // handle const CPAchecker_TMP first because it requires successor nodes and edges
      if (MPORUtil.isConstCpaCheckerTmpDeclaration(threadEdge.cfaEdge)) {
        rStatements.add(buildConstCpaCheckerTmpStatement(threadEdge, pCoveredNodes));

        // we exclude all function summaries, the calling context is handled by return edges
      } else if (!(threadEdge.cfaEdge instanceof FunctionSummaryEdge)) {
        if (substituteEdges.containsKey(threadEdge)) {
          SubstituteEdge substitute = Objects.requireNonNull(substituteEdges.get(threadEdge));
          rStatements.add(buildStatementFromThreadEdge(i == 0, threadEdge, substitute));
        }
      }
    }
    return rStatements.build();
  }

  // const CPAchecker_TMP ==========================================================================

  private SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
      CFAEdgeForThread pThreadEdge, Set<CFANodeForThread> pCoveredNodes) {

    // ensure there are two single successors that are both statement edges
    CFANodeForThread firstSuccessor = pThreadEdge.getSuccessor();
    pCoveredNodes.add(firstSuccessor);
    assert firstSuccessor.leavingEdges().size() == 1
        : "const CPAchecker_TMP declarations can have only 1 successor edge";
    CFAEdgeForThread firstSuccessorEdge = firstSuccessor.firstLeavingEdge();
    assert firstSuccessorEdge.cfaEdge instanceof CStatementEdge
        : "successor edge of const CPAchecker_TMP declaration must be CStatementEdge";
    CFANodeForThread secondSuccessor = firstSuccessorEdge.getSuccessor();
    assert secondSuccessor.leavingEdges().size() == 1
        : "second successor of const CPAchecker_TMP declarations can have only 1 successor edge";
    CFAEdgeForThread secondSuccessorEdge = secondSuccessor.firstLeavingEdge();

    CStatementEdge secondSuccessorStatement = (CStatementEdge) secondSuccessorEdge.cfaEdge;
    // there are programs where a const CPAchecker_TMP statement has only two parts.
    // in the tested programs, this only happened when the statement was followed by a function call
    if (secondSuccessorStatement.getStatement() instanceof CFunctionCallStatement) {
      return buildTwoPartConstCpaCheckerTmpStatement(pThreadEdge, firstSuccessorEdge);
    } else {
      // cover second successor only when it is a three part const CPAchecker_TMP statement
      pCoveredNodes.add(secondSuccessor);
      return buildThreePartConstCpaCheckerTmpStatement(
          pThreadEdge, firstSuccessorEdge, secondSuccessorEdge);
    }
  }

  private SeqConstCpaCheckerTmpStatement buildTwoPartConstCpaCheckerTmpStatement(
      CFAEdgeForThread pThreadEdge, CFAEdgeForThread pSuccessorEdge) {

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge substituteEdge = Objects.requireNonNull(substituteEdges.get(pThreadEdge));
    CFAEdge cfaEdge = substituteEdge.cfaEdge;
    assert cfaEdge instanceof CDeclarationEdge : "cfaEdge must declare const CPAchecker_TMP";
    SubstituteEdge substituteEdgeA = Objects.requireNonNull(substituteEdges.get(pSuccessorEdge));
    int newTargetPc = pSuccessorEdge.getSuccessor().pc;

    return buildConstCpaCheckerTmpStatement(
        cfaEdge,
        substituteEdgeA,
        Optional.empty(),
        ImmutableSet.of(substituteEdge, substituteEdgeA),
        newTargetPc);
  }

  private SeqConstCpaCheckerTmpStatement buildThreePartConstCpaCheckerTmpStatement(
      CFAEdgeForThread pThreadEdge,
      CFAEdgeForThread pFirstSuccessorEdge,
      CFAEdgeForThread pSecondSuccessorEdge) {

    // treat const CPAchecker_TMP var as atomic (3 statements in 1 case)
    SubstituteEdge substituteEdge = Objects.requireNonNull(substituteEdges.get(pThreadEdge));
    CFAEdge cfaEdge = substituteEdge.cfaEdge;
    assert cfaEdge instanceof CDeclarationEdge : "cfaEdge must declare const CPAchecker_TMP";
    SubstituteEdge firstSuccessorEdge =
        Objects.requireNonNull(substituteEdges.get(pFirstSuccessorEdge));
    SubstituteEdge secondSuccessorEdge =
        Objects.requireNonNull(substituteEdges.get(pSecondSuccessorEdge));
    int newTargetPc = pSecondSuccessorEdge.getSuccessor().pc;

    return buildConstCpaCheckerTmpStatement(
        cfaEdge,
        firstSuccessorEdge,
        Optional.of(secondSuccessorEdge),
        ImmutableSet.of(substituteEdge, firstSuccessorEdge, secondSuccessorEdge),
        newTargetPc);
  }

  private SeqConstCpaCheckerTmpStatement buildConstCpaCheckerTmpStatement(
      CFAEdge pCfaEdge,
      SubstituteEdge pFirstSuccessorEdge,
      Optional<SubstituteEdge> pSecondSuccessorEdge,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pNewTargetPc) {

    // ensure that declaration is variable declaration and cast accordingly
    CDeclarationEdge declarationEdge = (CDeclarationEdge) pCfaEdge;
    CDeclaration declaration = declarationEdge.getDeclaration();
    assert declaration instanceof CVariableDeclaration : "declarationEdge must declare variable";
    CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;

    return new SeqConstCpaCheckerTmpStatement(
        options,
        variableDeclaration,
        pFirstSuccessorEdge,
        pSecondSuccessorEdge,
        pcLeftHandSide,
        pSubstituteEdges,
        pNewTargetPc);
  }

  // Statement build methods =======================================================================

  private CSeqThreadStatement buildStatementFromThreadEdge(
      boolean pFirstEdge, CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge)
      throws UnsupportedCodeException {

    CFAEdge cfaEdge = pThreadEdge.cfaEdge;
    int targetPc = pThreadEdge.getSuccessor().pc;
    CFANode successor = pThreadEdge.getSuccessor().cfaNode;

    if (yieldsNoStatement(pSubstituteEdge, successor)) {
      return new SeqBlankStatement(options, pcLeftHandSide, targetPc);
    }

    ImmutableSet<SubstituteEdge> substituteEdgeSet = ImmutableSet.of(pSubstituteEdge);
    return switch (pSubstituteEdge.cfaEdge) {
      case CAssumeEdge assumeEdge ->
          // for the first assume edge, use "if (expression)", for second, use "else"
          pFirstEdge
              ? new SeqAssumeStatement(
                  options, assumeEdge.getExpression(), pcLeftHandSide, substituteEdgeSet, targetPc)
              : new SeqAssumeStatement(options, pcLeftHandSide, substituteEdgeSet, targetPc);

      case CDeclarationEdge declarationEdge -> {
        // "leftover" declarations should be local variables with an initializer
        CVariableDeclaration variableDeclaration =
            (CVariableDeclaration) declarationEdge.getDeclaration();
        yield new SeqLocalVariableDeclarationWithInitializerStatement(
            options, variableDeclaration, pcLeftHandSide, substituteEdgeSet, targetPc);
      }

      case CFunctionCallEdge functionCallEdge ->
          buildFunctionCallStatement(pThreadEdge, functionCallEdge, substituteEdgeSet, targetPc);

      case CReturnStatementEdge ignore ->
          buildReturnValueAssignmentStatement(pThreadEdge, substituteEdgeSet, targetPc);

      case CFAEdge edge when PthreadUtil.isExplicitlyHandledPthreadFunction(edge) ->
          buildStatementFromPthreadFunction(pThreadEdge, pSubstituteEdge, targetPc);

      case CStatementEdge statementEdge ->
          new SeqDefaultStatement(
              options, statementEdge, pcLeftHandSide, substituteEdgeSet, targetPc);

      default ->
          throw new AssertionError("Unhandled CFAEdge type: " + cfaEdge.getClass().getSimpleName());
    };
  }

  private CSeqThreadStatement buildFunctionCallStatement(
      CFAEdgeForThread pThreadEdge,
      CFunctionCallEdge pFunctionCallEdge,
      ImmutableSet<SubstituteEdge> pSubstituteEdges,
      int pTargetPc) {

    // function calls -> store parameters in ghost variables
    if (MPORUtil.isReachErrorCall(pThreadEdge.cfaEdge)) {
      // inject non-inlined reach_error
      return new SeqReachErrorStatement(options, pcLeftHandSide, pSubstituteEdges, pTargetPc);

    } else {
      if (functionStatements.parameterAssignments().containsKey(pThreadEdge)) {
        // handle function with parameters
        ImmutableList<FunctionParameterAssignment> assignments =
            functionStatements.parameterAssignments().get(pThreadEdge);
        return new SeqParameterAssignmentStatements(
            options, assignments, pcLeftHandSide, pSubstituteEdges, pTargetPc);

      } else {
        // handle function without parameters
        CFunctionDeclaration functionDeclaration =
            pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
        assert functionDeclaration.getParameters().isEmpty()
            : "function has parameters, but they are not present in pFunctionStatements";
        return new SeqBlankStatement(options, pcLeftHandSide, pTargetPc);
      }
    }
  }

  private CSeqThreadStatement buildReturnValueAssignmentStatement(
      CFAEdgeForThread pThreadEdge, ImmutableSet<SubstituteEdge> pSubstituteEdges, int pTargetPc) {

    // returning from non-start-routine function: assign return value to return vars
    if (functionStatements.returnValueAssignments().containsKey(pThreadEdge)) {
      FunctionReturnValueAssignment assignment =
          Objects.requireNonNull(functionStatements.returnValueAssignments().get(pThreadEdge));
      return new SeqReturnValueAssignmentStatement(
          options, assignment.statement(), pcLeftHandSide, pSubstituteEdges, pTargetPc);
    } else {
      // -> function does not return anything, i.e. return;
      return new SeqBlankStatement(options, pcLeftHandSide, pTargetPc);
    }
  }

  private CSeqThreadStatement buildStatementFromPthreadFunction(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    CFunctionCall functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge).orElseThrow();
    PthreadFunctionType pthreadFunctionType = PthreadUtil.getPthreadFunctionType(functionCall);

    return switch (pthreadFunctionType) {
      case PTHREAD_COND_SIGNAL ->
          buildCondSignalStatement(functionCall, pSubstituteEdge, pTargetPc);
      case PTHREAD_COND_WAIT ->
          throw new AssertionError(
              "pthread_cond_wait is handled separately, it requires two clauses");
      case PTHREAD_CREATE ->
          buildThreadCreationStatement(functionCall, pThreadEdge, pSubstituteEdge, pTargetPc);
      case PTHREAD_EXIT -> buildThreadExitStatement(pThreadEdge, pSubstituteEdge, pTargetPc);
      case PTHREAD_JOIN -> buildThreadJoinStatement(functionCall, pSubstituteEdge, pTargetPc);
      case PTHREAD_MUTEX_LOCK -> buildMutexLockStatement(functionCall, pSubstituteEdge, pTargetPc);
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexUnlockStatement(functionCall, pSubstituteEdge, pTargetPc);
      case PTHREAD_RWLOCK_RDLOCK, PTHREAD_RWLOCK_UNLOCK, PTHREAD_RWLOCK_WRLOCK ->
          buildRwLockStatement(functionCall, pSubstituteEdge, pTargetPc, pthreadFunctionType);
      case VERIFIER_ATOMIC_BEGIN -> buildAtomicBeginStatement(pSubstituteEdge, pTargetPc);
      case VERIFIER_ATOMIC_END -> buildAtomicEndStatement(pSubstituteEdge, pTargetPc);
      default ->
          throw new AssertionError(
              "unhandled relevant pthread method: " + pthreadFunctionType.name);
    };
  }

  private SeqCondSignalStatement buildCondSignalStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    CIdExpression pthreadCondT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_COND_T);
    CondSignaledFlag condSignaledFlag = threadSyncFlags.getCondSignaledFlag(pthreadCondT);
    return new SeqCondSignalStatement(
        options, condSignaledFlag, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public SeqCondWaitStatement buildCondWaitStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    CIdExpression pthreadCondT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_COND_T);
    CondSignaledFlag condSignaledFlag = threadSyncFlags.getCondSignaledFlag(pthreadCondT);

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_MUTEX_T);
    MutexLockedFlag mutexLockedFlag = threadSyncFlags.getMutexLockedFlag(pthreadMutexT);

    return new SeqCondWaitStatement(
        options,
        condSignaledFlag,
        mutexLockedFlag,
        pcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private SeqThreadCreationStatement buildThreadCreationStatement(
      CFunctionCall pFunctionCall,
      CFAEdgeForThread pThreadEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc)
      throws UnsupportedCodeException {

    CFAEdge cfaEdge = pSubstituteEdge.cfaEdge;
    checkArgument(
        cfaEdge instanceof CFunctionCallEdge || cfaEdge instanceof CStatementEdge,
        "cfaEdge must be CFunctionCallEdge or CStatementEdge");

    CExpression pthreadTObject =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_T);
    MPORThread createdThread =
        MPORThreadUtil.getThreadByObject(allThreads, Optional.of(pthreadTObject));
    Optional<FunctionParameterAssignment> startRoutineArgAssignment =
        functionStatements.tryGetStartRoutineArgAssignmentByThreadEdge(pThreadEdge);
    return new SeqThreadCreationStatement(
        options,
        startRoutineArgAssignment,
        pcLeftHandSide,
        pcVariables.getPcLeftHandSide(createdThread.id()),
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private SeqThreadExitStatement buildThreadExitStatement(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    checkArgument(
        functionStatements.startRoutineExitAssignments().containsKey(pThreadEdge),
        "could not find pThreadEdge in returnValueAssignments");

    return new SeqThreadExitStatement(
        options,
        Objects.requireNonNull(functionStatements.startRoutineExitAssignments().get(pThreadEdge)),
        pcLeftHandSide,
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc);
  }

  private SeqThreadJoinStatement buildThreadJoinStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    MPORThread targetThread = MPORThreadUtil.getThreadByCFunctionCall(allThreads, pFunctionCall);
    return new SeqThreadJoinStatement(
        options,
        targetThread.startRoutineExitVariable(),
        ImmutableSet.of(pSubstituteEdge),
        pTargetPc,
        pcVariables.getThreadInactiveExpression(targetThread.id()),
        pcLeftHandSide);
  }

  private SeqMutexLockStatement buildMutexLockStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_MUTEX_T);
    MutexLockedFlag mutexLockedFlag = threadSyncFlags.getMutexLockedFlag(pthreadMutexT);
    return new SeqMutexLockStatement(
        options, mutexLockedFlag, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public SeqMutexUnlockStatement buildMutexUnlockStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_MUTEX_T);
    MutexLockedFlag mutexLocked = threadSyncFlags.getMutexLockedFlag(pthreadMutexT);
    return new SeqMutexUnlockStatement(
        options, mutexLocked, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private CSeqThreadStatement buildRwLockStatement(
      CFunctionCall pFunctionCall,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc,
      PthreadFunctionType pPthreadFunctionType)
      throws UnsupportedCodeException {

    CIdExpression rwLockT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_RWLOCK_T);
    RwLockNumReadersWritersFlag rwLockFlags = threadSyncFlags.getRwLockFlag(rwLockT);
    return switch (pPthreadFunctionType) {
      case PTHREAD_RWLOCK_RDLOCK ->
          new SeqRwLockRdLockStatement(
              options, rwLockFlags, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case PTHREAD_RWLOCK_UNLOCK ->
          new SeqRwLockUnlockStatement(
              options, rwLockFlags, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case PTHREAD_RWLOCK_WRLOCK ->
          new SeqRwLockWrLockStatement(
              options, rwLockFlags, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      default ->
          throw new AssertionError(
              String.format("pPthreadFunctionType is no rwlock method: %s", pPthreadFunctionType));
    };
  }

  private SeqAtomicBeginStatement buildAtomicBeginStatement(
      SubstituteEdge pSubstituteEdge, int pTargetPc) {

    return new SeqAtomicBeginStatement(
        options, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  private SeqAtomicEndStatement buildAtomicEndStatement(
      SubstituteEdge pSubstituteEdge, int pTargetPc) {

    return new SeqAtomicEndStatement(
        options, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  /**
   * Returns true if the resulting statement has only pc adjustments, i.e. no code changing the
   * input program state.
   */
  private boolean yieldsNoStatement(SubstituteEdge pSubstituteEdge, CFANode pSuccessor) {
    // exiting start_routine of thread -> blank, just set pc[i] = -1;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().equals(thread.startRoutine())) {
      return true;

    } else if (pSuccessor.getFunctionName().equals(SeqNameUtil.REACH_ERROR_FUNCTION_NAME)) {
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

    } else {
      Optional<CFunctionCall> functionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(pSubstituteEdge.cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToAnyPthreadFunction(functionCall.orElseThrow())) {
          // not explicitly handled PthreadFunc -> empty case code
          return !PthreadUtil.isExplicitlyHandledPthreadFunction(pSubstituteEdge.cfaEdge);
        }
      }
    }
    return false;
  }
}
