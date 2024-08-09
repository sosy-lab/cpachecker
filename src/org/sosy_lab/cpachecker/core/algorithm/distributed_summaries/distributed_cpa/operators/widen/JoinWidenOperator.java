// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.widen;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public class JoinWidenOperator implements WidenOperator {

  private final ConfigurableProgramAnalysis cpa;
  private final boolean defaultToTop;
  private final BlockNode block;

  public JoinWidenOperator(
      ConfigurableProgramAnalysis pCPA, BlockNode pNode, boolean pDefaultToTop) {
    cpa = pCPA;
    defaultToTop = pDefaultToTop;
    block = pNode;
  }

  @Override
  public AbstractState combine(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    try {
      return cpa.getAbstractDomain().join(state1, state2);
    } catch (UnsupportedCodeException | UnsupportedOperationException exception) {
      if (defaultToTop) {
        return cpa.getInitialState(block.getFirst(), StateSpacePartition.getDefaultPartition());
      }
      throw exception;
    }
  }
}
