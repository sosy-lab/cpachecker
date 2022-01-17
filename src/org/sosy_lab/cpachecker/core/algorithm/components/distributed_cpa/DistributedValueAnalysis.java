// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.distributed_cpa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.components.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Message;
import org.sosy_lab.cpachecker.core.algorithm.components.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.SolverException;

public class DistributedValueAnalysis extends AbstractDistributedCPA<ValueAnalysisCPA, ValueAnalysisState> {

  public DistributedValueAnalysis(
      String pId,
      BlockNode pNode,
      SSAMap pTypeMap,
      Precision pPrecision,
      AnalysisDirection pDirection)
      throws CPAException {
    super(pId, pNode, pTypeMap, pPrecision, pDirection);
  }

  @Override
  public Payload encode(Collection<ValueAnalysisState> statesAtBlockEntry) {
    if (statesAtBlockEntry.isEmpty()) {
      return Payload.empty();
    }
    List<ValueAnalysisState> states = new ArrayList<>(statesAtBlockEntry);
    AbstractState first = states.remove(0);
    for (ValueAnalysisState valueAnalysisState : statesAtBlockEntry) {
      try {
        first = parentCPA.getMergeOperator().merge(first, valueAnalysisState, precision);
      } catch (CPAException | InterruptedException pE) {
        return Payload.builder().addEntry("exception", pE.toString()).build();
      }
    }
    for (Entry<MemoryLocation, ValueAndType> constant : ((ValueAnalysisState) first).getConstants()) {

    }
    return null;
  }

  @Override
  public ValueAnalysisState decode(
      Collection<Payload> messages, ValueAnalysisState previousAbstractState) {
    return null;
  }

  @Override
  public MessageProcessing stopForward(Message newMessage) {
    // continue if in block
    return null;
  }

  @Override
  public MessageProcessing stopBackward(Message newMessage)
      throws SolverException, InterruptedException {
    // full Path variable values dont match
    return null;
  }

  @Override
  public Class<ValueAnalysisCPA> getParentCPAClass() {
    return ValueAnalysisCPA.class;
  }

  @Override
  public Class<ValueAnalysisState> getAbstractStateClass() {
    return ValueAnalysisState.class;
  }
}
