// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqStatementBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIntegerLiteralExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionParameterAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.function_statements.FunctionReturnValueAssignment;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.CondSignaledFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.MutexLockedFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags.RwLockNumReadersWritersFlag;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.expression.CInitializerWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCommentStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CExpressionAssignmentStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.statement.CStatementWrapper;

public class SeqThreadStatementFactory {

  // Functions for all SeqThreadStatementType

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

  // Functions for specific SeqThreadStatementType

  public static SeqThreadStatement buildAssumeStatement(
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

  public static SeqThreadStatement buildAtomicBeginStatement(SeqThreadStatementData pData) {
    // just add a comment with the function name for better overview in the output program
    CCommentStatement commentStatement =
        new CCommentStatement(PthreadFunctionType.VERIFIER_ATOMIC_BEGIN.name + ";");
    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, ImmutableList.of(commentStatement)));
  }

  public static SeqThreadStatement buildAtomicEndStatement(SeqThreadStatementData pData) {
    // just add a comment with the function name for better overview in the output program
    CCommentStatement commentStatement =
        new CCommentStatement(PthreadFunctionType.VERIFIER_ATOMIC_END.name + ";");
    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, ImmutableList.of(commentStatement)));
  }

  public static SeqThreadStatement buildCondSignalStatement(
      SeqThreadStatementData pData, CondSignaledFlag pCondSignaledFlag) {

    CExpressionAssignmentStatement setCondSignaledTrue =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pCondSignaledFlag.idExpression(), SeqIntegerLiteralExpressions.INT_1);
    return new SeqThreadStatement(
        pData,
        finalizeExportStatements(
            pData, ImmutableList.of(new CStatementWrapper(setCondSignaledTrue))));
  }

  public static SeqThreadStatement buildCondWaitStatement(
      SeqThreadStatementData pData,
      CondSignaledFlag pCondSignaledFlag,
      MutexLockedFlag pMutexLockedFlag) {

    // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait

    // step 1: the calling thread blocks on the condition variable -> assume(signaled == 1)
    CFunctionCallStatement assumeSignaled =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(
            pCondSignaledFlag.isSignaledExpression());
    CExpressionAssignmentStatement setSignaledFalse =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pCondSignaledFlag.idExpression(), SeqIntegerLiteralExpressions.INT_0);

    // step 2: on return, the mutex is locked and owned by the calling thread -> mutex_locked = 1
    CExpressionAssignmentStatement setMutexLockedTrue =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pMutexLockedFlag.idExpression(), SeqIntegerLiteralExpressions.INT_1);

    return new SeqThreadStatement(
        pData,
        finalizeExportStatements(
            pData,
            ImmutableList.of(
                new CStatementWrapper(assumeSignaled),
                new CStatementWrapper(setSignaledFalse),
                new CStatementWrapper(setMutexLockedTrue))));
  }

  public static SeqThreadStatement buildDefaultStatement(
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

  public static SeqThreadStatement buildGhostOnlyStatement(
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

  public static SeqThreadStatement buildMutexLockStatement(
      SeqThreadStatementData pData, MutexLockedFlag pMutexLockedFlag) {

    CFunctionCallStatement assumeCall =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(pMutexLockedFlag.notLockedExpression());
    CExpressionAssignmentStatement setMutexLockedTrue =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pMutexLockedFlag.idExpression(), SeqIntegerLiteralExpressions.INT_1);

    return new SeqThreadStatement(
        pData,
        finalizeExportStatements(
            pData,
            ImmutableList.of(
                new CStatementWrapper(assumeCall), new CStatementWrapper(setMutexLockedTrue))));
  }

  public static SeqThreadStatement buildMutexUnlockStatement(
      SeqThreadStatementData pData, MutexLockedFlag pMutexLockedFlag) {

    CStatementWrapper lockedFalseAssignment =
        new CStatementWrapper(
            SeqStatementBuilder.buildExpressionAssignmentStatement(
                pMutexLockedFlag.idExpression(), SeqIntegerLiteralExpressions.INT_0));

    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, ImmutableList.of(lockedFalseAssignment)));
  }

  // Parameter Assignment Statements

  public static final String REACH_ERROR_FUNCTION_NAME = "reach_error";

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

  public static SeqThreadStatement buildParameterAssignmentStatement(
      String pFunctionName,
      ImmutableList<FunctionParameterAssignment> pFunctionParameterAssignments,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    checkArgument(
        !pFunctionParameterAssignments.isEmpty() || pFunctionName.equals(REACH_ERROR_FUNCTION_NAME),
        "If pAssignments is empty, then the function name must be reach_error.");

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.PARAMETER_ASSIGNMENT,
            pSubstituteEdge,
            pPcLeftHandSide,
            pTargetPc);

    ImmutableList.Builder<CExportStatement> functionStatements = ImmutableList.builder();
    // if the function name is "reach_error", inject a "reach_error()" call for reachability
    if (pFunctionName.equals(REACH_ERROR_FUNCTION_NAME)) {
      functionStatements.add(new CStatementWrapper(REACH_ERROR_FUNCTION_CALL_STATEMENT));
    }
    for (FunctionParameterAssignment assignment : pFunctionParameterAssignments) {
      functionStatements.add(new CStatementWrapper(assignment.toExpressionAssignmentStatement()));
    }

    return new SeqThreadStatement(data, finalizeExportStatements(data, functionStatements.build()));
  }

  public static SeqThreadStatement buildReturnValueAssignmentStatement(
      CExpressionAssignmentStatement pReturnValueAssignment,
      SubstituteEdge pSubstituteEdge,
      CLeftHandSide pPcLeftHandSide,
      int pTargetPc) {

    SeqThreadStatementData data =
        SeqThreadStatementData.of(
            SeqThreadStatementType.DEFAULT, pSubstituteEdge, pPcLeftHandSide, pTargetPc);
    return new SeqThreadStatement(
        data,
        finalizeExportStatements(
            data, ImmutableList.of(new CStatementWrapper(pReturnValueAssignment))));
  }

  public static SeqThreadStatement buildRwLockRdLockStatement(
      SeqThreadStatementData pData, RwLockNumReadersWritersFlag pRwLockFlags) {

    CStatementWrapper assumption =
        new CStatementWrapper(
            SeqAssumeFunction.buildAssumeFunctionCallStatement(pRwLockFlags.writerEqualsZero()));
    CStatementWrapper rwLockReadersIncrement =
        new CStatementWrapper(pRwLockFlags.readersIncrement());

    return new SeqThreadStatement(
        pData,
        finalizeExportStatements(pData, ImmutableList.of(assumption, rwLockReadersIncrement)));
  }

  public static SeqThreadStatement buildRwLockUnlockStatement(
      SeqThreadStatementData pData, RwLockNumReadersWritersFlag pRwLockFlags) {

    CExpressionAssignmentStatement setNumWritersToZero =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pRwLockFlags.writersIdExpression(), SeqIntegerLiteralExpressions.INT_0);
    CIfStatement ifStatement =
        new CIfStatement(
            new CExpressionWrapper(pRwLockFlags.writerEqualsZero()),
            new CCompoundStatement(new CStatementWrapper(pRwLockFlags.readersDecrement())),
            new CCompoundStatement(new CStatementWrapper(setNumWritersToZero)));

    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, ImmutableList.of(ifStatement)));
  }

  public static SeqThreadStatement buildRwLockWrLockStatement(
      SeqThreadStatementData pData, RwLockNumReadersWritersFlag pRwLockFlags) {

    CExpressionAssignmentStatement setWritersToOne =
        SeqStatementBuilder.buildExpressionAssignmentStatement(
            pRwLockFlags.writersIdExpression(), SeqIntegerLiteralExpressions.INT_1);

    CFunctionCallStatement assumptionWriters =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(pRwLockFlags.writerEqualsZero());
    CFunctionCallStatement assumptionReaders =
        SeqAssumeFunction.buildAssumeFunctionCallStatement(pRwLockFlags.readersEqualsZero());

    return new SeqThreadStatement(
        pData,
        finalizeExportStatements(
            pData,
            ImmutableList.of(
                new CStatementWrapper(assumptionWriters),
                new CStatementWrapper(assumptionReaders),
                new CStatementWrapper(setWritersToOne))));
  }

  public static SeqThreadStatement buildThreadCreationStatement(
      SeqThreadStatementData pData,
      // The assignment of the parameter given in the {@code pthread_create} call. This is present
      // if the start_routine has exactly one parameter, even if the parameter is not used.
      Optional<FunctionParameterAssignment> pStartRoutineArgAssignment,
      CLeftHandSide pCreatedThreadPc,
      Optional<ImmutableList<SeqBitVectorAssignmentStatement>> pBitVectorInitializations) {

    ImmutableList.Builder<CExportStatement> exportStatements = ImmutableList.builder();
    if (pStartRoutineArgAssignment.isPresent()) {
      exportStatements.add(
          new CStatementWrapper(
              pStartRoutineArgAssignment.orElseThrow().toExpressionAssignmentStatement()));
    }
    if (pBitVectorInitializations.isPresent()) {
      pBitVectorInitializations
          .orElseThrow()
          .forEach(i -> exportStatements.addAll(i.toCExportStatements()));
    }
    exportStatements.add(
        new CStatementWrapper(
            ProgramCounterVariables.buildPcAssignmentStatement(
                pCreatedThreadPc, ProgramCounterVariables.INIT_PC)));

    return new SeqThreadStatement(pData, finalizeExportStatements(pData, exportStatements.build()));
  }

  public static SeqThreadStatement buildThreadExitStatement(
      SeqThreadStatementData pData, FunctionReturnValueAssignment pReturnValueAssignment) {

    CStatementWrapper returnValueAssignment =
        new CStatementWrapper(pReturnValueAssignment.statement());
    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, ImmutableList.of(returnValueAssignment)));
  }

  // Thread Join Functions

  public static SeqThreadStatement buildThreadJoinStatement(
      SeqThreadStatementData pData,
      Optional<CIdExpression> pJoinedThreadExitVariable,
      CBinaryExpression pJoinedThreadNotActive)
      throws UnrecognizedCodeException {

    CStatementWrapper assumeCall =
        new CStatementWrapper(
            SeqAssumeFunction.buildAssumeFunctionCallStatement(pJoinedThreadNotActive));
    if (pJoinedThreadExitVariable.isPresent()) {
      CStatementWrapper returnValueRead =
          new CStatementWrapper(
              buildReturnValueRead(
                  pJoinedThreadExitVariable.orElseThrow(), pData.substituteEdges()));
      return new SeqThreadStatement(
          pData, finalizeExportStatements(pData, ImmutableList.of(assumeCall, returnValueRead)));
    }
    return new SeqThreadStatement(
        pData, finalizeExportStatements(pData, ImmutableList.of(assumeCall)));
  }

  private static CStatement buildReturnValueRead(
      CIdExpression pJoinedThreadExitVariable, ImmutableSet<SubstituteEdge> pSubstituteEdges)
      throws UnrecognizedCodeException {

    // only a single SubstituteEdge should be linked to a pthread_join statement
    SubstituteEdge substituteEdge = checkNotNull(Iterables.getOnlyElement(pSubstituteEdges));
    int returnValueIndex =
        PthreadFunctionType.PTHREAD_JOIN.getParameterIndex(PthreadObjectType.RETURN_VALUE);
    CFunctionCall functionCall =
        PthreadUtil.tryGetFunctionCallFromCfaEdge(substituteEdge.cfaEdge).orElseThrow();
    CExpression returnValueParameter =
        functionCall.getFunctionCallExpression().getParameterExpressions().get(returnValueIndex);
    if (returnValueParameter instanceof CUnaryExpression unaryExpression) {
      // extract retval from unary expression &retval
      if (unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
        if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
          return SeqStatementBuilder.buildExpressionAssignmentStatement(
              idExpression, pJoinedThreadExitVariable);
        }
      }
    }
    throw new UnrecognizedCodeException(
        "pthread_join retval could not be extracted from the following expression: "
            + returnValueParameter,
        substituteEdge.cfaEdge);
  }
}
