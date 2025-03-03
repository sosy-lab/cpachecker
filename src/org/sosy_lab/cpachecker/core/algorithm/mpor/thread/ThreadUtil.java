// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.line_of_code.LineOfCode;

public class ThreadUtil {

  protected static ImmutableMap<CFunctionDeclaration, Integer> getCalledFunctions(
      ImmutableSet<ThreadEdge> pThreadEdges) {

    Map<CFunctionDeclaration, Integer> rCalledFunctions = new HashMap<>();
    for (ThreadEdge threadEdge : pThreadEdges) {
      if (threadEdge.cfaEdge instanceof CFunctionCallEdge functionCall) {
        CFunctionDeclaration functionDeclaration =
            functionCall.getFunctionCallExpression().getDeclaration();
        rCalledFunctions.merge(functionDeclaration, 1, Integer::sum);
      }
    }
    return ImmutableMap.copyOf(rCalledFunctions);
  }

  /**
   * Creates {@link LineOfCode}s for all non-variable declarations (e.g. function and struct
   * declarations) for the given thread.
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

  /** Returns the MPORThread in pThreads whose pthread_t object is empty i.e. the main thread. */
  public static MPORThread extractMainThread(ImmutableSet<MPORThread> pThreads) {
    for (MPORThread thread : pThreads) {
      if (thread.isMain()) {
        return thread;
      }
    }
    throw new IllegalArgumentException("pThreads does not contain the main thread");
  }
}
