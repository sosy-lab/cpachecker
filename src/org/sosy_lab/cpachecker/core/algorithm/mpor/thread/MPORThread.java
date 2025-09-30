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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLabelStatement;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 */
public class MPORThread {

  private final int id;

  /** The pthread_t object. Set to empty for the main thread. */
  public final Optional<CIdExpression> threadObject;

  /**
   * The {@link CFunctionDeclaration} of the startRoutine (pthreads) or main function (main thread).
   */
  public final CFunctionDeclaration startRoutine;

  public final Optional<ThreadEdge> startRoutineCall;

  // TODO also need a bool flag is_retrieved, because it can only be retrieved once
  //  (multiple joins to the same thread are undefined behavior)
  /**
   * The intermediate variable storing the {@code retval} given to {@code pthread_exit}, if called
   * anywhere in this thread.
   */
  public final Optional<CIdExpression> startRoutineExitVariable;

  /** The set of context-sensitive local variable declarations of this thread. */
  public final ImmutableListMultimap<CVariableDeclaration, Optional<ThreadEdge>> localVariables;

  /** The subset of the original CFA executed by the thread. */
  public final ThreadCFA cfa;

  // TODO move both into GhostElements
  /**
   * The thread-specific nondeterministic {@code K{thread_id}} variable (statement round counter).
   */
  private final Optional<CIdExpression> kVariable;

  private final Optional<SeqThreadLabelStatement> label;

  protected MPORThread(
      int pId,
      Optional<CIdExpression> pThreadObject,
      CFunctionDeclaration pStartRoutine,
      Optional<ThreadEdge> pStartRoutineCall,
      Optional<CIdExpression> pStartRoutineExitVariable,
      ImmutableListMultimap<CVariableDeclaration, Optional<ThreadEdge>> pLocalVariables,
      ThreadCFA pCfa,
      Optional<CIdExpression> pKVariable,
      Optional<SeqThreadLabelStatement> pLabel) {

    id = pId;
    threadObject = pThreadObject;
    startRoutine = pStartRoutine;
    startRoutineCall = pStartRoutineCall;
    startRoutineExitVariable = pStartRoutineExitVariable;
    localVariables = pLocalVariables;
    cfa = pCfa;
    kVariable = pKVariable;
    label = pLabel;
  }

  public int getId() {
    return id;
  }

  public boolean isMain() {
    return threadObject.isEmpty();
  }

  public Optional<CIdExpression> getKVariable() {
    return kVariable;
  }

  public Optional<SeqThreadLabelStatement> getLabel() {
    return label;
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
    return pOther instanceof MPORThread other
        && id == other.id
        && threadObject.equals(other.threadObject)
        && startRoutine.equals(other.startRoutine)
        && startRoutineCall.equals(other.startRoutineCall)
        && startRoutineExitVariable.equals(other.startRoutineExitVariable)
        && localVariables.equals(other.localVariables)
        && cfa.equals(other.cfa);
  }
}
