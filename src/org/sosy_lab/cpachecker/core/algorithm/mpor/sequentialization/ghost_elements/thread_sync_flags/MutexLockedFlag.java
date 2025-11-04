// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/** A simple wrapper for {@link CIdExpression}s of {@code {mutex}_LOCKED} variables. */
public record MutexLockedFlag(
    CIdExpression idExpression,
    CBinaryExpression isLockedExpression,
    CBinaryExpression notLockedExpression) {

  public MutexLockedFlag {
    checkNotNull(idExpression);
    checkNotNull(isLockedExpression);
    checkNotNull(notLockedExpression);
  }
}
