// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.BackwardTransferViolationConditionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.ViolationConditionOperator;
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

  private final CallstackCPA callstackCPA;
  private final CFA cfa;

  private final ViolationConditionOperator verificationConditionOperator;

  public DistributedCallstackCPA(
      CallstackCPA pCallstackCPA, CFA pCFA, Map<Integer, CFANode> pIdToNodeMap) {
    callstackCPA = pCallstackCPA;
    cfa = pCFA;
    serialize = new SerializeCallstackStateOperator();
    deserialize = new DeserializeCallstackStateOperator(pCallstackCPA, pIdToNodeMap::get);
    verificationConditionOperator =
        new BackwardTransferViolationConditionOperator(
            callstackCPA.getTransferRelation().copyBackwards(), pCallstackCPA);
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return getCPA().getInitialState(node, partition);
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
    return ProceedOperator.always();
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return CallstackState.class;
  }

  @Override
  public ConfigurableProgramAnalysis getCPA() {
    return callstackCPA;
  }

  @Override
  public boolean isTop(AbstractState pAbstractState) {
    return true;
  }

  @Override
  public ViolationConditionOperator getViolationConditionOperator() {
    return verificationConditionOperator;
  }
}
