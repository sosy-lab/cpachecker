// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

/**
 * A function call is represented in three different edges throughout the cfa. This class is an
 *
 * <p>aggregate of these three edges.
 */
public class LoopConditionAggregate {
  private final CAssumeEdge condition;
  private final CAssumeEdge oppositeCondition;

  public LoopConditionAggregate(CAssumeEdge pCondition, CAssumeEdge pOppositeCondition) {
    condition = pCondition;
    oppositeCondition = pOppositeCondition;
  }

  public CAssumeEdge getCondition() {
    return condition;
  }

  public CAssumeEdge getOppositeCondition() {
    return oppositeCondition;
  }
}
