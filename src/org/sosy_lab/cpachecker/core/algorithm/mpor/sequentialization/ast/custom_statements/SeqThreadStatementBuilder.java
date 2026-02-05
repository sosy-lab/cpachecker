// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
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
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFAEdgeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThreadUtil;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CInitializerWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExpressionAssignmentStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

public record SeqThreadStatementBuilder(
    MPORThread thread,
    ImmutableList<MPORThread> allThreads,
    ImmutableMap<CFAEdgeForThread, SubstituteEdge> substituteEdges,
    FunctionStatements functionStatements,
    ThreadSyncFlags threadSyncFlags,
    CLeftHandSide pcLeftHandSide,
    ProgramCounterVariables pcVariables) {

  private static final String REACH_ERROR_FUNCTION_NAME = "reach_error";

  private static final CFunctionTypeWithNames REACH_ERROR_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(), false);

  public static final CFunctionDeclaration REACH_ERROR_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          REACH_ERROR_FUNCTION_TYPE,
          REACH_ERROR_FUNCTION_NAME,
          ImmutableList.of(),
          ImmutableSet.of(FunctionAttribute.NO_RETURN));

  private static final CIdExpression REACH_ERROR_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, REACH_ERROR_FUNCTION_DECLARATION);

  private static final CFunctionCallExpression REACH_ERROR_FUNCTION_CALL_EXPRESSION =
      new CFunctionCallExpression(
          FileLocation.DUMMY,
          CVoidType.VOID,
          REACH_ERROR_ID_EXPRESSION,
          ImmutableList.of(),
          REACH_ERROR_FUNCTION_DECLARATION);

  private static final CFunctionCallStatement REACH_ERROR_FUNCTION_CALL_STATEMENT =
      new CFunctionCallStatement(FileLocation.DUMMY, REACH_ERROR_FUNCTION_CALL_EXPRESSION);

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
      } else {
        // exclude all function summaries, the calling context is handled by return edges
        if (!isExcludedSummaryEdge(threadEdge.cfaEdge)) {
          if (substituteEdges.containsKey(threadEdge)) {
            SubstituteEdge substitute = Objects.requireNonNull(substituteEdges.get(threadEdge));
            rStatements.add(buildStatementFromThreadEdge(i == 0, threadEdge, substitute));
          }
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
        variableDeclaration,
        pFirstSuccessorEdge,
        pSecondSuccessorEdge,
        pcLeftHandSide,
        pSubstituteEdges,
        pNewTargetPc);
  }

  // Statement build methods =======================================================================

  private SeqThreadStatement buildStatementFromThreadEdge(
      boolean pFirstEdge, CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge)
      throws UnsupportedCodeException {

    CFAEdge cfaEdge = pThreadEdge.cfaEdge;
    int targetPc = pThreadEdge.getSuccessor().pc;
    CFANode successor = pThreadEdge.getSuccessor().cfaNode;

    if (resultsInBlankStatement(pSubstituteEdge, successor)) {
      return buildGhostOnlyStatement(pcLeftHandSide, targetPc);
    }

    return switch (pSubstituteEdge.cfaEdge) {
      case CAssumeEdge assumeEdge ->
          buildAssumeStatement(assumeEdge, pFirstEdge, pcLeftHandSide, pSubstituteEdge, targetPc);

      case CDeclarationEdge declarationEdge -> {
        // "leftover" declarations should be local variables with an initializer
        CVariableDeclaration variableDeclaration =
            (CVariableDeclaration) declarationEdge.getDeclaration();
        yield buildLocalVariableDeclarationWithInitializerStatement(
            variableDeclaration, pSubstituteEdge, pcLeftHandSide, targetPc);
      }

      case CFunctionCallEdge functionCallEdge ->
          buildFunctionCallStatement(pThreadEdge, functionCallEdge, pSubstituteEdge, targetPc);

      case CReturnStatementEdge ignore ->
          buildReturnValueAssignmentStatement(pThreadEdge, pSubstituteEdge, targetPc);

      case CFAEdge edge when PthreadUtil.isExplicitlyHandledPthreadFunction(edge) ->
          buildStatementFromPthreadFunction(pThreadEdge, pSubstituteEdge, targetPc);

      case CStatementEdge statementEdge ->
          buildDefaultStatement(statementEdge, pSubstituteEdge, pcLeftHandSide, targetPc);

      default ->
          throw new AssertionError("Unhandled CFAEdge type: " + cfaEdge.getClass().getSimpleName());
    };
  }

  private static SeqThreadStatement buildAssumeStatement(
      CAssumeEdge pAssumeEdge,
      boolean pFirstEdge,
      CLeftHandSide pPcLeftHandSide,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc) {

    // for the first assume edge, use "if (expression)", for second, use "else" (no expression)
    Optional<CExpression> ifExpression =
        pFirstEdge ? Optional.of(pAssumeEdge.getExpression()) : Optional.empty();

    SeqThreadStatementData data =
        new SeqThreadStatementData(
            SeqThreadStatementType.ASSUME,
            ImmutableSet.of(pSubstituteEdge),
            pPcLeftHandSide,
            Optional.of(pTargetPc),
            Optional.empty(),
            ImmutableList.of(),
            ifExpression);

    // just return the finalized statements, the block handles the if-else branch
    return new SeqThreadStatement(data, finalizeExportStatements(data, ImmutableList.of()));
  }

  private static SeqThreadStatement buildDefaultStatement(
      CStatementEdge pStatementEdge,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.DEFAULT, pSubstituteEdge, pPcLeftHandSide, pTargetPc);
    return new SeqThreadStatement(
        data, ImmutableList.of(new CStatementWrapper(pStatementEdge.getStatement())));
  }

  private static SeqThreadStatement buildGhostOnlyStatement(
      CLeftHandSide pPcLeftHandSide, int pTargetPc) {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.GHOST_ONLY, ImmutableSet.of(), pPcLeftHandSide, pTargetPc);
    return new SeqThreadStatement(data, finalizeExportStatements(data, ImmutableList.of()));
  }

  public static SeqThreadStatement buildLocalVariableDeclarationWithInitializerStatement(
      CVariableDeclaration pVariableDeclaration,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    checkArgument(!pVariableDeclaration.isGlobal(), "pVariableDeclaration must be local");
    checkArgument(
        pVariableDeclaration.getInitializer() != null,
        "pVariableDeclaration must have an initializer");

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.LOCAL_VARIABLE_DECLARATION_WITH_INITIALIZER,
            pSubstituteEdge,
            pPcLeftHandSide,
            pTargetPc);

    // the local variable is declared outside main() without an initializer e.g. 'int x;', and here
    // it is assigned the initializer e.g. 'x = 7;'
    CIdExpression idExpression =
        new CIdExpression(pVariableDeclaration.getFileLocation(), pVariableDeclaration);
    CInitializerWrapper initializer =
        new CInitializerWrapper(pVariableDeclaration.getInitializer());
    CExpressionAssignmentStatementWrapper assignment =
        new CExpressionAssignmentStatementWrapper(idExpression, initializer);

    return new SeqThreadStatement(
        data, finalizeExportStatements(data, ImmutableList.of(assignment)));
  }

  private SeqThreadStatement buildFunctionCallStatement(
      CFAEdgeForThread pThreadEdge,
      CFunctionCallEdge pFunctionCallEdge,
      SubstituteEdge pSubstituteEdge,
      int pTargetPc) {

    String functionName =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration().getOrigName();
    SeqThreadStatementData parameterAssignmentData =
        SeqThreadStatementData.of(
            SeqThreadStatementType.PARAMETER_ASSIGNMENT,
            pSubstituteEdge,
            pcLeftHandSide,
            pTargetPc);

    // handle (some arbitrary) function with parameters
    if (functionStatements.parameterAssignments().containsKey(pThreadEdge)) {
      ImmutableList<FunctionParameterAssignment> assignments =
          functionStatements.parameterAssignments().get(pThreadEdge);
      return buildParameterAssignmentStatement(functionName, assignments, parameterAssignmentData);
    }

    // handle function without parameters that is a call to "reach_error"
    if (functionName.equals(REACH_ERROR_FUNCTION_NAME)) {
      return buildParameterAssignmentStatement(
          functionName, ImmutableList.of(), parameterAssignmentData);
    }

    // handle function without parameters that is not "reach_error" -> blank statement
    CFunctionDeclaration functionDeclaration =
        pFunctionCallEdge.getFunctionCallExpression().getDeclaration();
    assert functionDeclaration.getParameters().isEmpty()
        : "function has parameters, but they are not present in pFunctionStatements";

    return buildGhostOnlyStatement(pcLeftHandSide, pTargetPc);
  }

  private static SeqThreadStatement buildParameterAssignmentStatement(
      String pFunctionName,
      ImmutableList<FunctionParameterAssignment> pFunctionParameterAssignments,
      SeqThreadStatementData pData) {

    checkArgument(
        !pFunctionParameterAssignments.isEmpty() || pFunctionName.equals(REACH_ERROR_FUNCTION_NAME),
        "If pAssignments is empty, then the function name must be reach_error.");

    ImmutableList.Builder<CExportStatement> functionStatements = ImmutableList.builder();
    // if the function name is "reach_error", inject a "reach_error()" call for reachability
    if (pFunctionName.equals(REACH_ERROR_FUNCTION_NAME)) {
      functionStatements.add(new CStatementWrapper(REACH_ERROR_FUNCTION_CALL_STATEMENT));
    }
    for (FunctionParameterAssignment assignment : pFunctionParameterAssignments) {
      functionStatements.add(new CStatementWrapper(assignment.toExpressionAssignmentStatement()));
    }
    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, functionStatements.build()));
  }

  private SeqThreadStatement buildReturnValueAssignmentStatement(
      CFAEdgeForThread pThreadEdge, SubstituteEdge pSubstituteEdge, int pTargetPc) {

    // returning from non-start-routine function: assign return value to return vars
    if (functionStatements.returnValueAssignments().containsKey(pThreadEdge)) {
      FunctionReturnValueAssignment assignment =
          Objects.requireNonNull(functionStatements.returnValueAssignments().get(pThreadEdge));
      SeqThreadStatementData data =
          SeqThreadStatementData.of(
              SeqThreadStatementType.DEFAULT, pSubstituteEdge, pcLeftHandSide, pTargetPc);
      return new SeqThreadStatement(
          data,
          finalizeExportStatements(
              data, ImmutableList.of(new CStatementWrapper(assignment.statement()))));
    }

    // -> function does not return anything, i.e. return;
    return buildGhostOnlyStatement(pcLeftHandSide, pTargetPc);
  }

  private SeqThreadStatement buildStatementFromPthreadFunction(
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
      case VERIFIER_ATOMIC_BEGIN ->
          new SeqAtomicBeginStatement(pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case VERIFIER_ATOMIC_END ->
          new SeqAtomicEndStatement(pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
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
        condSignaledFlag, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
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
        mutexLockedFlag, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
  }

  public SeqMutexUnlockStatement buildMutexUnlockStatement(
      CFunctionCall pFunctionCall, SubstituteEdge pSubstituteEdge, int pTargetPc)
      throws UnsupportedCodeException {

    CIdExpression pthreadMutexT =
        PthreadUtil.extractPthreadObject(pFunctionCall, PthreadObjectType.PTHREAD_MUTEX_T);
    MutexLockedFlag mutexLocked = threadSyncFlags.getMutexLockedFlag(pthreadMutexT);
    return new SeqMutexUnlockStatement(
        mutexLocked, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
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
              rwLockFlags, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case PTHREAD_RWLOCK_UNLOCK ->
          new SeqRwLockUnlockStatement(
              rwLockFlags, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      case PTHREAD_RWLOCK_WRLOCK ->
          new SeqRwLockWrLockStatement(
              rwLockFlags, pcLeftHandSide, ImmutableSet.of(pSubstituteEdge), pTargetPc);
      default ->
          throw new AssertionError(
              String.format("pPthreadFunctionType is no rwlock method: %s", pPthreadFunctionType));
    };
  }

  /**
   * Returns {@code true} if the resulting statement has only {@code pc} adjustments, i.e. no code
   * changing the input program state, {@code false} otherwise.
   *
   * <p>This is the case when:
   *
   * <ul>
   *   <li>{@code pSuccessor} marks the termination of a thread
   *   <li>{@code pSubstituteEdge} itself is a {@link BlankEdge}
   *   <li>{@code pSubstituteEdge} is a {@code PTHREAD_MUTEX_INITIALIZER} assignment
   *   <li>{@code pSubstituteEdge} is a {@link CDeclarationEdge}, except for local variable
   *       declarations with an initializer
   *   <li>{@code pSubstituteEdge} is a call to a {@code pthread} function that is not explicitly
   *       handled.
   * </ul>
   */
  private boolean resultsInBlankStatement(SubstituteEdge pSubstituteEdge, CFANode pSuccessor) {
    // exiting start_routine of thread -> blank, just set pc = EXIT_PC;
    if (pSuccessor instanceof FunctionExitNode
        && pSuccessor.getFunction().equals(thread.startRoutine())) {
      return true;

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

  /**
   * Takes the given {@link CExportStatement}s and appends the {@link SeqInjectedStatement} to them.
   * Given that all {@link CSeqThreadStatement}s have injected statements that are placed after the
   * actual statements, this is handled here and not by each specific {@link CSeqThreadStatement}s.
   */
  private static ImmutableList<CExportStatement> finalizeExportStatements(
      SeqThreadStatementData pData, ImmutableList<CExportStatement> pExportStatements) {

    checkState(
        pData.targetPc().isPresent() || pData.targetGoto().isPresent(),
        "Either targetPc or targetGoto must be present.");

    // first build the CExportStatements of the SeqInjectedStatement
    ImmutableList<SeqInjectedStatement> preparedInjectedStatements =
        pData.targetPc().isPresent()
            ? SeqThreadStatementUtil.prepareInjectedStatementsByTargetPc(
                pData.pcLeftHandSide(), pData.targetPc().orElseThrow(), pData.injectedStatements())
            : SeqThreadStatementUtil.prepareInjectedStatementsByTargetGoto(
                pData.targetGoto().orElseThrow(), pData.injectedStatements());

    ImmutableList<CExportStatement> injectedExportStatements =
        preparedInjectedStatements.stream()
            .flatMap(injected -> injected.toCExportStatements().stream())
            .collect(ImmutableList.toImmutableList());

    return ImmutableList.<CExportStatement>builder()
        .addAll(pExportStatements)
        .addAll(injectedExportStatements)
        .build();
  }

  private boolean isExcludedSummaryEdge(CFAEdge pCfaEdge) {
    return pCfaEdge instanceof CFunctionSummaryEdge
        || pCfaEdge instanceof CFunctionSummaryStatementEdge;
  }
}
