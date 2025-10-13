// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_sync_flags;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class RwLockNumReadersWritersFlag {

  public final CIdExpression readersIdExpression;

  public final CIdExpression writersIdExpression;

  public RwLockNumReadersWritersFlag(
      CIdExpression pReadersIdExpression, CIdExpression pWritersIdExpression) {

    checkNotNull(pReadersIdExpression);
    checkNotNull(pWritersIdExpression);
    readersIdExpression = pReadersIdExpression;
    writersIdExpression = pWritersIdExpression;
  }
}
