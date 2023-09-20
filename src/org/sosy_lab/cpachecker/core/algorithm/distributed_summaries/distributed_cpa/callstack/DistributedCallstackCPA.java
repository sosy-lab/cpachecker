// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.GhostEdge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.ForwardingDistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.VerificationConditionException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackTransferRelationBackwards;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class DistributedCallstackCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  static final String DELIMITER = ",  ";

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;

  private final CallstackCPA callstackCPA;
  private final CFA cfa;
  private final CallstackTransferRelationBackwards backwardsTransfer;

  public DistributedCallstackCPA(
      CallstackCPA pCallstackCPA, CFA pCFA, Map<Integer, CFANode> pIdToNodeMap) {
    callstackCPA = pCallstackCPA;
    cfa = pCFA;
    serialize = new SerializeCallstackStateOperator();
    deserialize = new DeserializeCallstackStateOperator(pCallstackCPA, pIdToNodeMap::get);
    backwardsTransfer = callstackCPA.getTransferRelation().copyBackwards();
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
  public AbstractState computeVerificationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws InterruptedException, CPATransferException, VerificationConditionException {
    AbstractState error;
    if (pPreviousCondition == null) {
      error =
          getInitialState(
              Objects.requireNonNull(AbstractStates.extractLocation(pARGPath.getLastState())),
              StateSpacePartition.getDefaultPartition());
    } else {
      error =
          Objects.requireNonNull(
              AbstractStates.extractStateByType(pPreviousCondition, CallstackState.class));
    }
    for (CFAEdge cfaEdge : Lists.reverse(pARGPath.getFullPath())) {
      if (cfaEdge instanceof GhostEdge) {
        continue;
      }
      Collection<? extends AbstractState> abstractSuccessorsForEdge =
          backwardsTransfer.getAbstractSuccessorsForEdge(
              error,
              getInitialPrecision(cfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
              cfaEdge);
      if (abstractSuccessorsForEdge.isEmpty()) {
        throw new VerificationConditionException("Callstack not feasible");
      }
      error = Iterables.getOnlyElement(abstractSuccessorsForEdge);
    }
    return error;
  }
}
