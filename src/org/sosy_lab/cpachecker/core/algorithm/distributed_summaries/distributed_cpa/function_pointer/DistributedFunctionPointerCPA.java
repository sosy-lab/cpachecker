// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineWithMerge;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.AlwaysProceed;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerCPA;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DistributedFunctionPointerCPA implements DistributedConfigurableProgramAnalysis {

  private final SerializeOperator serialize;
  private final DeserializeOperator deserialize;
  private final CombineOperator combine;
  private final ProceedOperator proceed;

  private final FunctionPointerCPA functionPointerCPA;

  public DistributedFunctionPointerCPA(FunctionPointerCPA pParentCPA, BlockNode pNode) {
    functionPointerCPA = pParentCPA;
    serialize = new SerializeFunctionPointerStateOperator();
    deserialize = new DeserializeFunctionPointerStateOperator(pParentCPA, pNode);
    combine = new CombineWithMerge(getMergeOperator());
    proceed = new AlwaysProceed();
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
    return FunctionPointerState.class;
  }

  @Override
  public BooleanFormula getErrorCondition(FormulaManagerView pFormulaManagerView) {
    return pFormulaManagerView.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public void updateErrorCondition(BlockSummaryErrorConditionMessage pMessage)
      throws InterruptedException {}

  @Override
  public void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis)
      throws InterruptedException {}

  @Override
  public AbstractDomain getAbstractDomain() {
    return functionPointerCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return functionPointerCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return functionPointerCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return functionPointerCPA.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return functionPointerCPA.getInitialState(node, partition);
  }
}
