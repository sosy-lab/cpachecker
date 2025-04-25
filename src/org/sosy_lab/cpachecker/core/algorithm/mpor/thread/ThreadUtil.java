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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ThreadUtil {

  /**
   * Returns either {@code pCallContext} if present or the start_routine call of {@code pThread},
   * which serves as the default call context.
   */
  public static Optional<ThreadEdge> getCallContextOrStartRoutineCall(
      Optional<ThreadEdge> pCallContext, MPORThread pThread) {

    return pCallContext.isPresent() ? pCallContext : pThread.startRoutineCall;
  }

  public static Optional<ThreadEdge> getCallContextOrStartRoutineCall(
      Optional<ThreadEdge> pCallContext, Optional<ThreadEdge> pStartRoutineCall) {

    return pCallContext.isPresent() ? pCallContext : pStartRoutineCall;
  }

  protected static <T extends CFAEdge> ImmutableList<ThreadEdge> getEdgesByClass(
      ImmutableSet<ThreadEdge> pThreadEdges, Class<T> pEdgeClass) {

    ImmutableList.Builder<ThreadEdge> rEdges = ImmutableList.builder();
    for (ThreadEdge threadEdge : pThreadEdges) {
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
    for (ThreadEdge threadEdge : pThread.cfa.threadEdges) {
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

  public static MPORThread extractMainThread(ImmutableSet<MPORThread> pThreads) {
    return pThreads.stream().filter(t -> t.isMain()).findAny().orElseThrow();
  }

  public static MPORThread extractThread(ImmutableSet<MPORThread> pThreads, CFAEdge pEdge) {
    checkArgument(
        PthreadUtil.callsAnyPthreadFunctionWithPthreadT(pEdge),
        "pEdge must be call to a pthread method with a pthread_t param");

    PthreadFunctionType funcType = PthreadUtil.getPthreadFunctionType(pEdge);
    CExpression pthreadTParam = CFAUtils.getParameterAtIndex(pEdge, funcType.getPthreadTIndex());

    return getThreadByObject(
        pThreads,
        Optional.of(
            funcType.isPthreadTPointer()
                ? CFAUtils.getValueFromAddress(pthreadTParam)
                : pthreadTParam));
  }

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
}
