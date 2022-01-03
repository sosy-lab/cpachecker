// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.automatic_program_repair;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;

public class LoopConditionEdgeMutator extends EdgeMutator {
  LoopConditionAggregate loopConditionAggregate;

  public LoopConditionEdgeMutator(
      CFA cfa, Configuration config, LogManager logger, CAssumeEdge pOriginalEdge) {
    super(cfa, config, logger);
    final CAssumeEdge edgeToMutate =
        (CAssumeEdge)
            CorrespondingEdgeProvider.findCorrespondingEdge(pOriginalEdge, getClonedCFA());
    final CAssumeEdge reverseConditionEdgeToMutate =
        CorrespondingEdgeProvider.findCorrespondingAssumeEdge(edgeToMutate);
    loopConditionAggregate = new LoopConditionAggregate(edgeToMutate, reverseConditionEdgeToMutate);
  }

  /** Returns a new assume edge with a different expression. */
  public LoopConditionAggregate replaceExpressionInLoopConditionAggregate(
      CExpression newExpression) {
    CAssumeEdge originalAssumeEdge = loopConditionAggregate.getCondition();
    CAssumeEdge originalReverseAssumeEdge = loopConditionAggregate.getOppositeCondition();

    final CAssumeEdge newAssumeEdge =
        new CAssumeEdge(
            newExpression.toASTString(),
            originalAssumeEdge.getFileLocation(),
            originalAssumeEdge.getPredecessor(),
            originalAssumeEdge.getSuccessor(),
            newExpression,
            originalAssumeEdge.getTruthAssumption());
    final CAssumeEdge newReverseAssumeEdge =
        new CAssumeEdge(
            newExpression.toASTString(),
            originalReverseAssumeEdge.getFileLocation(),
            originalReverseAssumeEdge.getPredecessor(),
            originalReverseAssumeEdge.getSuccessor(),
            newExpression,
            originalReverseAssumeEdge.getTruthAssumption());

    return new LoopConditionAggregate(newAssumeEdge, newReverseAssumeEdge);
  }
}
