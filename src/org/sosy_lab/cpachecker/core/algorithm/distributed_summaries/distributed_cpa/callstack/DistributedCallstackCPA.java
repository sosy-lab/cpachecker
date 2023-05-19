// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.AlwaysProceed;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class DistributedCallstackCPA implements DistributedConfigurableProgramAnalysis {

  static final String DELIMITER = ",  ";

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final CombineOperator combine;
  private final ProceedOperator proceed;

  private final CallstackCPA callstackCPA;

  public DistributedCallstackCPA(
      CallstackCPA pCallstackCPA, BlockNode pNode, AnalysisDirection pDirection) {
    callstackCPA = pCallstackCPA;
    proceed = new AlwaysProceed();
    combine = new CombineCallstackStateOperator(pDirection, pCallstackCPA, pNode);
    serialize = new SerializeCallstackStateOperator();
    deserialize = new DeserializeCallstackStateOperator(pCallstackCPA, pNode);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combine;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserialize;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceed;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return CallstackState.class;
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return callstackCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return callstackCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return callstackCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return callstackCPA.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return callstackCPA.getInitialState(node, partition);
  }
}
