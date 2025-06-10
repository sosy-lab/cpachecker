// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.thread_simulation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class ThreadSimulationVariables {

  /** The map of {@code pthread_mutex_t} objects to their {@code {mutex}_LOCKED} variables. */
  public final ImmutableMap<CIdExpression, MutexLocked> locked;

  public ThreadSimulationVariables(ImmutableMap<CIdExpression, MutexLocked> pLocked) {
    locked = pLocked;
  }

  /** Returns all CIdExpressions of the vars in the order locked - locks - joins. */
  public ImmutableList<CIdExpression> getIdExpressions() {
    ImmutableList.Builder<CIdExpression> rIdExpressions = ImmutableList.builder();
    for (MutexLocked var : locked.values()) {
      rIdExpressions.add(var.idExpression);
    }
    return rIdExpressions.build();
  }
}
