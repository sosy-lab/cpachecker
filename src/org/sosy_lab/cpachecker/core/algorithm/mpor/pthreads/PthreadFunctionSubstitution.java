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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType.PthreadObjectSubstitutions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
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

  public static ImmutableList<CExportFunctionDefinition> getAllFunctionDefinitions(
      CBinaryExpressionBuilder pBinaryExpressionBuilder) throws UnrecognizedCodeException {

    ImmutableList.Builder<CExportFunctionDefinition> rDefinitions = ImmutableList.builder();

    rDefinitions.add(COND_SIGNAL_FUNCTION_DEFINITION);
    rDefinitions.add(COND_WAIT_FUNCTION_DEFINITION);

    rDefinitions.add(MUTEX_LOCK_FUNCTION_DEFINITION);
    rDefinitions.add(MUTEX_UNLOCK_FUNCTION_DEFINITION);

    CExportFunctionDefinition rwlockRdlockFunctionDefinition =
        new CExportFunctionDefinition(
            RWLOCK_RDLOCK_FUNCTION_DECLARATION,
            new CCompoundStatement(
                elementAndList(
                    RWLOCK_NUM_WRITERS_ASSUMPTION,
                    buildIncrementOrDecrementFromFieldReference(
                        RWLOCK_NUM_READERS_FIELD_REFERENCE,
                        pBinaryExpressionBuilder,
                        BinaryOperator.PLUS))));

    // if NUM_WRITERS is 1, then set NUM_WRITERS to 0 (= unlock the write lock)
    // if NUM_WRITERS is 0, then decrement NUM_READERS (= unlock the read lock)
    CIfStatement ifStatement =
        new CIfStatement(
            new CExpressionWrapper(RWLOCK_NUM_WRITERS_FIELD_REFERENCE),
            new CCompoundStatement(RWLOCK_UNLOCK_ASSIGNMENT),
            new CCompoundStatement(
                buildIncrementOrDecrementFromFieldReference(
                    RWLOCK_NUM_READERS_FIELD_REFERENCE,
                    pBinaryExpressionBuilder,
                    BinaryOperator.MINUS)));
    CExportFunctionDefinition rwlockUnlockFunctionDefinition =
        new CExportFunctionDefinition(
            RWLOCK_UNLOCK_FUNCTION_DECLARATION, new CCompoundStatement(ifStatement));

    rDefinitions.add(rwlockRdlockFunctionDefinition);
    rDefinitions.add(rwlockUnlockFunctionDefinition);
    rDefinitions.add(RWLOCK_WRLOCK_FUNCTION_DEFINITION);

    return rDefinitions.build();
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
            idExpression,
            pBinaryExpressionBuilder.buildBinaryExpression(
                idExpression, CIntegerLiteralExpression.ONE, pBinaryOperator));

    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pFieldReference, idExpression);

    return ImmutableList.of(
        new CVariableDeclarationWrapper(variableDeclaration),
        new CStatementWrapper(incrementOrDecrementStatement),
        new CStatementWrapper(assignmentStatement));
  }

  private static final String POINTER_SUFFIX = "_pointer";

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

  private static final CFieldReference MUTEX_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          PthreadObjectSubstitutions.MUTEX_ELABORATED_TYPE,
          PthreadObjectSubstitutions.MUTEX_INNER_LIST_MEMBER_DECLARATION.getName(),
          MUTEX_ID_EXPRESSION,
          true);

  private static final CPointerType MUTEX_INNER_POINTER_COMPOSITE_TYPE =
      new CPointerType(
          CTypeQualifiers.NONE, PthreadObjectSubstitutions.MUTEX_INNER_LIST_ELABORATED_TYPE);

  private static final CVariableDeclaration MUTEX_INNER_LIST_POINTER_DECLARATION =
      new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          MUTEX_INNER_POINTER_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.INNER_LIST_NAME + POINTER_SUFFIX,
          PthreadObjectSubstitutions.INNER_LIST_NAME + POINTER_SUFFIX,
          PthreadObjectSubstitutions.INNER_LIST_NAME + POINTER_SUFFIX,
          new CInitializerExpression(
              FileLocation.DUMMY,
              new CUnaryExpression(
                  FileLocation.DUMMY,
                  MUTEX_INNER_POINTER_COMPOSITE_TYPE,
                  MUTEX_FIELD_REFERENCE,
                  UnaryOperator.AMPER)));

  private static final CFieldReference MUTEX_LOCKED_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          MUTEX_INNER_POINTER_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.MUTEX_LOCKED_MEMBER_DECLARATION.getName(),
          new CIdExpression(FileLocation.DUMMY, MUTEX_INNER_LIST_POINTER_DECLARATION),
          true);

  private static final CCompoundStatementElement MUTEX_LOCK_ASSIGNMENT =
      new CStatementWrapper(
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY, MUTEX_LOCKED_FIELD_REFERENCE, CIntegerLiteralExpression.ONE));

  private static final CCompoundStatementElement MUTEX_UNLOCK_ASSIGNMENT =
      new CStatementWrapper(
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY, MUTEX_LOCKED_FIELD_REFERENCE, CIntegerLiteralExpression.ZERO));

  public static final CExportFunctionDefinition MUTEX_LOCK_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          MUTEX_LOCK_FUNCTION_DECLARATION,
          new CCompoundStatement(
              new CVariableDeclarationWrapper(MUTEX_INNER_LIST_POINTER_DECLARATION),
              SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                  new CLogicalNotExpression(new CExpressionWrapper(MUTEX_LOCKED_FIELD_REFERENCE))),
              MUTEX_LOCK_ASSIGNMENT));

  public static final CExportFunctionDefinition MUTEX_UNLOCK_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          MUTEX_UNLOCK_FUNCTION_DECLARATION,
          new CCompoundStatement(
              new CVariableDeclarationWrapper(MUTEX_INNER_LIST_POINTER_DECLARATION),
              MUTEX_UNLOCK_ASSIGNMENT));

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

  private static final CFieldReference COND_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          PthreadObjectSubstitutions.COND_ELABORATED_TYPE,
          PthreadObjectSubstitutions.COND_MEMBER_DECLARATION.getName(),
          COND_ID_EXPRESSION,
          true);

  private static final CCompoundStatementElement COND_SIGNAL_ASSIGNMENT =
      new CStatementWrapper(
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY, COND_FIELD_REFERENCE, CIntegerLiteralExpression.ONE));

  private static final CCompoundStatementElement COND_WAIT_ASSIGNMENT =
      new CStatementWrapper(
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY, COND_FIELD_REFERENCE, CIntegerLiteralExpression.ZERO));

  public static final CExportFunctionDefinition COND_SIGNAL_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          COND_SIGNAL_FUNCTION_DECLARATION, new CCompoundStatement(COND_SIGNAL_ASSIGNMENT));

  // for a breakdown on this behavior, cf. https://linux.die.net/man/3/pthread_cond_wait
  public static final CExportFunctionDefinition COND_WAIT_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          COND_WAIT_FUNCTION_DECLARATION,
          new CCompoundStatement(
              // the calling thread blocks on the condition variable -> assume(signaled == 1)
              new CStatementWrapper(
                  SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(COND_FIELD_REFERENCE)),
              COND_WAIT_ASSIGNMENT,
              new CVariableDeclarationWrapper(MUTEX_INNER_LIST_POINTER_DECLARATION),
              // on return, the mutex is locked and owned by the calling thread
              MUTEX_LOCK_ASSIGNMENT));

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

  private static final CFieldReference RWLOCK_NUM_READERS_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          PthreadObjectSubstitutions.RWLOCK_ELABORATED_TYPE,
          PthreadObjectSubstitutions.RWLOCK_NUM_READERS_MEMBER_DECLARATION.getName(),
          RWLOCK_ID_EXPRESSION,
          true);

  private static final CFieldReference RWLOCK_NUM_WRITERS_FIELD_REFERENCE =
      new CFieldReference(
          FileLocation.DUMMY,
          PthreadObjectSubstitutions.RWLOCK_ELABORATED_TYPE,
          PthreadObjectSubstitutions.RWLOCK_NUM_WRITERS_MEMBER_DECLARATION.getName(),
          RWLOCK_ID_EXPRESSION,
          true);

  private static final CExportStatement RWLOCK_NUM_WRITERS_ASSUMPTION =
      SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
          new CLogicalNotExpression(new CExpressionWrapper(RWLOCK_NUM_WRITERS_FIELD_REFERENCE)));

  private static final CCompoundStatementElement RWLOCK_UNLOCK_ASSIGNMENT =
      new CStatementWrapper(
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              RWLOCK_NUM_WRITERS_FIELD_REFERENCE,
              CIntegerLiteralExpression.ZERO));

  private static final CCompoundStatementElement RWLOCK_WRLOCK_ASSIGNMENT =
      new CStatementWrapper(
          new CExpressionAssignmentStatement(
              FileLocation.DUMMY,
              RWLOCK_NUM_WRITERS_FIELD_REFERENCE,
              CIntegerLiteralExpression.ONE));

  public static final CExportFunctionDefinition RWLOCK_WRLOCK_FUNCTION_DEFINITION =
      new CExportFunctionDefinition(
          RWLOCK_WRLOCK_FUNCTION_DECLARATION,
          new CCompoundStatement(
              SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                  new CLogicalNotExpression(
                      new CExpressionWrapper(RWLOCK_NUM_WRITERS_FIELD_REFERENCE))),
              SeqAssumeFunctionBuilder.buildAssumeFunctionCallStatement(
                  new CLogicalNotExpression(
                      new CExpressionWrapper(RWLOCK_NUM_READERS_FIELD_REFERENCE))),
              RWLOCK_WRLOCK_ASSIGNMENT));
}
