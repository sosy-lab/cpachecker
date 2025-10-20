// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class PthreadUtil {

  public static PthreadFunctionType getPthreadFunctionType(CFAEdge pEdge) {
    checkArgument(CFAUtils.isCfaEdgeCFunctionCall(pEdge));
    String functionName = CFAUtils.getFunctionNameFromCfaEdge(pEdge);
    for (PthreadFunctionType functionType : PthreadFunctionType.values()) {
      if (functionType.name.equals(functionName)) {
        return functionType;
      }
    }
    throw new IllegalArgumentException("unrecognized pthread method: " + pEdge.getRawAST());
  }

  public static CIdExpression extractPthreadObject(
      CFAEdge pCfaEdge, PthreadObjectType pPthreadObjectType) {

    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pCfaEdge, pPthreadObjectType),
        "pCfaEdge must be call to a pthread method with a %s parameter",
        pPthreadObjectType);

    PthreadFunctionType functionType = getPthreadFunctionType(pCfaEdge);
    CExpression parameterExpression =
        CFAUtils.getParameterAtIndex(pCfaEdge, functionType.getParameterIndex(pPthreadObjectType));

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

  public static CFunctionType extractStartRoutineType(CFAEdge pEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pEdge, PthreadObjectType.START_ROUTINE),
        "pEdge must be call to a pthread method with a start_routine param");

    PthreadFunctionType functionType = getPthreadFunctionType(pEdge);
    return CFAUtils.getCFunctionTypeFromCExpression(
        CFAUtils.getParameterAtIndex(
            pEdge, functionType.getParameterIndex(PthreadObjectType.START_ROUTINE)));
  }

  public static CFunctionDeclaration extractStartRoutineDeclaration(CFAEdge pEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pEdge, PthreadObjectType.START_ROUTINE),
        "pEdge must be call to a pthread method with a start_routine param");

    PthreadFunctionType functionType = getPthreadFunctionType(pEdge);
    CExpression startRoutineParameter =
        CFAUtils.getParameterAtIndex(
            pEdge, functionType.getParameterIndex(PthreadObjectType.START_ROUTINE));
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

  public static CExpression extractStartRoutineArg(CFAEdge pEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pEdge, PthreadObjectType.START_ROUTINE),
        "pEdge must be a call to a pthread method with a start_routine parameter");

    PthreadFunctionType functionType = getPthreadFunctionType(pEdge);
    return CFAUtils.getParameterAtIndex(
        pEdge, functionType.getParameterIndex(PthreadObjectType.START_ROUTINE_ARGUMENT));
  }

  public static CExpression extractExitReturnValue(CFAEdge pEdge) {
    checkArgument(
        isCallToAnyPthreadFunctionWithObjectType(pEdge, PthreadObjectType.RETURN_VALUE),
        "pEdge must be a call to a pthread method with a start_routine parameter");

    PthreadFunctionType functionType = getPthreadFunctionType(pEdge);
    return CFAUtils.getParameterAtIndex(
        pEdge, functionType.getParameterIndex(PthreadObjectType.RETURN_VALUE));
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

  public static boolean isPthreadObjectType(CType pType) {
    String typeName = SeqStringUtil.getTypeName(pType);
    for (PthreadObjectType pthreadObjectType : PthreadObjectType.values()) {
      if (typeName.equals(pthreadObjectType.name)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Tries to extract the {@link CFunctionCallStatement} from pEdge and returns true if it is a call
   * to pFunctionType.
   */
  public static boolean isCallToPthreadFunction(CFAEdge pEdge, PthreadFunctionType pFunctionType) {
    return CFAUtils.isCfaEdgeCFunctionCall(pEdge)
        && CFAUtils.getFunctionNameFromCfaEdge(pEdge).equals(pFunctionType.name);
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
