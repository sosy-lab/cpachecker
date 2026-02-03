// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class PthreadUtil {

  /**
   * Tries to extract a {@link CFunctionCall} from {@link AAstNode} of the given {@link CFAEdge}. If
   * the AST is absent or not a {@link CFunctionCall}, {@link Optional#empty()} is returned.
   */
  public static Optional<CFunctionCall> tryGetFunctionCallFromCfaEdge(CFAEdge pCfaEdge) {
    if (pCfaEdge.getRawAST().isPresent()) {
      if (pCfaEdge.getRawAST().orElseThrow() instanceof CFunctionCall functionCall) {
        return Optional.of(functionCall);
      }
    }
    return Optional.empty();
  }

  public static PthreadFunctionType getPthreadFunctionType(CFunctionCall pFunctionCall) {
    String functionName =
        pFunctionCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();
    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (functionType.name.equals(functionName)) {
        return functionType;
      }
    }
    throw new IllegalArgumentException(
        "could not extract any PthreadFunctionType from pFunctionCall: "
            + pFunctionCall.toASTString());
  }

  public static CIdExpression extractPthreadObject(
      CFunctionCall pFunctionCall, PthreadObjectType pPthreadObjectType)
      throws UnsupportedCodeException {

    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pFunctionCall, pPthreadObjectType),
        "pFunctionCall %s must be call to a pthread method with a %s parameter",
        pFunctionCall.toASTString(),
        pPthreadObjectType);

    PthreadFunctionType functionType = getPthreadFunctionType(pFunctionCall);
    int parameterIndex = functionType.getParameterIndex(pPthreadObjectType);
    CExpression parameterExpression =
        pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(parameterIndex);
    CExpression expression = MPORUtil.getOperandFromUnaryExpression(parameterExpression);
    // first handle pthread_t, the only pthread object type that may not be a pointer
    if (pPthreadObjectType.equals(PthreadObjectType.PTHREAD_T)
        && functionType.isPthreadTPointer()) {
      if (expression instanceof CIdExpression pthreadT) {
        return pthreadT;
      }
    } else {
      if (expression instanceof CIdExpression idExpression) {
        return idExpression;
      } else if (expression instanceof CFieldReference fieldReference) {
        // TODO this may have to be adjusted, if the struct itself contains arrays
        if (fieldReference.getFieldOwner() instanceof CIdExpression idExpression) {
          return idExpression;
        }
      }
    }
    throw new IllegalArgumentException(
        String.format(
            "could not extract pthread object of type %s from expression %s",
            pPthreadObjectType, parameterExpression.toASTString()));
  }

  // START_ROUTINE =================================================================================

  public static CFunctionDeclaration extractStartRoutineDeclaration(CFunctionCall pFunctionCall) {
    PthreadFunctionType functionType = getPthreadFunctionType(pFunctionCall);
    int startRoutineIndex = functionType.getParameterIndex(PthreadObjectType.START_ROUTINE);
    CExpression startRoutineParameter =
        pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(startRoutineIndex);
    if (startRoutineParameter instanceof CIdExpression idExpression) {
      if (idExpression.getDeclaration() instanceof CFunctionDeclaration functionDeclaration) {
        return functionDeclaration;
      }
    }
    if (startRoutineParameter instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
        if (idExpression.getDeclaration() instanceof CFunctionDeclaration functionDeclaration) {
          return functionDeclaration;
        }
      }
    }
    throw new IllegalArgumentException(
        "could not extract start_routine declaration from pFunctionCall: "
            + pFunctionCall.toASTString());
  }

  public static CExpression extractStartRoutineArg(CFunctionCall pFunctionCall) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(
            pFunctionCall, PthreadObjectType.START_ROUTINE_ARGUMENT),
        "pFunctionCall %s must be a call to a pthread method with a start_routine parameter",
        pFunctionCall.toASTString());
    PthreadFunctionType functionType = getPthreadFunctionType(pFunctionCall);
    int returnValueIndex = functionType.getParameterIndex(PthreadObjectType.START_ROUTINE_ARGUMENT);
    return pFunctionCall
        .getFunctionCallExpression()
        .getParameterExpressions()
        .get(returnValueIndex);
  }

  // RETURN_VALUE ==================================================================================

  public static CExpression extractExitReturnValue(CFunctionCall pFunctionCall) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pFunctionCall, PthreadObjectType.RETURN_VALUE),
        "pFunctionCall %s must be a call to a pthread method with a start_routine parameter",
        pFunctionCall.toASTString());

    PthreadFunctionType functionType = getPthreadFunctionType(pFunctionCall);
    int returnValueIndex = functionType.getParameterIndex(PthreadObjectType.RETURN_VALUE);
    return pFunctionCall
        .getFunctionCallExpression()
        .getParameterExpressions()
        .get(returnValueIndex);
  }

  // PTHREAD_MUTEX_INITIALIZER =====================================================================

  /**
   * Returns true if {@code pCfaEdge} assigns a {@code PTHREAD_MUTEX_INITIALIZER}:
   *
   * <ul>
   *   <li>{@code pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;} or
   *   <li>{@code m = PTHREAD_MUTEX_INITIALIZER;} where m is a {@code pthread_mutex_t} declared
   *       beforehand
   * </ul>
   */
  public static boolean isPthreadMutexInitializerAssignment(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CDeclarationEdge declarationEdge) {
      // pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER; (declaration)
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
        return isPthreadMutexTWithInitializerList(variableDeclaration);
      }
    } else if (pCfaEdge instanceof CStatementEdge statementEdge) {
      // m = PTHREAD_MUTEX_INITIALIZER; (assignment)
      if (statementEdge.getStatement() instanceof CExpressionAssignmentStatement assignment) {
        if (assignment.getLeftHandSide() instanceof CIdExpression idExpression) {
          if (idExpression.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
            return isPthreadMutexTWithInitializerList(variableDeclaration);
          }
        }
      }
    }
    return false;
  }

  private static boolean isPthreadMutexTWithInitializerList(
      CVariableDeclaration pVariableDeclaration) {

    // if the type yields a trailing white space (from declaration edge), we strip it
    String typeName = pVariableDeclaration.getType().toASTString("").strip();
    if (typeName.equals(PthreadObjectType.PTHREAD_MUTEX_T.name)) {
      // preprocessing yields initializer lists for PTHREAD_MUTEX_INITIALIZER
      // see e.g. goblint-regression/13-privatized_34-traces-minepp-L-needs-to-be-um_true
      return pVariableDeclaration.getInitializer() instanceof CInitializerList;
    }
    return false;
  }

  // boolean helpers ===============================================================================

  /**
   * Returns {@code true} if {@code pType} matches any {@link PthreadObjectType} by name and {@code
   * false} otherwise.
   */
  public static boolean isAnyPthreadObjectType(CType pType) {
    String typeName = pType.toString().strip();
    for (PthreadObjectType pthreadObjectType : PthreadObjectType.values()) {
      if (typeName.equals(pthreadObjectType.name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pFunctionCall} is a call to {@code pFunctionType} and {@code
   * false} otherwise.
   */
  public static boolean isCallToPthreadFunction(
      CFunctionCall pFunctionCall, PthreadFunctionType pFunctionType) {

    return pFunctionCall
        .getFunctionCallExpression()
        .getFunctionNameExpression()
        .toASTString()
        .equals(pFunctionType.name);
  }

  /**
   * Returns {@code true} if {@code pFunctionCallExpression} is a call to {@code pFunctionType} and
   * {@code false} otherwise.
   */
  public static boolean isCallToPthreadFunction(
      CFunctionCallExpression pFunctionCallExpression, PthreadFunctionType pFunctionType) {

    return pFunctionCallExpression
        .getFunctionNameExpression()
        .toASTString()
        .equals(pFunctionType.name);
  }

  /**
   * Returns {@code true} if {@code pFunctionCall} is a call to any function in {@link
   * PthreadFunctionType} and {@code false} otherwise.
   */
  public static boolean isCallToAnyPthreadFunction(CFunctionCall pFunctionCall) {
    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (isCallToPthreadFunction(pFunctionCall, functionType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pFunctionCall} is a call to a {@link PthreadFunctionType} that
   * contains {@code pPthreadObjectType} as at least one parameter and {@code false} otherwise.
   */
  public static boolean isCallToAnyPthreadFunctionWithObjectType(
      CFunctionCall pFunctionCall, PthreadObjectType pPthreadObjectType) {

    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (functionType.isParameterPresent(pPthreadObjectType)) {
        if (isCallToPthreadFunction(pFunctionCall, functionType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns {@code true} if {@code pFunctionCallExpression} is a call to a {@link
   * PthreadFunctionType} that contains {@code pPthreadObjectType} as at least one parameter and
   * {@code false} otherwise.
   */
  public static boolean isCallToAnyPthreadFunctionWithObjectType(
      CFunctionCallExpression pFunctionCallExpression, PthreadObjectType pPthreadObjectType) {

    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (functionType.isParameterPresent(pPthreadObjectType)) {
        if (isCallToPthreadFunction(pFunctionCallExpression, functionType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the semantics of the pthread method in pCfaEdge is considered in the
   * sequentialization, i.e. the case block contains code. A function may be supported by MPOR but
   * not considered in the sequentialization.
   */
  public static boolean isExplicitlyHandledPthreadFunction(CFAEdge pCfaEdge) {
    Optional<CFunctionCall> functionCall = tryGetFunctionCallFromCfaEdge(pCfaEdge);
    if (functionCall.isPresent()) {
      if (isCallToAnyPthreadFunction(functionCall.orElseThrow())) {
        return PthreadUtil.getPthreadFunctionType(functionCall.orElseThrow()).isExplicitlyHandled;
      }
    }
    return false;
  }
}
