// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost.thread_simulation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

/** A simple wrapper for {@link CIdExpression}s of {@code {thread}_JOINS_{thread}} variables. */
public class ThreadJoinsThread {

  public final CIdExpression idExpression;

  public ThreadJoinsThread(@NonNull CIdExpression pIdExpression) {
    checkNotNull(pIdExpression);
    idExpression = pIdExpression;
  }
}
