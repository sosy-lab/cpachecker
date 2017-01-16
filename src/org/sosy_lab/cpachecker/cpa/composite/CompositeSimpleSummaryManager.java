/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.simple.CPAWithSummarySupport;
import org.sosy_lab.cpachecker.cpa.summary.simple.SimpleSummaryManager;

/**
 * New summary framework for composite CPA.
 */
public class CompositeSimpleSummaryManager implements SimpleSummaryManager {
  private final List<SimpleSummaryManager> managers;

  public CompositeSimpleSummaryManager(ImmutableList<ConfigurableProgramAnalysis> pCpas) {
    managers = pCpas.stream().map(cpa -> ((CPAWithSummarySupport) cpa).getSimpleSummaryManager())
      .collect(Collectors.toList());
  }

  @Override
  public List<? extends AbstractState> applyFunctionSummary(
      AbstractState callSite,
      AbstractState exitState,
      CFANode callNode,
      Block calledBlock) {
    CompositeState cCallState = (CompositeState) callSite;
    CompositeState cExitState = (CompositeState) exitState;
    List<List<? extends AbstractState>> produced = IntStream.range(0, managers.size()).mapToObj(
        i -> managers.get(i).applyFunctionSummary(
            cCallState.get(i), cExitState.get(i), callNode, calledBlock
        )
    ).collect(Collectors.toList());

    return Lists.cartesianProduct(produced)
        .stream()
        .map(l -> new CompositeState(l))
        .collect(Collectors.toList());
  }

  @Override
  public List<? extends AbstractState> getEntryStates(
      AbstractState callSite, CFANode callNode, Block calledBlock) {
    CompositeState cCallState = (CompositeState) callSite;

    List<List<? extends AbstractState>> produced = IntStream.range(0, managers.size()).mapToObj(
        i -> managers.get(i).getEntryStates(
            cCallState.get(i), callNode, calledBlock
        )
    ).collect(Collectors.toList());
    return Lists.cartesianProduct(produced)
        .stream()
        .map(l -> new CompositeState(l))
        .collect(Collectors.toList());
  }

  @Override
  public boolean isSummaryApplicable(
      AbstractState callSite, AbstractState exitState, CFANode callNode, Block calledBlock) {
    CompositeState cCallState = (CompositeState) callSite;
    CompositeState cExitState = (CompositeState) exitState;
    return IntStream.range(0, managers.size()).allMatch(
        i -> managers.get(i).isSummaryApplicable(
            cCallState.get(i), cExitState.get(i), callNode, calledBlock
        )
    );
  }
}
