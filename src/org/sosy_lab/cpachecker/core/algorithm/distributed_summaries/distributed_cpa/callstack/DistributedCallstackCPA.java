// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.AlwaysProceed;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class DistributedCallstackCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  static final String DELIMITER = ",  ";

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final ProceedOperator proceed;

  private final CallstackCPA callstackCPA;
  private final CFA cfa;

  public DistributedCallstackCPA(CallstackCPA pCallstackCPA, CFA pCFA, BlockNode pNode) {
    callstackCPA = pCallstackCPA;
    cfa = pCFA;
    proceed = new AlwaysProceed();
    serialize = new SerializeCallstackStateOperator();
    deserialize = new DeserializeCallstackStateOperator(pCallstackCPA, pNode);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return getCPA().getInitialState(cfa.getAllFunctions().get(node.getFunctionName()), partition);
  }

  @Override
  public Precision getInitialPrecision(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return getCPA()
        .getInitialPrecision(cfa.getAllFunctions().get(node.getFunctionName()), partition);
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serialize;
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
  public ConfigurableProgramAnalysis getCPA() {
    return callstackCPA;
  }
}
