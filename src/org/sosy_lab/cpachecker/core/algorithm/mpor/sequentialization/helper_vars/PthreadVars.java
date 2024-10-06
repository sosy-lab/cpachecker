// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.helper_vars;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class PthreadVars {

  public final ImmutableMap<CIdExpression, CIdExpression> threadActive;

  public final ImmutableMap<CIdExpression, CIdExpression> mutexLocked;

  public final ImmutableMap<MPORThread, ImmutableMap<MPORThread, CIdExpression>> threadJoining;

  public PthreadVars(
      ImmutableMap<CIdExpression, CIdExpression> pThreadActiveVars,
      ImmutableMap<CIdExpression, CIdExpression> pMutexLockedVars,
      ImmutableMap<MPORThread, ImmutableMap<MPORThread, CIdExpression>> pThreadJoiningVars) {

    threadActive = pThreadActiveVars;
    mutexLocked = pMutexLockedVars;
    threadJoining = pThreadJoiningVars;
  }
}
