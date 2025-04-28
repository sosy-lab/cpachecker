// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector;

import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;

public class BitVectorWriteVariable implements BitVectorVariable {

  public final MPORThread thread;

  public final CIdExpression idExpression;

  public BitVectorWriteVariable(MPORThread pThread, CIdExpression pIdExpression) {
    thread = pThread;
    idExpression = pIdExpression;
  }

  @Override
  public MPORThread getThread() {
    return thread;
  }

  @Override
  public CIdExpression getIdExpression() {
    return idExpression;
  }
}
