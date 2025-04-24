// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqStringUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class PthreadUtil {

  // TODO move to ThreadUtil
  public static MPORThread extractThread(ImmutableSet<MPORThread> pThreads, CFAEdge pEdge) {
    checkArgument(
        callsAnyPthreadFuncWithPthreadT(pEdge),
        "pEdge must be call to a pthread method with a pthread_t param");

    PthreadFunctionType funcType = getPthreadFuncType(pEdge);
    CExpression pthreadTParam = CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadTIndex());

    if (funcType.isPthreadTPointer()) {
      return getThreadByObject(pThreads, Optional.of(CFAUtils.getValueFromAddress(pthreadTParam)));
    } else {
      return getThreadByObject(pThreads, Optional.of(pthreadTParam));
    }
  }

  public static CIdExpression extractPthreadT(CFAEdge pEdge) {
    checkArgument(
        callsAnyPthreadFuncWithPthreadT(pEdge),
        "pEdge must be call to a pthread method with a pthread_t param");

    PthreadFunctionType funcType = getPthreadFuncType(pEdge);
    CExpression param = CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadTIndex());

    if (funcType.isPthreadTPointer()) {
      CExpression value = CFAUtils.getValueFromAddress(param);
      if (value instanceof CIdExpression pthreadT) {
        return pthreadT;
      }
    } else {
      if (param instanceof CIdExpression pthreadT) {
        return pthreadT;
      }
    }
    throw new IllegalArgumentException("pthread_t must be CIdExpression");
  }

  public static CIdExpression extractPthreadMutexT(CFAEdge pEdge) {
    checkArgument(
        callsAnyPthreadFuncWithPthreadMutexT(pEdge),
        "pEdge must be call to a pthread method with a pthread_mutex_t param");

    PthreadFunctionType funcType = getPthreadFuncType(pEdge);
    CExpression pthreadMutexT =
        CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadMutexTIndex());

    CExpression expr = CFAUtils.getValueFromAddress(pthreadMutexT);
    if (expr instanceof CIdExpression idExpr) {
      return idExpr;
    }
    throw new IllegalArgumentException("pthread_mutex_t must be a CIdExpression");
  }

  public static CFunctionType extractStartRoutine(CFAEdge pEdge) {
    checkArgument(
        callsAnyPthreadFunctionWithStartRoutineParameter(pEdge),
        "pEdge must be call to a pthread method with a start_routine param");

    PthreadFunctionType funcType = getPthreadFuncType(pEdge);
    return CFAUtils.getCFunctionTypeFromCExpression(
        CFAUtils.getParameterAtIndex(pEdge, funcType.getStartRoutineIndex()));
  }

  public static CExpression extractStartRoutineArgument(CFAEdge pEdge) {
    checkArgument(
        callsAnyPthreadFunctionWithStartRoutineParameter(pEdge),
        "pEdge must be a call to a pthread method with a start_routine parameter");

    PthreadFunctionType funcType = getPthreadFuncType(pEdge);
    return CFAUtils.getParameterAtIndex(pEdge, funcType.getStartRoutineArgumentIndex());
  }

  // TODO move to ThreadUtil
  /** Searches the given map of MPORThreads for the given thread object. */
  public static MPORThread getThreadByObject(
      ImmutableCollection<MPORThread> pThreads, Optional<CExpression> pThreadObject) {

    for (MPORThread rThread : pThreads) {
      if (rThread.threadObject.equals(pThreadObject)) {
        return rThread;
      }
    }
    throw new IllegalArgumentException("no MPORThread with pThreadObject found in pThreads");
  }

  /**
   * Returns true if {@code pCfaEdge} assigns a {@code PTHREAD_MUTEX_INITIALIZER}:
   *
   * <ul>
   *   <li>{@code pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER;} or
   *   <li>{@code m = PTHREAD_MUTEX_INITIALIZER;} where m is a {@code pthread_mutex_t} declared
   *       beforehand
   * </ul>
   */
  public static boolean assignsPthreadMutexInitializer(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CDeclarationEdge declarationEdge) {
      // pthread_mutex_t m = PTHREAD_MUTEX_INITIALIZER; (declaration)
      if (declarationEdge.getDeclaration() instanceof CVariableDeclaration variableDeclaration) {
        return isPthreadMutexTWithInitializerList(variableDeclaration);
      }
    } else if (pCfaEdge instanceof CStatementEdge statementEdge) {
      // m = PTHREAD_MUTEX_INITIALIZER; (assignment)
      if (statementEdge.getStatement() instanceof CExpressionAssignmentStatement assignment) {
        // TODO this means we only support CIdExpression for pthread_mutex_t
        //  -> check if this still holds later when unrolling loops etc.
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
    String typeName = pVariableDeclaration.getType().toString().strip();
    if (typeName.equals(PthreadObjectType.PTHREAD_MUTEX_T.name)) {
      // preprocessing yields initializer lists for PTHREAD_MUTEX_INITIALIZER
      // see e.g. goblint-regression/13-privatized_34-traces-minepp-L-needs-to-be-um_true
      return pVariableDeclaration.getInitializer() instanceof CInitializerList;
    }
    return false;
  }

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
   * to pFuncType.
   */
  public static boolean callsPthreadFunction(CFAEdge pEdge, PthreadFunctionType pFuncType) {
    return CFAUtils.isCfaEdgeCFunctionCall(pEdge)
        && CFAUtils.getFunctionNameFromCfaEdge(pEdge).equals(pFuncType.name);
  }

  public static boolean callsAnyPthreadFunction(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (callsPthreadFunction(pEdge, funcType)) {
        return true;
      }
    }
    return false;
  }

  public static boolean callsAnyPthreadFuncWithPthreadT(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.pthreadTIndex.isPresent()) {
        if (callsPthreadFunction(pEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean callsAnyPthreadFuncWithPthreadMutexT(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.pthreadMutexTIndex.isPresent()) {
        if (callsPthreadFunction(pEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean callsAnyPthreadFunctionWithStartRoutineParameter(CFAEdge pEdge) {
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.startRoutineIndex.isPresent()) {
        if (callsPthreadFunction(pEdge, funcType)) {
          return true;
        }
      }
    }
    return false;
  }

  public static PthreadFunctionType getPthreadFuncType(CFAEdge pEdge) {
    checkArgument(CFAUtils.isCfaEdgeCFunctionCall(pEdge));
    String funcName = CFAUtils.getFunctionNameFromCfaEdge(pEdge);
    for (PthreadFunctionType funcType : PthreadFunctionType.values()) {
      if (funcType.name.equals(funcName)) {
        return funcType;
      }
    }
    throw new IllegalArgumentException("unrecognized pthread method: " + pEdge.getRawAST());
  }

  /**
   * Returns true if the semantics of the pthread method in pEdge is considered in the
   * sequentialization, i.e. the case block contains code. A function may be supported by MPOR but
   * not considered in the sequentialization.
   */
  public static boolean isExplicitlyHandledPthreadFunction(CFAEdge pEdge) {
    if (PthreadUtil.callsAnyPthreadFunction(pEdge)) {
      return PthreadUtil.getPthreadFuncType(pEdge).isExplicitlyHandled;
    }
    return false;
  }
}
