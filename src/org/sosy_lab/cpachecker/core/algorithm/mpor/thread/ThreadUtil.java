// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;

public class ThreadUtil {

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
}
