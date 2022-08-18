// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.function.Supplier;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombineOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DistributedPredicateCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.block.BlockCPA;
import org.sosy_lab.cpachecker.cpa.block.BlockCPABackward;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class DistributedBlockCPA implements DistributedConfigurableProgramAnalysis {

  private final DeserializeOperator deserializeOperator;
  private final SerializeOperator serializeOperator;
  private final ProceedOperator proceedOperator;
  private final CombineOperator combineOperator;

  private final ConfigurableProgramAnalysis blockCPA;
  private final BlockSummaryMessage topMessage;

  private FormulaManagerView fmgr;

  public DistributedBlockCPA(ConfigurableProgramAnalysis pBlockCPA, BlockNode pNode, AnalysisDirection pDirection, Supplier<Collection<DistributedConfigurableProgramAnalysis>> pFutureErrorCondition, BlockSummaryAnalysisOptions pOptions)
      throws InvalidConfigurationException {
    checkArgument(pBlockCPA instanceof BlockCPA || pBlockCPA instanceof BlockCPABackward, /* TODO make lazy */ pBlockCPA.getClass() + " is no block CPA.");
    blockCPA = pBlockCPA;
    // FormulaManager is only used for calculating conjunctions -> dummy is fine (this formula manager needs to be independent)

    serializeOperator = new SerializeBlockStateOperator();
    deserializeOperator = new DeserializeBlockStateOperator(pNode,
        pDirection,
        () -> pFutureErrorCondition.get()
            .stream()
            .map(analysis -> analysis.getErrorCondition(obtainFormulaMangerWithCorrectContext(pFutureErrorCondition)))
            .collect(obtainFormulaMangerWithCorrectContext(pFutureErrorCondition).getBooleanFormulaManager().toConjunction()));
    combineOperator = new CombineBlockStateOperator();
    proceedOperator = new ProceedBlockStateOperator(pNode, pDirection);
    topMessage =
        BlockSummaryMessage.newBlockPostCondition(
            pNode.getId(),
            pDirection == AnalysisDirection.FORWARD
                ? pNode.getStartNode().getNodeNumber()
                : pNode.getLastNode().getNodeNumber(),
            BlockSummaryMessagePayload.empty(),
            false,
            true,
            ImmutableSet.of());
  }

  private FormulaManagerView obtainFormulaMangerWithCorrectContext(Supplier<Collection<DistributedConfigurableProgramAnalysis>> pFutureErrorCondition) {
    if (fmgr == null) {
      for (DistributedConfigurableProgramAnalysis analysis : pFutureErrorCondition.get()) {
        if (analysis instanceof DistributedPredicateCPA) {
          fmgr = ((DistributedPredicateCPA) analysis).getSolver().getFormulaManager();
        }
      }
    }
    return fmgr;
  }

  @Override
  public SerializeOperator getSerializeOperator() {
    return serializeOperator;
  }

  @Override
  public CombineOperator getCombineOperator() {
    return combineOperator;
  }

  @Override
  public DeserializeOperator getDeserializeOperator() {
    return deserializeOperator;
  }

  @Override
  public ProceedOperator getProceedOperator() {
    return proceedOperator;
  }

  @Override
  public Class<? extends AbstractState> getAbstractStateClass() {
    return BlockState.class;
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
      throws InterruptedException {

  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return blockCPA.getAbstractDomain();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return blockCPA.getTransferRelation();
  }

  @Override
  public MergeOperator getMergeOperator() {
    return blockCPA.getMergeOperator();
  }

  @Override
  public StopOperator getStopOperator() {
    return blockCPA.getStopOperator();
  }

  @Override
  public AbstractState getInitialState(CFANode node, StateSpacePartition partition)
      throws InterruptedException {
    return deserializeOperator.deserialize(topMessage);
  }
}
