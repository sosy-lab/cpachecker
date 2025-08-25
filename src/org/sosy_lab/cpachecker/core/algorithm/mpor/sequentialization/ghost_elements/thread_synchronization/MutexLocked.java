// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/** A simple wrapper for {@link CIdExpression}s of {@code {mutex}_LOCKED} variables. */
public class MutexLocked {

  public final CIdExpression idExpression;

  public final CBinaryExpression notLockedExpression;

  public MutexLocked(CIdExpression pIdExpression, CBinaryExpression pNotLockedExpression) {
    checkNotNull(pIdExpression);
    checkNotNull(pNotLockedExpression);
    idExpression = pIdExpression;
    notLockedExpression = pNotLockedExpression;
  }
}
