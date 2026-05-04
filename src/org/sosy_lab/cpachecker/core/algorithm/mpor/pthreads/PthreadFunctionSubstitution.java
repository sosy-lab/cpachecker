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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType.PthreadObjectSubstitutions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.SeqThreadStatementType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportFunctionDefinition;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CVariableDeclarationWrapper;

public class PthreadFunctionSubstitution {

  public static CFunctionCallExpression buildFunctionCallExpression(
      ImmutableList<CExpression> pParameterExpressions, PthreadFunctionType pFunctionType) {

    return switch (pFunctionType) {
      case PTHREAD_COND_SIGNAL ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              COND_SIGNAL_FUNCTION_TYPE,
              COND_SIGNAL_ID_EXPRESSION,
              pParameterExpressions,
              COND_SIGNAL_FUNCTION_DECLARATION);
      case PTHREAD_COND_WAIT ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              COND_WAIT_FUNCTION_TYPE,
              COND_WAIT_ID_EXPRESSION,
              pParameterExpressions,
              COND_WAIT_FUNCTION_DECLARATION);
      case PTHREAD_MUTEX_LOCK ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              MUTEX_FUNCTION_TYPE,
              MUTEX_LOCK_ID_EXPRESSION,
              pParameterExpressions,
              MUTEX_LOCK_FUNCTION_DECLARATION);
      case PTHREAD_MUTEX_UNLOCK ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              MUTEX_FUNCTION_TYPE,
              MUTEX_UNLOCK_ID_EXPRESSION,
              pParameterExpressions,
              MUTEX_UNLOCK_FUNCTION_DECLARATION);
      case PTHREAD_RWLOCK_RDLOCK ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              RWLOCK_FUNCTION_TYPE,
              RWLOCK_RDLOCK_ID_EXPRESSION,
              pParameterExpressions,
              RWLOCK_RDLOCK_FUNCTION_DECLARATION);
      case PTHREAD_RWLOCK_UNLOCK ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              RWLOCK_FUNCTION_TYPE,
              RWLOCK_UNLOCK_ID_EXPRESSION,
              pParameterExpressions,
              RWLOCK_UNLOCK_FUNCTION_DECLARATION);
      case PTHREAD_RWLOCK_WRLOCK ->
          new CFunctionCallExpression(
              FileLocation.DUMMY,
              RWLOCK_FUNCTION_TYPE,
              RWLOCK_WRLOCK_ID_EXPRESSION,
              pParameterExpressions,
              RWLOCK_WRLOCK_FUNCTION_DECLARATION);
      default ->
          throw new IllegalArgumentException(
              "Cannot build CFunctionCallExpression for the following pFunctionType: "
                  + pFunctionType);
    };
  }

  public static ImmutableList<CCompoundStatementElement> buildInlinedFunctionCallStatements(
      ImmutableList<CExpression> pParameterExpressions,
      PthreadFunctionType pFunctionType,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList<CIdExpression> idExpressions =
        getIdExpressionsFromExpressions(pParameterExpressions);

    return switch (pFunctionType) {
      case PTHREAD_COND_SIGNAL ->
          buildCondSignalFunctionStatements(Iterables.getOnlyElement(idExpressions));
      case PTHREAD_COND_WAIT -> {
        checkArgument(idExpressions.size() == 2);
        yield buildCondWaitFunctionStatements(idExpressions.getFirst(), idExpressions.getLast());
      }
      case PTHREAD_MUTEX_LOCK -> buildMutexLockAssignment(Iterables.getOnlyElement(idExpressions));
      case PTHREAD_MUTEX_UNLOCK ->
          ImmutableList.of(buildMutexUnlockAssignment(Iterables.getOnlyElement(idExpressions)));
      case PTHREAD_RWLOCK_RDLOCK ->
          buildRwLockRdLockFunctionStatements(
              Iterables.getOnlyElement(idExpressions), pBinaryExpressionBuilder);
      case PTHREAD_RWLOCK_UNLOCK ->
          buildRwLockUnlockFunctionStatements(
              Iterables.getOnlyElement(idExpressions), pBinaryExpressionBuilder);
      case PTHREAD_RWLOCK_WRLOCK ->
          buildRwLockWrLockFunctionStatements(Iterables.getOnlyElement(idExpressions));
      default ->
          throw new IllegalArgumentException(
              "Cannot build inlined function call statements for the following pFunctionType: "
                  + pFunctionType);
    };
  }

  private static ImmutableList<CIdExpression> getIdExpressionsFromExpressions(
      ImmutableList<CExpression> pExpressions) {

    ImmutableList.Builder<CIdExpression> rIdExpressions = ImmutableList.builder();
    for (CExpression expression : pExpressions) {
      CSimpleDeclaration declaration =
          Iterables.getOnlyElement(SeqPointerAliasingUtil.getNestedSimpleDeclarations(expression));
      CVariableDeclaration variableDeclaration = MPORUtil.convertToVariableDeclaration(declaration);
      rIdExpressions.add(new CIdExpression(FileLocation.DUMMY, variableDeclaration));
    }
    return rIdExpressions.build();
  }

  public static Optional<CExportFunctionDefinition> tryGetFunctionDefinitionByStatementType(
      SeqThreadStatementType pType, CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    return switch (pType) {
      case COND_SIGNAL -> Optional.of(COND_SIGNAL_FUNCTION_DEFINITION);
      case COND_WAIT -> Optional.of(COND_WAIT_FUNCTION_DEFINITION);
      case MUTEX_LOCK -> Optional.of(MUTEX_LOCK_FUNCTION_DEFINITION);
      case MUTEX_UNLOCK -> Optional.of(MUTEX_UNLOCK_FUNCTION_DEFINITION);
      case RW_LOCK_RD_LOCK ->
          Optional.of(
              new CExportFunctionDefinition(
                  RWLOCK_RDLOCK_FUNCTION_DECLARATION,
                  new CCompoundStatement(
                      buildRwLockRdLockFunctionStatements(
                          RWLOCK_ID_EXPRESSION, pBinaryExpressionBuilder))));
      case RW_LOCK_UNLOCK ->
          Optional.of(
              new CExportFunctionDefinition(
                  RWLOCK_UNLOCK_FUNCTION_DECLARATION,
                  new CCompoundStatement(
                      buildRwLockUnlockFunctionStatements(
                          RWLOCK_ID_EXPRESSION, pBinaryExpressionBuilder))));
      case RW_LOCK_WR_LOCK -> Optional.of(RWLOCK_WRLOCK_FUNCTION_DEFINITION);
      default -> Optional.empty();
    };
  }

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

  // pthread_mutex_t

  private static final CParameterDeclaration MUTEX_PARAMETER =
      new CParameterDeclaration(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, PthreadObjectSubstitutions.MUTEX_ELABORATED_TYPE),
          "mutex");

  // the same function type can be used for both lock and unlock, because the only parameter is a
  // pointer to the mutex object
  private static final CFunctionTypeWithNames MUTEX_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(MUTEX_PARAMETER), false);

  private static final CFunctionDeclaration MUTEX_LOCK_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          MUTEX_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_MUTEX_LOCK.name,
          ImmutableList.of(MUTEX_PARAMETER),
          ImmutableSet.of());

  private static final CFunctionDeclaration MUTEX_UNLOCK_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          MUTEX_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_MUTEX_UNLOCK.name,
          ImmutableList.of(MUTEX_PARAMETER),
          ImmutableSet.of());

  private static final CIdExpression MUTEX_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, MUTEX_PARAMETER);

  private static final CIdExpression MUTEX_LOCK_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, MUTEX_LOCK_FUNCTION_DECLARATION);

  private static final CIdExpression MUTEX_UNLOCK_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, MUTEX_UNLOCK_FUNCTION_DECLARATION);

  public static final CExportFunctionDefinition MUTEX_LOCK_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          MUTEX_LOCK_FUNCTION_DECLARATION,
          new CCompoundStatement(buildMutexLockAssignment(MUTEX_ID_EXPRESSION)));

  public static final CExportFunctionDefinition MUTEX_UNLOCK_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          MUTEX_UNLOCK_FUNCTION_DECLARATION,
          new CCompoundStatement(buildMutexUnlockAssignment(MUTEX_ID_EXPRESSION)));

  private static CFieldReference buildMutexFieldReference(CIdExpression pMutexExpression) {

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

  private static ImmutableList<CCompoundStatementElement> buildMutexLockAssignment(
      CIdExpression pMutexExpression) {

    CFieldReference mutexFieldReference = buildMutexFieldReference(pMutexExpression);
    CCompoundStatementElement mutexLockAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, mutexFieldReference, CIntegerLiteralExpression.ONE));
    CCompoundStatementElement mutexLockAssumption =
        SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
            new CLogicalNotExpression(new CExpressionWrapper(mutexFieldReference)));
    return ImmutableList.of(mutexLockAssumption, mutexLockAssignment);
  }

  private static CCompoundStatementElement buildMutexUnlockAssignment(
      CIdExpression pMutexExpression) {

    CFieldReference mutexFieldReference = buildMutexFieldReference(pMutexExpression);
    return new CStatementWrapper(
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY, mutexFieldReference, CIntegerLiteralExpression.ZERO));
  }

  // pthread_cond_t

  private static final CParameterDeclaration COND_PARAMETER =
      new CParameterDeclaration(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, PthreadObjectSubstitutions.COND_ELABORATED_TYPE),
          "cond");

  private static final CFunctionTypeWithNames COND_SIGNAL_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(COND_PARAMETER), false);

  private static final CFunctionTypeWithNames COND_WAIT_FUNCTION_TYPE =
      new CFunctionTypeWithNames(
          CVoidType.VOID, ImmutableList.of(COND_PARAMETER, MUTEX_PARAMETER), false);

  private static final CFunctionDeclaration COND_SIGNAL_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          COND_SIGNAL_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_COND_SIGNAL.name,
          ImmutableList.of(COND_PARAMETER),
          ImmutableSet.of());

  private static final CFunctionDeclaration COND_WAIT_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          COND_WAIT_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_COND_WAIT.name,
          ImmutableList.of(COND_PARAMETER, MUTEX_PARAMETER),
          ImmutableSet.of());

  private static final CIdExpression COND_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, COND_PARAMETER);

  private static final CIdExpression COND_SIGNAL_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, COND_SIGNAL_FUNCTION_DECLARATION);

  private static final CIdExpression COND_WAIT_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, COND_WAIT_FUNCTION_DECLARATION);

  public static final CExportFunctionDefinition COND_SIGNAL_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          COND_SIGNAL_FUNCTION_DECLARATION,
          new CCompoundStatement(buildCondSignalFunctionStatements(COND_ID_EXPRESSION)));

  // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait
  public static final CExportFunctionDefinition COND_WAIT_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          COND_WAIT_FUNCTION_DECLARATION,
          new CCompoundStatement(
              buildCondWaitFunctionStatements(COND_ID_EXPRESSION, MUTEX_ID_EXPRESSION)));

  private static ImmutableList<CCompoundStatementElement> buildCondSignalFunctionStatements(
      CIdExpression pCondExpression) {

    CFieldReference condFieldReference = buildCondFieldReference(pCondExpression);
    CExportStatement condAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, condFieldReference, CIntegerLiteralExpression.ZERO));
    return ImmutableList.of(condAssignment);
  }

  private static ImmutableList<CCompoundStatementElement> buildCondWaitFunctionStatements(
      CIdExpression pCondExpression, CIdExpression pMutexExpression) {

    CFieldReference condFieldReference = buildCondFieldReference(pCondExpression);
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
        buildMutexUnlockAssignment(pMutexExpression));
  }

  private static CFieldReference buildCondFieldReference(CIdExpression pCondExpression) {
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

  private static final CParameterDeclaration RWLOCK_PARAMETER =
      new CParameterDeclaration(
          FileLocation.DUMMY,
          new CPointerType(CTypeQualifiers.NONE, PthreadObjectSubstitutions.RWLOCK_ELABORATED_TYPE),
          "rwlock");

  // the same function type can be used for rdlock, unlock and wrlock, because the only parameter is
  // a pointer to the rwlock object
  private static final CFunctionTypeWithNames RWLOCK_FUNCTION_TYPE =
      new CFunctionTypeWithNames(CVoidType.VOID, ImmutableList.of(RWLOCK_PARAMETER), false);

  private static final CFunctionDeclaration RWLOCK_RDLOCK_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          RWLOCK_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_RWLOCK_RDLOCK.name,
          ImmutableList.of(RWLOCK_PARAMETER),
          ImmutableSet.of());

  private static final CFunctionDeclaration RWLOCK_UNLOCK_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          RWLOCK_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_RWLOCK_UNLOCK.name,
          ImmutableList.of(RWLOCK_PARAMETER),
          ImmutableSet.of());

  private static final CFunctionDeclaration RWLOCK_WRLOCK_FUNCTION_DECLARATION =
      new CFunctionDeclaration(
          FileLocation.DUMMY,
          RWLOCK_FUNCTION_TYPE,
          Sequentialization.MPOR_PREFIX + PthreadFunctionType.PTHREAD_RWLOCK_WRLOCK.name,
          ImmutableList.of(RWLOCK_PARAMETER),
          ImmutableSet.of());

  private static final CIdExpression RWLOCK_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, RWLOCK_PARAMETER);

  private static final CIdExpression RWLOCK_RDLOCK_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, RWLOCK_RDLOCK_FUNCTION_DECLARATION);

  private static final CIdExpression RWLOCK_UNLOCK_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, RWLOCK_UNLOCK_FUNCTION_DECLARATION);

  private static final CIdExpression RWLOCK_WRLOCK_ID_EXPRESSION =
      new CIdExpression(FileLocation.DUMMY, RWLOCK_WRLOCK_FUNCTION_DECLARATION);

  private static final CExportFunctionDefinition RWLOCK_WRLOCK_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          RWLOCK_WRLOCK_FUNCTION_DECLARATION,
          new CCompoundStatement(buildRwLockWrLockFunctionStatements(RWLOCK_ID_EXPRESSION)));

  private static ImmutableList<CCompoundStatementElement> buildRwLockRdLockFunctionStatements(
      CIdExpression pRwLockExpression, CBinaryExpressionBuilder pBinaryExpressionBuilder)
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

  private static ImmutableList<CCompoundStatementElement> buildRwLockUnlockFunctionStatements(
      CIdExpression pRwLockExpression, CBinaryExpressionBuilder pBinaryExpressionBuilder)
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

  private static ImmutableList<CCompoundStatementElement> buildRwLockWrLockFunctionStatements(
      CIdExpression pRwLockExpression) {

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
      CIdExpression pRwLockExpression, CCompositeTypeMemberDeclaration pMemberDeclaration) {

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
}
