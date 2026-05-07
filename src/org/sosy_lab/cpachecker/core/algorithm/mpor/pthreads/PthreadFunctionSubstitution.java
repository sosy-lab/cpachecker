// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.elementAndList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pointer_aliasing.SeqPointerAliasingUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType.PthreadObjectSubstitutions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.functions.SeqAssumeFunctionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CCompoundStatementElement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExportStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CExpressionWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CIfStatement;
import org.sosy_lab.cpachecker.util.cwriter.export.CLogicalNotExpression;
import org.sosy_lab.cpachecker.util.cwriter.export.CStatementWrapper;
import org.sosy_lab.cpachecker.util.cwriter.export.CVariableDeclarationWrapper;

public class PthreadFunctionSubstitution {

  public static ImmutableList<CCompoundStatementElement> buildInlinedFunctionStatements(
      CFAEdge pOriginalCfaEdge,
      ImmutableList<CExpression> pParameterExpressions,
      PthreadFunctionType pFunctionType,
      CBinaryExpressionBuilder pBinaryExpressionBuilder)
      throws UnrecognizedCodeException {

    ImmutableList<CExpression> expressions;
    if (isAnyPointer(pParameterExpressions)) {
      expressions = pParameterExpressions;
    } else {
      ImmutableList.Builder<CExpression> nonPointerExpressions = ImmutableList.builder();
      for (CExpression expression : pParameterExpressions) {
        if (expression instanceof CUnaryExpression unaryExpression
            && unaryExpression.getOperator().equals(UnaryOperator.AMPER)) {
          nonPointerExpressions.add(unaryExpression.getOperand());
        } else {
          throw new UnsupportedCodeException(
              "Expected CUnaryExpression with UnaryOperator.AMPER as parameter expression but got ",
              pOriginalCfaEdge);
        }
      }
      expressions = nonPointerExpressions.build();
    }

    return switch (pFunctionType) {
      case PTHREAD_COND_SIGNAL ->
          buildCondSignalFunctionStatements(Iterables.getOnlyElement(expressions));
      case PTHREAD_COND_WAIT -> buildCondWaitFunctionStatements(expressions);
      case PTHREAD_MUTEX_LOCK ->
          buildMutexLockFunctionStatements(Iterables.getOnlyElement(expressions));
      case PTHREAD_MUTEX_UNLOCK ->
          buildMutexUnlockFunctionStatements(Iterables.getOnlyElement(expressions));
      case PTHREAD_RWLOCK_RDLOCK ->
          buildRwLockRdLockFunctionStatements(
              Iterables.getOnlyElement(expressions), pBinaryExpressionBuilder);
      case PTHREAD_RWLOCK_UNLOCK ->
          buildRwLockUnlockFunctionStatements(
              Iterables.getOnlyElement(expressions), pBinaryExpressionBuilder);
      case PTHREAD_RWLOCK_WRLOCK ->
          buildRwLockWrLockFunctionStatements(Iterables.getOnlyElement(expressions));
      default ->
          throw new IllegalArgumentException(
              "Cannot build inlined function call statements for the following pFunctionType: "
                  + pFunctionType);
    };
  }

  private static boolean isAnyPointer(ImmutableList<CExpression> pExpressions) {
    for (CExpression expression : pExpressions) {
      ImmutableSet<CSimpleDeclaration> declarations =
          SeqPointerAliasingUtil.getAllSimpleDeclarationsInExpression(expression, true);
      checkState(!declarations.isEmpty());
      // if there are multiple declarations, such as pthread_mutex_array[i], then use the pointer
      if (declarations.size() > 1) {
        return true;
      }
      CSimpleDeclaration declaration = Iterables.getOnlyElement(declarations);
      if (declaration.getType().getCanonicalType() instanceof CPointerType) {
        return true;
      }
    }
    return false;
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

  private static ImmutableList<CCompoundStatementElement> buildMutexLockFunctionStatements(
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

  private static ImmutableList<CCompoundStatementElement> buildMutexUnlockFunctionStatements(
      CExpression pMutexExpression) {

    CFieldReference mutexFieldReference = buildMutexFieldReference(pMutexExpression);
    return ImmutableList.of(
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, mutexFieldReference, CIntegerLiteralExpression.ZERO)));
  }

  // pthread_cond_t

  private static ImmutableList<CCompoundStatementElement> buildCondSignalFunctionStatements(
      CExpression pCondExpression) {

    CFieldReference condFieldReference = buildCondFieldReference(pCondExpression);
    CExportStatement condAssignment =
        new CStatementWrapper(
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY, condFieldReference, CIntegerLiteralExpression.ONE));
    return ImmutableList.of(condAssignment);
  }

  private static ImmutableList<CCompoundStatementElement> buildCondWaitFunctionStatements(
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

  private static ImmutableList<CCompoundStatementElement> buildRwLockRdLockFunctionStatements(
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

  private static ImmutableList<CCompoundStatementElement> buildRwLockUnlockFunctionStatements(
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

  private static ImmutableList<CCompoundStatementElement> buildRwLockWrLockFunctionStatements(
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
}
