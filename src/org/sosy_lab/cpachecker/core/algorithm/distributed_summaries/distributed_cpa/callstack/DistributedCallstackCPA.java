// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
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
import org.sosy_lab.cpachecker.util.CFAUtils;

public class DistributedCallstackCPA implements ForwardingDistributedConfigurableProgramAnalysis {

  static final String DELIMITER = ",  ";

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;

  private final BlockNode blockNode;

  private final CallstackCPA callstackCPA;
  private final CFA cfa;
  private final CallstackTransferRelationBackwards backwardsTransfer;

  public DistributedCallstackCPA(
      CallstackCPA pCallstackCPA,
      BlockNode pBlockNode,
      CFA pCFA,
      Map<Integer, CFANode> pIdToNodeMap) {
    callstackCPA = pCallstackCPA;
    cfa = pCFA;
    blockNode = pBlockNode;
    serialize = new SerializeCallstackStateOperator();
    deserialize = new DeserializeCallstackStateOperator(pCallstackCPA, pIdToNodeMap::get);
    backwardsTransfer = callstackCPA.getTransferRelation().copyBackwards();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    ImmutableList<CFANode> cfaNodes =
        CFAUtils.enteringEdges(blockNode.getLast())
            .filter(
                e ->
                    e.getEdgeType() == CFAEdgeType.ReturnStatementEdge
                        && e.getPredecessor()
                            .getFunctionName()
                            .equals(blockNode.getFirst().getFunctionName()))
            .transform(e -> e.getSuccessor())
            .filter(n -> n.getEnteringSummaryEdge() != null)
            .transform(n -> n.getEnteringSummaryEdge().getPredecessor())
            .toList();
    if (cfaNodes.isEmpty()) {
      return getCPA().getInitialState(node, partition);
    }
    return getCPA().getInitialState(Iterables.getOnlyElement(cfaNodes), partition);
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
    AbstractState error =
        Objects.requireNonNull(
            AbstractStates.extractStateByType(pPreviousCondition, getAbstractStateClass()));
    for (CFAEdge cfaEdge : Lists.reverse(pARGPath.getFullPath())) {
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
