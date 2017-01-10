/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * This CPA tracks the value of the artificial program counter variable
 * introduced by single loop transformation.
 */
public class ProgramCounterCPA extends AbstractCPA implements ConfigurableProgramAnalysis {

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
      LoopStructure loopStructure = cfa.getLoopStructure().get();
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
