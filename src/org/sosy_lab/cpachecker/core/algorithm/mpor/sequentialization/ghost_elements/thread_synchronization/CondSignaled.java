// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.thread_synchronization;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;

public class CondSignaled {

  public final CIdExpression idExpression;

  public final CBinaryExpression notSignaledExpression;

  public CondSignaled(CIdExpression pIdExpression, CBinaryExpression pNotSignaledExpression) {
    checkNotNull(pIdExpression);
    checkNotNull(pNotSignaledExpression);
    idExpression = pIdExpression;
    notSignaledExpression = pNotSignaledExpression;
  }
}
