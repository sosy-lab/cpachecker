// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.preference_order;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * An object for a pthread_create call containing the created thread and the CFAEdges preceding the
 * pthread_create call inside the thread.
 */
public class MPORCreate {

  /** The first parameter of pthread_create, i.e. the pthread_t object. */
  public final CExpression pthreadT;

  /** All edges executed by the thread calling pthread_create before calling pthread_create. */
  public final ImmutableSet<CFAEdge> precedingEdges;

  public MPORCreate(CExpression pPthreadT, ImmutableSet<CFAEdge> pPrecedingEdges) {
    pthreadT = pPthreadT;
    precedingEdges = pPrecedingEdges;
  }
}
