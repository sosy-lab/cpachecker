/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.Summary;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.SummaryManager;
import org.sosy_lab.cpachecker.cpa.summary.interfaces.UseSummaryCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Summary manager for {@link ARGCPA}.
 * Operates over wrapped summaries directly.
 *
 * <p>For now, does not add any value, and simply propagates all calls.
 */
public class ARGSummaryManager implements SummaryManager {

  private final SummaryManager wrapped;

  ARGSummaryManager(
      ConfigurableProgramAnalysis pCpa) {
    Preconditions.checkArgument(pCpa instanceof UseSummaryCPA,
        "For summary generation all nested CPAs have to "
            + "implement UseSummaryCPA interface.");
    wrapped = ((UseSummaryCPA) pCpa).getSummaryManager();
  }

  @Override
  public AbstractState getAbstractSuccessorsForSummary(
      AbstractState pState, Precision pPrecision, List<Summary> pSummaries, Block pBlock)
      throws CPAException, InterruptedException {
    ARGState aState = (ARGState) pState;

    AbstractState successor =
        wrapped.getAbstractSuccessorsForSummary(
            aState.getWrappedState(), pPrecision, pSummaries, pBlock
        );
    // todo: should the parent element be null? In BAMReducer it is.
    return new ARGState(successor, null);
  }

  @Override
  public AbstractState getWeakenedCallState(
      AbstractState pState, Precision pPrecision, Block pBlock) {
    return new ARGState(
        wrapped.getWeakenedCallState(pState, pPrecision, pBlock),
        null
    );
  }

  @Override
  public AbstractState projectToCallsite(Summary pSummary) {
    return wrapped.projectToCallsite(pSummary);
  }

  @Override
  public AbstractState projectToPostcondition(Summary pSummary) {
    return wrapped.projectToPostcondition(pSummary);
  }

  @Override
  public List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pEntryPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pReturnPrecision,
      CFANode pEntryNode,
      Block pBlock) {

    ARGState aEntryState = (ARGState) pCallState;
    FluentIterable<ARGState> aExitStates = FluentIterable.from(pReturnStates).filter(ARGState.class);
    return wrapped.generateSummaries(
        aEntryState.getWrappedState(),
        pEntryPrecision,
        aExitStates.transform(a -> a.getWrappedState()).toList(),
        pReturnPrecision,
        pEntryNode,
        pBlock
    );
  }

  @Override
  public Summary merge(
      Summary pSummary1, Summary pSummary2) throws CPAException, InterruptedException {
    return wrapped.merge(pSummary1, pSummary2);
  }
}
