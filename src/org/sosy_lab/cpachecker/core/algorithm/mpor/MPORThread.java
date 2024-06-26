// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 */
public class MPORThread {

  /** The pthread_t object. Set to null for the main thread. */
  public final @Nullable CIdExpression threadObject;

  /** FunctionEntryNode of the main function (main thread) or start routine (pthreads). */
  public final FunctionEntryNode entryNode;

  /**
   * FunctionExitNode of the main function (main thread) or start routine (pthreads). Can be null,
   * see {@link FunctionEntryNode#exitNode}.
   */
  public final @Nullable FunctionExitNode exitNode;

  public MPORThread(
      CIdExpression pPthreadT, FunctionEntryNode pEntryNode, FunctionExitNode pExitNode) {
    threadObject = pPthreadT;
    entryNode = pEntryNode;
    exitNode = pExitNode;
  }

  public boolean isMain() {
    return threadObject == null;
  }
}
