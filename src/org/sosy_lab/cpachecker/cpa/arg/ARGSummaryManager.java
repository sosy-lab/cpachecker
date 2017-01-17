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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.CPAWithSummarySupport;
import org.sosy_lab.cpachecker.cpa.summary.SummaryManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGSummaryManager implements SummaryManager {
  private final SummaryManager wrapped;

  public ARGSummaryManager(ConfigurableProgramAnalysis pWrapped) {
    CPAWithSummarySupport cpa = (CPAWithSummarySupport) pWrapped;
    wrapped = cpa.getSimpleSummaryManager();
  }

  @Override
  public List<? extends AbstractState> applyFunctionSummary(
      AbstractState callSite, AbstractState exitState, CFANode callNode, Block calledBlock)
      throws CPAException, InterruptedException {
    ARGState aCallState = (ARGState) callSite;
    ARGState aExitState = (ARGState) exitState;

    List<? extends AbstractState> out = wrapped.applyFunctionSummary(
        aCallState.getWrappedState(),
        aExitState.getWrappedState(), callNode, calledBlock);
    return out.stream().map(
        s -> {
          ARGState o = new ARGState(s, aCallState);
          o.addParent(aExitState);
          return o;
        }
    ).collect(Collectors.toList());
  }

  @Override
  public List<? extends AbstractState> getEntryStates(
      AbstractState callSite, CFANode callNode, Block calledBlock)
      throws CPAException, InterruptedException {
    ARGState aCallState = (ARGState) callSite;
    List<? extends AbstractState> wrappedEntryStates =
        wrapped.getEntryStates(aCallState.getWrappedState(), callNode, calledBlock);
    return wrappedEntryStates.stream()
        .map(e -> new ARGState(e, aCallState)).collect(Collectors.toList());
  }
}
