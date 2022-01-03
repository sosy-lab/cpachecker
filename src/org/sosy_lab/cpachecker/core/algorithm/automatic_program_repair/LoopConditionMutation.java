// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

/**
 * This class represents a simple mutation of an edge that does not contain a function call (see
 * FunctionCallMutation).
 */
public class LoopConditionMutation extends Mutation {
  private final LoopConditionAggregate loopConditionAggregate;

  public LoopConditionMutation(
      CAssumeEdge pSuspiciousEdge, LoopConditionAggregate pLoopConditionAggregate, CFA pCFA) {
    super(pSuspiciousEdge, pCFA);
    loopConditionAggregate = pLoopConditionAggregate;

    exchangeEdge(loopConditionAggregate.getCondition());
    exchangeEdge(loopConditionAggregate.getOppositeCondition());
  }

  @Override
  public CFAEdge getNewEdge() {
    return loopConditionAggregate.getCondition();
  }
}
