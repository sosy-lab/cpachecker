// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/**
 * An object for a thread containing an identifier (threadObject) and entry / exit Nodes of the
 * threads to identify which parts of a CFA are executed by the thread.
 */
public class MPORThread {

  public final int id;

  /** The pthread_t object. Set to empty for the main thread. */
  public final Optional<CExpression> threadObject;

  /** The subset of the original CFA executed by the thread. */
  public final ThreadCFA cfa;

  protected MPORThread(int pId, Optional<CExpression> pThreadObject, ThreadCFA pCfa) {
    id = pId;
    threadObject = pThreadObject;
    cfa = pCfa;
  }

  public boolean isMain() {
    return threadObject.isEmpty();
  }
}
