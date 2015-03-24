/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import java.util.Collection;
import java.util.Deque;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.refiner.StrongestPostOperator;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;

/**
 * Strongest post-operator using {@link ValueAnalysisTransferRelation}.
 */
public class ValueAnalysisStrongestPostOperator implements StrongestPostOperator<ValueAnalysisState> {

  private final ValueAnalysisTransferRelation transfer;

  public ValueAnalysisStrongestPostOperator(
      final LogManager pLogger,
      final CFA pCfa
  ) throws InvalidConfigurationException {

    transfer = new ValueAnalysisTransferRelation(Configuration.builder().build(), pLogger, pCfa);
  }

  @Override
  public Optional<ValueAnalysisState> getStrongestPost(
      final ValueAnalysisState pOrigin,
      final Precision pPrecision,
      final CFAEdge pOperation
  ) throws CPAException {

    final Collection<ValueAnalysisState> successors =
        transfer.getAbstractSuccessorsForEdge(pOrigin, pPrecision, pOperation);

    if (successors.isEmpty()) {
      return Optional.absent();

    } else {
      return Optional.of(Iterables.getOnlyElement(successors));
    }
  }

  @Override
  public ValueAnalysisState handleFunctionCall(ValueAnalysisState state, CFAEdge edge,
      Deque<ValueAnalysisState> callstack) {
    callstack.addLast(state);
    return state;
  }

  @Override
  public ValueAnalysisState handleFunctionReturn(ValueAnalysisState next, CFAEdge edge,
      Deque<ValueAnalysisState> callstack) {

    final ValueAnalysisState callState = callstack.removeLast();
    return next.rebuildStateAfterFunctionCall(callState, (FunctionExitNode)edge.getPredecessor());
  }
}
