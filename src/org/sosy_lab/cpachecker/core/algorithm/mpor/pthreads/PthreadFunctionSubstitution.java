// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType.PthreadObjectSubstitutions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CVariableDeclarationWrapper;

public class PthreadFunctionSubstitution {

  // pthread_barrier_t

  public static ImmutableList<CCompoundStatementElement> buildBarrierInitFunctionStatements(
      ImmutableList<CExpression> pExpressions) {

    CExpression barrierExpression =
        pExpressions.get(
            PthreadFunctionType.PTHREAD_BARRIER_INIT.getParameterIndex(
                PthreadObjectType.PTHREAD_BARRIER_T));
    CExpression countExpression =
        pExpressions.get(
            PthreadFunctionType.PTHREAD_BARRIER_INIT.getParameterIndex(PthreadObjectType.COUNT));

    CFieldReference barrierCountFieldReference =
        buildBarrierFieldReference(
            barrierExpression, PthreadObjectSubstitutions.BARRIER_COUNT_MEMBER_DECLARATION);
    return ImmutableList.of(
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, barrierCountFieldReference, countExpression)));
  }

  public static ImmutableList<CCompoundStatementElement> buildBarrierIncrementFunctionStatements(
      CExpression pBarrierExpression, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFieldReference barrierNumWaitersFieldReference =
        buildBarrierFieldReference(
            pBarrierExpression, PthreadObjectSubstitutions.BARRIER_NUM_WAITERS_MEMBER_DECLARATION);
    return buildIncrementOrDecrementFromFieldReference(
        barrierNumWaitersFieldReference, pBinaryExpressionBuilder, BinaryOperator.PLUS);
  }

  public static ImmutableList<CCompoundStatementElement> buildBarrierAssumptionFunctionStatements(
      CExpression pBarrierExpression, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFieldReference barrierCountFieldReference =
        buildBarrierFieldReference(
            pBarrierExpression, PthreadObjectSubstitutions.BARRIER_COUNT_MEMBER_DECLARATION);
    CFieldReference barrierNumWaitersFieldReference =
        buildBarrierFieldReference(
            pBarrierExpression, PthreadObjectSubstitutions.BARRIER_NUM_WAITERS_MEMBER_DECLARATION);

    // Equality checks on composite types are not allowed, e.g., "(A.member) == (B.member)"
    // so instead local variables are declared that are primitive and hold the values.
    // The declarations are inside compound statements, so they do not collide with other names.
    CVariableDeclaration barrierCountDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            PthreadObjectSubstitutions.BARRIER_COUNT_MEMBER_DECLARATION.getType(),
            PthreadObjectSubstitutions.BARRIER_COUNT_MEMBER_DECLARATION.getName(),
            PthreadObjectSubstitutions.BARRIER_COUNT_MEMBER_DECLARATION.getName(),
            PthreadObjectSubstitutions.BARRIER_COUNT_MEMBER_DECLARATION.getName(),
            new CInitializerExpression(FileLocation.DUMMY, barrierCountFieldReference));
    CVariableDeclaration barrierNumWaitersDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            PthreadObjectSubstitutions.BARRIER_NUM_WAITERS_MEMBER_DECLARATION.getType(),
            PthreadObjectSubstitutions.BARRIER_NUM_WAITERS_MEMBER_DECLARATION.getName(),
            PthreadObjectSubstitutions.BARRIER_NUM_WAITERS_MEMBER_DECLARATION.getName(),
            PthreadObjectSubstitutions.BARRIER_NUM_WAITERS_MEMBER_DECLARATION.getName(),
            new CInitializerExpression(FileLocation.DUMMY, barrierNumWaitersFieldReference));

    CBinaryExpression equalsExpression =
        pBinaryExpressionBuilder.buildBinaryExpression(
            new CIdExpression(FileLocation.DUMMY, barrierCountDeclaration),
            new CIdExpression(FileLocation.DUMMY, barrierNumWaitersDeclaration),
            BinaryOperator.EQUALS);
    CCompoundStatementElement countEqualsNumWaitersAssumption =
        new CStatementWrapper(
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(equalsExpression));

    return ImmutableList.of(
        new CVariableDeclarationWrapper(barrierCountDeclaration),
        new CVariableDeclarationWrapper(barrierNumWaitersDeclaration),
        countEqualsNumWaitersAssumption);
  }

  private static CFieldReference buildBarrierFieldReference(
      CExpression pBarrierExpression, CCompositeTypeMemberDeclaration pMemberDeclaration) {
    return new CFieldReference(
        FileLocation.DUMMY,
        PthreadObjectSubstitutions.BARRIER_ELABORATED_TYPE,
        pMemberDeclaration.getName(),
        pBarrierExpression,
        pBarrierExpression.getExpressionType().getCanonicalType() instanceof CPointerType);
  }

  // pthread_mutex_t

  public static ImmutableList<CCompoundStatementElement> buildMutexLockFunctionStatements(
      CExpression pMutexExpression) {

    CFieldReference mutexFieldReference = buildMutexFieldReference(pMutexExpression);
    CCompoundStatementElement mutexLockAssumption =
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            new CLogicalNotExpression(new CExpressionWrapper(mutexFieldReference)));
    return ImmutableList.of(mutexLockAssumption, buildMutexLockAssignment(pMutexExpression));
  }

  private static CCompoundStatementElement buildMutexLockAssignment(CExpression pMutexExpression) {
    CFieldReference mutexFieldReference = buildMutexFieldReference(pMutexExpression);
    return new CStatementWrapper(
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, mutexFieldReference, CIntegerLiteralExpression.ONE));
  }

  public static ImmutableList<CCompoundStatementElement> buildMutexUnlockFunctionStatements(
      CExpression pMutexExpression) {

    CFieldReference mutexFieldReference = buildMutexFieldReference(pMutexExpression);
    return ImmutableList.of(
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, mutexFieldReference, CIntegerLiteralExpression.ZERO)));
  }

  private static CFieldReference buildMutexFieldReference(CExpression pMutexExpression) {
    CFieldReference mutexFieldReference =
        new CFieldReference(
            FileLocation.DUMMY,
            PthreadObjectSubstitutions.MUTEX_INNER_LIST_ELABORATED_TYPE,
            PthreadObjectSubstitutions.MUTEX_INNER_LIST_MEMBER_DECLARATION.getName(),
            pMutexExpression,
            pMutexExpression.getExpressionType().getCanonicalType() instanceof CPointerType);
    return new CFieldReference(
        FileLocation.DUMMY,
        PthreadObjectSubstitutions.MUTEX_INNER_LIST_ELABORATED_TYPE,
        PthreadObjectSubstitutions.MUTEX_LOCKED_MEMBER_DECLARATION.getName(),
        mutexFieldReference,
        false);
  }

  // pthread_cond_t

  public static ImmutableList<CCompoundStatementElement> buildCondSignalFunctionStatements(
      CExpression pCondExpression) {

    CFieldReference condFieldReference = buildCondFieldReference(pCondExpression);
    CExportStatement condAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, condFieldReference, CIntegerLiteralExpression.ONE));
    return ImmutableList.of(condAssignment);
  }

  public static ImmutableList<CCompoundStatementElement> buildCondWaitUnlockFunctionStatements(
      ImmutableList<CExpression> pExpressions) {

    checkArgument(pExpressions.size() == 2);
    CExpression mutexExpression =
        pExpressions.get(
            PthreadFunctionType.PTHREAD_COND_WAIT.getParameterIndex(
                PthreadObjectType.PTHREAD_MUTEX_T));
    return buildMutexUnlockFunctionStatements(mutexExpression);
  }

  public static ImmutableList<CCompoundStatementElement> buildCondWaitLockFunctionStatements(
      ImmutableList<CExpression> pExpressions) {

    checkArgument(pExpressions.size() == 2);

    CExpression condExpression =
        pExpressions.get(
            PthreadFunctionType.PTHREAD_COND_WAIT.getParameterIndex(
                PthreadObjectType.PTHREAD_COND_T));
    CExpression mutexExpression =
        pExpressions.get(
            PthreadFunctionType.PTHREAD_COND_WAIT.getParameterIndex(
                PthreadObjectType.PTHREAD_MUTEX_T));

    CFieldReference condFieldReference = buildCondFieldReference(condExpression);
    CExportStatement condAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, condFieldReference, CIntegerLiteralExpression.ZERO));
    // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait
    return ImmutableList.of(
        // the calling thread blocks on the condition variable -> assume(signaled == 1)
        new CStatementWrapper(
            SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(condFieldReference)),
        condAssignment,
        // on return, the mutex is locked and owned by the calling thread
        buildMutexLockAssignment(mutexExpression));
  }

  private static CFieldReference buildCondFieldReference(CExpression pCondExpression) {
    CFieldReference condFieldReference =
        new CFieldReference(
            FileLocation.DUMMY,
            PthreadObjectSubstitutions.COND_INNER_LIST_ELABORATED_TYPE,
            PthreadObjectSubstitutions.COND_INNER_LIST_MEMBER_DECLARATION.getName(),
            pCondExpression,
            pCondExpression.getExpressionType().getCanonicalType() instanceof CPointerType);
    return new CFieldReference(
        FileLocation.DUMMY,
        PthreadObjectSubstitutions.COND_INNER_LIST_ELABORATED_TYPE,
        PthreadObjectSubstitutions.COND_SIGNALED_MEMBER_DECLARATION.getName(),
        condFieldReference,
        false);
  }

  // pthread_rwlock_t

  public static ImmutableList<CCompoundStatementElement> buildRwLockRdLockFunctionStatements(
      CExpression pRwLockExpression, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFieldReference numWritersFieldReference =
        buildRwLockFieldReference(
            pRwLockExpression, PthreadObjectSubstitutions.RWLOCK_NUM_WRITERS_MEMBER_DECLARATION);
    CExportStatement rwLockNumWritersAssumption =
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            new CLogicalNotExpression(new CExpressionWrapper(numWritersFieldReference)));

    return elementAndList(
        rwLockNumWritersAssumption,
        buildIncrementOrDecrementFromFieldReference(
            numWritersFieldReference, pBinaryExpressionBuilder, BinaryOperator.PLUS));
  }

  public static ImmutableList<CCompoundStatementElement> buildRwLockUnlockFunctionStatements(
      CExpression pRwLockExpression, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    CFieldReference numReadersFieldReference =
        buildRwLockFieldReference(
            pRwLockExpression, PthreadObjectSubstitutions.RWLOCK_NUM_READERS_MEMBER_DECLARATION);
    CFieldReference numWritersFieldReference =
        buildRwLockFieldReference(
            pRwLockExpression, PthreadObjectSubstitutions.RWLOCK_NUM_WRITERS_MEMBER_DECLARATION);
    CCompoundStatementElement rwLockUnlockAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, numWritersFieldReference, CIntegerLiteralExpression.ZERO));

    // if NUM_WRITERS is 1, then set NUM_WRITERS to 0 (= unlock the write lock)
    // if NUM_WRITERS is 0, then decrement NUM_READERS (= unlock the read lock)
    CIfStatement ifStatement =
        new CIfStatement(
            new CExpressionWrapper(numWritersFieldReference),
            new CCompoundStatement(rwLockUnlockAssignment),
            new CCompoundStatement(
                buildIncrementOrDecrementFromFieldReference(
                    numReadersFieldReference, pBinaryExpressionBuilder, BinaryOperator.MINUS)));
    return ImmutableList.of(ifStatement);
  }

  public static ImmutableList<CCompoundStatementElement> buildRwLockWrLockFunctionStatements(
      CExpression pRwLockExpression) {

    CFieldReference numReadersFieldReference =
        buildRwLockFieldReference(
            pRwLockExpression, PthreadObjectSubstitutions.RWLOCK_NUM_READERS_MEMBER_DECLARATION);
    CFieldReference numWritersFieldReference =
        buildRwLockFieldReference(
            pRwLockExpression, PthreadObjectSubstitutions.RWLOCK_NUM_WRITERS_MEMBER_DECLARATION);

    CCompoundStatementElement rwLockWrLockAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, numWritersFieldReference, CIntegerLiteralExpression.ONE));

    return ImmutableList.of(
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            new CLogicalNotExpression(new CExpressionWrapper(numWritersFieldReference))),
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            new CLogicalNotExpression(new CExpressionWrapper(numReadersFieldReference))),
        rwLockWrLockAssignment);
  }

  private static CFieldReference buildRwLockFieldReference(
      CExpression pRwLockExpression, CCompositeTypeMemberDeclaration pMemberDeclaration) {

    CFieldReference rwLockFieldReference =
        new CFieldReference(
            FileLocation.DUMMY,
            PthreadObjectSubstitutions.RWLOCK_INNER_LIST_ELABORATED_TYPE,
            PthreadObjectSubstitutions.RWLOCK_INNER_LIST_MEMBER_DECLARATION.getName(),
            pRwLockExpression,
            pRwLockExpression.getExpressionType().getCanonicalType() instanceof CPointerType);
    return new CFieldReference(
        FileLocation.DUMMY,
        PthreadObjectSubstitutions.RWLOCK_INNER_LIST_ELABORATED_TYPE,
        pMemberDeclaration.getName(),
        rwLockFieldReference,
        false);
  }

  // Helper

  private static ImmutableList<CCompoundStatementElement>
      buildIncrementOrDecrementFromFieldReference(
          CFieldReference pFieldReference,
          CBinaryExpressionBuilder pBinaryExpressionBuilder,
          BinaryOperator pBinaryOperator)
          throws UnrecognizedCodeException {

    checkArgument(
        pBinaryOperator.equals(BinaryOperator.PLUS)
            || pBinaryOperator.equals(BinaryOperator.MINUS));

    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            // use unsigned int by default, if the RHS is an unsigned char it is converted anyway
            CNumericTypes.UNSIGNED_INT,
            pFieldReference.getFieldName(),
            pFieldReference.getFieldName(),
            pFieldReference.getFieldName(),
            new CInitializerExpression(FileLocation.DUMMY, pFieldReference));
    CIdExpression idExpression =
        new CIdExpression(
            FileLocation.DUMMY,
            variableDeclaration.getType(),
            variableDeclaration.getName(),
            variableDeclaration);

    CExpressionAssignmentStatement incrementOrDecrementStatement =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            pFieldReference,
            pBinaryExpressionBuilder.buildBinaryExpression(
                idExpression, CIntegerLiteralExpression.ONE, pBinaryOperator));

    return ImmutableList.of(
        new CVariableDeclarationWrapper(variableDeclaration),
        new CStatementWrapper(incrementOrDecrementStatement));
  }
}
