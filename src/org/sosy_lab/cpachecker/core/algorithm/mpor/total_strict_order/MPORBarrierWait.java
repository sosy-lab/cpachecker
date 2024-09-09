// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.total_strict_order;

import org.sosy_lab.cpachecker.cfa.model.CFANode;

/** TODO */
public class MPORBarrierWait {

  public final CFANode preBarrierWaitNode;

  /**
   * TODO
   *
   * @param pPreBarrierWaitNode TODO
   */
  public MPORBarrierWait(CFANode pPreBarrierWaitNode) {
    preBarrierWaitNode = pPreBarrierWaitNode;
  }
}
