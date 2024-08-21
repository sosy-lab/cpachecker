// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public class NoopBlockAnalysis extends BlockAnalysis {

  private final BlockNode block;
  private final DistributedCompositeCPA distributedCompositeCPA;

  public NoopBlockAnalysis(
      LogManager pLogger,
      BlockNode pBlock,
      CFA pCFA,
      AnalysisDirection pDirection,
      Specification pSpecification,
      Configuration pConfiguration,
      ShutdownManager pShutdownManager,
      BlockSummaryAnalysisOptions pOptions)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    super(
        pLogger,
        pBlock,
        pCFA,
        pDirection,
        pSpecification,
        pConfiguration,
        pShutdownManager,
        pOptions);
    block = pBlock;
    distributedCompositeCPA = getDistributedCompositeCPA();
  }

  @Override
  public Collection<BlockSummaryMessage> analyze(Collection<BlockSummaryMessage> condition)
      throws CPAException, InterruptedException, SolverException {
    return ImmutableSet.of();
  }

  /**
   * Broadcast one initial message such that successors know that they are connected to the root
   *
   * @return Message containing the T-element of the underlying composite CPA
   * @throws InterruptedException thread interrupted
   * @throws CPAException forwarded exception (wraps internal errors)
   */
  @Override
  public Collection<BlockSummaryMessage> performInitialAnalysis()
      throws InterruptedException, CPAException {
    return ImmutableSet.of(
        BlockSummaryMessage.newBlockPostCondition(
            block.getId(),
            block.getLastNode().getNodeNumber(),
            distributedCompositeCPA
                .getSerializeOperator()
                .serialize(
                    distributedCompositeCPA.getInitialState(
                        block.getStartNode(), StateSpacePartition.getDefaultPartition())),
            true,
            true,
            ImmutableSet.of(block.getId())));
  }
}
