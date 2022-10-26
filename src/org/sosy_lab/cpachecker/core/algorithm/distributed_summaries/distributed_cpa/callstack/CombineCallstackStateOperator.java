// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineCallstackStateOperator implements CombineOperator {

  private final AnalysisDirection direction;
  private final CallstackCPA parentCPA;
  private final BlockNode block;

  public CombineCallstackStateOperator(
      AnalysisDirection pDirection, CallstackCPA pParentCPA, BlockNode pBlockNode) {
    direction = pDirection;
    parentCPA = pParentCPA;
    block = pBlockNode;
  }

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    if (direction == AnalysisDirection.FORWARD) {
      if (pState1.equals(pState2)) {
        return ImmutableList.of(pState1);
      }
      return ImmutableList.of(
          parentCPA.getInitialState(
              block.getStartNode(), StateSpacePartition.getDefaultPartition()));
    }
    throw new AssertionError("BackwardAnalysis should never combine two callstack states");
  }
}
