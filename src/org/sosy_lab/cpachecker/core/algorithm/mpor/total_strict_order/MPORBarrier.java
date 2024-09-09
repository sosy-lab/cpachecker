// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;

/** TODO */
public class MPORBarrier {

  public final CExpression barrierT;

  public final int count;

  /**
   * TODO
   *
   * @param pBarrierT TODO
   * @param pCount TODO
   */
  public MPORBarrier(CExpression pBarrierT, int pCount) {
    barrierT = pBarrierT;
    count = pCount;
  }
}
