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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class PthreadUtil {

  public static PthreadFunctionType getPthreadFunctionType(CFAEdge pCfaEdge) {
    if (pCfaEdge.getRawAST().isPresent()) {
      if (pCfaEdge.getRawAST().orElseThrow() instanceof CFunctionCall functionCall) {
        String functionName =
            functionCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();
        for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
          if (functionType.name.equals(functionName)) {
            return functionType;
          }
        }
      }
    }
    throw new IllegalArgumentException(
        "could not extract any PthreadFunctionType from pCfaEdge: " + pCfaEdge.getCode());
  }

  public static CIdExpression extractPthreadObject(
      CFAEdge pCfaEdge, PthreadObjectType pPthreadObjectType) {

    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pCfaEdge, pPthreadObjectType),
        "pCfaEdge must be call to a pthread method with a %s parameter",
        pPthreadObjectType);

    PthreadFunctionType functionType = getPthreadFunctionType(pCfaEdge);
    Optional<CExpression> parameterExpression =
        CFAUtils.tryGetParameterAtIndex(
            pCfaEdge, functionType.getParameterIndex(pPthreadObjectType));

    CExpression expression =
        MPORUtil.getOperandFromUnaryExpression(parameterExpression.orElseThrow());
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
            pPthreadObjectType, parameterExpression.orElseThrow().toASTString()));
  }

  // START_ROUTINE =================================================================================

  public static CFunctionDeclaration extractStartRoutineDeclaration(CFAEdge pEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pEdge, PthreadObjectType.START_ROUTINE),
        "pEdge must be call to a pthread method with a start_routine param");

    PthreadFunctionType functionType = getPthreadFunctionType(pEdge);
    CExpression startRoutineParameter =
        CFAUtils.tryGetParameterAtIndex(
                pEdge, functionType.getParameterIndex(PthreadObjectType.START_ROUTINE))
            .orElseThrow();
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
    throw new IllegalArgumentException("could not extract start_routine declaration from pEdge");
  }

  public static CFunctionType extractStartRoutineType(CFAEdge pCfaEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pCfaEdge, PthreadObjectType.START_ROUTINE),
        "pCfaEdge must be call to a pthread method with a start_routine param");

    PthreadFunctionType pthreadFunctionType = getPthreadFunctionType(pCfaEdge);
    int startRoutineIndex = pthreadFunctionType.getParameterIndex(PthreadObjectType.START_ROUTINE);
    CExpression parameterExpression =
        CFAUtils.tryGetParameterAtIndex(pCfaEdge, startRoutineIndex).orElseThrow();
    if (parameterExpression instanceof CUnaryExpression unaryExpression) {
      if (unaryExpression.getExpressionType() instanceof CPointerType) {
        if (unaryExpression.getOperand() instanceof CIdExpression idExpression) {
          if (idExpression.getExpressionType() instanceof CFunctionType functionType) {
            return functionType;
          }
        }
      }
    }
    throw new IllegalArgumentException(
        "could not extract start_routine from pCfaEdge: " + pCfaEdge.getCode());
  }

  public static CExpression extractStartRoutineArg(CFAEdge pCfaEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(
            pCfaEdge, PthreadObjectType.START_ROUTINE_ARGUMENT),
        "pEdge must be a call to a pthread method with a start_routine parameter");

    PthreadFunctionType functionType = getPthreadFunctionType(pCfaEdge);
    int returnValueIndex = functionType.getParameterIndex(PthreadObjectType.START_ROUTINE_ARGUMENT);
    return CFAUtils.tryGetParameterAtIndex(pCfaEdge, returnValueIndex).orElseThrow();
  }

  // RETURN_VALUE ==================================================================================

  public static CExpression extractExitReturnValue(CFAEdge pEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pEdge, PthreadObjectType.RETURN_VALUE),
        "pEdge must be a call to a pthread method with a start_routine parameter");

    PthreadFunctionType functionType = getPthreadFunctionType(pEdge);
    int returnValueIndex = functionType.getParameterIndex(PthreadObjectType.RETURN_VALUE);
    return CFAUtils.tryGetParameterAtIndex(pEdge, returnValueIndex).orElseThrow();
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

  public static boolean isAnyPthreadObjectType(CType pType) {
    String typeName = SeqStringUtil.getTypeName(pType);
    for (PthreadObjectType pthreadObjectType : PthreadObjectType.values()) {
      if (typeName.equals(pthreadObjectType.name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tries to extract the {@link CFunctionCallStatement} from pCfaEdge and returns true if it is a
   * call to pFunctionType.
   */
  public static boolean isCallToPthreadFunction(
      CFAEdge pCfaEdge, PthreadFunctionType pFunctionType) {

    if (pCfaEdge.getRawAST().isPresent()) {
      if (pCfaEdge.getRawAST().orElseThrow() instanceof CFunctionCall functionCall) {
        return functionCall
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString()
            .equals(pFunctionType.name);
      }
    }
    return false;
  }

  public static boolean isCallToAnyPthreadFunction(CFAEdge pEdge) {
    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (isCallToPthreadFunction(pEdge, functionType)) {
        return true;
      }
    }
    return false;
  }

  public static boolean isCallToAnyPthreadFunctionWithObjectType(
      CFAEdge pCfaEdge, PthreadObjectType pPthreadObjectType) {

    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (functionType.isParameterPresent(pPthreadObjectType)) {
        if (isCallToPthreadFunction(pCfaEdge, functionType)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns true if the semantics of the pthread method in pEdge is considered in the
   * sequentialization, i.e. the case block contains code. A function may be supported by MPOR but
   * not considered in the sequentialization.
   */
  public static boolean isExplicitlyHandledPthreadFunction(CFAEdge pEdge) {
    if (PthreadUtil.isCallToAnyPthreadFunction(pEdge)) {
      return PthreadUtil.getPthreadFunctionType(pEdge).isExplicitlyHandled;
    }
    return false;
  }
}
