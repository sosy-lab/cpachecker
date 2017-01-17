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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.summary.CPAWithSummarySupport;
import org.sosy_lab.cpachecker.cpa.summary.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * New summary framework for composite CPA.
 */
public class CompositeSummaryManager implements SummaryManager {
  private final List<SummaryManager> managers;

  public CompositeSummaryManager(ImmutableList<ConfigurableProgramAnalysis> pCpas) {
    managers = pCpas.stream().map(cpa -> ((CPAWithSummarySupport) cpa).getSimpleSummaryManager())
      .collect(Collectors.toList());
  }

  @Override
  public List<? extends AbstractState> applyFunctionSummary(
      AbstractState callSite,
      AbstractState exitState,
      CFANode callNode,
      Block calledBlock) throws CPAException, InterruptedException {
    CompositeState cCallState = (CompositeState) callSite;
    CompositeState cExitState = (CompositeState) exitState;

    List<List<? extends AbstractState>> produced = new ArrayList<>();

    for (int i=0; i<managers.size(); i++) {
      produced.add(managers.get(i).applyFunctionSummary(
            cCallState.get(i), cExitState.get(i), callNode, calledBlock
        ));
    }

    return Lists.cartesianProduct(produced)
        .stream()
        .map(l -> new CompositeState(l))
        .collect(Collectors.toList());
  }

  @Override
  public List<? extends AbstractState> getEntryStates(
      AbstractState callSite, CFANode callNode, Block calledBlock)
      throws CPAException, InterruptedException {
    CompositeState cCallState = (CompositeState) callSite;

    List<List<? extends AbstractState>> produced = new ArrayList<>();

    for (int i=0; i<managers.size(); i++) {
      produced.add(managers.get(i).getEntryStates(
          cCallState.get(i), callNode, calledBlock
      ));
    }

    return Lists.cartesianProduct(produced)
        .stream()
        .map(l -> new CompositeState(l))
        .collect(Collectors.toList());
  }
}
