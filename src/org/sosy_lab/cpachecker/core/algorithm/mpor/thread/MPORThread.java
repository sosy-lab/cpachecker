// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.collect.ImmutableListMultimap;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 *
 * @param threadObject The pthread_t object. Set to empty for the main thread.
 * @param startRoutine The {@link CFunctionDeclaration} of the startRoutine (pthreads) or main
 *     function (main thread).
 * @param startRoutineExitVariable The intermediate variable storing the {@code retval} given to
 *     {@code pthread_exit}, if called anywhere in this thread.
 * @param localVariables The set of context-sensitive local variable declarations of this thread.
 * @param cfa The subset of the original CFA executed by the thread.
 */
public record MPORThread(
    int id,
    Optional<CIdExpression> threadObject,
    CFunctionDeclaration startRoutine,
    Optional<CFAEdgeForThread> startRoutineCall,
    Optional<CIdExpression> startRoutineExitVariable,
    ImmutableListMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>> localVariables,
    CFAForThread cfa) {

  public boolean isMain() {
    return threadObject.isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        threadObject,
        startRoutine,
        startRoutineCall,
        startRoutineExitVariable,
        localVariables,
        cfa);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    return pOther
            instanceof
            MPORThread(
                int pId,
                Optional<CIdExpression> pThreadObject,
                CFunctionDeclaration pStartRoutine,
                Optional<CFAEdgeForThread> pStartRoutineCall,
                Optional<CIdExpression> pStartRoutineExitVariable,
                ImmutableListMultimap<CVariableDeclaration, Optional<CFAEdgeForThread>>
                    pLocalVariables,
                CFAForThread pCfa)
        && id == pId
        && threadObject.equals(pThreadObject)
        && startRoutine.equals(pStartRoutine)
        && startRoutineCall.equals(pStartRoutineCall)
        && startRoutineExitVariable.equals(pStartRoutineExitVariable)
        && localVariables.equals(pLocalVariables)
        && cfa.equals(pCfa);
  }
}
