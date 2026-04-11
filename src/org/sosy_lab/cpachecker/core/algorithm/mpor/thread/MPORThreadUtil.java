// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.ProgramCounterVariables;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class MPORThreadUtil {

  /**
   * Returns either {@code pCallContext} if present or the start_routine call of {@code pThread},
   * which serves as the default call context.
   */
  public static Optional<CFAEdgeForThread> getCallContextOrStartRoutineCall(
      Optional<CFAEdgeForThread> pCallContext, MPORThread pThread) {

    return pCallContext.isPresent() ? pCallContext : pThread.startRoutineCall();
  }

  public static Optional<CFAEdgeForThread> getCallContextOrStartRoutineCall(
      Optional<CFAEdgeForThread> pCallContext, Optional<CFAEdgeForThread> pStartRoutineCall) {

    return pCallContext.isPresent() ? pCallContext : pStartRoutineCall;
  }

  protected static <T extends CFAEdge> ImmutableList<CFAEdgeForThread> getEdgesByClass(
      ImmutableSet<CFAEdgeForThread> pThreadEdges, Class<T> pEdgeClass) {

    ImmutableList.Builder<CFAEdgeForThread> rEdges = ImmutableList.builder();
    for (CFAEdgeForThread threadEdge : pThreadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      if (pEdgeClass.isInstance(cfaEdge)) {
        rEdges.add(threadEdge);
      }
    }
    return rEdges.build();
  }

  /**
   * Extracts all non-variable declarations (e.g. function and struct declarations) for the given
   * thread.
   */
  public static ImmutableList<CDeclaration> extractNonVariableDeclarations(MPORThread pThread) {
    ImmutableList.Builder<CDeclaration> rNonVariableDeclarations = ImmutableList.builder();
    for (CFAEdgeForThread threadEdge : pThread.cfa().threadEdges) {
      if (threadEdge.cfaEdge instanceof CDeclarationEdge declarationEdge) {
        CDeclaration declaration = declarationEdge.getDeclaration();
        if (!(declaration instanceof CVariableDeclaration)) {
          // check if only main thread declares non-vars, e.g. functions
          assert pThread.isMain();
          rNonVariableDeclarations.add(declaration);
        }
      }
    }
    return rNonVariableDeclarations.build();
  }

  public static MPORThread extractMainThread(ImmutableCollection<MPORThread> pThreads) {
    return pThreads.stream().filter(t -> t.isMain()).findAny().orElseThrow();
  }

  public static MPORThread getThreadByCFunctionCall(
      ImmutableCollection<MPORThread> pThreads, CFunctionCall pFunctionCall)
      throws UnsupportedCodeException {

    checkArgument(
        PthreadUtil.isCallToAnyPthreadFunctionWithObjectType(
            pFunctionCall, PthreadObjectType.PTHREAD_T),
        "pFunctionCall must be call to a pthread method with a pthread_t param");

    PthreadFunctionType functionType = PthreadUtil.getPthreadFunctionType(pFunctionCall);
    int pthreadTIndex = functionType.getParameterIndex(PthreadObjectType.PTHREAD_T);
    CExpression pthreadTParameter =
        pFunctionCall.getFunctionCallExpression().getParameterExpressions().get(pthreadTIndex);

    return getThreadByObject(
        pThreads,
        Optional.of(
            functionType.isPthreadTPointer()
                ? MPORUtil.getOperandFromUnaryExpression(pthreadTParameter)
                : pthreadTParameter));
  }

  /** Searches the given map of MPORThreads for the given thread object. */
  public static MPORThread getThreadByObject(
      ImmutableCollection<MPORThread> pThreads, Optional<CExpression> pThreadObject) {

    for (MPORThread rThread : pThreads) {
      if (rThread.threadObject().equals(pThreadObject)) {
        return rThread;
      }
    }
    throw new IllegalArgumentException("no MPORThread with pThreadObject found in threads");
  }

  public static MPORThread getThreadById(ImmutableCollection<MPORThread> pThreads, int pId) {
    for (MPORThread thread : pThreads) {
      if (thread.id() == pId) {
        return thread;
      }
    }
    throw new IllegalArgumentException("no MPORThread with pId found in threads");
  }

  static int getHighestPc(ImmutableList<CFANodeForThread> pThreadNodes) {
    int highestPc = ProgramCounterVariables.EXIT_PC;
    for (CFANodeForThread threadNode : pThreadNodes) {
      if (threadNode.pc > highestPc) {
        highestPc = threadNode.pc;
      }
    }
    return highestPc;
  }
}
