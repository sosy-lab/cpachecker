// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.programcounter;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.singleloop.SingleLoopHead;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This CPA tracks the value of the artificial program counter variable
 * introduced by single loop transformation.
 */
public class ProgramCounterCPA extends AbstractCPA {

  private final CFA cfa;

  public ProgramCounterCPA(CFA pCFA) {
    super("sep", "sep", DelegateAbstractDomain.<ProgramCounterState>getInstance(), ProgramCounterTransferRelation.INSTANCE);
    this.cfa = pCFA;
  }

  /**
   * Gets a factory for creating ProgramCounterCPAs.
   *
   * @return a factory for creating ProgramCounterCPAs.
   */
  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(ProgramCounterCPA.class);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    // Try to get all possible program counter values
    SingleLoopHead singleLoopHead = null;
    if (pNode instanceof SingleLoopHead) {
      singleLoopHead = (SingleLoopHead) pNode;
    } else if (cfa.getLoopStructure().isPresent()) {
      LoopStructure loopStructure = cfa.getLoopStructure().orElseThrow();
      if (loopStructure.getCount() == 1) {
        Loop singleLoop = Iterables.getOnlyElement(loopStructure.getAllLoops());
        if (singleLoop.getLoopHeads().size() == 1) {
          CFANode loopHead = Iterables.getOnlyElement(singleLoop.getLoopHeads());
          if (loopHead instanceof SingleLoopHead) {
            singleLoopHead = (SingleLoopHead) loopHead;
          }
        }
      }
    }

    if (singleLoopHead != null) {
      FluentIterable<BigInteger> potentialValues = FluentIterable.from(
          singleLoopHead.getProgramCounterValues()).transform(BigInteger::valueOf);

      if (!potentialValues.isEmpty()) {
        return ProgramCounterState.getStateForValues(potentialValues);
      }
    }

    // If the possible program counter values cannot be determined, return TOP
    return ProgramCounterState.getTopState();
  }
}
